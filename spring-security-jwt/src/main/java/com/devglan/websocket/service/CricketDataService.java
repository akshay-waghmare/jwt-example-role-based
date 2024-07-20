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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Service;

import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.OversData;
import com.devglan.dao.SessionOverData;
import com.devglan.model.Bets;
import com.devglan.model.CricketDataEntity;
import com.devglan.model.TeamSessionData;
import com.devglan.repository.CricketDataRepository;
import com.devglan.repository.OversDataRepository;
import com.devglan.repository.SessionOverDataRepository;
import com.devglan.repository.TeamSessionDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CricketDataService implements ApplicationListener<BrokerAvailabilityEvent> {

	private static Log logger = LogFactory.getLog(CricketDataService.class);

	private final MessageSendingOperations<String> messagingTemplate;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();

	private final CricketDataRepository cricketDataRepository;
    
    @Autowired
    private OversDataRepository oversDataRepository;

    @Autowired
	private TeamSessionDataRepository teamSessionDataRepository;
    
    @Autowired
    private SessionOverDataRepository SessionOverDataRepository;
	


	/*
	 * @Autowired private LiveMatchRepository liveMatchRepository;
	 */

	@Autowired
	public CricketDataService(MessageSendingOperations<String> messagingTemplate, CricketDataRepository cricketDataRepository) {
		this.messagingTemplate = messagingTemplate;
        this.cricketDataRepository = cricketDataRepository;
		
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
	
	public void notifyBetStatus(Bets bet) {
		messagingTemplate.convertAndSend("/topic/bet-status", bet);

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
        //lastUpdatedDataMap.put(url, data);
        CricketDataEntity entity = convertDtoToEntity(url, data);
        cricketDataRepository.save(entity);
    }

    // Method to get the last updated data for a specific URL
    @org.springframework.transaction.annotation.Transactional
    public synchronized CricketDataDTO getLastUpdatedData(String url) {
        //return lastUpdatedDataMap.get(url);
    	CricketDataEntity entity = cricketDataRepository.findByUrlWithTeamWiseSessionData(url);
    	if (entity != null) {
    		Hibernate.initialize(entity.getMatchOdds());
            Hibernate.initialize(entity.getTeamWiseSessionData()); // Explicitly initialize
        }
        return convertEntityToDto(entity);
    	
    	
    }
    
    @org.springframework.transaction.annotation.Transactional
    public CricketDataDTO getCricData(String url) {
        //return lastUpdatedDataMap.get(url);
    	CricketDataEntity entity = cricketDataRepository.findByUrlContaining(url);
    	if (entity != null) {
    		Hibernate.initialize(entity.getMatchOdds());
            Hibernate.initialize(entity.getTeamWiseSessionData()); // Explicitly initialize
        }
        return convertEntityToDto(entity);
    	
    	
    }
    
    @Transactional
    private CricketDataEntity convertDtoToEntity(String url, CricketDataDTO data) {
        CricketDataEntity entity = new CricketDataEntity();
        entity.setUrl(url);
        entity.setMatchOdds(data.getMatchOdds());
        entity.setTeamOdds(data.getTeamOdds());
        entity.setBattingTeamName(data.getBattingTeamName());
        entity.setOver(data.getOver());
        entity.setScore(data.getScore());
        entity.setCurrentBall(data.getCurrentBall());
        entity.setRunsOnBall(data.getRunsOnBall());
        entity.setFavTeam(data.getFavTeam());
        entity.setSessionOdds(data.getSessionOdds());
        entity.setCurrentRunRate(data.getCurrentRunRate());
        entity.setFinalResultText(data.getFinalResultText());
        entity.setUpdatedTimeStamp(System.currentTimeMillis());
//        entity.setOversData(data.getOversData());
        //entity.setTossWonCountry(data.getTossWonCountry());
        //entity.setBatOrBallSelected(data.getBatOrBallSelected());
        //entity.setUpdatedTimeStamp());
     // Save each OversData
        List<OversData> oversDataList = data.getOversData();
        if (oversDataList != null) {
            List<OversData> savedOversDataList = new ArrayList<>();
            for (OversData oversData : oversDataList) {
                OversData savedOversData = oversDataRepository.save(oversData);
                savedOversDataList.add(savedOversData);
            }
            entity.setOversData(savedOversDataList);
        }
        
     // Update each TeamSessionData
        Map<String, List<SessionOverData>> teamWiseSessionData = data.getTeamWiseSessionData();
        if (teamWiseSessionData != null) {
            List<TeamSessionData> savedTeamSessionDataList = new ArrayList<>();
            for (Map.Entry<String, List<SessionOverData>> entry : teamWiseSessionData.entrySet()) {
                TeamSessionData teamSessionData = teamSessionDataRepository.findByTeamNameAndCricketDataEntity(entry.getKey(), entity);
                if (teamSessionData == null) {
                    teamSessionData = new TeamSessionData();
                    teamSessionData.setTeamName(entry.getKey());
                    teamSessionData.setCricketDataEntity(entity);  // Set the reference to the parent entity
                }
                List<SessionOverData> sessionOverDataList = new ArrayList<>();
                for (SessionOverData sessionOverData : entry.getValue()) {
                    sessionOverData = SessionOverDataRepository.save(sessionOverData);  // Save the SessionOverData first
                    sessionOverDataList.add(sessionOverData);
                }
                teamSessionData.setSessionOverDataList(sessionOverDataList);
                teamSessionDataRepository.save(teamSessionData);  // Save the TeamSessionData
                savedTeamSessionDataList.add(teamSessionData);
            }
            entity.setTeamWiseSessionData(savedTeamSessionDataList);
        }
        
        return entity;
    }
    
    @org.springframework.transaction.annotation.Transactional
    public CricketDataDTO convertEntityToDto(CricketDataEntity entity) {
        if (entity == null) {
            return null;
        }
        CricketDataDTO data = new CricketDataDTO();
        data.setMatchOdds(entity.getMatchOdds());
        data.setTeamOdds(entity.getTeamOdds());
        data.setBattingTeamName(entity.getBattingTeamName());
        data.setOver(entity.getOver());
        data.setScore(entity.getScore());
        data.setCurrentBall(entity.getCurrentBall());
        data.setRunsOnBall(entity.getRunsOnBall());
        data.setFavTeam(entity.getFavTeam());
        data.setSessionOdds(entity.getSessionOdds());
        data.setCurrentRunRate(entity.getCurrentRunRate());
        data.setFinalResultText(entity.getFinalResultText());
        data.setOversData(entity.getOversData());
        data.setUpdatedTimeStamp(entity.getUpdatedTimeStamp());
        //data.setTossWonCountry(entity.getTossWonCountry());
        //data.setBatOrBallSelected(entity.getBatOrBallSelected());
        //data.setUpdatedTimeStamp(entity.getUpdatedTimeStamp());
        
        // Convert TeamSessionData to Map
        List<TeamSessionData> teamSessionDataList = entity.getTeamWiseSessionData();
        if (teamSessionDataList != null) {
            Map<String, List<SessionOverData>> teamWiseSessionData = new HashMap<>();
            for (TeamSessionData teamSessionData : teamSessionDataList) {
                teamWiseSessionData.put(teamSessionData.getTeamName(), teamSessionData.getSessionOverDataList());
            }
            data.setTeamWiseSessionData(teamWiseSessionData);
        }
        return data;
    }   

}
