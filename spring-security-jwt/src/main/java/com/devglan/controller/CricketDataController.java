package com.devglan.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devglan.dao.CricketDataDTO;
import com.devglan.model.LiveMatch;
import com.devglan.service.LiveMatchService;
import com.devglan.websocket.service.CricketDataService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/cricket-data")
public class CricketDataController {

	private static final Logger log = LoggerFactory.getLogger(CricketDataController.class);

	@Autowired
	CricketDataService cricketDataService;
	
	@Autowired
	private LiveMatchService liveMatchService;

	@PostMapping
	public ResponseEntity<String> receiveCricketData(@RequestBody CricketDataDTO data) {
		try {
			// Perform processing on the received data here
			// For example, you can print the received data
	        CricketDataDTO existingData = cricketDataService.getLastUpdatedData(data.getUrl());

			// If no existing data found, create a new one
			if (existingData == null) {
				existingData = new CricketDataDTO();
				existingData.setUrl(data.getUrl());
			}
	        
			System.out.println("Received cricket data: " + data);

			Map<String, Object> nonNullFields = new HashMap<>();

			// Check each field for non-null values and add them to the Maps
			if (data.getTeamOdds() != null) {
				nonNullFields.put("team_odds", data.getTeamOdds());
				existingData.setTeamOdds(data.getTeamOdds());
			}
			if (data.getCurrentRunRate() != null) {
	            nonNullFields.put("crr", data.getCurrentRunRate());
	            existingData.setCurrentRunRate(data.getCurrentRunRate());
	        }
	        if (data.getFinalResultText() != null) {
	            nonNullFields.put("final_result_text", data.getFinalResultText());
	            existingData.setFinalResultText(data.getFinalResultText());
	        }
			// Assuming MatchOdds class is used
	        if (data.getMatchOdds() != null && !data.getMatchOdds().isEmpty()) {
	            nonNullFields.put("match_odds", data.getMatchOdds());
	            existingData.setMatchOdds(data.getMatchOdds());
	        }
			if (data.getOver() != null) {
				nonNullFields.put("over", data.getOver());
				existingData.setOver(data.getOver());
			}
			if (data.getScore() != null) {
				nonNullFields.put("score", data.getScore());
				existingData.setScore(data.getScore());
			}
			if (data.getCurrentBall() != null) {
				nonNullFields.put("current_ball", data.getCurrentBall());
				existingData.setCurrentBall(data.getCurrentBall());
			}
			if (data.getRunsOnBall() != null) {
				nonNullFields.put("runs_on_ball", data.getRunsOnBall());
				existingData.setRunsOnBall(data.getRunsOnBall());
				
			}
			if (data.getFavTeam() != null) {
				nonNullFields.put("fav_team", data.getFavTeam());
				existingData.setFavTeam(data.getFavTeam());
			}
			if (data.getBattingTeamName() != null) {
				nonNullFields.put("batting_team", data.getBattingTeamName());
				existingData.setBattingTeamName(data.getBattingTeamName());
			}
			/*
			 * if (data.getTeamPlayerInfo() != null) { nonNullFields.put("team_player_info",
			 * data.getTeamPlayerInfo());
			 * existingData.setTeam_player_info(data.getTeamPlayerInfo());
			 * 
			 * }
			 */
			if (data.getBat_or_ball_selected() != null) {
				nonNullFields.put("bat_or_ball_selected", data.getBat_or_ball_selected());
				existingData.setBat_or_ball_selected(data.getBat_or_ball_selected());
			}
			if (data.getToss_won_country() != null) {
				nonNullFields.put("toss_won_country", data.getToss_won_country());
				existingData.setToss_won_country(data.getToss_won_country());
			}
			if(data.getSessionOdds() != null) {
				nonNullFields.put("session_odds", data.getSessionOdds());
				existingData.setSessionOdds(data.getSessionOdds());
			}
			if(data.getUrl() != null) {
				nonNullFields.put("url", data.getUrl());
				existingData.setUrl(data.getUrl());
			}
			// Handling overs_data assuming it's a List or similar collection
		    if (data.getOversData() != null && !data.getOversData().isEmpty()) {
		        nonNullFields.put("overs_data", data.getOversData());
		        existingData.setOversData(data.getOversData());
		    }

			existingData.setLastUpdated(System.currentTimeMillis());
	        cricketDataService.setLastUpdatedData(existingData.getUrl(), existingData);

			
			cricketDataService.sendCricketData(data.getUrl() , nonNullFields);

			// Return a success response
			return ResponseEntity.ok("Data received successfully!");
		} catch (Exception e) {
			// Handle exceptions and return an error response if needed
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}
	
	 @GetMapping("/last-updated-data")
	    public ResponseEntity<CricketDataDTO> getLastUpdatedData(@RequestParam String url) {
	        // Retrieve the last updated data for the specific URL
	        List<LiveMatch> liveMatches = liveMatchService.findAll();
	        for (LiveMatch liveMatch : liveMatches) {
	        	if (liveMatch.getUrl().contains(url)) {
	                // Assuming you have a method to append the base URL
	                String completeUrl = liveMatchService.appendBaseUrl(liveMatch.getUrl()); // Implement this method
	                return liveMatchService.fetchAndSendData(completeUrl);
	            }
	        }
	        return ResponseEntity.notFound().build();
	    }
	 
	@PostMapping("/add-live-matches")
	public ResponseEntity<String> addLiveMatches(@RequestBody String[] urls) {
	    try {

	    	// Print the data
            for (String url : urls) {
                System.out.println(url);
            }

            liveMatchService.syncLiveMatches(urls);
            
	        return ResponseEntity.ok("URLs received successfully!");
	    } catch (Exception e) {
	        // Handle exceptions and return an error response if needed
	        return ResponseEntity.status(500).body("Error: " + e.getMessage());
	    }
	}
	
	@GetMapping("/live-matches") // Map to  "/cricket-data/live-matches"
    public ResponseEntity<List<LiveMatch>> getAllLiveMatches() {
        try {
            List<LiveMatch> liveMatches = null;
			try {
				liveMatches = liveMatchService.findAll();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  // Retrieve all entities 
            return ResponseEntity.ok(liveMatches); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null); // Simplified error handling
        }
    }
	
	 

}
