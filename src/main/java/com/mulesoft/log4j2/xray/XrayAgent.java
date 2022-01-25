package com.mulesoft.log4j2.xray;

import java.util.Arrays;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.xray.AWSXRayAsync;
import com.amazonaws.services.xray.AWSXRayAsyncClientBuilder;
import com.amazonaws.services.xray.model.PutTraceSegmentsRequest;
import com.amazonaws.services.xray.model.PutTraceSegmentsResult;

public class XrayAgent {

	private final AWSXRayAsync xrayClient;
	private final XrayJsonLoggerConverter jsonLoggerConverter = new XrayJsonLoggerConverter();

	public XrayAgent(String awsRegion, String awsAccessKey, String awsAccessSecret) {
		AWSStaticCredentialsProvider creds = new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(awsAccessKey, awsAccessSecret));
		this.xrayClient = AWSXRayAsyncClientBuilder.standard().withRegion(Regions.fromName(awsRegion))
				.withCredentials(creds).build();

	}

	public void batchTransaction(JsonLoggerTransaction transaction) {
		PutTraceSegmentsRequest request = new PutTraceSegmentsRequest();
		String xrayDocument = jsonLoggerConverter.convert(transaction);
		
		request.setTraceSegmentDocuments(Arrays.asList(xrayDocument));

		PutTraceSegmentsResult result = xrayClient.putTraceSegments(request);
		System.out.println("Status: " + result.getSdkHttpMetadata().getHttpStatusCode());
		System.out.println("RequestId: " + result.getSdkResponseMetadata().getRequestId());
	}
	
	public void publishPendingMessages() {
		
	}
}
