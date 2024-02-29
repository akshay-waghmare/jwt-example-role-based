package com.devglan.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.devglan.dao.CricketDataDTO;
import com.devglan.model.LiveMatch;
import com.devglan.repository.LiveMatchRepository;
import com.devglan.service.LiveMatchService;
import com.devglan.websocket.service.CricketDataService;

@Service
public class LiveMatchServiceImpl implements LiveMatchService {

	private final LiveMatchRepository liveMatchRepository;
	private final CricketDataService cricketDataService;

	@Autowired
	public LiveMatchServiceImpl(LiveMatchRepository liveMatchRepository, CricketDataService cricketDataService) {
		this.liveMatchRepository = liveMatchRepository;
		this.cricketDataService = cricketDataService;
	}

	public void syncLiveMatches(String[] urls) {
		try {
			System.out.println("Saving logic started");

			List<String> urlList = Arrays.asList(urls);
			List<LiveMatch> allMatches = liveMatchRepository.findAll();

			for (LiveMatch match : allMatches) {
				if (!urlList.contains(match.getUrl())) {
					liveMatchRepository.delete(match);
					notifyMatchStatusChange(match.getUrl(), "deleted");
				}
			}
			
			for (String url : urls) {
				if (!liveMatchRepository.existsByUrl(url)) {
					LiveMatch liveMatch = new LiveMatch(url);
					liveMatchRepository.save(liveMatch);
					// Notify about the new match over WebSocket
					notifyMatchStatusChange(url, "added");
				} else {
					System.out.println("URL already exists: " + url);
				}
			}

			System.out.println("Live matches saved successfully!");
		} catch (Exception e) {
			// Handle exceptions and log an error message if needed
			System.err.println("Error saving live matches: " + e.getMessage());
		}
	}

	private void notifyMatchStatusChange(String url, String status) {
		cricketDataService.notifyMatchStatusChange(url, status);
		
	}
	
	
	public List<LiveMatch> findAll() {
        return liveMatchRepository.findAll();
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

}
