package com.intelematics.mule.log4j2.xray;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

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

  private static final int EXPIRE_AFTER_SECONDS = 120;

  private static Logger logger = LogManager.getLogger(XrayAppender.class);

  static final Boolean DEBUG_MODE = System.getProperty("log4j.debug") != null;
  static String JsonLoggerClass = "org.mule.extension.jsonlogger.JsonLogger";

  /**
   * The queue used to buffer log entries
   */
  private XrayAgent xrayAgent;

  private String awsRegion;

  private HashMap<String, JsonLoggerTransaction> transactions = new HashMap<>();
  private TreeMap<Instant, String> transactionExpiry = new TreeMap<>();

  private XrayAppender(final String name, final Layout layout, final Filter filter, final boolean ignoreExceptions, final String awsRegion, Integer queueLength, Integer messagesBatchSize) {

    super(name, filter, layout, ignoreExceptions);
    this.awsRegion = awsRegion == null ? System.getProperty("awsRegion") : awsRegion;

    System.out.println("## Xray logging started  O.O"); // Can't use the logger here, as it is never setup right
    // now.
  }

  @Override
  public void append(LogEvent event) {
    try {
      sendXrayEvent(event);
    } catch (Exception e) {
      logger.error("## Couldn't send to Xray", e);
    }
  }

  public void sendXrayEvent(LogEvent event) throws JsonMappingException, JsonProcessingException {
    String loggerName = event.getLoggerName();

    if (JsonLoggerClass.equals(loggerName)) {

      if (DEBUG_MODE)
        logger.info("## Xray event found ");
      JsonLoggerEntry entry = new JsonLoggerEntry(event.getMessage().getFormattedMessage());
      JsonLoggerTransaction transaction = null, requestTransaction;

      switch (entry.getTrace()) {
      case START:
        transaction = getTransaction(entry);
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
      case EXCEPTION:
        transaction = getTransaction(entry);
        transaction.addException(entry);
        break;
      case END:
        transaction = getTransaction(entry);
        transaction.setEnd(entry);
        transactions.remove(entry.getCorrelationId());
        break;
      }

      if (transaction != null) {
        getXrayAgent().processTransaction(transaction);
      }

      if (DEBUG_MODE)
        logger.info("## Xray logged " + entry.getTrace());
    }
  }

  private XrayAgent getXrayAgent() {
    if (this.xrayAgent == null) {
      this.xrayAgent = XrayAgent.getInstance(this.awsRegion);
    }
    return this.xrayAgent;
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

    // Remove any expired transactions - this will keep our maps tidy.
    SortedMap<Instant, String> exipredKeys = transactionExpiry.headMap(timeNow);
    if (!exipredKeys.isEmpty()) {
      if (DEBUG_MODE)
        logger.debug("## Xray Purging keys: " + exipredKeys.values().stream().collect(Collectors.joining(",")));
      exipredKeys.values().forEach(correlationId -> transactions.remove(correlationId));

      Set<Instant> expiredInstants = new HashSet<>();
      expiredInstants.addAll(exipredKeys.keySet()); // Avoid Concurrent modification
      expiredInstants.forEach(key -> transactionExpiry.remove(key));
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
    getXrayAgent().stop();
  }

  @Override
  public String toString() {
    return XrayAppender.class.getSimpleName();

  }

  @PluginFactory
  public static XrayAppender createXrayAppender(@PluginAttribute(value = "queueLength") Integer queueLength, @PluginElement("Layout") Layout layout,
      @PluginAttribute(value = "awsRegion") String awsRegion, @PluginAttribute(value = "name") String name,
      @PluginAttribute(value = "ignoreExceptions", defaultBoolean = false) Boolean ignoreExceptions,

      @PluginAttribute(value = "messagesBatchSize") Integer messagesBatchSize) {
    return new XrayAppender(name, layout, null, ignoreExceptions, awsRegion, queueLength, messagesBatchSize);
  }
}