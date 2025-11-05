package com.devglan.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.devglan.dao.CricketDataDTO;
import com.devglan.model.LiveMatch;
import com.devglan.repository.LiveMatchRepository;
import com.devglan.service.LiveMatchService;
import com.devglan.websocket.service.CricketDataService;

@Service
public class LiveMatchServiceImpl implements LiveMatchService {

	private static final Logger logger = LoggerFactory.getLogger(LiveMatchServiceImpl.class);

	private final LiveMatchRepository liveMatchRepository;
	private final CricketDataService cricketDataService;
	private final RestTemplate restTemplate;

	@Value("${stop.scrape.url:http://localhost:5000/stop-scrape}")
	private String stopScrapeUrl;

	@Autowired
	public LiveMatchServiceImpl(LiveMatchRepository liveMatchRepository, CricketDataService cricketDataService,
			RestTemplate restTemplate) {
		this.liveMatchRepository = liveMatchRepository;
		this.cricketDataService = cricketDataService;
		this.restTemplate = restTemplate;
	}

	public void syncLiveMatches(String[] urls) {
		try {
			logger.info("Starting the sync live matches logic.");

			List<String> urlList = Arrays.asList(urls);
			List<LiveMatch> allNotDeletedMatches = liveMatchRepository.findByDeletionAttemptsLessThanAndIsDeletedFalse(Integer.valueOf(2));

			for (LiveMatch match : allNotDeletedMatches) {
				if (!urlList.contains(match.getUrl())) {
					match.setDeletionAttempts(match.getDeletionAttempts() + 1);
					
					if (match.getDeletionAttempts() >= 2) {
						CricketDataDTO lastUpdatedData = cricketDataService
								.getLastUpdatedData(appendBaseUrl(match.getUrl()));
						if (lastUpdatedData != null) {
							match.setLastKnownState(lastUpdatedData.getCurrentBall());
						}
						match.setDeleted(true);
						liveMatchRepository.save(match);
						stopScraping(match.getUrl());
						notifyMatchStatusChange(match.getUrl(), "deleted");
					} else {
						liveMatchRepository.save(match);
					}
				}
			}

			for (String url : urls) {
				if (!liveMatchRepository.existsByUrl(url)) {
					LiveMatch liveMatch = new LiveMatch(url);
					liveMatchRepository.save(liveMatch);
					notifyMatchStatusChange(url, "added");
				} else {
					logger.info("URL already exists: {}", url);
				}
			}

			logger.info("Live matches saved successfully!");
		} catch (Exception e) {
			logger.error("Error saving live matches: ", e);
		}
	}

	private void stopScraping(String url) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, String> requestBody = new HashMap<>();
			requestBody.put("url", url);

			HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> response = restTemplate.exchange(stopScrapeUrl, HttpMethod.POST, request,
					String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				logger.info("Successfully requested to stop scraping for URL: {}", url);
			} else {
				logger.error("Failed to request to stop scraping for URL: {}", url);
			}
		} catch (Exception e) {
			logger.error("Exception while requesting to stop scraping for URL: {}. Error: ", url, e);
		}
	}

	private void notifyMatchStatusChange(String url, String status) {
		cricketDataService.notifyMatchStatusChange(url, status);
	}

	public List<LiveMatch> findAll() {
		return liveMatchRepository.findByIsDeletedFalse();
	}
	
	public List<LiveMatch> findAllLiveMatches() {
		return liveMatchRepository.findByDeletionAttemptsLessThanAndIsDeletedFalse(Integer.valueOf(2));
	}

	public List<LiveMatch> findAllMatches() {
		return liveMatchRepository.findAll();
	}

	public List<LiveMatch> findAllFinishedMatches() {
		return liveMatchRepository.findByIsDeletedTrue();
	}
	
	public LiveMatch findByUrl(String url) {
		return liveMatchRepository.findByUrlContaining(url);
	}

	public ResponseEntity<CricketDataDTO> fetchAndSendData(String url) {
		CricketDataDTO lastUpdatedData = cricketDataService.getLastUpdatedData(url);
		if (lastUpdatedData != null) {
			return ResponseEntity.ok(lastUpdatedData);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	public String appendBaseUrl(String url) {
		// Implement logic to append base URL if needed
		return "https://crex.live" + url;
	}

	
	@Override
	public LiveMatch update(LiveMatch match) {
		return liveMatchRepository.save(match);
	}
}
