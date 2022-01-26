package com.intelematics.mule.log4j2.xray;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.joda.time.Instant;

public class LoggingTester {
	
	public static void main(String[] args) {
		XrayAppender.JsonLoggerClass = "com.intelematics.mule.log4j2.xray.LoggingTester";
		String correleationId1=UUID.randomUUID().toString();
		String correleationId2=UUID.randomUUID().toString();
		Instant now = Instant.now();
		
		LogManager.getLogger().info("{\n"
				+ "    \"correlationId\": \""+correleationId1+"\",\n"
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
				+ "    \"timestamp\": \""+now.toString()+"\",\n"
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
				+ "}");
		LogManager.getLogger().info("{\n"
				+ "    \"correlationId\": \""+correleationId1+"\",\n"
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
				+ "    \"timestamp\": \""+now.plus(100).toString()+"\",\n"
				+ "    \"content\": {\n"
				+ "        \"statusCode\": 400,"
				+ "        \"payload\": {\n"
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
				+ "}");
		LogManager.getLogger().info("{\n"
				+ "    \"correlationId\": \""+correleationId2+"\",\n"
				+ "    \"message\": \"response\",\n"
				+ "    \"tracePoint\": \"START\",\n"
				+ "    \"priority\": \"INFO\",\n"
				+ "    \"elapsed\": 629,\n"
				+ "    \"locationInfo\": {\n"
				+ "        \"lineInFile\": \"26\",\n"
				+ "        \"component\": \"json-logger:logger\",\n"
				+ "        \"fileName\": \"interface.xml\",\n"
				+ "        \"rootContainer\": \"s-azure-api-main\"\n"
				+ "    },\n"
				+ "    \"timestamp\": \""+now.plus(500).toString()+"\",\n"
				+ "    \"content\": {\n"
				+ "			\"url\": \"/users/whatever?a=1\","
				+ "			\"method\": \"GET\","
				+ "        \"payload\": \n"
				+ "            {\n"
				+ "                \"id\": \"cb74c367-c6b0-4e11-99dd-173c51e4c1b5\",\n"
				+ "                \"name\": \"INSIGHT-STUDIO\",\n"
				+ "                \"description\": \"INSIGHT-STUDIO\",\n"
				+ "                \"dynamic\": {\n"
				+ "                    \"userTag\": \"INS:studio\"\n"
				+ "                }\n"
				+ "            }\n"
				+ "        \n"
				+ "    },\n"
				+ "    \"applicationName\": \"s-azure-api\",\n"
				+ "    \"applicationVersion\": \"1.0.0\",\n"
				+ "    \"environment\": \"sandbox\",\n"
				+ "    \"threadName\": \"[MuleRuntime].uber.21: [s-azure-api-sandbox-ia].s-azure-api-main.BLOCKING @671d4dfb\"\n"
				+ "}");
		LogManager.getLogger().info("{\n"
				+ "    \"correlationId\": \""+correleationId2+"\",\n"
				+ "    \"message\": \"response\",\n"
				+ "    \"tracePoint\": \"BEFORE_REQUEST\",\n"
				+ "    \"priority\": \"INFO\",\n"
				+ "    \"elapsed\": 629,\n"
				+ "    \"locationInfo\": {\n"
				+ "        \"lineInFile\": \"26\",\n"
				+ "        \"component\": \"json-logger:logger\",\n"
				+ "        \"fileName\": \"interface.xml\",\n"
				+ "        \"rootContainer\": \"s-azure-api-main\"\n"
				+ "    },\n"
				+ "    \"timestamp\": \""+now.plus(600).toString()+"\",\n"
				+ "    \"content\": \n"
				+ "            {\n"
				+ "                \"id\": \"cb74c367-c6b0-4e11-99dd-173c51e4c1b5\",\n"
				+ "                \"name\": \"INSIGHT-STUDIO\",\n"
				+ "                \"description\": \"INSIGHT-STUDIO\",\n"
				+ "                \"dynamic\": {\n"
				+ "                    \"userTag\": \"INS:studio\"\n"
				+ "                }\n"
				+ "            }\n"
				+ "        \n"
				+ "    ,\n"
				+ "    \"applicationName\": \"s-azure-api\",\n"
				+ "    \"applicationVersion\": \"1.0.0\",\n"
				+ "    \"environment\": \"sandbox\",\n"
				+ "    \"threadName\": \"[MuleRuntime].uber.21: [s-azure-api-sandbox-ia].s-azure-api-main.BLOCKING @671d4dfb\"\n"
				+ "}");
		LogManager.getLogger().info("{\n"
				+ "    \"correlationId\": \""+correleationId2+"\",\n"
				+ "    \"message\": \"response\",\n"
				+ "    \"tracePoint\": \"AFTER_REQUEST\",\n"
				+ "    \"priority\": \"INFO\",\n"
				+ "    \"elapsed\": 629,\n"
				+ "    \"locationInfo\": {\n"
				+ "        \"lineInFile\": \"26\",\n"
				+ "        \"component\": \"json-logger:logger\",\n"
				+ "        \"fileName\": \"interface.xml\",\n"
				+ "        \"rootContainer\": \"s-azure-api-main\"\n"
				+ "    },\n"
				+ "    \"timestamp\": \""+now.plus(800).toString()+"\",\n"
				+ "    \"content\": {\n"
				+ "                \"id\": \"cb74c367-c6b0-4e11-99dd-173c51e4c1b5\",\n"
				+ "                \"name\": \"INSIGHT-STUDIO\",\n"
				+ "                \"description\": \"INSIGHT-STUDIO\",\n"
				+ "                \"dynamic\": {\n"
				+ "                    \"userTag\": \"INS:studio\"\n"
				+ "                }\n"
				+ "        \n"
				+ "    },\n"
				+ "    \"applicationName\": \"s-azure-api\",\n"
				+ "    \"applicationVersion\": \"1.0.0\",\n"
				+ "    \"environment\": \"sandbox\",\n"
				+ "    \"threadName\": \"[MuleRuntime].uber.21: [s-azure-api-sandbox-ia].s-azure-api-main.BLOCKING @671d4dfb\"\n"
				+ "}");
		LogManager.getLogger().info("{\n"
				+ "    \"correlationId\": \""+correleationId2+"\",\n"
				+ "    \"message\": \"response\",\n"
				+ "    \"tracePoint\": \"END\",\n"
				+ "    \"priority\": \"INFO\",\n"
				+ "    \"elapsed\": 629,\n"
				+ "    \"locationInfo\": {\n"
				+ "        \"lineInFile\": \"26\",\n"
				+ "        \"component\": \"json-logger:logger\",\n"
				+ "        \"fileName\": \"interface.xml\",\n"
				+ "        \"rootContainer\": \"s-azure-api-main\"\n"
				+ "    },\n"
				+ "    \"timestamp\": \""+now.plus(900).toString()+"\",\n"
				+ "    \"content\": {\n"
				+ "        \"statusCode\": 200,\n"
				+ "        \"payload\": \n"
				+ "            {\n"
				+ "                \"id\": \"cb74c367-c6b0-4e11-99dd-173c51e4c1b5\",\n"
				+ "                \"name\": \"INSIGHT-STUDIO\",\n"
				+ "                \"description\": \"INSIGHT-STUDIO\",\n"
				+ "                \"dynamic\": {\n"
				+ "                    \"userTag\": \"INS:studio\"\n"
				+ "                }\n"
				+ "            }\n"
				+ "        \n"
				+ "    },\n"
				+ "    \"applicationName\": \"s-azure-api\",\n"
				+ "    \"applicationVersion\": \"1.0.0\",\n"
				+ "    \"environment\": \"sandbox\",\n"
				+ "    \"threadName\": \"[MuleRuntime].uber.21: [s-azure-api-sandbox-ia].s-azure-api-main.BLOCKING @671d4dfb\"\n"
				+ "}");
		XrayAgent.getInstance("").stop();
	}
}
