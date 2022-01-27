package com.intelematics.mule.log4j2.xray;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.utils.ByteUtils;

public class XrayJsonLoggerConverter {

	private static Logger logger = LogManager.getLogger(XrayJsonLoggerConverter.class);
	AWSXRayRecorder unusedRecorder = new AWSXRayRecorder();

	class Segment extends SegmentImpl {

		public Segment(String name, TraceID traceId) {
			super(unusedRecorder, name, traceId);
		}

		@Override
		public void addSubsegment(Subsegment subsegment) {
			this.getSubsegments().add(subsegment);
		}
	}

	class SubSegment extends SubsegmentImpl {
		public SubSegment(String name, Segment parentSegment) {
			super(unusedRecorder, name, parentSegment);
		}
	}

	public String convert(JsonLoggerTransaction transaction) {
		JsonLoggerEntry baseEvent = transaction.getFirstEvent();
		
		String correlationId = transaction.getCorrelationId();
		TraceID trace = TraceID.fromString(correlationId);
		if (!correlationId.equals(trace.toString())) {
			logger.error("Bad traceId - this will probably cause disconnected logs (aws: "+trace.toString()+" vs generated: "+correlationId+") please see the Log4J-Xray-JsonLogger Appender docs on generating correlationIDs.");
		}

		Segment s = new Segment(baseEvent.getEnvironment() + ":" + baseEvent.getApplicationName() + ":" + baseEvent.getFlow(), trace);

		s.setTraceId(trace);
		setSegmentAttributes(s, transaction);

		if (transaction.getStart() != null) {
			JsonLoggerEntry start = transaction.getStart();

			Subsegment sub = new SubSegment("startLogEntry", s);
			setEventAttributes(s, sub, "start", start);
		}

		for (JsonLoggerTransaction request : transaction.getRequestTransactions()) {

			JsonLoggerEntry baseRequestEvent = request.getStart() != null ? request.getStart() : request.getEnd();

			Subsegment reqSeg = new SubSegment(baseRequestEvent.getMessage(), s);

			setSegmentAttributes(reqSeg, request);

			if (request.getStart() != null) {
				JsonLoggerEntry start = request.getStart();

				Subsegment sub = new SubSegment("beforeRequest", s);
				setEventAttributes(reqSeg, sub, "before_request", start);
				reqSeg.addSubsegment(sub);
			}

			if (request.getStart() != null) {
				JsonLoggerEntry end = request.getEnd();

				Subsegment sub = new SubSegment("afterRequest", s);
				setEventAttributes(reqSeg, sub, "after_request", end);
				reqSeg.addSubsegment(sub);
			}

			s.addSubsegment(reqSeg);
		}

		for (JsonLoggerEntry exception : transaction.getExceptions()) {
			Subsegment exSeg = new SubSegment("Eexception", s);
			setEventAttributes(s, exSeg, "exception", exception);
			exSeg.addSubsegment(exSeg);
		}
		
		if (transaction.getEnd() != null) {
			JsonLoggerEntry end = transaction.getEnd();
			Subsegment sub = new SubSegment("endLogEntry", s);

			setEventAttributes(s, sub, "end", end);
		}

		String document = s.serialize();

		logger.debug("## Xray document: " + document);

		return document;
	}

	private void setEventAttributes(Entity s, Subsegment sub, String prefix, JsonLoggerEntry event) {
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

	private void setSegmentAttributes(Entity s, JsonLoggerTransaction transaction) {

		JsonLoggerEntry baseEvent = transaction.getFirstEvent();

		Instant startTime = baseEvent.getTimeAsInstant();
		Instant endTime = transaction.getEnd() != null ? transaction.getEnd().getTimeAsInstant() : null;

		s.setStartTime(((double) startTime.toEpochMilli()) / 1000);

		if (endTime == null) {
			s.setInProgress(true);
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
			putRequestField(s, baseEvent, null, "user_agent", baseEvent.getDeviceOS() + " " + baseEvent.getDeviceBuildVersion());
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

	private void putResponseValue(Entity s, Object payloadValue, String xrayKey) {
		if (payloadValue != null) {
			Map<String, Object> response = (HashMap<String, Object>) s.getHttp().get("response");

			if (response == null) {
				response = new HashMap<String, Object>();
				s.getHttp().put("response", response);
			}

			response.put(xrayKey, payloadValue);
		}
	}

	private void putRequestField(Entity s, JsonLoggerEntry baseEvent, String payloadKey, String xrayKey) {
		putRequestField(s, baseEvent, payloadKey, xrayKey, "");
	}

	private void putRequestField(Entity s, JsonLoggerEntry baseEvent, String payloadKey, String xrayKey, String prefix) {
		String payloadValue = payloadKey == null ? null : baseEvent.getPayload().get(payloadKey);
		if (payloadValue != null || payloadKey == null) {
			Map<String, Object> request = (HashMap<String, Object>) s.getHttp().get("request");

			if (request == null) {
				request = new HashMap<String, Object>();
				s.getHttp().put("request", request);
			}

			request.put(xrayKey, prefix + (payloadValue == null ? "" : payloadValue));
		}
	}
}
