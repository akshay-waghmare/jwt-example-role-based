package com.devglan.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.devglan.dao.ProfitLossDTO;
import com.devglan.dao.SessionOverData;
import com.devglan.model.Bets;
import com.devglan.model.BlogPost;
import com.devglan.model.ExposureResult;
import com.devglan.model.LiveMatch;
import com.devglan.model.User;
import com.devglan.service.BetService;
import com.devglan.service.LiveMatchService;
import com.devglan.service.MatchInfoService;
import com.devglan.service.RssFeedService;
import com.devglan.service.ScorecardService;
import com.devglan.service.UserService;
import com.devglan.websocket.service.CricketDataService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Autowired
	private MatchInfoService matchInfoService;
	
	@Autowired
	private RssFeedService rssFeedService;
	
	@Autowired
	private ScorecardService scoreCardService;

	@PostMapping
	public ResponseEntity<String> receiveCricketData(@RequestBody CricketDataDTO data) {
	    try {
	        // Fetch the existing data including the merged matchInfo data
	        CricketDataDTO existingData = cricketDataService.getLastUpdatedData(data.getUrl());

	        // If no existing data found, create a new one
	        if (existingData == null) {
	            existingData = new CricketDataDTO();
	            existingData.setUrl(data.getUrl());
	        }

	        //System.out.println("Received cricket data: " + data);

	        Map<String, Object> nonNullFields = new HashMap<>();

	        // Check each field for non-null values and update the existing data
	        if (data.getTeamOdds() != null) {
	            nonNullFields.put("team_odds", data.getTeamOdds());
	            existingData.setTeamOdds(data.getTeamOdds());
	            existingData.setLastUpdated(System.currentTimeMillis());
	        }
	        if (data.getCurrentRunRate() != null) {
	            nonNullFields.put("crr", data.getCurrentRunRate());
	            existingData.setCurrentRunRate(data.getCurrentRunRate());
	        }
	        if (data.getFinalResultText() != null) {
	            nonNullFields.put("final_result_text", data.getFinalResultText());
	            existingData.setFinalResultText(data.getFinalResultText());
	        }
	        if (data.getMatchOdds() != null && !data.getMatchOdds().isEmpty()) {
	            nonNullFields.put("match_odds", data.getMatchOdds());
	            existingData.setMatchOdds(data.getMatchOdds());
	            existingData.setLastUpdated(System.currentTimeMillis());
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
	        if (data.getBat_or_ball_selected() != null) {
	            nonNullFields.put("bat_or_ball_selected", data.getBat_or_ball_selected());
	            existingData.setBat_or_ball_selected(data.getBat_or_ball_selected());
	        }
	        if (data.getToss_won_country() != null) {
	            nonNullFields.put("toss_won_country", data.getToss_won_country());
	            existingData.setToss_won_country(data.getToss_won_country());
	        }
	        // Handle session odds
	        if (data.getSessionOddsList() != null && !data.getSessionOddsList().isEmpty()) {
	            nonNullFields.put("session_odds", data.getSessionOddsList());
	            existingData.setSessionOddsList(data.getSessionOddsList());  // Updating to handle multiple session odds
	            existingData.setLastUpdated(System.currentTimeMillis());
	        }
	        if (data.getUrl() != null) {
	            nonNullFields.put("url", data.getUrl());
	            existingData.setUrl(data.getUrl());
	        }
	        if (data.getOversData() != null && !data.getOversData().isEmpty()) {
	            nonNullFields.put("overs_data", data.getOversData());
	            existingData.setOversData(data.getOversData());
	        }
	        if (data.getTeamWiseSessionData() != null && !data.getTeamWiseSessionData().isEmpty()) {
	            nonNullFields.put("team_wise_session_data", data.getTeamWiseSessionData());
	            existingData.setTeamWiseSessionData(data.getTeamWiseSessionData());
	        }

	        // Handle matchInfo fields
	        if (data.getMatchDate() != null) {
	            nonNullFields.put("match_date", data.getMatchDate());
	            existingData.setMatchDate(data.getMatchDate());
	        }
	        if (data.getVenue() != null) {
	            nonNullFields.put("venue", data.getVenue());
	            existingData.setVenue(data.getVenue());
	        }
	        if (data.getMatchName() != null) {
	            nonNullFields.put("match_name", data.getMatchName());
	            existingData.setMatchName(data.getMatchName());
	        }
	        if (data.getTossInfo() != null) {
	            nonNullFields.put("toss_info", data.getTossInfo());
	            existingData.setTossInfo(data.getTossInfo());
	        }
	        if (data.getTeamComparison() != null) {
	            nonNullFields.put("team_comparison", data.getTeamComparison());
	            existingData.setTeamComparison(data.getTeamComparison());
	        }
	        if (data.getTeamForm() != null) {
	            nonNullFields.put("team_form", data.getTeamForm());
	            existingData.setTeamForm(data.getTeamForm());
	        }
	        if (data.getVenueStats() != null) {
	            nonNullFields.put("venue_stats", data.getVenueStats());
	            existingData.setVenueStats(data.getVenueStats());
	        }
			if (data.getPlayingXI() != null) {
				nonNullFields.put("playing_xi", data.getPlayingXI());
				existingData.setPlayingXI(data.getPlayingXI());
			}
			// Extracting and handling batsman and bowler data
	        if (data.getBatsmanData() != null && !data.getBatsmanData().isEmpty()) {
	            nonNullFields.put("batsman_data", data.getBatsmanData());
	            //existingData.setBatsmanData(data.getBatsmanData()); // Save Batsman Data
	        }
	        if (data.getBowlerData() != null && !data.getBowlerData().isEmpty()) {
	            nonNullFields.put("bowler_data", data.getBowlerData());
	            //existingData.setBowlerData(data.getBowlerData()); // Save Bowler Data
	        }

	        // Update and persist the data
	        cricketDataService.setLastUpdatedData(existingData.getUrl(), existingData);
	        cricketDataService.sendCricketData(data.getUrl(), nonNullFields);

	        // Return a success response
	        return ResponseEntity.ok("Data received successfully!");
	    } catch (Exception e) {
	        // Handle exceptions and return an error response if needed
	        return ResponseEntity.status(500).body("Error: " + e.getMessage());
	    }
	}

	@PostMapping("/match-info/save")
	public ResponseEntity<String> saveMatchInfo(@RequestBody String data) {
	    try {
	        System.out.println("Received data: " + data);

	        // Parse the JSON string
	        ObjectMapper objectMapper = new ObjectMapper();
	        Map<String, Object> payload = objectMapper.readValue(data, new TypeReference<Map<String, Object>>(){});

	        String url = (String) payload.get("url");
	        payload.remove("url");

	        String dataJson = objectMapper.writeValueAsString(payload);

	        // Save the data
	        matchInfoService.saveMatchInfo(url, dataJson);

	        return ResponseEntity.ok("Match info saved successfully.");
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving match info.");
	    }
	}
	
	@PostMapping("/sC4-stats/save")
	public ResponseEntity<String> receiveSC4Stats(@RequestBody String data) {
	    try {
	    	System.out.println("Received data: " + data);

	        // Parse the JSON string
	        ObjectMapper objectMapper = new ObjectMapper();
	        Map<String, Object> payload = objectMapper.readValue(data, new TypeReference<Map<String, Object>>(){});

	        String url = (String) payload.get("url");
	        payload.remove("url");

	        String dataJson = objectMapper.writeValueAsString(payload);
	        
	        scoreCardService.saveMatchInfo(url, dataJson);
	        
	        return ResponseEntity.ok("sC4_stats data received and saved successfully!");
	    } catch (Exception e) {
	        log.error("Error receiving sC4_stats data: {}", e.getMessage(), e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("Error saving sC4_stats data: " + e.getMessage());
	    }
	}
	
	@GetMapping("/match-info/get")
	public ResponseEntity<?> getMatchInfo(@RequestParam("url") String url) {
	    try {
	        String dataJson = matchInfoService.getMatchInfo(url);
	        if (dataJson != null) {
	            // Parse the JSON string back into an object
	            ObjectMapper objectMapper = new ObjectMapper();
	            Map<String, Object> data = objectMapper.readValue(dataJson, new TypeReference<Map<String, Object>>() {});
	            
	            return ResponseEntity.ok(data);
	        } else {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No data found for the given URL.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving match info.");
	    }
	}
	
	@GetMapping("/sC4-stats/get")
	public ResponseEntity<?> getScorecardInfo(@RequestParam("url") String url) {
	    try {
	        String dataJson = scoreCardService.getMatchInfo(url);
	        if (dataJson != null) {
	            // Parse the JSON string back into an object
	            ObjectMapper objectMapper = new ObjectMapper();
	            Map<String, Object> data = objectMapper.readValue(dataJson, new TypeReference<Map<String, Object>>() {});
	            
	            return ResponseEntity.ok(data);
	        } else {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No data found for the given URL.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving match info.");
	    }
	}
	 
	@GetMapping("/bet/profit-loss")
	public ResponseEntity<Map<String, ProfitLossDTO>> getProfitLoss(
	    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
	    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {

	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    String currentUsername = authentication.getName();
	    User user = userService.findOne(currentUsername);

	    if (user != null) {
	        Map<String, ProfitLossDTO> profitLoss = betService.calculateOverallProfitLoss(user.getId(), startDate, endDate);
	        return ResponseEntity.ok(profitLoss);
	    } else {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
	    }
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
		BetResponse response = new BetResponse(filteredBets, null, null);

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
				liveMatches = liveMatchService.findAllLiveMatches();
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
		
		Set<String> excludedStatuses = new HashSet<>(Arrays.asList("Won", "Lost", "Pending", "Cancelled"));

		List<Bets> bets = betService.getBetsForMatchForUser(url, user.getId());
		Map<String, List<Bets>> betsByTeam = bets.stream()
                .filter(bet -> !excludedStatuses.contains(bet.getStatus()) && !bet.getIsSessionBet())
                .collect(Collectors.groupingBy(Bets::getTeamName));

		Map<String, Map<String, BigDecimal>> matchExposures = betService.calculateMatchExposures(betsByTeam);
		Map<String, BigDecimal> adjustedExposuresForAllTeams = betService.adjustExposuresForAllTeams(matchExposures);
		
		Map<String, List<Bets>> sessionBetList = bets.stream()
                .filter(bet -> !excludedStatuses.contains(bet.getStatus()) && bet.getIsSessionBet())
                .collect(Collectors.groupingBy(Bets::getSessionName));

		 Map<String, BigDecimal> sessionExposures = new HashMap<>();
	        sessionBetList.forEach((sessionName, sessionBets) -> {
	            ExposureResult exposure = betService.calculateSessionExposureExtended(sessionBets);
	            sessionExposures.put(sessionName + " " + exposure.getHigherExposure() , exposure.getTotalExposure());
	        });
	        
		BetResponse response = new BetResponse(bets, adjustedExposuresForAllTeams , sessionExposures);
		log.info("sending all bets response {}", response.getBets());

		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/update-winning-team")
	public ResponseEntity<?> updateWinningTeam(@RequestParam String matchUrl, @RequestParam String winningTeam) {
		try {
			LiveMatch match = liveMatchService.findByUrl(matchUrl);
			if (match == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Match not found");
			}

			match.setLastKnownState(winningTeam + " won by correction");
			match.setDistributionDone(false); // Mark the match for redistribution
			liveMatchService.update(match);

			betService.correctPreviousWinnings(match);

			// Re-trigger the winnings distribution
			//betService.distributeExposureAndWinnings(match);

			return ResponseEntity.ok("Winning team updated successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating winning team");
		}
	}
	

	
	
	@GetMapping("/blog-posts")
	public ResponseEntity<List<BlogPost>> getBlogPosts() {
		String feedUrl = "https://victoryviews.blogspot.com/feeds/posts/default"; // Use the actual
		List<BlogPost> blogPosts = rssFeedService.fetchBlogPosts(feedUrl);
		return ResponseEntity.ok(blogPosts);
	}

	@GetMapping("/get-match-bet-with-exposure")
	public ResponseEntity<Map<String, BetResponse>> getMatchBetsWithExposure() {

		Set<String> excludedStatuses = new HashSet<>(Arrays.asList("Won", "Lost", "Pending", "Cancelled"));

		List<LiveMatch> allLive = liveMatchService.findAll();
	    
	    Map<String, BetResponse> responseMap = new HashMap<>();

	    allLive.forEach(liveMatch -> {
	        String matchUrl = liveMatch.getUrl();
	        List<Bets> bets = betService.getBetsForMatch(matchUrl);
	        Map<String, List<Bets>> betsByTeam = bets.stream()
	                .filter(bet -> !excludedStatuses.contains(bet.getStatus()) && !bet.getIsSessionBet())
	                .collect(Collectors.groupingBy(Bets::getTeamName));
	        
	        Map<String, Map<String, BigDecimal>> matchExposures = betService.calculateMatchExposures(betsByTeam);
	        Map<String, BigDecimal> adjustedExposuresForAllTeams = betService.adjustExposuresForAllTeams(matchExposures);
	        
	        Map<String, List<Bets>> sessionBetList = bets.stream()
	                .filter(bet -> !excludedStatuses.contains(bet.getStatus()) && bet.getIsSessionBet())
	                .collect(Collectors.groupingBy(Bets::getSessionName));
	        
	        Map<String, BigDecimal> sessionExposures = new HashMap<>();
	        sessionBetList.forEach((sessionName, sessionBets) -> {
	            ExposureResult exposure = betService.calculateSessionExposureExtended(sessionBets);
	            sessionExposures.put(sessionName + " " + exposure.getHigherExposure() , exposure.getTotalExposure());
	        });
	        
	        BetResponse response = new BetResponse(bets, adjustedExposuresForAllTeams , sessionExposures );
	        log.info("Sending all bets response for URL {}: {}", matchUrl, response.getBets());
	        
	        responseMap.put(matchUrl, response);
	    });

	    return ResponseEntity.ok(responseMap);
	}

	@PostMapping("/placeBet")
	@Transactional
	public CompletableFuture<ResponseEntity<?>> placeBet(@RequestBody Bets bet) {
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    String currentUsername = authentication.getName();
	    User user = userService.findOne(currentUsername);

	    if (user != null) {
	      
	        BigDecimal betAmount = bet.getAmount();
	        String cancellationReason = "";

	        boolean isBetValid = false;
	        LiveMatch match = liveMatchService.findByUrl(bet.getMatchUrl());
	        if (match != null) {

	            if (match.isDeleted() || match.getDeletionAttempts() > 1) {
	                // trying to place bet on a finished match
	                cancellationReason = "Cannot accept bet on finished match";
	                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(cancellationReason));
	            }
	            
	            if (bet.getIsSessionBet()) {
	                SessionOverData sessionResult = betService.getSessionResult(match, bet.getSessionName());
	                if (sessionResult != null && sessionResult.getName() != null
	                        && (sessionResult.getName() != bet.getSessionName())) {
	                    // trying to place a bet which is finished
	                    cancellationReason = "Cannot accept session bet for finished session : " + sessionResult.getName();
	                    return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(cancellationReason));
	                }
	            }
	        }
	        
	        

	        // Check if the odds are null
	        if (bet.getOdd() == null || bet.getOdd().compareTo(BigDecimal.ONE) == 0) {
	            cancellationReason = "Odds must not be null";
	            cricketDataService.notifyBetStatus(betService.cancelBet(bet));
	            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(cancellationReason));
	        }
	        
	        if(bet.getAmount().compareTo(BigDecimal.valueOf(1000)) < 0) {
	            cancellationReason = "Minimum bet 1000";
	            cricketDataService.notifyBetStatus(betService.cancelBet(bet));
	            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(cancellationReason));
	        }

	        // Validate if the odds are in numeric format
			try {
				BigDecimal odds = new BigDecimal(bet.getOdd().toString());
				bet.setOdd(odds);
			} catch (NumberFormatException e) {
				cancellationReason = "Odds must be in numeric format";
				cricketDataService.notifyBetStatus(betService.cancelBet(bet));
				return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(cancellationReason));
			}

			if ("back".equalsIgnoreCase(bet.getBetType())) {
				isBetValid = true;
			} else if ("lay".equalsIgnoreCase(bet.getBetType())) {
				/*
				 * if (userBalance.subtract(userExposure).compareTo(potentialPayout) >= 0) {
				 * isBetValid = true; } else { isBetValid = false; cancellationReason =
				 * "Insufficient balance for this bet"; }
				 */
				isBetValid = true;
			} else if (bet.getIsSessionBet() != null && bet.getIsSessionBet()) {

				/*
				 * if (userBalance.subtract(userExposure).compareTo(betAmount) >= 0) {
				 * isBetValid = true; } else { isBetValid = false; cancellationReason =
				 * "Insufficient balance for this bet"; }
				 */
				isBetValid = true;
			}
			 

	        if (isBetValid) {
	            bet.setUser(user);
	            bet.setPlacedAt(new Date());
	            bet.setStatus("Pending");
	            Bets savedBet = betService.placeBet(bet);
	            //cricketDataService.notifyBetStatus(savedBet);
	            CompletableFuture<ResponseEntity<?>> responseEntityFuture = betService.checkAndConfirmBet(savedBet, currentUsername);
	            //userService.updateUser(user);
	            return responseEntityFuture;
	        } else {
	            cricketDataService.notifyBetStatus(betService.cancelBet(bet));
	            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(cancellationReason));
	        }
	    } else {
	        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found."));
	    }
	}
}