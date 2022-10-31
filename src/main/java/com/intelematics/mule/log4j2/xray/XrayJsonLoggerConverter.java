package com.intelematics.mule.log4j2.xray;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.amazonaws.xray.entities.TraceID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class XrayJsonLoggerConverter {

	private static final boolean DEBUG_MODE = XrayAppender.getDebugMode();
	private ObjectMapper mapper = new ObjectMapper();

	@Data
	@JsonInclude(Include.NON_EMPTY)
	class Segment extends SubSegment{
		@JsonProperty("trace_id")
		String traceId;
		@JsonProperty("parent_id")
		String parentId;
		
		
		@JsonIgnore
		private
		Random rand;
		public Random getRand() {
			if (rand == null)
				rand = new Random(traceId.hashCode()); //Using the traceID as the start will make this more deterministic, 
			
			return rand;
		}

		Map<String, Object> annotations = new HashMap<>();

		public Segment(String name, String traceId) {
			super (name, null);
			this.traceId = traceId;
			this.id = Long.toHexString(getRand().nextLong());
		}


		public String serialize() throws JsonProcessingException {
			return mapper.writeValueAsString(this);
		}
	}

	@Data
	@JsonInclude(Include.NON_EMPTY)
	class SubSegment {
		String name;
		String id;
		@JsonProperty("start_time")
		double startTime;
		@JsonProperty("end_time")
		double endTime;
		@JsonProperty("in_progress")
		@JsonInclude(Include.NON_DEFAULT)
		boolean inProgress;
		@JsonInclude(Include.NON_DEFAULT)
		boolean error;
		@JsonInclude(Include.NON_DEFAULT)
		boolean fault;

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

			JsonLoggerEntry baseRequestEvent = request.getStart() != null ? request.getStart() : request.getEnd();

			SubSegment reqSeg = new SubSegment(baseRequestEvent.getMessage(), s);

			setSegmentAttributes(reqSeg, request);

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

			s.addSubsegment(reqSeg);
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

		if (DEBUG_MODE)
			log.debug("## Xray document: " + document);

		return document;
	}

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

	private void putRequestField(SubSegment s, JsonLoggerEntry baseEvent, String payloadKey, String xrayKey) {
		putRequestField(s, baseEvent, payloadKey, xrayKey, "");
	}

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
