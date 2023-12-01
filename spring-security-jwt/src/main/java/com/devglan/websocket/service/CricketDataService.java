/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devglan.websocket.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CricketDataService implements ApplicationListener<BrokerAvailabilityEvent> {

	private static Log logger = LogFactory.getLog(CricketDataService.class);

	private final MessageSendingOperations<String> messagingTemplate;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();

	@Autowired
	public CricketDataService(MessageSendingOperations<String> messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		this.brokerAvailable.set(event.isBrokerAvailable());
	}

	public void sendCricketData(Map<String, Object> dataToSend) {

		ObjectMapper objectMapper = new ObjectMapper();
		for (Map.Entry<String, Object> entry : dataToSend.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// Create a JSON representation of the field and its value
			String jsonField = null;
			try {
				jsonField = objectMapper.writeValueAsString(Collections.singletonMap(key, value));
			} catch (JsonProcessingException e) {
				logger.info("error in writing object");
			}
			if (logger.isTraceEnabled()) {
				logger.info("Sending cricketData " + jsonField);
			}
			// Sending payload (jsonField) to the WebSocket topic (/topic/cricket.{key})
			if (this.brokerAvailable.get()) {
				messagingTemplate.convertAndSend("/topic/cricket." + key, jsonField);
			}
		}

	}

}
