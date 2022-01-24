package com.mulesoft.log4j2.xray;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.xray.AWSXRayAsync;
import com.amazonaws.services.xray.AWSXRayAsyncClientBuilder;
import com.amazonaws.services.xray.AWSXRayClient;
import com.amazonaws.services.xray.AWSXRayClientBuilder;
import com.amazonaws.services.xray.model.PutTraceSegmentsRequest;
import com.amazonaws.services.xray.model.PutTraceSegmentsResult;
import com.amazonaws.services.xray.model.Segment;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.TraceID;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
 
@Plugin(name = "Xray", category = "Core", elementType = "appender", printObject = true)
public class XrayAppender extends AbstractAppender {


	 private static Logger logger2 = LogManager.getLogger(XrayAppender.class);
	 
	 private final Boolean DEBUG_MODE = System.getProperty("log4j.debug") != null;
	 
	    /**
	     * Used to make sure that on close() our daemon thread isn't also trying to sendMessage()s
	     */
	    private Object sendMessagesLock = new Object();
	 
	    /**
	     * The queue used to buffer log entries
	     */
	    private LinkedBlockingQueue<LogEvent> loggingEventsQueue;
	 
	    //private AWSXRayRecorder xray;
	    private AWSXRayAsync xrayClient;
	 
	    /**
	     * The queue / buffer size
	     */
	    private int queueLength = 1024;
	    
	    private String awsAccessKey = "AKIASDBJD5HVS7FVQSOY";
	    private String awsAccessSecret = "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";
	    private String awsRegion = "ap-southeast-2";


	 
	    /**
	     * The maximum number of log entries to send in one go to the AWS Cloudwatch Log service
	     */
	    private int messagesBatchSize = 128;
	 
	    private AtomicBoolean appenderInitialised = new AtomicBoolean(false);
	  
	 
	    private XrayAppender(final String name,
	                           final Layout layout,
	                           final Filter filter,
	                           final boolean ignoreExceptions,String logGroupName, 
	                           String logStreamName,
	                           final String awsRegion,
	                           Integer queueLength,
	                           Integer messagesBatchSize) {
	        super(name, filter, layout, ignoreExceptions);
	        //this.awsAccessKey = new DefaultAWSCredentialsProviderChain().getCredentials().getAWSAccessKeyId();
	        //this.awsAccessSecret = new DefaultAWSCredentialsProviderChain().getCredentials().getAWSSecretKey();
	        this.awsRegion = awsRegion;
	        this.queueLength = queueLength;
	        this.messagesBatchSize = messagesBatchSize;
	        this.activateOptions();
	    }


	    @Override
	    public void append(LogEvent event) {
	    	System.out.println("Logging here");
	    
	      if (appenderInitialised.get()) {
	             loggingEventsQueue.offer(event.toImmutable());
	         } else {
	             // just do nothing
	         }
	      testSendXray(event);
	    }
	    
	    
	    public void testSendXray(LogEvent event) {
	    	ReadOnlyStringMap contextMap = event.getContextData();
        	
        	String application = contextMap.getValue("application_name");
        	String correlationId = contextMap.getValue("correlationId");
        	String processorPath = contextMap.getValue("processorPath");
        	String fqdn = event.getLoggerFqcn();
        	String loggerName = event.getLoggerName();
        	
        	System.out.println("---LOG---");
        	System.out.println(application);
        	System.out.println(correlationId);
        	System.out.println(processorPath);
        	System.out.println(fqdn);
        	System.out.println(loggerName);
        	System.out.println(event.getMessage());
        	ObjectMapper mapper = new ObjectMapper();
        	try {
				JsonNode root = mapper.readTree(event.getMessage().getFormattedMessage());

				String tracePoint = root.get("tracePoint").asText();
				correlationId = root.get("correlationId").asText();
				

				System.out.println("tracePoint: "+tracePoint);
				System.out.println("correlationId: "+correlationId);

				TraceID trace = TraceID.fromString(correlationId);

				SegmentImpl s = new SegmentImpl(new AWSXRayRecorder(), "Test");
				s.setTraceId(trace);
				long mili = java.util.Calendar.getInstance().getTimeInMillis();
				s.setStartTime(((double)mili - 1000) / 1000);
				s.setEndTime(((double)mili) / 1000);
				s.setInProgress(false);
				
				PutTraceSegmentsRequest request = 
						new PutTraceSegmentsRequest();
				
				String document = s.prettySerialize();
				
				System.out.println("document: "+document);
				
				request.setTraceSegmentDocuments(Arrays.asList(document));
				
				
				PutTraceSegmentsResult result = xrayClient.putTraceSegments(request);
				System.out.println("Status: "+ result.getSdkHttpMetadata().getHttpStatusCode());
				System.out.println("RequestId: "+ result.getSdkResponseMetadata().getRequestId());
			} catch (Exception e) {
				logger2.error("Couldn't detect message",e);
			}
	    }
	    
	    
	     
