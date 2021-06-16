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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;


@Service
public class FootballOddsService implements ApplicationListener<BrokerAvailabilityEvent> {

	private static Log logger = LogFactory.getLog(FootballOddsService.class);

	private final MessageSendingOperations<String> messagingTemplate;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();


	@Autowired
	public FootballOddsService(MessageSendingOperations<String> messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		this.brokerAvailable.set(event.isBrokerAvailable());
	}

	@Scheduled(fixedDelay=30000)
	public void sendQuotes() {
			String inplayData = this.getOddsInplay();
			
			if (this.brokerAvailable.get()) {
				//sending payload quote to destination "/topic/price.inplay"
				this.messagingTemplate.convertAndSend("/topic/football.inplay", inplayData);
			}
		}
		
		


	

		public String getOddsInplay() {
			OkHttpClient client = new OkHttpClient();
			 ObjectMapper objectMapper = new ObjectMapper();
			 Request request = new Request.Builder()
						.url("https://football-betting-odds1.p.rapidapi.com/provider1/live/inplaying")
						.get()
						.addHeader("x-rapidapi-host", "football-betting-odds1.p.rapidapi.com")
						.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e")
						.build();

			//implement global error controller advice
			try {
				ResponseBody responseBody = client.newCall(request).execute().body();
				System.out.println(request.toString());


				return responseBody.string();
			} catch (IOException e) {
				logger.info("failed fetching events" + e.getMessage());
			}
			return null;
		}


	}

