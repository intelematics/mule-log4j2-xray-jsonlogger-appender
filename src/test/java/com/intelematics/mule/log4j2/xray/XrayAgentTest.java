package com.intelematics.mule.log4j2.xray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.intelematics.mule.log4j2.xray.model.JsonLoggerTransaction;

public class XrayAgentTest {

	XrayAgent agent;
	
	@BeforeEach
	public void before() {
		agent = new XrayAgent("ap-southeast-1");
	}
	
	@Test
	public void getReadyItemsTest_noItems() {
		agent.getReadyItems();
		
		assertEquals(agent.getProcessingQueueCount(), 0);
		assertEquals(agent.getReadyItemCount(), 0);
	}

	@Test
	public void getReadyItemsTest_oneReadyItem() {
		JsonLoggerTransaction transaction = mock(JsonLoggerTransaction.class);
		when(transaction.isReadyToSend()).thenReturn(true);
		agent.processTransaction(transaction);
		

		agent.getReadyItems();
		
		assertEquals(agent.getProcessingQueueCount(), 0);
		assertEquals(agent.getReadyItemCount(), 1);
	}

	@Test
	public void getReadyItemsTest_oneNotReadyItem() {
		JsonLoggerTransaction transaction = mock(JsonLoggerTransaction.class);
		when(transaction.isReadyToSend()).thenReturn(false);
		agent.processTransaction(transaction);
		

		agent.getReadyItems();
		
		assertEquals(agent.getProcessingQueueCount(), 1);
		assertEquals(agent.getReadyItemCount(), 0);
	}
	
	@Test
	public void getReadyItemsTest_AFewItems() {
		JsonLoggerTransaction transaction = mock(JsonLoggerTransaction.class);
		when(transaction.isReadyToSend()).thenReturn(false);
		agent.processTransaction(transaction);


		transaction = Mockito.mock(JsonLoggerTransaction.class);
		when(transaction.isReadyToSend()).thenReturn(true);
		agent.processTransaction(transaction);

		transaction = Mockito.mock(JsonLoggerTransaction.class);
		when(transaction.isReadyToSend()).thenReturn(true);
		agent.processTransaction(transaction);
		
		
		agent.getReadyItems();
		
		assertEquals(agent.getProcessingQueueCount(), 1);
		assertEquals(agent.getReadyItemCount(), 2);
	}
}