	    public void activateOptions() {
        	//this.awsLogsClient = com.amazonaws.services.logs.AWSLogsClientBuilder.standard().withRegion(Regions.fromName(awsRegion)).withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(this.awsAccessKey, this.awsAccessSecret))).build();
        	
            loggingEventsQueue = new LinkedBlockingQueue<>(queueLength);
            try {
            	this.xrayClient = AWSXRayAsyncClientBuilder
						.standard()
						.withRegion(Regions.fromName(awsRegion))
						.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(this.awsAccessKey, this.awsAccessSecret)))
						.build();
        		/*this.xray = AWSXRay.getGlobalRecorder();
        		xray.setEmitter(null);*/
                //initializeCloudwatchResources();
                //initXrayDaemon();
                appenderInitialised.set(true);
            } catch (Exception e) {
                logger2.error("Could not initialise Xray", e);
                if (DEBUG_MODE) {
                    System.err.println("Could not initialise Xray");
                    e.printStackTrace();
                }
            }
	    }
	     
	    private void initXrayDaemon() {
	     Thread t = new Thread(() -> {
	            while (true) {
	                try {
	                    if (loggingEventsQueue.size() > 0) {
	                        sendMessages();
	                    }
	                    Thread.currentThread().sleep(20L);
	                } catch (InterruptedException e) {
	                    if (DEBUG_MODE) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	        });
	     t.setName("CloudwatchThread");
	     t.setDaemon(true);
	     t.start();
	    }
	     
	    private void sendMessages() {
	        synchronized (sendMessagesLock) {
	            LogEvent polledLoggingEvent;
	            final Layout layout = getLayout();
	            List<LogEvent> loggingEvents = new ArrayList<>();
	 
	            try {
	 
	                while ((polledLoggingEvent = (LogEvent) loggingEventsQueue.poll()) != null && loggingEvents.size() <= messagesBatchSize) {
	                    loggingEvents.add(polledLoggingEvent);
	                }
	               
	                if (!loggingEvents.isEmpty()) {
		                loggingEvents.stream()
	                        .sorted(comparing(LogEvent::getTimeMillis))
	                        .forEach (loggingEvent -> {
	                        	ReadOnlyStringMap contextMap = loggingEvent.getContextData();
	                        	
	                        	String application = contextMap.getValue("application_name");
	                        	String correlationId = contextMap.getValue("correlationId");
	                        	String processorPath = contextMap.getValue("processorPath");
	                        	String fqdn = loggingEvent.getLoggerFqcn();
	                        	String loggerName = loggingEvent.getLoggerName();
	                        	
	                        	System.out.println("---LOG---");
	                        	System.out.println(application);
	                        	System.out.println(correlationId);
	                        	System.out.println(processorPath);
	                        	System.out.println(fqdn);
	                        	System.out.println(loggerName);
			                	//xray.beginSegment(contextMap.getValue("application_name"), null, awsAccessKey);
			                }
		                );
	                }
	                /*List inputLogEvents = loggingEvents.stream()
	                        .map(loggingEvent -> new InputLogEvent().withTimestamp(loggingEvent.getTimeMillis())
	                          .withMessage
	                          (
	                            layout == null ?
	                            loggingEvent.getMessage().getFormattedMessage():
	                            new String(layout.toByteArray(loggingEvent), StandardCharsets.UTF_8)
	                            )
	                          )
	                        .sorted(comparing(InputLogEvent::getTimestamp))
	                        .collect(toList());
	 
	                if (!inputLogEvents.isEmpty()) {
	                	
	                
	                	PutLogEventsRequest putLogEventsRequest = new PutLogEventsRequest(
	                            logGroupName,
	                            logStreamName,
	                            inputLogEvents);
	 
	                    try {
	                        putLogEventsRequest.setSequenceToken((String)lastSequenceToken.get());
	                        PutLogEventsResult result = awsLogsClient.putLogEvents(putLogEventsRequest);
	                        lastSequenceToken.set(result.getNextSequenceToken());
	                    } catch (DataAlreadyAcceptedException dataAlreadyAcceptedExcepted) {
	                      
	                        putLogEventsRequest.setSequenceToken(dataAlreadyAcceptedExcepted.getExpectedSequenceToken());
	                        PutLogEventsResult result = awsLogsClient.putLogEvents(putLogEventsRequest);
	                        lastSequenceToken.set(result.getNextSequenceToken());
	                        if (DEBUG_MODE) {
	                            dataAlreadyAcceptedExcepted.printStackTrace();
	                        }
	                    } catch (InvalidSequenceTokenException invalidSequenceTokenException) {
	                        putLogEventsRequest.setSequenceToken(invalidSequenceTokenException.getExpectedSequenceToken());
	                        PutLogEventsResult result = awsLogsClient.putLogEvents(putLogEventsRequest);
	                        lastSequenceToken.set(result.getNextSequenceToken());
	                        if (DEBUG_MODE) {
	                            invalidSequenceTokenException.printStackTrace();
	                        }
	                    }
	                }
	                					*/

	            } catch (Exception e) {
	                if (DEBUG_MODE) {
	                 logger2.error(" error inserting xray:",e);
	                    e.printStackTrace();
	                }
	            }
	        }
	    }
	 
	    private void initializeCloudwatchResources() {
	 /*
	        DescribeLogGroupsRequest describeLogGroupsRequest = new DescribeLogGroupsRequest();
	        describeLogGroupsRequest.setLogGroupNamePrefix(logGroupName);
	 
	        Optional logGroupOptional = awsLogsClient
	                .describeLogGroups(describeLogGroupsRequest)
	                .getLogGroups()
	                .stream()
	                .filter(logGroup -> logGroup.getLogGroupName().equals(logGroupName))
	                .findFirst();
	 
	        if (!logGroupOptional.isPresent()) {
	            CreateLogGroupRequest createLogGroupRequest = new CreateLogGroupRequest().withLogGroupName(logGroupName);
	            awsLogsClient.createLogGroup(createLogGroupRequest);
	        }
	 
	        DescribeLogStreamsRequest describeLogStreamsRequest = new DescribeLogStreamsRequest().withLogGroupName(logGroupName).withLogStreamNamePrefix(logStreamName);
	 
	        Optional logStreamOptional = awsLogsClient
	                .describeLogStreams(describeLogStreamsRequest)
	                .getLogStreams()
	                .stream()
	                .filter(logStream -> logStream.getLogStreamName().equals(logStreamName))
	                .findFirst();
	        if (!logStreamOptional.isPresent()) {
	            CreateLogStreamRequest createLogStreamRequest = new CreateLogStreamRequest().withLogGroupName(logGroupName).withLogStreamName(logStreamName);
	            CreateLogStreamResult o = awsLogsClient.createLogStream(createLogStreamRequest);
	        }*/
	 
	    }
	     
	    private boolean isBlank(String string) {
	        return null == string || string.trim().length() == 0;
	    }
	    protected String getSimpleStacktraceAsString(final Throwable thrown) {
	        final StringBuilder stackTraceBuilder = new StringBuilder();
	        for (StackTraceElement stackTraceElement : thrown.getStackTrace()) {
	            new Formatter(stackTraceBuilder).format("%s.%s(%s:%d)%n",
	                    stackTraceElement.getClassName(),
	                    stackTraceElement.getMethodName(),
	                    stackTraceElement.getFileName(),
	                    stackTraceElement.getLineNumber());
	        }
	        return stackTraceBuilder.toString();
	    }
	 
	    @Override
	    public void start() {
	        super.start();
	    }
	 
	    @Override
	    public void stop() {
	        super.stop();
	        while (loggingEventsQueue != null && !loggingEventsQueue.isEmpty()) {
	            this.sendMessages();
	        }
	    }
	 
	    @Override
	    public String toString() {
	        return XrayAppender.class.getSimpleName();
	                
	    }
	 
	    @PluginFactory
	    public static XrayAppender createXrayAppender(
	      @PluginAttribute(value = "queueLength" ) Integer queueLength,
	                                                  @PluginElement("Layout") Layout layout,
	                                                  @PluginAttribute(value = "logGroupName") String logGroupName,
	                                                  @PluginAttribute(value = "logStreamName") String logStreamName,
	                                                  @PluginAttribute(value = "awsRegion") String awsRegion,
	                                                  @PluginAttribute(value = "name") String name,
	                                                  @PluginAttribute(value = "ignoreExceptions", defaultBoolean = false) Boolean ignoreExceptions,
	                                                   
	                                                  @PluginAttribute(value = "messagesBatchSize") Integer messagesBatchSize)
	    {
	     return new XrayAppender(name, layout, null, ignoreExceptions, logGroupName, logStreamName , awsRegion, queueLength,messagesBatchSize);
	    }
	}