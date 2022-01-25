package com.mulesoft.log4j2.xray;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class JsonLoggerTransaction {
	private JsonLoggerEntry start, end;
	private String correlationId;
	private List<JsonLoggerTransaction> requestTransactions = new ArrayList<>();
	
	public void setStart(JsonLoggerEntry entry) {
		this.start = entry;
		this.correlationId = start.getCorrelationId();
	}

	public void setEnd(JsonLoggerEntry entry) {
		this.end = entry;
		this.correlationId = correlationId == null ? end.getCorrelationId() : correlationId ;
	}
	
	public boolean isReadyToSend() {
		return start != null && end != null;
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
}
