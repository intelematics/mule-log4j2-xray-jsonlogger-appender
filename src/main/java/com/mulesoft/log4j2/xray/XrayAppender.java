package com.mulesoft.log4j2.xray;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

@Plugin(name = "Xray", category = "Core", elementType = "appender", printObject = true)
public class XrayAppender extends AbstractAppender {

	private static Logger logger2 = LogManager.getLogger(XrayAppender.class);

	private final Boolean DEBUG_MODE = System.getProperty("log4j.debug") != null;

	/**
	 * The queue used to buffer log entries
	 */
	private LinkedBlockingQueue<LogEvent> loggingEventsQueue;

	private final XrayAgent xrayAgent;

	private String awsAccessKey;
	private String awsAccessSecret;
	private String awsRegion;
	
	private HashMap<String, JsonLoggerTransaction> transactions = new HashMap<>();

	private XrayAppender(final String name, final Layout layout, final Filter filter, final boolean ignoreExceptions,
			String logGroupName, String logStreamName, final String awsRegion, Integer queueLength,
			Integer messagesBatchSize) {

		super(name, filter, layout, ignoreExceptions);
		// this.awsAccessKey = new
		// DefaultAWSCredentialsProviderChain().getCredentials().getAWSAccessKeyId();
		// this.awsAccessSecret = new
		// DefaultAWSCredentialsProviderChain().getCredentials().getAWSSecretKey();
		this.awsRegion = awsRegion;

		this.awsAccessKey = "-accessId-";
		this.awsAccessSecret = "-secret-";
		this.awsRegion = "ap-southeast-2";

		this.xrayAgent = new XrayAgent(this.awsRegion, this.awsAccessKey, this.awsAccessSecret);
	}

	@Override
	public void append(LogEvent event) {
		try {
			sendXrayEvent(event);
		} catch (Exception e) {
			logger2.error("Couldn't send to Xray", e);
		}
	}
	
	public void sendXrayEvent(LogEvent event) throws JsonMappingException, JsonProcessingException {
		String loggerName = event.getLoggerName();

		if ("com.mulesoft.log4j2.xray.LoggingTester".equals(loggerName)
				|| "org.mule.extension.jsonlogger.JsonLogger".equals(loggerName)) {
			
			JsonLoggerEntry entry = new JsonLoggerEntry(event.getMessage().getFormattedMessage());
			JsonLoggerTransaction transaction,requestTransaction;
			switch (entry.getTrace()) {
				case START: 
					transaction = new JsonLoggerTransaction();
					transaction.setStart(entry);
					transactions.put(entry.getCorrelationId(), transaction);
					break;
				case BEFORE_REQUEST:
					transaction = getTransaction(entry);
					requestTransaction = transaction.addRequestTransaction();
					requestTransaction.setStart(entry);
					break;
				case AFTER_REQUEST:
					transaction = getTransaction(entry);
					requestTransaction = transaction.getOpenRequestTransaction();
					requestTransaction.setEnd(entry);
					break;
				case END:
					transaction = getTransaction(entry);
					transaction.setEnd(entry);
					xrayAgent.batchTransaction(transaction);
					transactions.remove(entry.getCorrelationId());
					break;
			}
			
		}
	}

	private JsonLoggerTransaction getTransaction(JsonLoggerEntry entry) {
		JsonLoggerTransaction transaction;
		transaction = transactions.get(entry.getCorrelationId());
		if (transaction == null) {
			transaction = new JsonLoggerTransaction();
			transactions.put(entry.getCorrelationId(), transaction);
		}
		
		return transaction;
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		while (loggingEventsQueue != null && !loggingEventsQueue.isEmpty()) {
			xrayAgent.publishPendingMessages();
		}
	}

	@Override
	public String toString() {
		return XrayAppender.class.getSimpleName();

	}

	@PluginFactory
	public static XrayAppender createXrayAppender(@PluginAttribute(value = "queueLength") Integer queueLength,
			@PluginElement("Layout") Layout layout, @PluginAttribute(value = "logGroupName") String logGroupName,
			@PluginAttribute(value = "logStreamName") String logStreamName,
			@PluginAttribute(value = "awsRegion") String awsRegion, @PluginAttribute(value = "name") String name,
			@PluginAttribute(value = "ignoreExceptions", defaultBoolean = false) Boolean ignoreExceptions,

			@PluginAttribute(value = "messagesBatchSize") Integer messagesBatchSize) {
		return new XrayAppender(name, layout, null, ignoreExceptions, logGroupName, logStreamName, awsRegion,
				queueLength, messagesBatchSize);
	}
}