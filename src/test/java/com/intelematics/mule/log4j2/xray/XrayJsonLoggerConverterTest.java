package com.intelematics.mule.log4j2.xray;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class XrayJsonLoggerConverterTest {

	@Test
	public void testConvert() throws JsonMappingException, JsonProcessingException {
		JsonLoggerTransaction transaction = new JsonLoggerTransaction();
		transaction.setStart(new JsonLoggerEntry("{\n"
				+ "    \"correlationId\": \"1-635b5654-814fa4dedafd41683fa03ffa\",\n"
				+ "    \"message\": \"request\",\n"
				+ "    \"tracePoint\": \"START\",\n"
				+ "    \"priority\": \"INFO\",\n"
				+ "    \"elapsed\": 0,\n"
				+ "    \"locationInfo\": {\n"
				+ "        \"lineInFile\": \"21\",\n"
				+ "        \"component\": \"json-logger:logger\",\n"
				+ "        \"fileName\": \"interface.xml\",\n"
				+ "        \"rootContainer\": \"interface-main\"\n"
				+ "    },\n"
				+ "    \"timestamp\": \"2022-10-28T01:47:23.944Z\",\n"
				+ "    \"content\": {\n"
				+ "        \"url\": \"/api/app-config?flavour=tfnsw\",\n"
				+ "        \"method\": \"GET\",\n"
				+ "        \"payload\": null,\n"
				+ "        \"session\": {\n"
				+ "            \"deviceOS\": \"iOS\",\n"
				+ "            \"buildVersion\": \"2.2.6.1\"\n"
				+ "        }\n"
				+ "    },\n"
				+ "    \"applicationName\": \"e-arevo-api\",\n"
				+ "    \"applicationVersion\": \"1.0.0\",\n"
				+ "    \"environment\": \"sandbox\",\n"
				+ "    \"threadName\": \"[MuleRuntime].uber.4252: [e-arevo-api-sandbox-ia].interface-main.BLOCKING @6fe81b51\"\n"
				+ "}"));
		transaction.setEnd(new JsonLoggerEntry("{\n"
				+ "    \"correlationId\": \"1-635b5654-814fa4dedafd41683fa03ffa\",\n"
				+ "    \"message\": \"response\",\n"
				+ "    \"tracePoint\": \"END\",\n"
				+ "    \"priority\": \"INFO\",\n"
				+ "    \"elapsed\": 8,\n"
				+ "    \"locationInfo\": {\n"
				+ "        \"lineInFile\": \"31\",\n"
				+ "        \"component\": \"json-logger:logger\",\n"
				+ "        \"fileName\": \"interface.xml\",\n"
				+ "        \"rootContainer\": \"interface-main\"\n"
				+ "    },\n"
				+ "    \"timestamp\": \"2022-10-28T01:47:24.044Z\",\n"
				+ "    \"content\": {\n"
				+ "        \"statusCode\": 400,        \"payload\": {\n"
				+ "            \"id\": \"tfnsw.sandbox\",\n"
				+ "            \"defaultZoomLevel\": 15.5,\n"
				+ "            \"searchPaddingPercent\": 0.7,\n"
				+ "            \"requeryPaddingTolerancePercent\": 0.7,\n"
				+ "            \"gpsUpdateIntervalSec\": 2,\n"
				+ "            \"gpsUpdateDistanceMeters\": 2,\n"
				+ "            \"features\": {\n"
				+ "                \"turnByTurn\": {\n"
				+ "                    \"zoom\": 18,\n"
				+ "                    \"tilt\": 40,\n"
				+ "                    \"zoomInitial\": 18,\n"
				+ "                    \"tiltInitial\": 0,\n"
				+ "                    \"reroutingDeviationMeters\": 30,\n"
				+ "                    \"reroutingDeviationTimeLimitSec\": 6,\n"
				+ "                    \"announcementDistanceMeters\": 15,\n"
				+ "                    \"stepCompleteRangeMeters\": 5,\n"
				+ "                    \"positionRecordingIntervalSec\": 15,\n"
				+ "                    \"positionRecordingBatchIntervalSec\": 120,\n"
				+ "                    \"destinationCompleteMeters\": 50,\n"
				+ "                    \"feedback\": {\n"
				+ "                        \"reasonCodes\": [\n"
				+ "                            {\n"
				+ "                                \"code\": \"incorrect_destination\",\n"
				+ "                                \"codeDescription\": \"Destination was incorrect\"\n"
				+ "                            },\n"
				+ "                            {\n"
				+ "                                \"code\": \"voice_guidance_issue\",\n"
				+ "                                \"codeDescription\": \"Confusing voice guidance\"\n"
				+ "                            },\n"
				+ "                            {\n"
				+ "                                \"code\": \"bad_directions\",\n"
				+ "                                \"codeDescription\": \"Bad directions\"\n"
				+ "                            },\n"
				+ "                            {\n"
				+ "                                \"code\": \"road_closed\",\n"
				+ "                                \"codeDescription\": \"Road was closed\"\n"
				+ "                            },\n"
				+ "                            {\n"
				+ "                                \"code\": \"interface_issues\",\n"
				+ "                                \"codeDescription\": \"Interface issues\"\n"
				+ "                            },\n"
				+ "                            {\n"
				+ "                                \"code\": \"other\",\n"
				+ "                                \"codeDescription\": \"Other reasons\"\n"
				+ "                            }\n"
				+ "                        ]\n"
				+ "                    }\n"
				+ "                }\n"
				+ "            },\n"
				+ "            \"android\": {\n"
				+ "                \"minVersion\": \"2.2.3.280\",\n"
				+ "                \"denylist\": []\n"
				+ "            },\n"
				+ "            \"ios\": {\n"
				+ "                \"minVersion\": \"2.2.3.416\",\n"
				+ "                \"denylist\": []\n"
				+ "            }\n"
				+ "        },\n"
				+ "        \"session\": {\n"
				+ "            \"deviceOS\": \"iOS\",\n"
				+ "            \"buildVersion\": \"2.2.6.1\"\n"
				+ "        }\n"
				+ "    },\n"
				+ "    \"applicationName\": \"e-arevo-api\",\n"
				+ "    \"applicationVersion\": \"1.0.0\",\n"
				+ "    \"environment\": \"sandbox\",\n"
				+ "    \"threadName\": \"[MuleRuntime].uber.4253: [e-arevo-api-sandbox-ia].interface-main.BLOCKING @6fe81b51\"\n"
				+ "}"));
		
		String convertedStr = new XrayJsonLoggerConverter().convert(transaction);
		String expectedStr = "{\"name\":\"sandbox:e-arevo-api:interface-main\",\"id\":\"d664e05588ece19e\",\"error\":true,\"http\":{\"request\":{\"method\":\"GET\",\"url\":\"e-arevo-api/api/app-config?flavour=tfnsw\",\"user_agent\":\"iOS 2.2.6.1\"},\"response\":{\"status\":400}},\"annotations\":{\"end_flow\":\"interface-main\",\"application_name\":\"e-arevo-api\",\"end_file\":\"interface.xml\",\"correlation_id\":\"1-635b5654-814fa4dedafd41683fa03ffa\",\"start_line\":\"21\",\"start_file\":\"interface.xml\",\"start_flow\":\"interface-main\",\"end_line\":\"31\"},\"subsegments\":[{\"name\":\"startLogEntry\",\"id\":\"4691eadfc830a46\",\"annotations\":{\"method\":\"GET\",\"payload\":\"null\",\"session\":\"{\\\"deviceOS\\\":\\\"iOS\\\",\\\"buildVersion\\\":\\\"2.2.6.1\\\"}\",\"url\":\"/api/app-config?flavour=tfnsw\"},\"start_time\":1.666921643944E9,\"end_time\":1.666921643944E9},{\"name\":\"endLogEntry\",\"id\":\"80d5aafc7a806abb\",\"annotations\":{\"features\":\"<Long value excluded>\",\"requeryPaddingTolerancePercent\":\"0.7\",\"defaultZoomLevel\":\"15.5\",\"android\":\"{\\\"minVersion\\\":\\\"2.2.3.280\\\",\\\"denylist\\\":[]}\",\"gpsUpdateDistanceMeters\":\"2\",\"id\":\"tfnsw.sandbox\",\"ios\":\"{\\\"minVersion\\\":\\\"2.2.3.416\\\",\\\"denylist\\\":[]}\",\"searchPaddingPercent\":\"0.7\",\"gpsUpdateIntervalSec\":\"2\"},\"start_time\":1.666921644044E9,\"end_time\":1.666921644044E9}],\"start_time\":1.666921643944E9,\"end_time\":1.666921644044E9,\"trace_id\":\"1-635b5654-814fa4dedafd41683fa03ffa\"}";
		
		System.out.println("Output:");
		System.out.println(convertedStr);
		
		System.out.println("Expected:");
		System.out.println(expectedStr);
		
		
		assertEquals(convertedStr, expectedStr);
	
	}
}