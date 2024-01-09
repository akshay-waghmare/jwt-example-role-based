package com.devglan.websocket.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.weaver.patterns.ThisOrTargetAnnotationPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

@Service
public class TennisOddsService implements ApplicationListener<BrokerAvailabilityEvent> {
	

	private static Log logger = LogFactory.getLog(TennisOddsService.class);

	private final MessageSendingOperations<String> messagingTemplate;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();


	@Autowired
	public TennisOddsService(MessageSendingOperations<String> messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		this.brokerAvailable.set(event.isBrokerAvailable());
		logger.info(event.isBrokerAvailable());
	}

	@Scheduled(fixedDelay=6000)
	public void sendQuotes() {
			String inplayData = this.getOddsInplay();
		//System.out.println(inplayData.toString());
		if (this.brokerAvailable.get()) {
			//sending payload quote to destination "/topic/price.inplay"
				System.out.println("*****nirmal****");
				
				this.messagingTemplate.convertAndSend("/topic/tennis.inplay", inplayData);
			}
		}


	

	public String getOddsInplay() {
		OkHttpClient client = new OkHttpClient();
		 ObjectMapper objectMapper = new ObjectMapper();
		 Request request = new Request.Builder()
				 .url("https://api.sofascore.com/api/v1/sport/tennis/events/live")
					.get()
					.build();

		 System.out.println("valvvvvvvvvvvvvvi");
		//implement global error controller advice
		try {
			ResponseBody responseBody = client.newCall(request).execute().body();
			System.out.println(request.toString());
			//System.out.println("nnnnnnnnnnnnnnnnnnnnnn");
			
			return responseBody.string();
		
		} catch (IOException e) {
			logger.info("failed fetching events" + e.getMessage());
		}
		return null;
		

			
		}

}
