package com.intelematics.mule.log4j2.xray;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.LogEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class XrayLogReceiver implements Runnable {
  private static final int EXPIRE_AFTER_SECONDS = 120;
  private static final long THREAD_DELAY = 500L;
  private static final boolean DEBUG_MODE = XrayAppender.getDebugMode();

  private HashMap<String, JsonLoggerTransaction> transactions = new HashMap<>();
  private TreeMap<Instant, String> transactionExpiry = new TreeMap<>();
  private static XrayLogReceiver instance;
  private static Thread agentThread;
  private final XrayAgent xrayAgent;
  
  private LinkedBlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>();
  private boolean running = true;

  private XrayLogReceiver(String awsRegion) {
    xrayAgent = XrayAgent.getInstance(awsRegion);
  }

  public static XrayLogReceiver getInstance(String awsRegion) {
    if (instance == null) {
      instance = new XrayLogReceiver(awsRegion);
      agentThread = new Thread(instance);
      agentThread.setDaemon(true);
      agentThread.setName("Xray Receiver Agent");
      agentThread.start();
    }
    return instance;
  }
  
  
  /** Take incoming events and add them to the list */
  public void processEvent(LogEvent event) {
    if (this.running) {
      queue.add(event);
    }
  }

  /** Process the list and send the sanitised items to the Xray sender process*/
  @Override
  public void run() {
    while (this.running || queue.size() > 0) {
      try {
        LogEvent item = queue.poll();

        if (item == null) {
          Thread.sleep(THREAD_DELAY);
        } else {
          sendXrayEvent(item);
          System.out.println("## Sent Message");
        }
      } catch (Exception e) {
        log.error("## Xray Uncaught error while processing message, Ignoring item", e);
      }
    }
  }

  public void stop() {
    this.running = false;
    log.info("## Stopping Xray Receiver");
    try {
      agentThread.join();
    } catch (InterruptedException e) {
      // Don't actually care or need this
    }
    xrayAgent.stop();
  }

  
  public void sendXrayEvent(LogEvent event) throws JsonMappingException, JsonProcessingException {

    if (DEBUG_MODE)
      log.info("## Xray event found ");
    JsonLoggerEntry entry = new JsonLoggerEntry(event.getMessage().getFormattedMessage());
    JsonLoggerTransaction transaction = null;

    switch (entry.getTrace()) {
    case START:
      transaction = getTransaction(entry);
      transaction.setStart(entry);
      transactions.put(entry.getCorrelationId(), transaction);
      break;
    case BEFORE_REQUEST:
      transaction = getTransaction(entry);
      transaction.addRequestTransaction().setStart(entry);
      break;
    case AFTER_REQUEST:
      transaction = getTransaction(entry);
      transaction.setTransactionEndRequest(entry);
      break;
    case EXCEPTION:
      transaction = getTransaction(entry);
      transaction.addException(entry);
      break;
    case END:
      transaction = getTransaction(entry);
      transaction.setEnd(entry);
      break;
    default:
    }

    if (transaction != null) {
      this.xrayAgent.processTransaction(transaction);
    }

    if (DEBUG_MODE)
      log.info("## Xray logged " + entry.getTrace());

  }

  private JsonLoggerTransaction getTransaction(JsonLoggerEntry entry) {
    Instant timeNow = Instant.now();

    JsonLoggerTransaction transaction;
    transaction = transactions.get(entry.getCorrelationId());
    if (transaction == null) {
      transaction = new JsonLoggerTransaction();
      transactions.put(entry.getCorrelationId(), transaction);
      transactionExpiry.put(timeNow.plusSeconds(EXPIRE_AFTER_SECONDS), entry.getCorrelationId());
    }

    if (DEBUG_MODE)
      log.info("## Xray keys to expire: " + transactionExpiry.size() + ", open transactions: " + transactions.size());

    // Remove any expired transactions - this will keep our maps tidy.
    SortedMap<Instant, String> exipredKeys = transactionExpiry.headMap(timeNow);
    if (!exipredKeys.isEmpty()) {
      if (DEBUG_MODE)
        log.info("## Xray Purging keys: " + exipredKeys.values().stream().collect(Collectors.joining(",")));
      exipredKeys.values().forEach(correlationId -> transactions.remove(correlationId));

      Set<Instant> expiredInstants = new HashSet<>();
      expiredInstants.addAll(exipredKeys.keySet()); // Avoid Concurrent modification
      expiredInstants.forEach(key -> transactionExpiry.remove(key));
    }

    return transaction;
  }
}
