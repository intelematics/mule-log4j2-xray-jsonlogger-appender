package com.intelematics.mule.log4j2.xray.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class XrayJsonLoggerConverterAWSTest extends XrayJsonLoggerConverterData{

	@Test
	public void testConvert() throws JsonMappingException, JsonProcessingException {

		
		String convertedStr = new XrayJsonLoggerConverterImplAWS().convert(getBasicTransaction());
		String expectedStr = "{\"name\":\"sandbox:e-arevo-api:interface-main\",\"id\":\"09646366135a7d67\",\"parent_id\":\"8913443fd396fea8\",\"start_time\":1.666921643944E9,\"trace_id\":\"1-635b5654-814fa4dedafd41683fa03ffa\",\"end_time\":1.668156409746E9,\"subsegments\":[{\"name\":\"startLogEntry\",\"id\":\"5352459b0c25d6e0\",\"start_time\":1.666921643944E9,\"end_time\":1.666921643944E9,\"annotations\":{\"method\":\"GET\",\"payload\":\"null\",\"session\":\"{\\\"deviceOS\\\":\\\"iOS\\\",\\\"buildVersion\\\":\\\"2.2.6.1\\\"}\",\"url\":\"/api/app-config?flavour=tfnsw\"}},{\"name\":\"GET /getSitePrices by state\",\"id\":\"78258e8c46edb236\",\"start_time\":1.668157564348E9,\"end_time\":1.668157565375E9,\"subsegments\":[{\"name\":\"before request\",\"id\":\"3172ca5e7c468d2e\",\"start_time\":1.668157564348E9,\"end_time\":1.668157564348E9,\"annotations\":{\"requestQueryParams\":\"{\\\"countryId\\\":\\\"21\\\",\\\"geoRegionLevel\\\":\\\"3\\\",\\\"geoRegionId\\\":\\\"3\\\"}\"}},{\"name\":\"after request\",\"id\":\"2fabd5f24ac10261\",\"start_time\":1.668157565375E9,\"end_time\":1.668157565375E9,\"annotations\":{\"traceId\":\"78258e8c46edb236\",\"statusCode\":\"200\"}}],\"http\":{\"response\":{\"status\":200}},\"annotations\":{\"before_request_line\":\"359\",\"application_name\":\"s-poi-api\",\"after_request_file\":\"implementation/fuel-impl.xml\",\"after_request_flow\":\"fuel-impl:buildFuelStationPriceMap\",\"correlation_id\":\"1-636e106f-861f40cdbb2d211420b7e620\",\"after_request_line\":\"369\",\"before_request_flow\":\"fuel-impl:buildFuelStationPriceMap\",\"before_request_file\":\"implementation/fuel-impl.xml\"}},{\"name\":\"endLogEntry\",\"id\":\"5278d39fcc0da5d1\",\"start_time\":1.668156409746E9,\"end_time\":1.668156409746E9,\"annotations\":{\"traceId\":\"8913443fd396fea8\",\"statusCode\":\"200\"}}],\"http\":{\"request\":{\"method\":\"GET\",\"url\":\"e-arevo-api/api/app-config?flavour=tfnsw\",\"user_agent\":\"iOS 2.2.6.1\"},\"response\":{\"status\":200}},\"annotations\":{\"end_flow\":\"interface-main\",\"application_name\":\"e-arevo-api\",\"end_file\":\"interface.xml\",\"correlation_id\":\"1-635b5654-814fa4dedafd41683fa03ffa\",\"start_line\":\"21\",\"start_file\":\"interface.xml\",\"start_flow\":\"interface-main\",\"end_line\":\"27\"}}";
		
		System.out.println("Output:");
		System.out.println(convertedStr);
		
		System.out.println("Expected:");
		System.out.println(expectedStr);
		
		convertedStr = convertedStr.replaceAll("\\\"id\\\":\\\"[a-z0-9]+\\\"", "<id>");
		expectedStr = expectedStr.replaceAll("\\\"id\\\":\\\"[a-z0-9]+\\\"", "<id>");
		
		System.out.println("Translated converted:");
		System.out.println(convertedStr);

		assertEquals(convertedStr, expectedStr);
	
	}
}