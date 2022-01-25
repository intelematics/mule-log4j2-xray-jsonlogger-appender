package com.mulesoft.log4j2.xray;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class JsonLoggerEntry {
	public enum TraceType {START, END, BEFORE_REQUEST, AFTER_REQUEST, UNKNOWN};
	
	private String environment, correlationId, tracePoint, applicationName, flow, file, fileLine, time, message, deviceOS, deviceBuildVersion;
	private int statusCode;
	private Map<String, String> payload;
	private TraceType trace;
	private static final ObjectMapper mapper = new ObjectMapper();
	
	/* Example message
	 
	{
	    "correlationId": "f39e6a90-7cd0-11ec-9cda-0679e83acb76",
	    "message": "response",
	    "tracePoint": "START",
	    "priority": "INFO",
	    "elapsed": 629,
	    "locationInfo": {
	        "lineInFile": "26",
	        "component": "json-logger:logger",
	        "fileName": "interface.xml",
	        "rootContainer": "s-azure-api-main"
	    },
	    "timestamp": "2022-01-24T04:49:03.055Z",
	    "content": {
	        "payload": [
	            {
	                "id": "cb74c367-c6b0-4e11-99dd-173c51e4c1b5",
	                "name": "INSIGHT-STUDIO",
	                "description": "INSIGHT-STUDIO",
	                "dynamic": {
	                    "userTag": "INS:studio"
	                }
	            }
	        ]
	    },
	    "applicationName": "s-azure-api",
	    "applicationVersion": "1.0.0",
	    "environment": "sandbox",
	    "threadName": "[MuleRuntime].uber.21: [s-azure-api-sandbox-ia].s-azure-api-main.BLOCKING @671d4dfb"
	}	  
 	*/
	public JsonLoggerEntry(String logMessage) throws JsonMappingException, JsonProcessingException {
		JsonNode root = mapper.readTree(logMessage);

		environment = root.get("environment").asText();
		correlationId = root.get("correlationId").asText();
		tracePoint = root.get("tracePoint").asText();
		trace = TraceType.valueOf(tracePoint);
		applicationName = root.get("applicationName").asText();
		flow = root.get("locationInfo").get("rootContainer").asText();
		file = root.get("locationInfo").get("fileName").asText();
		fileLine = root.get("locationInfo").get("lineInFile").asText();
		time = root.get("timestamp").asText();
		message = root.get("message").asText();
		JsonNode payloadObj = root.get("content");

		payload = new HashMap<>();
		if (payloadObj == null || payloadObj.isEmpty()) {}
		else if (payloadObj.isObject()) {
			payloadObj.fields().forEachRemaining(field -> {
				payload.put(field.getKey(), field.getValue().asText());
			});

			JsonNode session = payloadObj.get("session");
			if (session != null) {
				deviceOS = session.get("deviceOS").asText();
				deviceBuildVersion = session.get("buildVersion").asText();
			}
			
			JsonNode statusCodeObj = payloadObj.get("statusCode");
			statusCode = statusCodeObj == null ? 0 : statusCodeObj.asInt();
		}
		else {
			payload.put("value", payloadObj.toString());
		}
	}
	
	public Instant getTimeAsInstant() {
		return Instant.parse(getTime());
	}
}
