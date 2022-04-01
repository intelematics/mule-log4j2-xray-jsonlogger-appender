package com.intelematics.mule.log4j2.xray;

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
  public enum TraceType {
    START, END, BEFORE_REQUEST, AFTER_REQUEST, EXCEPTION, UNKNOWN
  };

  private String environment, correlationId, tracePoint, applicationName, flow, file, fileLine, time, message, deviceOS, deviceBuildVersion, traceId;
  private Instant timeAsInstant;
  private int statusCode;
  private Map<String, String> payload;
  private TraceType trace;

  /*
   * Example message
   * 
   * { "correlationId": "f39e6a90-7cd0-11ec-9cda-0679e83acb76", "message":
   * "response", "tracePoint": "START", "priority": "INFO", "elapsed": 629,
   * "locationInfo": { "lineInFile": "26", "component": "json-logger:logger",
   * "fileName": "interface.xml", "rootContainer": "s-azure-api-main" },
   * "timestamp": "2022-01-24T04:49:03.055Z", "content": { "payload": [ { "id":
   * "cb74c367-c6b0-4e11-99dd-173c51e4c1b5", "name": "INSIGHT-STUDIO",
   * "description": "INSIGHT-STUDIO", "dynamic": { "userTag": "INS:studio" } } ]
   * }, "applicationName": "s-azure-api", "applicationVersion": "1.0.0",
   * "environment": "sandbox", "threadName":
   * "[MuleRuntime].uber.21: [s-azure-api-sandbox-ia].s-azure-api-main.BLOCKING @671d4dfb"
   * }
   */
  public JsonLoggerEntry(String logMessage) throws JsonMappingException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(logMessage);

    environment = root.get("environment").asText();
    correlationId = root.get("correlationId").asText();
    tracePoint = root.get("tracePoint").asText();

    try {
      trace = TraceType.valueOf(tracePoint);
    } catch (IllegalArgumentException e) {
      trace = TraceType.UNKNOWN;
    }

    applicationName = root.get("applicationName").asText();
    flow = root.get("locationInfo").get("rootContainer").asText();
    file = root.get("locationInfo").get("fileName").asText();
    fileLine = root.get("locationInfo").get("lineInFile").asText();
    time = root.get("timestamp").asText();
    timeAsInstant = Instant.parse(time);
    message = root.get("message").asText();
    JsonNode contentObj = root.get("content");

    payload = new HashMap<>();
    if (isNull(contentObj)) {
    } else if (contentObj.isObject()) {
      
      int unknownFields[] = {0};
      
      contentObj.fields().forEachRemaining(field -> {
        JsonNode value = field.getValue();
        switch(field.getKey()) {
          case "session":
            //Session fields from a device should be pulled out
            if (hasValue(value)) {
              deviceOS = value.get("deviceOS").asText();
              deviceBuildVersion = value.get("buildVersion").asText();
            }
            break;
            
          case "statusCode":
            //Usually on a END trace - this will be the status code of the whole request
            statusCode = value == null ? 0 : value.asInt();
            break;
            
          case "traceId":
            //The trace id from the AFTER_REQUEST, etc. This is used to map the requests up
            traceId = value == null ? null : value.asText();
            break;
            
          case "payload":
            break;
            
          default:
            unknownFields[0]++;
        }
      });
      
      
      // We have the payload and optionally the traceid - so lets split it out for usability
      if (unknownFields[0] == 0 && contentObj.has("payload")) {
        JsonNode payloadObj = contentObj.get("payload");
        if (payloadObj.isObject()) {
          appendPayloadFields(payloadObj);
        } else {
          payload.put("payload", payloadObj.asText());
        }
      } else {
        appendPayloadFields(contentObj);
      }


    } else {
      payload.put("value", contentObj.toString());
    }
  }

  private void appendPayloadFields(JsonNode contentObj) {
    contentObj.fields().forEachRemaining(field -> {
      if (field.getValue().isTextual()) {
        payload.put(field.getKey(), field.getValue().asText());
      } else {
        payload.put(field.getKey(), field.getValue().toString());
      }
    });
  }

  private boolean isNull(JsonNode node) {
    return !hasValue(node);
  }

  private boolean hasValue(JsonNode node) {
    return node != null && !node.isNull();
  }
}
