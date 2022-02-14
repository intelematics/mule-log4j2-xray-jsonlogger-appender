package com.intelematics.mule.log4j2.xray;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class JsonLoggerTransaction {
  private JsonLoggerEntry start, end;
  private String correlationId;
  private List<JsonLoggerTransaction> requestTransactions = new ArrayList<>();
  private List<JsonLoggerEntry> exceptions = new ArrayList<>();

  public void setStart(JsonLoggerEntry entry) {
    this.start = entry;
    this.correlationId = start.getCorrelationId();
  }

  public void setEnd(JsonLoggerEntry entry) {
    this.end = entry;
    this.correlationId = correlationId == null ? end.getCorrelationId() : correlationId;
  }

  public String getCorrelationId() {
    if (this.correlationId != null) {
      return correlationId;
    }

    correlationId = getFirstEvent().getCorrelationId();
    return correlationId;
  }

  public boolean isReadyToSend() {
    //Lets give a 5 second delay, to allow cleanup
    if (end != null)
      return end.getTimeAsInstant().plusSeconds(5).compareTo(Instant.now()) < 0;;

    Instant lastActivity = getLastRecordedActivity();
    if (lastActivity == null)
      return false;

    //if we haven't got any activity after 60 seconds, lets assume it is done, and send it.
    return lastActivity.plusSeconds(60).compareTo(Instant.now()) < 0;
  }

  public synchronized List<JsonLoggerEntry> getAllEntries() {
    List<JsonLoggerEntry> entries = new ArrayList<JsonLoggerEntry>();

    if (start != null) {
      entries.add(start);
    }

    for (JsonLoggerTransaction req : requestTransactions) {
      entries.addAll(req.getAllEntries());
    }
    
    entries.addAll(exceptions);

    if (end != null) {
      entries.add(end);
    }

    return entries;
  }

  public Instant getLastRecordedActivity() {
    Instant lastTime = null;

    for (JsonLoggerEntry ent : getAllEntries()) {
      if (lastTime == null || ent.getTimeAsInstant().compareTo(lastTime) < 0) {
        lastTime = ent.getTimeAsInstant();
      }
    }
    ;
    return lastTime;
  }

  public JsonLoggerTransaction addRequestTransaction() {
    JsonLoggerTransaction reqtransaction = new JsonLoggerTransaction();
    requestTransactions.add(reqtransaction);
    return reqtransaction;
  }

  public JsonLoggerTransaction getOpenRequestTransaction() {
    if (requestTransactions.size() == 0) {
      return addRequestTransaction();
    }

    JsonLoggerTransaction lastTransaction = requestTransactions.get(requestTransactions.size() - 1);
    if (lastTransaction.getEnd() == null) {
      return lastTransaction;
    }

    return addRequestTransaction();
  }

  public void addException(JsonLoggerEntry entry) {
    exceptions.add(entry);
  }

  public JsonLoggerEntry getFirstEvent() {
    return getAllEntries().get(0);
  }

  public JsonLoggerEntry getLastEvent() {
    List<JsonLoggerEntry> entries = getAllEntries();
    return entries.get(entries.size() - 1);
  }
}
