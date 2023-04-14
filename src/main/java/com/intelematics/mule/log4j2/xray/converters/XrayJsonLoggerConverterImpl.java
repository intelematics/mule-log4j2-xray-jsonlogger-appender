package com.intelematics.mule.log4j2.xray.converters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.amazonaws.xray.entities.TraceID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.intelematics.mule.log4j2.xray.XrayAppender;
import com.intelematics.mule.log4j2.xray.model.JsonLoggerEntry;
import com.intelematics.mule.log4j2.xray.model.JsonLoggerTransaction;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class XrayJsonLoggerConverterImpl {

	private static final boolean DEBUG_MODE = XrayAppender.getDebugMode();
	protected static transient Random rand;

	@Data
	class Segment extends SubSegment{
		@SerializedName("trace_id")
		String traceId;
		@SerializedName("parent_id")
		String parentId;
		
		
		public Random getRand() {
			if (rand == null)
				rand = new Random(name.hashCode() + traceId.hashCode()); //Using the traceID as the start will make this more deterministic, 
			
			return rand;
		}

		public Segment(String name, String traceId) {
			super (name, null);
			this.traceId = traceId;
			this.id = Long.toHexString(getRand().nextLong());
		}


		public String serialize() throws JsonProcessingException {
			Gson gson = new Gson();
			return gson.toJson(this);
		}
	}

	@Data
	class SubSegment {
		String name;
		String id;
		@SerializedName("start_time")
		double startTime;
		@SerializedName("end_time")
		double endTime;
		@SerializedName("in_progress")
		boolean inProgress;
		boolean error;
		boolean fault;
		String namespace;

		Map<String, Map<String, Object>> http = new HashMap<>();

		Map<String, Object> annotations = new HashMap<>();
		List<SubSegment> subsegments = new ArrayList<>();

		public SubSegment(String name, Segment parentSegment) {
			this.name = name;
			if (parentSegment != null) {
				this.id = Long.toHexString(parentSegment.getRand().nextLong());
			}
		}

		public void addSubsegment(SubSegment subsegment) {
			this.getSubsegments().add(subsegment);
		}
}

	/** Converts all the objects in a flattened transaction into a Xray object for processing */
	public String convert(JsonLoggerTransaction transaction) throws JsonProcessingException {
		JsonLoggerEntry baseEvent = transaction.getFirstEvent();

		String correlationId = transaction.getCorrelationId();
		TraceID trace = TraceID.fromString(correlationId);
		if (!correlationId.equals(trace.toString())) {
			log.error("Bad traceId - this will probably cause disconnected logs (aws: " + trace.toString()
					+ " vs generated: " + correlationId
					+ ") please see the Log4J-Xray-JsonLogger Appender docs on generating correlationIDs.");
		}

		Segment s = new Segment(
				baseEvent.getEnvironment() + ":" + baseEvent.getApplicationName() + ":" + baseEvent.getFlow(),
				correlationId);

		setSegmentAttributes(s, transaction);

		if (transaction.getStart() != null) {
			JsonLoggerEntry start = transaction.getStart();

			SubSegment sub = new SubSegment("startLogEntry", s);
			setEventAttributes(s, sub, "start", start);
		}

		for (JsonLoggerTransaction request : transaction.getRequestTransactions()) {

			s.addSubsegment(convertRequest(s, request));
		}

		for (JsonLoggerEntry exception : transaction.getExceptions()) {
			SubSegment exSeg = new SubSegment("exception", s);
			setEventAttributes(s, exSeg, "exception", exception);
		}

		if (transaction.getEnd() != null) {
			JsonLoggerEntry end = transaction.getEnd();
			SubSegment sub = new SubSegment("endLogEntry", s);

			// For a request, this trace ID is given back to the parent, and read out in
			// it's attributes
			if (transaction.getEnd().getTraceId() != null) {
				s.setParentId(transaction.getEnd().getTraceId());
			}

			setEventAttributes(s, sub, "end", end);
		}

		// This case often happens for async requests
		if (transaction.getStart() == null && transaction.getEnd() == null
				&& transaction.getRequestTransactions().size() == 1) {
			s.setInProgress(s.getSubsegments().get(0).isInProgress());
		}

		String document = s.serialize();


		return document;
	}

	/** Covert the before and after request into a segment based on the details provided*/
	private SubSegment convertRequest(Segment s, JsonLoggerTransaction request) {
		JsonLoggerEntry baseRequestEvent = request.getStart() != null ? request.getStart() : request.getEnd();

		SubSegment reqSeg = new SubSegment(baseRequestEvent.getMessage(), s);
		setSegmentAttributes(reqSeg, request);

		if (request.getEnd().getTraceId() == null) {
			reqSeg.setNamespace("remote");
			
			if (request.getStart() != null) {
				putRequestField(reqSeg, request.getStart(), "url", "url");
			}
		}

		if (request.getStart() != null) {
			JsonLoggerEntry start = request.getStart();

			SubSegment sub = new SubSegment("before " + start.getTrace().traceGroup.name().toLowerCase(), s);
			setEventAttributes(reqSeg, sub, "before_request", start);
		}

		if (request.getEnd() != null) {
			JsonLoggerEntry end = request.getEnd();

			SubSegment sub = new SubSegment("after " + end.getTrace().traceGroup.name().toLowerCase(), s);
			
			// This ID is recieved back from teh child request and backfilled as the ID used
			// to make the request
			if (request.getEnd().getTraceId() != null) {
				reqSeg.setId(request.getEnd().getTraceId());
			}

			setEventAttributes(reqSeg, sub, "after_request", end);
		}
		return reqSeg;
	}

	/** Sets debug objects so the flow is easier to identify */
	private void setEventAttributes(SubSegment s, SubSegment sub, String prefix, JsonLoggerEntry event) {
		if (event == null)
			return;

		Map<String, Object> annotations = s.getAnnotations();
		annotations.put(prefix + "_file", event.getFile());
		annotations.put(prefix + "_flow", event.getFlow());
		annotations.put(prefix + "_line", event.getFileLine());

		Instant time = event.getTimeAsInstant();
		double eventTime = ((double) time.toEpochMilli()) / 1000;

		sub.setStartTime(eventTime);
		sub.setEndTime(eventTime);

		sub.setAnnotations(filterLongAnnotations(event.getPayload()));
		sub.setInProgress(false);

		s.addSubsegment(sub);
	}

	/** Stops Xray not sending requests as the data is too long. Fields must be less than 250 characters to be accepted*/
	private Map<String, Object> filterLongAnnotations(Map<String, String> payload) {
		Map<String, Object> subAnnotations = new HashMap<>(payload);
		for (Entry<String, Object> entry : subAnnotations.entrySet()) {
			if (entry.getValue().toString().length() > 250) {
				entry.setValue("<Long value excluded>");
			}

		}
		return subAnnotations;
	}

	private void setSegmentAttributes(SubSegment s, JsonLoggerTransaction transaction) {

		JsonLoggerEntry baseEvent = transaction.getFirstEvent();

		Instant startTime = baseEvent.getTimeAsInstant();
		Instant endTime = transaction.getEnd() != null ? transaction.getEnd().getTimeAsInstant() : null;

		s.setStartTime(((double) startTime.toEpochMilli()) / 1000);

		if (endTime == null) {
			if (transaction.getStart() == null) {
				s.setInProgress(true);
			} else {
				JsonLoggerEntry lastEvent = transaction.getFirstEvent();
				s.setEndTime(((double) lastEvent.getTimeAsInstant().toEpochMilli()) / 1000);
				s.setInProgress(false);
			}
		} else {
			s.setEndTime(((double) endTime.toEpochMilli()) / 1000);
			s.setInProgress(false);
		}

		HashMap<String, Object> annotations = new HashMap<>();

		annotations.put("application_name", baseEvent.getApplicationName());
		annotations.put("correlation_id", baseEvent.getCorrelationId());

		putRequestField(s, baseEvent, "url", "url", baseEvent.getApplicationName());
		putRequestField(s, baseEvent, "method", "method");

		if (baseEvent.getDeviceOS() != null || baseEvent.getDeviceBuildVersion() != null) {
			putRequestField(s, baseEvent, null, "user_agent",
					baseEvent.getDeviceOS() + " " + baseEvent.getDeviceBuildVersion());
		}

		if (transaction.getEnd() != null) {
			int statusCode = transaction.getEnd().getStatusCode();
			putResponseValue(s, statusCode, "status");

			if (statusCode >= 400 && statusCode < 500) {
				s.setError(true);
			} else if (statusCode >= 500 && statusCode < 600) {
				s.setFault(true);
			}
		}

		s.setAnnotations(annotations);
	}

	/** Puts fields into the response object for Xray - creating if required. */
	private void putResponseValue(SubSegment s, Object payloadValue, String xrayKey) {
		if (payloadValue != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = (HashMap<String, Object>) s.getHttp().get("response");

			if (response == null) {
				response = new HashMap<String, Object>();
				s.getHttp().put("response", response);
			}

			response.put(xrayKey, payloadValue);
		}
	}
	
	/** Puts fields into the request object for Xray - creating if required. */
	private void putRequestField(SubSegment s, JsonLoggerEntry baseEvent, String payloadKey, String xrayKey) {
		putRequestField(s, baseEvent, payloadKey, xrayKey, "");
	}

	/** Puts fields into the request object for Xray - creating if required. */
	private void putRequestField(SubSegment s, JsonLoggerEntry baseEvent, String payloadKey, String xrayKey,
			String prefix) {
		String payloadValue = payloadKey == null ? null : baseEvent.getPayload().get(payloadKey);
		if (payloadValue != null || payloadKey == null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> request = (HashMap<String, Object>) s.getHttp().get("request");

			if (request == null) {
				request = new HashMap<String, Object>();
				s.getHttp().put("request", request);
			}

			request.put(xrayKey, prefix + (payloadValue == null ? "" : payloadValue));
		}
	}
}
