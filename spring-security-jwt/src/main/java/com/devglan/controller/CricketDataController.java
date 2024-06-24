package com.devglan.controller;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devglan.dao.BetResponse;
import com.devglan.dao.CricketDataDTO;
import com.devglan.model.Bets;
import com.devglan.model.LiveMatch;
import com.devglan.model.ProfitLoss;
import com.devglan.model.User;
import com.devglan.service.BetService;
import com.devglan.service.LiveMatchService;
import com.devglan.service.UserService;
import com.devglan.websocket.service.CricketDataService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/cricket-data")
public class CricketDataController {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(CricketDataController.class);

	@Autowired
	CricketDataService cricketDataService;

	@Autowired
	private LiveMatchService liveMatchService;

	@Autowired
	private BetService betService;

	@Autowired
	private UserService userService;

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
			if (data.getSessionOdds() != null) {
				nonNullFields.put("session_odds", data.getSessionOdds());
				existingData.setSessionOdds(data.getSessionOdds());
			}
			if (data.getUrl() != null) {
				nonNullFields.put("url", data.getUrl());
				existingData.setUrl(data.getUrl());
			}
			// Handling overs_data assuming it's a List or similar collection
			if (data.getOversData() != null && !data.getOversData().isEmpty()) {
				nonNullFields.put("overs_data", data.getOversData());
				existingData.setOversData(data.getOversData());
			}
			
			if (data.getTeamWiseSessionData() != null && !data.getTeamWiseSessionData().isEmpty()) {
                nonNullFields.put("team_wise_session_data", data.getTeamWiseSessionData());
                existingData.setTeamWiseSessionData(data.getTeamWiseSessionData());
            }

			existingData.setLastUpdated(System.currentTimeMillis());
			cricketDataService.setLastUpdatedData(existingData.getUrl(), existingData);

			cricketDataService.sendCricketData(data.getUrl(), nonNullFields);

			// Return a success response
			return ResponseEntity.ok("Data received successfully!");
		} catch (Exception e) {
			// Handle exceptions and return an error response if needed
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	 @GetMapping("/bet/profit-loss")
	    public ResponseEntity<List<ProfitLoss>> getProfitLoss() {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        String currentUsername = authentication.getName();
			User user = userService.findOne(currentUsername);
	        List<ProfitLoss> profitLoss = betService.calculateProfitLoss(user.getId());

	        return ResponseEntity.ok(profitLoss);
	    }
	 
	@GetMapping("/bet/history")
    public ResponseEntity<BetResponse> getBetHistory() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentUsername = authentication.getName();
		User user = userService.findOne(currentUsername);
		List<Bets> betsByUserId = betService.getBetsByUserId(user.getId());
		List<Bets> filteredBets = betsByUserId.stream()
                .filter(bet -> !("WON".equalsIgnoreCase(bet.getStatus()) || "LOST".equalsIgnoreCase(bet.getStatus())))
                .collect(Collectors.toList());
		 BetResponse response = new BetResponse(filteredBets, null);
		
        return ResponseEntity.ok(response);
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

	@GetMapping("/live-matches") // Map to "/cricket-data/live-matches"
	public ResponseEntity<List<LiveMatch>> getAllLiveMatches() {
		try {
			List<LiveMatch> liveMatches = null;
			try {
				liveMatches = liveMatchService.findAll();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // Retrieve all entities
			return ResponseEntity.ok(liveMatches);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(null); // Simplified error handling
		}
	}

	@GetMapping("/bets")
	public ResponseEntity<BetResponse> getBetsForMatch(@RequestParam String url) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentUsername = authentication.getName();
		User user = userService.findOne(currentUsername);

		List<Bets> bets = betService.getBetsForMatch(url, user.getId());
		Map<String, List<Bets>> betsByTeam = bets.stream()
                .filter(bet -> !"Cancelled".equals(bet.getStatus()))
                .collect(Collectors.groupingBy(Bets::getTeamName));
		
	    Map<String, Map<String, BigDecimal>> matchExposures = betService.calculateMatchExposures(betsByTeam);
	    Map<String, BigDecimal> adjustedExposuresForAllTeams = betService.adjustExposuresForAllTeams(
	    		matchExposures);

	    BetResponse response = new BetResponse(bets, adjustedExposuresForAllTeams);
	    log.info("sending all bets response {}", response.getBets());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/placeBet")
	@Transactional
	public ResponseEntity<?> placeBet(@RequestBody Bets bet) {
		// Retrieve the Authentication object from the SecurityContext
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		// Extract the username of the currently authenticated user
		String currentUsername = authentication.getName();

		// Use the UserService to fetch the User object based on the username
		User user = userService.findOne(currentUsername);
		if (user != null) {

			// Check if the user has enough balance to place the bet
			BigDecimal userBalance = user.getBalance();
			BigDecimal betAmount = bet.getAmount();

			// If the user is found, associate the bet with the user
			boolean isBetValid = false;

			if ("back".equalsIgnoreCase(bet.getBetType()) && userBalance.compareTo(betAmount) >= 0) {
				// If it's a back bet and the user has enough balance, proceed
				isBetValid = true;
				// Deduct the bet amount from the user's balance
				// user.setBalance(userBalance.subtract(betAmount));
			} else if ("lay".equalsIgnoreCase(bet.getBetType())) {
				// For lay bets, you might have different logic, depending on your betting rules
				// Here, we assume the user can place the bet without balance restrictions

				BigDecimal potentialPayout = bet.getOdd().subtract(BigDecimal.ONE).multiply(betAmount);
				if (userBalance.compareTo(potentialPayout) >= 0) {
					isBetValid = true;
					// Here, you might want to reserve the potential payout amount from the user's
					// balance
					// depending on your application's requirements.
					// user.setBalance(userBalance.subtract(potentialPayout));
				} else {
					isBetValid = false;
					// cancel the bet if not valid
				}
				// No balance deduction for lay bets in this example
			}

			if (isBetValid) {
				// Associate the bet with the user
				bet.setUser(user);
				// Set the placedAt time to the current date and time
				bet.setPlacedAt(new Date());
				// Set bet status as pending for now
				bet.setStatus("Pending");
				Bets savedBet = betService.placeBet(bet);
				// confirm/cancel bet with respect to the latest stable odds
				betService.checkAndConfirmBet(bet, currentUsername);

				// Save the updated user balance
				userService.updateUser(user);
				// Save the bet using the BetService and store the returned instance
				// Respond with the saved bet details
				return ResponseEntity.ok(savedBet);
			} else {

				cricketDataService.notifyBetStatus(betService.cancelBet(bet));
				// Respond indicating the user does not have enough balance
				return ResponseEntity.badRequest().body("Insufficient balance for this bet.");
			}
		} else {
			// If the user is not found, respond with an Unauthorized status
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
		}
	}
}
