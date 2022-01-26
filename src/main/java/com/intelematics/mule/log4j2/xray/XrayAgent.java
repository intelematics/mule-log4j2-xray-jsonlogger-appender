package com.intelematics.mule.log4j2.xray;

import java.util.Arrays;
import java.util.List;

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

	private static final long THREAD_DELAY = 500L;

	private static Logger logger = LogManager.getLogger(XrayAgent.class);

	private final AWSXRayAsync xrayClient;
	private final XrayJsonLoggerConverter jsonLoggerConverter = new XrayJsonLoggerConverter();
	private static XrayAgent xrayThread = null;
	private boolean stop = false;

	public static synchronized XrayAgent getInstance(String awsRegion) {
		if (xrayThread == null) {
			xrayThread = new XrayAgent(awsRegion);
			new Thread(xrayThread).run();
		}
		return xrayThread;
	}

	private XrayAgent(String awsRegion) {
		this(awsRegion, new DefaultAWSCredentialsProviderChain().getCredentials().getAWSAccessKeyId(),
				new DefaultAWSCredentialsProviderChain().getCredentials().getAWSSecretKey());

	}

	private XrayAgent(String awsRegion, String awsAccessKey, String awsAccessSecret) {
		awsAccessKey = "AKIASDBJD5HVS7FVQSOY";
		awsAccessSecret = "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";

		AWSStaticCredentialsProvider creds = new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(awsAccessKey, awsAccessSecret));
		this.xrayClient = AWSXRayAsyncClientBuilder.standard().withRegion(Regions.fromName(awsRegion))
				.withCredentials(creds).build();

	}

	public void stop() {
		this.stop = true;
		logger.info("## Stopping Xray");
	}

	@Override
	public void run() {
		try {
			while (!this.stop) {
				boolean hasQueueCleared = sendXrayBatch();
				
				if (hasQueueCleared) {
					Thread.sleep(THREAD_DELAY);
				}
			}
		} catch (InterruptedException e) {
			logger.error("## Xray interrupted", e);
			this.stop();
		}
		
		if (this.stop) {
			clearXrayQueues();
		}
	}

	private void clearXrayQueues() {
		logger.info("## Xray clearing all queues");
	}

	private boolean sendXrayBatch() {
		logger.info("## Xray Sent batch to xray");
		return false;
	}

	public void processTransaction(JsonLoggerTransaction transaction) {
		String xrayDocument = jsonLoggerConverter.convert(transaction);

		publishPendingDocuments(Arrays.asList(xrayDocument));
	}

	public void publishPendingMessages() {

	}

	public void publishPendingDocuments(List<String> documents) {
		PutTraceSegmentsRequest request = new PutTraceSegmentsRequest();

		request.setTraceSegmentDocuments(documents);

		PutTraceSegmentsResult result = xrayClient.putTraceSegments(request);
		logger.info("## Xray Status: " + result.getSdkHttpMetadata().getHttpStatusCode() + ", RequestId: "
				+ result.getSdkResponseMetadata().getRequestId());
	}

}
