package com.intelematics.mule.log4j2.xray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.xray.AWSXRayAsync;
import com.amazonaws.services.xray.AWSXRayAsyncClientBuilder;
import com.amazonaws.services.xray.model.PutTraceSegmentsRequest;
import com.amazonaws.services.xray.model.PutTraceSegmentsResult;

public class XrayAgent implements Runnable {

  private static final int FAIL_ON_FAILED_BATCH_SENDS = 2;
  private static final long THREAD_DELAY = 500L;
  private static final int MAX_ITEMS_IN_BATCH = 50;

  private static Logger logger = LogManager.getLogger(XrayAgent.class);
  private static final Boolean DEBUG_MODE = XrayAppender.DEBUG_MODE;

  private static XrayAgent instance;
  private static Thread agentThread;

  private final AWSXRayAsync xrayClient;
  private final XrayJsonLoggerConverter jsonLoggerConverter = new XrayJsonLoggerConverter();

  private boolean stop = false;
  private LinkedBlockingQueue<JsonLoggerTransaction> processingQueue = new LinkedBlockingQueue<JsonLoggerTransaction>();
  private LinkedBlockingQueue<JsonLoggerTransaction> inProgressQueue = new LinkedBlockingQueue<JsonLoggerTransaction>();
  private int lastBatchStatus;
  private String lastBatchRequestId;

  public static synchronized XrayAgent getInstance(String awsRegion) {
    if (instance == null) {
      instance = new XrayAgent(awsRegion);
      agentThread = new Thread(instance);
      agentThread.setDaemon(true);
      agentThread.setName("Xray Agent");
      agentThread.start();
    }
    return instance;
  }

  private XrayAgent(String awsRegion) {
    this(awsRegion, new DefaultAWSCredentialsProviderChain().getCredentials().getAWSAccessKeyId(), new DefaultAWSCredentialsProviderChain().getCredentials().getAWSSecretKey());

  }

