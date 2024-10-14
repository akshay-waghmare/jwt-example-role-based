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
import java.util.Map.Entry;
import java.util.Optional;
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
import com.devglan.dao.MatchInfoEntity;
import com.devglan.dao.OversData;
import com.devglan.dao.PlayingXIEntity;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.SessionOverData;
import com.devglan.model.Bets;
import com.devglan.model.CricketDataEntity;
import com.devglan.model.PlayingXI;
import com.devglan.model.TeamSessionData;
import com.devglan.repository.CricketDataRepository;
import com.devglan.repository.MatchInfoRepository;
import com.devglan.repository.OversDataRepository;
import com.devglan.repository.SessionOddsRepository;
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
    
    @Autowired
    private MatchInfoRepository matchInfoRepository;
    
    @Autowired
    private SessionOddsRepository sessionOddsRepository;
	


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
        
        if (dataContainsMatchInfo(data)) {
            MatchInfoEntity matchInfoEntity = convertDtoToMatchInfoEntity(data);
            matchInfoRepository.save(matchInfoEntity);
        }
    }

    // Method to get the last updated data for a specific URL
    @org.springframework.transaction.annotation.Transactional
    public synchronized CricketDataDTO getLastUpdatedData(String url) {
        //return lastUpdatedDataMap.get(url);
    	CricketDataEntity entity = cricketDataRepository.findByUrlWithTeamWiseSessionData(url);
        MatchInfoEntity matchInfoEntity = matchInfoRepository.findById(url).orElse(null); // Get MatchInfoEntity by URL

        if (entity == null && matchInfoEntity == null) {
            return null;
        }
        

    	if (entity != null) {
    		Hibernate.initialize(entity.getMatchOdds());
            Hibernate.initialize(entity.getTeamWiseSessionData()); // Explicitly initialize
        }
    	CricketDataDTO data = convertEntityToDto(entity);
    	if (matchInfoEntity != null) {
            data = mergeMatchInfoToCricketDataDTO(matchInfoEntity, data);
        }
        return data;
    	
    	
    }
    
 // Convert MatchInfoEntity to CricketDataDTO and merge it
    private CricketDataDTO mergeMatchInfoToCricketDataDTO(MatchInfoEntity matchInfoEntity, CricketDataDTO data) {
        if (matchInfoEntity != null) {
            data.setUrl(matchInfoEntity.getUrl());
            data.setMatchDate(matchInfoEntity.getMatchDate());
            data.setVenue(matchInfoEntity.getVenue());
            data.setMatchName(matchInfoEntity.getMatchName());
            data.setTossInfo(matchInfoEntity.getTossInfo());
            data.setTeamComparison(matchInfoEntity.getTeamComparison());
            data.setTeamForm(matchInfoEntity.getTeamForm());
            data.setVenueStats(matchInfoEntity.getVenueStats());
         // Transform List<PlayingXIEntity> to Map<String, List<PlayingXI>>
            if (matchInfoEntity.getPlayingXI() != null && !matchInfoEntity.getPlayingXI().isEmpty()) {
                Map<String, Set<PlayingXI>> convertedPlayingXIMap = new HashMap<>();
                for (PlayingXIEntity playingXIEntity : matchInfoEntity.getPlayingXI()) {
                    String teamName = playingXIEntity.getTeamName();
                    PlayingXI playingXI = new PlayingXI();
                    playingXI.setPlayerName(playingXIEntity.getPlayerName());
                    playingXI.setPlayerRole(playingXIEntity.getPlayerRole());

                    convertedPlayingXIMap.computeIfAbsent(teamName, k -> new HashSet<>()).add(playingXI);
                }
                data.setPlayingXI(convertedPlayingXIMap);
            }
        }
        return data;
    }
    
    private MatchInfoEntity convertDtoToMatchInfoEntity(CricketDataDTO dto) {
        MatchInfoEntity entity = new MatchInfoEntity();
        
        if (dto.getUrl() != null) {
            entity.setUrl(dto.getUrl());
        }
        if (dto.getMatchDate() != null) {
            entity.setMatchDate(dto.getMatchDate());
        }
        if (dto.getVenue() != null) {
            entity.setVenue(dto.getVenue());
        }
        if (dto.getMatchName() != null) {
            entity.setMatchName(dto.getMatchName());
        }
        if (dto.getTossInfo() != null) {
            entity.setTossInfo(dto.getTossInfo());
        }
        if (dto.getTeamComparison() != null) {
            entity.setTeamComparison(dto.getTeamComparison());
        }
        if (dto.getTeamForm() != null) {
            entity.setTeamForm(dto.getTeamForm());
        }
        if (dto.getVenueStats() != null) {
            entity.setVenueStats(dto.getVenueStats());
        }
     // Convert Map<String, List<PlayingXI>> to List<PlayingXIEntity>
        if (dto.getPlayingXI() != null && !dto.getPlayingXI().isEmpty()) {
            Set<PlayingXIEntity> playingXIEntityList = new HashSet<>();
            for (Entry<String, Set<PlayingXI>> entry : dto.getPlayingXI().entrySet()) {
                String teamName = entry.getKey();
                Set<PlayingXI> playingXIList = entry.getValue();

                for (PlayingXI playingXI : playingXIList) {
                    PlayingXIEntity playingXIEntity = new PlayingXIEntity();
                    playingXIEntity.setTeamName(teamName);
                    playingXIEntity.setPlayerName(playingXI.getPlayerName());
                    playingXIEntity.setPlayerRole(playingXI.getPlayerRole());
                    playingXIEntityList.add(playingXIEntity);
                }
            }
            entity.setPlayingXI(playingXIEntityList);
        }

        return entity;
    }
    
	private CricketDataDTO convertEntityToDto(MatchInfoEntity entity) {
		CricketDataDTO dto = new CricketDataDTO();

		if (entity != null) {
			if (entity.getUrl() != null) {
				dto.setUrl(entity.getUrl());
			}
			if (entity.getMatchDate() != null) {
				dto.setMatchDate(entity.getMatchDate());
			}
			if (entity.getVenue() != null) {
				dto.setVenue(entity.getVenue());
			}
			if (entity.getMatchName() != null) {
				dto.setMatchName(entity.getMatchName());
			}
			if (entity.getTossInfo() != null) {
				dto.setTossInfo(entity.getTossInfo());
			}
			if (entity.getTeamComparison() != null) {
				dto.setTeamComparison(entity.getTeamComparison());
			}
			if (entity.getTeamForm() != null) {
				dto.setTeamForm(entity.getTeamForm());
			}
			if (entity.getVenueStats() != null) {
				dto.setVenueStats(entity.getVenueStats());
			}
			// Transform List<PlayingXIEntity> to Map<String, List<PlayingXI>>
			if (entity.getPlayingXI() != null && !entity.getPlayingXI().isEmpty()) {
				Map<String, Set<PlayingXI>> playingXIMap = new HashMap<>();
				for (PlayingXIEntity playingXIEntity : entity.getPlayingXI()) {
					String teamName = playingXIEntity.getTeamName();
					PlayingXI playingXI = new PlayingXI();
					playingXI.setPlayerName(playingXIEntity.getPlayerName());
					playingXI.setPlayerRole(playingXIEntity.getPlayerRole());

					// Use a HashSet instead of ArrayList for the Set<PlayingXI>
					playingXIMap.computeIfAbsent(teamName, k -> new HashSet<>()).add(playingXI);
				}
			}
		}

		return dto;
	}
    
    private boolean dataContainsMatchInfo(CricketDataDTO data) {
        // Check if the DTO contains match info data that needs to be saved
        return true;
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
		// Handle multiple session odds
		// Handle multiple session odds
		// Update session odds logic (without immediate save)
		if (data.getSessionOddsList() != null && !data.getSessionOddsList().isEmpty()) {
			Set<SessionOdds> sessionOddsSet = new HashSet<>();

			for (SessionOdds sessionOdds : data.getSessionOddsList()) {
				Optional<SessionOdds> existingSessionOdds = sessionOddsRepository
						.findBySessionOverAndCricketDataEntityUrl(sessionOdds.getSessionOver(), entity.getUrl());

				if (existingSessionOdds.isPresent()) {
					// Update existing session odds
					SessionOdds existing = existingSessionOdds.get();
					existing.setSessionBackOdds(sessionOdds.getSessionBackOdds());
					existing.setSessionLayOdds(sessionOdds.getSessionLayOdds());
					sessionOddsSet.add(existing); // Add updated session odds to the set
				} else {
					// Create new session odds
					sessionOdds.setCricketDataEntity(entity);
					sessionOddsSet.add(sessionOdds); // Add new session odds to the set
				}
			}
			entity.setSessionOddsSet(sessionOddsSet);
		}

		entity.setCurrentRunRate(data.getCurrentRunRate());
		entity.setFinalResultText(data.getFinalResultText());

		entity.setLastOddsUpdatedTimeStamp(data.getLastUpdated());

//        entity.setOversData(data.getOversData());
		// entity.setTossWonCountry(data.getTossWonCountry());
		// entity.setBatOrBallSelected(data.getBatOrBallSelected());
		// entity.setUpdatedTimeStamp());
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
				TeamSessionData teamSessionData = teamSessionDataRepository
						.findByTeamNameAndCricketDataEntity(entry.getKey(), entity);
				if (teamSessionData == null) {
					teamSessionData = new TeamSessionData();
					teamSessionData.setTeamName(entry.getKey());
					teamSessionData.setCricketDataEntity(entity); // Set the reference to the parent entity
				}
				List<SessionOverData> sessionOverDataList = new ArrayList<>();
				for (SessionOverData sessionOverData : entry.getValue()) {
					sessionOverData = SessionOverDataRepository.save(sessionOverData); // Save the SessionOverData first
					sessionOverDataList.add(sessionOverData);
				}
				teamSessionData.setSessionOverDataList(sessionOverDataList);
				teamSessionDataRepository.save(teamSessionData); // Save the TeamSessionData
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
		// Convert multiple session odds
		if (entity.getSessionOddsSet() != null && !entity.getSessionOddsSet().isEmpty()) {
			data.setSessionOddsList(entity.getSessionOddsSet());
		}
		data.setCurrentRunRate(entity.getCurrentRunRate());
		data.setFinalResultText(entity.getFinalResultText());
		data.setOversData(entity.getOversData());
		data.setUpdatedTimeStamp(entity.getUpdatedTimeStamp());
		if (entity.getLastOddsUpdatedTimeStamp() == null) {
			data.setLastUpdated(0l);
		} else {
			data.setLastUpdated(entity.getLastOddsUpdatedTimeStamp());
		}
		// data.setTossWonCountry(entity.getTossWonCountry());
		// data.setBatOrBallSelected(entity.getBatOrBallSelected());
		// data.setUpdatedTimeStamp(entity.getUpdatedTimeStamp());

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
