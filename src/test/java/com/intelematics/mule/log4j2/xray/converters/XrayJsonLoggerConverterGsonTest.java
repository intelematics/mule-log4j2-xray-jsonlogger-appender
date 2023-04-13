package com.intelematics.mule.log4j2.xray.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class XrayJsonLoggerConverterGsonTest extends XrayJsonLoggerConverterData {

	@Test
	public void testConvert() throws JsonMappingException, JsonProcessingException {
		XrayJsonLoggerConverterImpl.rand = null;
		
		String convertedStr = new XrayJsonLoggerConverterImpl().convert(getBasicTransaction());
		String expectedStr = "{\"trace_id\":\"1-635b5654-814fa4dedafd41683fa03ffa\",\"parent_id\":\"8913443fd396fea8\",\"name\":\"sandbox:e-arevo-api:interface-main\",\"id\":\"25bde940413fd6aa\",\"start_time\":1.666921643944E9,\"end_time\":1.668156409746E9,\"in_progress\":false,\"error\":false,\"fault\":false,\"namespace\":\"aws\",\"http\":{\"request\":{\"method\":\"GET\",\"url\":\"e-arevo-api/api/app-config?flavour\\u003dtfnsw\",\"user_agent\":\"iOS 2.2.6.1\"},\"response\":{\"status\":200}},\"annotations\":{\"end_flow\":\"interface-main\",\"application_name\":\"e-arevo-api\",\"end_file\":\"interface.xml\",\"correlation_id\":\"1-635b5654-814fa4dedafd41683fa03ffa\",\"start_line\":\"21\",\"start_file\":\"interface.xml\",\"start_flow\":\"interface-main\",\"end_line\":\"27\"},\"subsegments\":[{\"name\":\"startLogEntry\",\"id\":\"958a5bc296f2d04f\",\"start_time\":1.666921643944E9,\"end_time\":1.666921643944E9,\"in_progress\":false,\"error\":false,\"fault\":false,\"namespace\":\"aws\",\"http\":{},\"annotations\":{\"method\":\"GET\",\"payload\":\"null\",\"session\":\"{\\\"deviceOS\\\":\\\"iOS\\\",\\\"buildVersion\\\":\\\"2.2.6.1\\\"}\",\"url\":\"/api/app-config?flavour\\u003dtfnsw\"},\"subsegments\":[]},{\"name\":\"GET /getSitePrices by state\",\"id\":\"78258e8c46edb236\",\"start_time\":1.668157564348E9,\"end_time\":1.668157565375E9,\"in_progress\":false,\"error\":false,\"fault\":false,\"namespace\":\"remote\",\"http\":{\"response\":{\"status\":200}},\"annotations\":{\"before_request_line\":\"359\",\"application_name\":\"s-poi-api\",\"after_request_file\":\"implementation/fuel-impl.xml\",\"after_request_flow\":\"fuel-impl:buildFuelStationPriceMap\",\"correlation_id\":\"1-636e106f-861f40cdbb2d211420b7e620\",\"after_request_line\":\"369\",\"before_request_flow\":\"fuel-impl:buildFuelStationPriceMap\",\"before_request_file\":\"implementation/fuel-impl.xml\"},\"subsegments\":[{\"name\":\"before request\",\"id\":\"6bc40f3caa80f330\",\"start_time\":1.668157564348E9,\"end_time\":1.668157564348E9,\"in_progress\":false,\"error\":false,\"fault\":false,\"namespace\":\"aws\",\"http\":{},\"annotations\":{\"requestQueryParams\":\"{\\\"countryId\\\":\\\"21\\\",\\\"geoRegionLevel\\\":\\\"3\\\",\\\"geoRegionId\\\":\\\"3\\\"}\"},\"subsegments\":[]},{\"name\":\"after request\",\"id\":\"5c3f1af16b1d9901\",\"start_time\":1.668157565375E9,\"end_time\":1.668157565375E9,\"in_progress\":false,\"error\":false,\"fault\":false,\"namespace\":\"aws\",\"http\":{},\"annotations\":{\"traceId\":\"78258e8c46edb236\",\"statusCode\":\"200\"},\"subsegments\":[]}]},{\"name\":\"endLogEntry\",\"id\":\"d702719e698c5a44\",\"start_time\":1.668156409746E9,\"end_time\":1.668156409746E9,\"in_progress\":false,\"error\":false,\"fault\":false,\"namespace\":\"aws\",\"http\":{},\"annotations\":{\"traceId\":\"8913443fd396fea8\",\"statusCode\":\"200\"},\"subsegments\":[]}]}";
		
		System.out.println("Output:");
		System.out.println(convertedStr);
		
		System.out.println("Expected:");
		System.out.println(expectedStr);
		
		assertEquals(convertedStr, expectedStr);
	
	}
}