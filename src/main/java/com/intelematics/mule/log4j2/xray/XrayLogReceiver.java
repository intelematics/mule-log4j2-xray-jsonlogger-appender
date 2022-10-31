package com.intelematics.mule.log4j2.xray;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
  private List<ExpiryTransaction> transactionExpiry = new ArrayList<>();
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
      queue.add(event.toImmutable());
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

          if (DEBUG_MODE)
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
    case BEFORE_TRANSFORM:
      transaction = getTransaction(entry);
      transaction.addRequestTransaction().setStart(entry);
      break;
    case AFTER_REQUEST:
    case AFTER_TRANSFORM:
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

  class ExpiryTransaction {
	  final JsonLoggerTransaction transaction;
	  final Instant expiry;
	  String correlationId;
	  
	  ExpiryTransaction(JsonLoggerTransaction transaction, Instant expiry, String correlationId) {
		  this.transaction = transaction;
		  this.expiry = expiry;
		  this.correlationId = correlationId;
	  }
  }
  
  private JsonLoggerTransaction getTransaction(JsonLoggerEntry entry) {
    Instant timeNow = Instant.now();

    JsonLoggerTransaction transaction;
    transaction = transactions.get(entry.getCorrelationId());
    if (transaction == null) {
      transaction = new JsonLoggerTransaction();
      String correlationId = entry.getCorrelationId();
      transactions.put(correlationId, transaction);
      transactionExpiry.add(new ExpiryTransaction(transaction, timeNow.plusSeconds(EXPIRE_AFTER_SECONDS), correlationId));
    }

    if (DEBUG_MODE)
      log.info("## Xray keys to expire: " + transactionExpiry.size() + ", open transactions: " + transactions.size());

    removeExpiredItems(timeNow);

    return transaction;
  }

	private void removeExpiredItems(Instant timeNow) {
		// Get all keys that are actually now expired.
	    ArrayList<ExpiryTransaction> expiredKeys = null;
	    for (ExpiryTransaction expTr : transactionExpiry) {
	    	if (expTr.expiry.isBefore(timeNow)) {
	    		
	    		if (expiredKeys == null) expiredKeys = new ArrayList<>();
	    		expiredKeys.add(expTr);
	    	} else {
	    		break;
	    	}
	    }
	    
	    if (expiredKeys != null) {
		    if (DEBUG_MODE)
		        log.info("## Xray Purging keys: " + expiredKeys.size());
		
		    for (ExpiryTransaction expTr : expiredKeys) {
		    	transactions.remove(expTr.correlationId);
		    	transactionExpiry.remove(expTr);
		    }
		
		    if (DEBUG_MODE)
		        log.info("## Xray keys to new keys: " + transactionExpiry.size() + ", open transactions: " + transactions.size());
	    }
	}
}
