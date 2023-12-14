package com.devglan.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devglan.dao.CricketDataDTO;
import com.devglan.websocket.service.CricketDataService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/cricket-data")
public class CricketDataController {

	private static final Logger log = LoggerFactory.getLogger(CricketDataController.class);

	@Autowired
	CricketDataService cricketDataService;

	@PostMapping
	public ResponseEntity<String> receiveCricketData(@RequestBody CricketDataDTO data) {
		try {
			// Perform processing on the received data here
			// For example, you can print the received data
			System.out.println("Received cricket data: " + data);

			Map<String, Object> nonNullFields = new HashMap<>();

			// Check each field for non-null values and add them to the Map
			if (data.getTeamOdds() != null) {
				nonNullFields.put("team_odds", data.getTeamOdds());
			}
			if (data.getOver() != null) {
				nonNullFields.put("over", data.getOver());
			}
			if (data.getScore() != null) {
				nonNullFields.put("score", data.getScore());
			}
			if (data.getCurrentBall() != null) {
				nonNullFields.put("current_ball", data.getCurrentBall());
			}
			if (data.getRunsOnBall() != null) {
				nonNullFields.put("runs_on_ball", data.getRunsOnBall());
			}
			if (data.getFavTeam() != null) {
				nonNullFields.put("fav_team", data.getFavTeam());
			}
			if (data.getTeamPlayerInfo() != null) {
				nonNullFields.put("team_player_info", data.getTeamPlayerInfo());
			}
			if (data.getBat_or_ball_selected() != null) {
				nonNullFields.put("bat_or_ball_selected", data.getBat_or_ball_selected());
			}
			if (data.getToss_won_country() != null) {
				nonNullFields.put("toss_won_country", data.getToss_won_country());
			}
			if(data.getSessionOdds() != null) {
				nonNullFields.put("session_odds", data.getSessionOdds());
			}

			cricketDataService.sendCricketData(nonNullFields);

			// Return a success response
			return ResponseEntity.ok("Data received successfully!");
		} catch (Exception e) {
			// Handle exceptions and return an error response if needed
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

}
