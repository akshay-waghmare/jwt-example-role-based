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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Service;

import com.devglan.dao.CricketDataDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CricketDataService implements ApplicationListener<BrokerAvailabilityEvent> {

	private static Log logger = LogFactory.getLog(CricketDataService.class);

	private final MessageSendingOperations<String> messagingTemplate;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();

	private final Map<String, CricketDataDTO> lastUpdatedDataMap = new ConcurrentHashMap<>();


	/*
	 * @Autowired private LiveMatchRepository liveMatchRepository;
	 */

	@Autowired
	public CricketDataService(MessageSendingOperations<String> messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		this.brokerAvailable.set(event.isBrokerAvailable());
	}

	public void sendCricketData(String url, Map<String, Object> dataToSend) {
		 // Extracting the desired part from the URL
	    String[] parts = url.split("/");
	    String match = parts[parts.length - 2]; // Get the second-to-last part of the URL
	    

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
				messagingTemplate.convertAndSend("/topic/cricket." + match + "." + key, jsonField);
			}
		}

	}

	public void notifyNewMatch(String url) {
		messagingTemplate.convertAndSend("/topic/live-matches", url);

	}

	public void notifyMatchStatusChange(String url, String status) {
	    Map<String, Object> notification = new HashMap<>();
	    notification.put("url", url);
	    notification.put("status", status);

	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
	        String jsonPayload = objectMapper.writeValueAsString(notification);
	        if (this.brokerAvailable.get()) {
	            messagingTemplate.convertAndSend("/topic/live-matches", jsonPayload); 
	        }
	    } catch (JsonProcessingException e) {
	        logger.error("Error converting match status notification to JSON", e);
	    }
	}

	
	 // Method to set the last updated data for a specific URL
    public synchronized void setLastUpdatedData(String url, CricketDataDTO data) {
        lastUpdatedDataMap.put(url, data);
    }

    // Method to get the last updated data for a specific URL
    public synchronized CricketDataDTO getLastUpdatedData(String url) {
        return lastUpdatedDataMap.get(url);
    }
    
    
    
    

}
