package com.intelematics.mule.log4j2.xray.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.intelematics.mule.log4j2.xray.model.JsonLoggerEntry;
import com.intelematics.mule.log4j2.xray.model.JsonLoggerTransaction;

public class XrayJsonLoggerConverterData {
	public JsonLoggerTransaction getBasicTransaction() throws JsonMappingException, JsonProcessingException {
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
		JsonLoggerTransaction request = transaction.addRequestTransaction();
		
		request.setStart(new JsonLoggerEntry("{\n"
				+ "  \"correlationId\" : \"1-636e106f-861f40cdbb2d211420b7e620\",\n"
				+ "  \"message\" : \"GET /getSitePrices by state\",\n"
				+ "  \"tracePoint\" : \"BEFORE_REQUEST\",\n"
				+ "  \"priority\" : \"INFO\",\n"
				+ "  \"elapsed\" : 0,\n"
				+ "  \"locationInfo\" : {\n"
				+ "    \"lineInFile\" : \"359\",\n"
				+ "    \"component\" : \"json-logger:logger\",\n"
				+ "    \"fileName\" : \"implementation/fuel-impl.xml\",\n"
				+ "    \"rootContainer\" : \"fuel-impl:buildFuelStationPriceMap\"\n"
				+ "  },\n"
				+ "  \"timestamp\" : \"2022-11-11T09:06:04.348Z\",\n"
				+ "  \"content\" : {\n"
				+ "    \"requestQueryParams\" : {\n"
				+ "      \"countryId\" : \"21\",\n"
				+ "      \"geoRegionLevel\" : \"3\",\n"
				+ "      \"geoRegionId\" : \"3\"\n"
				+ "    }\n"
				+ "  },\n"
				+ "  \"applicationName\" : \"s-poi-api\",\n"
				+ "  \"applicationVersion\" : \"1.0.0\",\n"
				+ "  \"environment\" : \"local\",\n"
				+ "  \"threadName\" : \"[MuleRuntime].uber.01: [mule-s-poi-api].fuel-impl:buildFuelStationPriceMap.BLOCKING @2d3eb0e0\"\n"
				+ "}"));
		
		request.setEnd(new JsonLoggerEntry("{\n"
				+ "  \"correlationId\" : \"1-636e106f-861f40cdbb2d211420b7e620\",\n"
				+ "  \"message\" : \"GET /getSitePrices by state\",\n"
				+ "  \"tracePoint\" : \"AFTER_REQUEST\",\n"
				+ "  \"priority\" : \"INFO\",\n"
				+ "  \"elapsed\" : 1027,\n"
				+ "  \"locationInfo\" : {\n"
				+ "    \"lineInFile\" : \"369\",\n"
				+ "    \"component\" : \"json-logger:logger\",\n"
				+ "    \"fileName\" : \"implementation/fuel-impl.xml\",\n"
				+ "    \"rootContainer\" : \"fuel-impl:buildFuelStationPriceMap\"\n"
				+ "  },\n"
				+ "  \"timestamp\" : \"2022-11-11T09:06:05.375Z\",\n"
				+ "  \"content\" : {"
						+ "    \"statusCode\" : \"200\",\n"
						+ "    \"traceId\" : \"78258e8c46edb236\"\n"
						+ "  },\n"
				+ "  \"applicationName\" : \"s-poi-api\",\n"
				+ "  \"applicationVersion\" : \"1.0.0\",\n"
				+ "  \"environment\" : \"local\",\n"
				+ "  \"threadName\" : \"[MuleRuntime].uber.15: [mule-s-poi-api].fuel-impl:buildFuelStationPriceMap.BLOCKING @2d3eb0e0\"\n"
				+ "}"));
		
		transaction.setEnd(new JsonLoggerEntry("{\n"
				+ "  \"correlationId\" : \"1-636e0bf3-eb9e41c49c5857805f3b55fa\",\n"
				+ "  \"message\" : \"response\",\n"
				+ "  \"tracePoint\" : \"END\",\n"
				+ "  \"priority\" : \"INFO\",\n"
				+ "  \"elapsed\" : 879,\n"
				+ "  \"locationInfo\" : {\n"
				+ "    \"lineInFile\" : \"27\",\n"
				+ "    \"component\" : \"json-logger:logger\",\n"
				+ "    \"fileName\" : \"interface.xml\",\n"
				+ "    \"rootContainer\" : \"interface-main\"\n"
				+ "  },\n"
				+ "  \"timestamp\" : \"2022-11-11T08:46:49.746Z\",\n"
				+ "  \"content\" : {\n"
				+ "    \"statusCode\" : \"200\",\n"
				+ "    \"traceId\" : \"8913443fd396fea8\"\n"
				+ "  },\n"
				+ "  \"applicationName\" : \"s-poi-api\",\n"
				+ "  \"applicationVersion\" : \"1.0.0\",\n"
				+ "  \"environment\" : \"local\",\n"
				+ "  \"threadName\" : \"[MuleRuntime].uber.13: [mule-s-poi-api].interface-main.BLOCKING @717a9a31\"\n"
				+ "}"));
		
		return transaction;
	}
}
