package com.intelematics.mule.log4j2.xray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.intelematics.mule.log4j2.xray.model.JsonLoggerTransaction;

@ExtendWith(MockitoExtension.class)
public class XrayLogReceiverTest {
	
	XrayLogReceiver receiver;
	@Mock
	XrayAgent agent;

	@Mock
	LogEvent event;
	@Mock
	Message message;
	
	@BeforeEach
	public void setup() {
		receiver = XrayLogReceiver.getInstance(agent);
		when(event.getMessage()).thenReturn(message);
	}

	@Test
	public void sendXrayEvent_basicTransaction() throws JsonMappingException, JsonProcessingException {
		
		final ArgumentCaptor<JsonLoggerTransaction> captor = ArgumentCaptor.forClass(JsonLoggerTransaction.class);
		
		when(message.getFormattedMessage()).thenReturn(START_MESSAGE);
		
		receiver.sendXrayEvent(event);
		
		
		verify(agent).processTransaction(captor.capture());
		JsonLoggerTransaction trans = captor.getValue();
		
		assertNotNull(trans.getStart());
		assertEquals(trans.getExceptions().size(), 0);
		assertEquals(trans.getRequestTransactions().size(), 0);
		assertNull(trans.getEnd());
		
		assertEquals(trans.getFirstEvent(), trans.getStart());
		assertEquals(trans.getLastEvent(), trans.getStart());
		
		assertEquals(trans.getCorrelationId(), "1-6363e898-1e954194a9b108403dccdc32");
		assertEquals(trans.getLastRecordedActivity().getEpochSecond(),1667492071);
	}
	
	@Test
	public void sendXrayEvent_endOnly() throws JsonMappingException, JsonProcessingException {
		
		final ArgumentCaptor<JsonLoggerTransaction> captor = ArgumentCaptor.forClass(JsonLoggerTransaction.class);
		
		when(message.getFormattedMessage()).thenReturn(END_MESSAGE);
		
		receiver.sendXrayEvent(event);
		
		
		verify(agent).processTransaction(captor.capture());
		JsonLoggerTransaction trans = captor.getValue();
		
		assertNull(trans.getStart());
		assertEquals(trans.getExceptions().size(), 0);
		assertEquals(trans.getRequestTransactions().size(), 0);
		assertNotNull(trans.getEnd());
		
		assertEquals(trans.getFirstEvent(), trans.getEnd());
		assertEquals(trans.getLastEvent(), trans.getEnd());
		
		assertEquals(trans.getCorrelationId(), "1-6363e898-1e954194a9b108403dccdc32");
		assertEquals(trans.getLastRecordedActivity().getEpochSecond(),1667492073);
	}
	
	@Test
	public void sendXrayEvent_simpleTransaction() throws JsonMappingException, JsonProcessingException {
		
		final ArgumentCaptor<JsonLoggerTransaction> captor = ArgumentCaptor.forClass(JsonLoggerTransaction.class);
		
		when(message.getFormattedMessage()).thenReturn(END_MESSAGE);
		receiver.sendXrayEvent(event);

		verify(agent).processTransaction(captor.capture());
		JsonLoggerTransaction trans = captor.getValue();

		when(message.getFormattedMessage()).thenReturn(START_MESSAGE);
		receiver.sendXrayEvent(event);
		
		
		assertNotNull(trans.getStart());
		assertEquals(trans.getExceptions().size(), 0);
		assertEquals(trans.getRequestTransactions().size(), 0);
		assertNotNull(trans.getEnd());
		
		assertEquals(trans.getFirstEvent(), trans.getStart());
		assertEquals(trans.getLastEvent(), trans.getEnd());
		
		assertEquals(trans.getCorrelationId(), "1-6363e898-1e954194a9b108403dccdc32");
		assertEquals(trans.getLastRecordedActivity().getEpochSecond(),1667492071);
	}
	
	static String START_MESSAGE = "{\n"
	          + "    \"correlationId\": \"1-6363e898-1e954194a9b108403dccdc32\",\n"
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
	          + "    \"timestamp\": \"2022-11-03T16:14:31.212Z\",\n"
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
	          + "}",
	          
	          END_MESSAGE = "{\n"
	          		+ "  \"correlationId\" : \"1-6363e898-1e954194a9b108403dccdc32\",\n"
	          		+ "  \"message\" : \"Pricing Prefill\",\n"
	          		+ "  \"tracePoint\" : \"END\",\n"
	          		+ "  \"priority\" : \"INFO\",\n"
	          		+ "  \"elapsed\" : 79059,\n"
	          		+ "  \"locationInfo\" : {\n"
	          		+ "    \"lineInFile\" : \"54\",\n"
	          		+ "    \"component\" : \"json-logger:logger\",\n"
	          		+ "    \"fileName\" : \"interface-scheduler.xml\",\n"
	          		+ "    \"rootContainer\" : \"interface-scheduler:pricing\"\n"
	          		+ "  },\n"
	          		+ "  \"timestamp\" : \"2022-11-03T16:14:33.212Z\",\n"
	          		+ "  \"content\" : { },\n"
	          		+ "  \"applicationName\" : \"s-poi-api\",\n"
	          		+ "  \"applicationVersion\" : \"1.0.0\",\n"
	          		+ "  \"environment\" : \"local\",\n"
	          		+ "  \"threadName\" : \"[MuleRuntime].uber.185: [mule-s-poi-api].interface-scheduler:pricing.BLOCKING @75dd5900\"\n"
	          		+ "}";
	
}