  private XrayAgent(String awsRegion, String awsAccessKey, String awsAccessSecret) {

    if (StringUtils.isBlank(awsRegion) || StringUtils.isBlank(awsAccessKey) || StringUtils.isBlank(awsAccessSecret)) {
      throw new IllegalArgumentException("Aws Region, AccessKey, and secret all need to be set.");
    }

    AWSStaticCredentialsProvider creds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsAccessSecret));
    this.xrayClient = AWSXRayAsyncClientBuilder.standard().withRegion(Regions.fromName(awsRegion)).withCredentials(creds).build();

  }

  public void stop() {
    this.stop = true;
    logger.info("## Stopping Xray");
    try {
      agentThread.join();
    } catch (InterruptedException e) {
      // Don't actually care or need this
    }
  }

  @Override
  public void run() {
    while (!this.stop) {
      try {
        boolean hasMoreItems = sendXrayBatch(false);

        if (!hasMoreItems) {
          try {
            Thread.sleep(THREAD_DELAY);
          } catch (InterruptedException e) {
            logger.error("## Xray interrupted", e);
            this.stop();
          }
        }
      } catch (Exception e) {
        logger.error("## Xray Uncaught error detected, clearing queues as to allow processsing to continue", e);

        this.inProgressQueue.clear();
        this.processingQueue.clear();
      }
    }

    if (this.stop) {
      clearXrayQueues();
    }
  }

  /**
   * Signal that this transaction should be sent. The xray agent will then handle
   * sending it, as part of a batch. Be aware that this is the only place where
   * access into the transaction queue is possible, and this must be thread safe
   * via the processing queue.
   * 
   * @param transaction
   */
  public void processTransaction(JsonLoggerTransaction transaction) {
    if (this.stop) {
      logger.error("## Xray Currently stopping. This activity was not recorded");
      return;
    }

    if (!processingQueue.contains(transaction) && !inProgressQueue.contains(transaction)) {
      processingQueue.add(transaction);
    }
  }

  private void clearXrayQueues() {
    logger.info("## Xray clearing all queues");
    int previousFailedAttempts = 0;

    while (sendXrayBatch(true)) {
      if (isLastBatchSuccessful()) {
        previousFailedAttempts = 0;
      } else if (previousFailedAttempts >= FAIL_ON_FAILED_BATCH_SENDS) {
        logger.error("## Xray Failed to send " + (1 + FAIL_ON_FAILED_BATCH_SENDS) + " batches. Abandoning.");
        return;
      } else {
        previousFailedAttempts++;
      }
    }
  }

  /**
   * 
   * @param forceSending
   * @return has more items?
   */
  private boolean sendXrayBatch(boolean forceSending) {
    LinkedBlockingQueue<JsonLoggerTransaction> notReadyQueue = new LinkedBlockingQueue<>();
    List<JsonLoggerTransaction> batchItems = new ArrayList<>();
    JsonLoggerTransaction nextItem = null;
    int processedItems = 0;

    if (processingQueue.peek() == null) {
      if (DEBUG_MODE)
        logger.debug("## Xray No items available to send");
      return false;
    }

    while (processedItems < MAX_ITEMS_IN_BATCH && (nextItem = processingQueue.poll()) != null) {
      inProgressQueue.add(nextItem);
      if (processingQueue.contains(nextItem)) {
        // Duplicate - likely from a completed item - we can ignore and process later to
        // eliminate duplicates
        break;
      }

      if (!forceSending && !nextItem.isReadyToSend()) {
        notReadyQueue.add(nextItem);
        break;
      }

      batchItems.add(nextItem);
      processedItems++;
    }

    logger.debug("## Xray Picked a batch of " + processedItems + " item(s).");
    logger.debug("## Xray correlation Ids: " + batchItems.stream().map(item -> item.getCorrelationId()).collect(Collectors.joining(",")));

    List<String> documents = generateXrayBatch(batchItems);

    boolean hasNoMoreReadyItems = processingQueue.peek() != null;
    processingQueue.addAll(notReadyQueue);
    inProgressQueue.clear();

    try {
      if (documents.size() == 0) {
      } else if (!publishXrayBatch(documents)) {
        // If we have an exception in sending, then
        logger.info("## Xray Failed to send batch of items due to a bad status code. Pushing back onto the queue.");
        processingQueue.addAll(batchItems);
      }
    } catch (Exception e) {
      // If we have an exception in sending, then
      logger.error("## Xray Failed to send batch of items. Pushing back onto the queue", e);
      processingQueue.addAll(batchItems);
    }

    return hasNoMoreReadyItems;
  }

  private List<String> generateXrayBatch(List<JsonLoggerTransaction> transactions) {
    List<String> documents = new ArrayList<>();

    for (JsonLoggerTransaction transaction : transactions) {
      try {
        String document = jsonLoggerConverter.convert(transaction);
        documents.add(document);
        if (DEBUG_MODE)
          logger.debug("## Xray document: " + document);
      } catch (Exception e) {
        // If we can't parse the transaction, it means that we can't progress with it.
        // At this point our best option is to just drop it, so that we don't foul up
        // the process,
        // or get stuck in a continual loop.
        logger.error("## Xray Couldn't parse transaction properly. Ignoring this transaction (" + transaction.getCorrelationId() + ")", e);
      }
    }

    return documents;
  }

  private boolean publishXrayBatch(List<String> documents) {
    PutTraceSegmentsRequest request = new PutTraceSegmentsRequest();

    request.setTraceSegmentDocuments(documents);

    PutTraceSegmentsResult result = xrayClient.putTraceSegments(request);

    lastBatchStatus = result.getSdkHttpMetadata().getHttpStatusCode();
    lastBatchRequestId = result.getSdkResponseMetadata().getRequestId();

    if (DEBUG_MODE)
      logger.debug("## Xray Status: " + lastBatchStatus + ", RequestId: " + lastBatchRequestId);

    return isLastBatchSuccessful();
  }

  private boolean isLastBatchSuccessful() {
    return lastBatchStatus == 200;
  }
}