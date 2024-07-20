package com.devglan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devglan.dao.BetRepository;
import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.MatchOdds;
import com.devglan.dao.ProfitLossDTO;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.SessionOverData;
import com.devglan.model.Bets;
import com.devglan.model.ExposureResult;
import com.devglan.model.LiveMatch;
import com.devglan.model.Transaction;
import com.devglan.model.User;
import com.devglan.repository.CricketDataRepository;
import com.devglan.repository.TransactionRepository;
import com.devglan.websocket.service.CricketDataService;

@Service
public class BetService {

	private static final Logger logger = LoggerFactory.getLogger(BetService.class);

	@Autowired
	private BetRepository betRepository;
	
	@Autowired
	private CricketDataRepository cricketDataRepository;
	
	@Autowired
	private CricketDataService cricketDataService;
	@Autowired
	private UserService userService;
	@Autowired
	private LiveMatchService liveMatchService;
	@Autowired
	private TransactionService transactionService;
	
	@Autowired
    private TransactionRepository transactionRepository;

	public Bets getBetById(Long id) throws NotFoundException {
		return betRepository.findById(id).orElseThrow(() -> new NotFoundException());
	}

	public List<Bets> getBetsByUserId(Long userId) {
		return betRepository.findByUserId(userId);
	}

	public Bets placeBet(Bets bet) {
		return betRepository.save(bet);
	}

	@Async("taskScheduler")
	@Scheduled(fixedRate = 60000) // Runs every 5 minutes
	public void checkMatchResults() {

		logger.info("Scheduled task checkMatchResults started");

		try {
			List<LiveMatch> liveMatches = liveMatchService.findAllMatches();
			logger.debug("Found {} finished matches", liveMatches.size());

			// Filter out matches that have already had winnings distributed
			liveMatches = liveMatches.stream().filter(match -> !match.isDistributionDone())
					.collect(Collectors.toList());
			logger.debug("Processing {} matches for distribution", liveMatches.size());

			for (LiveMatch match : liveMatches) {
				logger.debug("Processing match: {}", match.getId());
				try {
					distributeExposureAndWinnings(match);
				} catch (Exception e) {
					logger.error("Error distributing exposure and winnings for match: {}", match.getId(), e);
				}
			}
			calculateTotalExposureForAllUsers();
		} catch (Exception e) {
			logger.error("Error in scheduled task checkMatchResults", e);
		}

		logger.info("Scheduled task checkMatchResults finished");
	}

	@Transactional
	public void distributeExposureAndWinnings(LiveMatch match) {
		logger.info("Distributing exposure and winnings for match: {}", match.getId());

		List<Bets> betsForMatch = betRepository.findByMatchUrlContainingAndConfirmed(match.getUrl());
		// Group bets by user for exposure reversal
		Map<User, List<Bets>> userBetsMap = betsForMatch.stream().collect(Collectors.groupingBy(Bets::getUser));

		for (Entry<User, List<Bets>> entry : userBetsMap.entrySet()) { // Line 49
			User user = entry.getKey();
			List<Bets> userBets = entry.getValue();
			logger.debug("Processing bets for user: {}", user.getId());

			// Group bets by team to handle multi-team logic if necessary
			// Map<String, List<Bets>> betsByTeam =
			// userBets.stream().collect(Collectors.groupingBy(Bets::getTeamName));

			Map<Boolean, List<Bets>> betsBySession = userBets.stream()
					.collect(Collectors.partitioningBy(Bets::getIsSessionBet));

			// Handle session bets
			//this should be done first as it does not need a winning team 
			
			if (betsBySession.containsKey(true)) {
				processSessionBets(user, betsBySession.get(true), match);
			}

			// Handle one-day match bets
			if (betsBySession.containsKey(false)) {
				String winningTeam = match.getWinningTeam();
				logger.debug("Winning team for match {}: {}", match.getId(), winningTeam);
				if (winningTeam == null) {
					logger.warn("No winning team found for match: {}, skipping processing", match.getId());
					CricketDataDTO cricData = cricketDataService.getCricData(match.getUrl());
					if(cricData != null && cricData.getCurrentBall() != null ) {
						String currentBall = cricData.getCurrentBall();
						match.setLastKnownState(currentBall);
						liveMatchService.update(match);			
					}
					return; // No winning team found, skip processing
				}
				List<Bets> teamBetList = betsBySession.get(false);
				Map<String, List<Bets>> teamBets = teamBetList.stream()
						.collect(Collectors.groupingBy(Bets::getTeamName));

				// here it is calculating and reversing he exposure of this match 
				processMatchBets(user, teamBets, winningTeam);

				for (Bets bet : teamBetList) {
					logger.debug("Processing bet: {} for user: {}", bet.getBetId(), user.getId());

					BigDecimal stake = bet.getAmount();
					BigDecimal odds = bet.getOdd();
					BigDecimal liability = stake.multiply(odds.subtract(BigDecimal.ONE));
					BigDecimal winnings = stake.multiply(odds).subtract(stake);

					// Ensure exposure does not go negative
					if (user.getExposure().compareTo(BigDecimal.ZERO) < 0) {
						user.setExposure(BigDecimal.ZERO);
						logger.debug("User: {}, exposure set to zero before processing bet: {}", user.getId(),
								bet.getBetId());
					}

					String matchName = extractMatchFromUrl(bet.getMatchUrl());
					String remark =  "cricket / " + matchName + "/ match_odds" + "/ " + winningTeam;
					
					if (bet.getTeamName().equalsIgnoreCase(winningTeam)) {
						// User won the bet
						if ("back".equalsIgnoreCase(bet.getBetType())) {
							user.setBalance(user.getBalance().add(winnings)); // Add winnings and return stake
							bet.setStatus("Won");
				            transactionService.createTransaction(user, "Credit", winnings, remark, "match");
							logger.debug("User: {}, bet: {} won, new balance: {}", user.getId(), bet.getBetId(),
									user.getBalance());
							
						} else if ("lay".equalsIgnoreCase(bet.getBetType())) {
							user.setBalance(user.getBalance().subtract(liability)); // Subtract liability
							bet.setStatus("Lost");
							
				            transactionService.createTransaction(user, "Debit", liability.negate(), remark, "match");
							logger.debug("User: {}, bet: {} lost, new balance: {}", user.getId(), bet.getBetId(),
									user.getBalance());
						}
					} else {
						// User lost the bet
						if ("back".equalsIgnoreCase(bet.getBetType())) {
							user.setBalance(user.getBalance().subtract(stake)); // Subtract stake
							bet.setStatus("Lost");
				            transactionService.createTransaction(user, "Debit", stake.negate(), remark, "match");
							logger.debug("User: {}, bet: {} lost, new balance: {}", user.getId(), bet.getBetId(),
									user.getBalance());
						} else if ("lay".equalsIgnoreCase(bet.getBetType())) {
							user.setBalance(user.getBalance().add(stake)); // Return stake
							bet.setStatus("Won");
							transactionService.createTransaction(user, "Credit", stake, remark, "match");
							logger.debug("User: {}, bet: {} won, new balance: {}", user.getId(), bet.getBetId(),
									user.getBalance());
						}
					}

					betRepository.save(bet);
					userService.updateUser(user);
					logger.debug("Bet: {}, updated status: {}, user: {}, updated balance: {}", bet.getBetId(),
							bet.getStatus(), user.getId(), user.getBalance());
				}
				if (user.getExposure().compareTo(BigDecimal.ZERO) < 0) { // Line 76
					user.setExposure(BigDecimal.ZERO);
				}
				
				userService.updateUser(user); // Line 78
				logger.debug("User: {}, updated exposure: {}", user.getId(), user.getExposure());
				
				match.setDistributionDone(true);
				liveMatchService.update(match);
			}


		}

	}
	
	private void calculateTotalExposureForAllUsers() {
	    List<User> users = userService.findAll();
	    for (User user : users) {
	        BigDecimal totalExposure = BigDecimal.ZERO;
	        List<LiveMatch> allMatches = liveMatchService.findAllMatches();

	        for (LiveMatch match : allMatches) {
	            List<Bets> betsForMatch = betRepository.findConfirmedBetsByUserIdAndMatchUrl(user.getId(), match.getUrl());
	            Map<Boolean, List<Bets>> betsBySession = betsForMatch.stream()
	                .collect(Collectors.partitioningBy(Bets::getIsSessionBet));

	            // Calculate match exposure
	            if (betsBySession.containsKey(false)) {
	                List<Bets> matchBets = betsBySession.get(false);
	                Map<String, List<Bets>> betsByTeam = matchBets.stream().collect(Collectors.groupingBy(Bets::getTeamName));
	                totalExposure = totalExposure.add(calculateExposureForMatch(betsByTeam));
	            }

	            // Calculate session bets exposure
	            if (betsBySession.containsKey(true)) {
	                List<Bets> sessionBets = betsBySession.get(true);
	                totalExposure = totalExposure.add(calculateSessionExposure(sessionBets));
	            }
	        }

	        user.setExposure(totalExposure);
	        userService.updateUser(user);
	        logger.info("User: {}, total exposure: {}", user.getId(), user.getExposure());
	    }
	}
	
	private BigDecimal calculateExposureForMatch(Map<String, List<Bets>> betsByTeam) {
	    BigDecimal totalExposure = BigDecimal.ZERO;

	    if (betsByTeam.size() == 1) {
	        List<Bets> singleTeamBets = betsByTeam.values().iterator().next();
	        totalExposure = calculateNetExposuresInWinLoseCase(singleTeamBets).abs();
	    } else if (betsByTeam.size() > 1) {
	        Map<String, BigDecimal> adjustedExposuresForAllTeams = adjustExposuresForAllTeams(
	                calculateMatchExposures(betsByTeam));

	        // Assuming there are exactly 2 teams
	        String[] teams = adjustedExposuresForAllTeams.keySet().stream()
	                .map(key -> key.split(" ")[0])
	                .distinct()
	                .toArray(String[]::new);

	        // Create new objects for each team's exposures
	        Map<String, BigDecimal> team1Exposures = new HashMap<>();
	        Map<String, BigDecimal> team2Exposures = new HashMap<>();

	        for (String key : adjustedExposuresForAllTeams.keySet()) {
	            if (key.startsWith(teams[0])) {
	                team1Exposures.put(key.replace(teams[0] + " ", ""), adjustedExposuresForAllTeams.get(key));
	            } else if (key.startsWith(teams[1])) {
	                team2Exposures.put(key.replace(teams[1] + " ", ""), adjustedExposuresForAllTeams.get(key));
	            }
	        }

	        // Determine exposure based on conditions
	        totalExposure = totalExposure.add(calculateTeamExposure(team1Exposures));
//	        totalExposure = totalExposure.add(calculateTeamExposure(team2Exposures));
	    }
	    return totalExposure;
	}

	private BigDecimal calculateTeamExposure(Map<String, BigDecimal> teamExposures) {
	    BigDecimal winExposure = teamExposures.getOrDefault("Adjusted Win", BigDecimal.ZERO);
	    BigDecimal loseExposure = teamExposures.getOrDefault("Adjusted Lose", BigDecimal.ZERO);

	    if (winExposure.compareTo(BigDecimal.ZERO) < 0 && loseExposure.compareTo(BigDecimal.ZERO) < 0) {
	        // Both are negative, consider the more negative one
	        return winExposure.min(loseExposure).abs();
	    } else if (winExposure.compareTo(BigDecimal.ZERO) < 0 || loseExposure.compareTo(BigDecimal.ZERO) < 0) {
	        // Either win or lose is negative
	        return winExposure.min(loseExposure).abs();
	    } else {
	        // Both are positive
	        return BigDecimal.ZERO;
	    }
	}
	
	private String extractMatchFromUrl(String url) {
	    return url.replace("-", " ");
	}
	
	@org.springframework.transaction.annotation.Transactional
	public void processSessionBets(User user, List<Bets> sessionBets, LiveMatch match) {

		// reversal of exposure
		/*
		 * BigDecimal calculateSessionExposure = calculateSessionExposure(sessionBets);
		 * BigDecimal newExposure =
		 * user.getExposure().subtract(calculateSessionExposure);
		 * user.setExposure(newExposure.compareTo(BigDecimal.ZERO) > 0 ? newExposure :
		 * BigDecimal.ZERO);
		 */
		// win / loss session calculation

		
		for (Bets bet : sessionBets) {
			logger.debug("Processing session bet: {} for user: {}", bet.getBetId(), user.getId());

			// Assume we have a method to get the session result
			SessionOverData sessionResult = getSessionResult(match, bet.getSessionName());
			if (sessionResult == null) {
				logger.warn("No result found for session: {}, skipping bet: {}", bet.getSessionName(), bet.getBetId());
				continue;
			}

			String matchName = extractMatchFromUrl(bet.getMatchUrl());
			String remark = "cricket / " + matchName + "/ session_odds" + "/ " + bet.getSessionName();
			BigDecimal stake = bet.getAmount();

			if (isBetWinning(bet, sessionResult)) {
				// User won the bet
				if ("yes".equalsIgnoreCase(bet.getBetType())) {
					user.setBalance(user.getBalance().add(stake)); // Add winnings and return stake
					bet.setStatus("Won");
		            transactionService.createTransaction(user, "Credit", stake, remark, "Betting");
					logger.debug("User: {}, session bet: {} won, new balance: {}", user.getId(), bet.getBetId(),
							user.getBalance());
				} else if ("no".equalsIgnoreCase(bet.getBetType())) {
					user.setBalance(user.getBalance().add(stake)); // Subtract liability
					bet.setStatus("Won");
		            transactionService.createTransaction(user, "Credit", stake, remark, "Betting");
					logger.debug("User: {}, session bet: {} won, new balance: {}", user.getId(), bet.getBetId(),
							user.getBalance());
				}
			} else {
				// User lost the bet
				if ("yes".equalsIgnoreCase(bet.getBetType())) {
					user.setBalance(user.getBalance().subtract(stake)); // Subtract stake
					bet.setStatus("Lost");
		            transactionService.createTransaction(user, "Debit", stake.negate(), remark, "Betting");
					logger.debug("User: {}, session bet: {} lost, new balance: {}", user.getId(), bet.getBetId(),
							user.getBalance());
				} else if ("no".equalsIgnoreCase(bet.getBetType())) {
					user.setBalance(user.getBalance().subtract(stake)); // Return stake
					bet.setStatus("Lost");
		            transactionService.createTransaction(user, "Debit", stake.negate(), remark, "Betting");
					logger.debug("User: {}, session bet: {} lost, new balance: {}", user.getId(), bet.getBetId(),
							user.getBalance());
				}
			}

			
			userService.updateUser(user);
			cricketDataService.notifyBetStatus(betRepository.save(bet));
			logger.debug("Session bet: {}, updated status: {}, user: {}, updated balance: {}", bet.getBetId(),
					bet.getStatus(), user.getId(), user.getBalance());
		}

	}

	@Transactional
	public SessionOverData getSessionResult(LiveMatch match, String sessionName) {
		CricketDataDTO lastUpdatedData = cricketDataService.getLastUpdatedData(appendBaseUrl(match.getUrl()));
		String battingTeamName = lastUpdatedData.getBattingTeamName();
		String normalizedSessionName = normalizeSessionName(sessionName);
		//here 
		//Detail formatter error:
		//An exception occurred: org.hibernate.LazyInitializationException
		Map<String, List<SessionOverData>> teamWiseSessionData = lastUpdatedData.getTeamWiseSessionData();

		// Normalize the batting team name to match the format in teamWiseSessionData
		String normalizedBattingTeamName = normalizeTeamName(battingTeamName);
        
		Map<String, List<SessionOverData>> normalizedTeamWiseSessionData = new HashMap<>();
		for (Map.Entry<String, List<SessionOverData>> entry : teamWiseSessionData.entrySet()) {
		    String normalizedKey = normalizeTeamName(entry.getKey());
		    normalizedTeamWiseSessionData.put(normalizedKey, entry.getValue());
		}
		
		// Now use normalizedTeamWiseSessionData instead of teamWiseSessionData
		if (normalizedTeamWiseSessionData.containsKey(normalizedBattingTeamName)) {
		    List<SessionOverData> sessionOverDataList = normalizedTeamWiseSessionData.get(normalizedBattingTeamName);
		    for (SessionOverData sessionData : sessionOverDataList) {
		        if (sessionData.getName().equalsIgnoreCase(normalizedSessionName)) {
		            return sessionData;
		        }
		    }
		}

		return null;
	}

	public String appendBaseUrl(String url) {
		// Implement logic to append base URL if needed
		return "https://crex.live" + url;
	}

	private String normalizeSessionName(String sessionName) {
		// Extract the part of the session name before "Ov" to match the format in
		// SESSION_OVER_DATA
		String[] parts = sessionName.split(" ");
		if (parts.length > 0) {
			return parts[0] + " Over";
		}
		return sessionName;
	}

	private String normalizeTeamName(String teamName) {
		// Extract the team name part to match the format in teamWiseSessionData
		String[] parts = teamName.split(" - ");
		if (parts.length > 0) {
			return parts[0];
		}
		return teamName;
	}

	private void processMatchBets(User user, Map<String, List<Bets>> betsByTeam, String winningTeam) {
		if (betsByTeam.size() == 1) {
			// Bets are only on one team
			List<Bets> singleTeamBets = betsByTeam.values().iterator().next();
			BigDecimal exposure = calculateNetExposuresInWinLoseCase(singleTeamBets);
			logger.debug("User: {}, single team bets exposure: {}", user.getId(), exposure);
			user.setExposure(user.getExposure().subtract(exposure.abs()));
		} else if (betsByTeam.size() > 1) {
			// Bets are on multiple teams
			Map<String, BigDecimal> adjustedExposuresForAllTeams = adjustExposuresForAllTeams(
					calculateMatchExposures(betsByTeam));
			logger.debug("User: {}, adjusted exposures for all teams: {}", user.getId(), adjustedExposuresForAllTeams);
			BigDecimal overAllMaxExposure = BigDecimal.ZERO;

			for (String team : adjustedExposuresForAllTeams.keySet()) { // Line 67
				BigDecimal exposure = adjustedExposuresForAllTeams.getOrDefault(team, BigDecimal.ZERO);
				logger.debug("User: {}, team: {}, exposure: {}", user.getId(), team, exposure);
				if (exposure.compareTo(BigDecimal.ZERO) < 0) {
					overAllMaxExposure = exposure.min(overAllMaxExposure);
				}

			}

			user.setExposure(user.getExposure().subtract(overAllMaxExposure.abs()));
			logger.debug("User: {}, overall max exposure: {}", user.getId(), overAllMaxExposure);
		}

	}

	@Async("taskExecutor")
	@Transactional
	public CompletableFuture<ResponseEntity<?>> checkAndConfirmBet(Bets bet, String currentUsername) {
	    try {
	        // Fetch latest odds for the event and ensure collections are initialized
	    	Thread.sleep(5000);
	        CricketDataDTO cdo = new CricketDataDTO();
	        CricketDataDTO latestOdds = fetchLatestOdds(bet, cdo);
	        if (latestOdds == null) {
	            cancelBet(bet);
	            Map<String, Object> response = new HashMap<>();
	            response.put("message", "Bet cancelled due to no odds available");
	            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
	        }

	        long currentTimeMillis = System.currentTimeMillis();
	        long oddsTimestamp = latestOdds.getLastUpdated();
	        if ((currentTimeMillis - oddsTimestamp) > 5000) { // Check if the odds are older than 5 seconds
				
				  cancelBet(bet); Map<String, Object> response = new HashMap<>();
				  response.put("message", "Bet cancelled due to old odds"); return
				  CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
				 
				 
	        }

	        // Ensure collections are fully initialized
	        latestOdds.getMatchOdds().size(); // Initialize matchOdds collection
	        latestOdds.getTeamWiseSessionData().size(); // Initialize teamWiseSessionData collection

	        // Now pass the fully initialized latestOdds to supplyAsync
	        
	        return CompletableFuture.supplyAsync(() -> {
	            try {
	                // 5-second delay

	                if (bet.getIsSessionBet() != null && bet.getIsSessionBet()) {
	                    handleSessionBet(bet, currentUsername, latestOdds);
	                    Map<String, Object> response = new HashMap<>();
	                    response.put("message", "Session bet handled successfully");
	                    response.put("bet", bet);
	                    return ResponseEntity.ok(response);
	                } else {
	                    List<MatchOdds> matchOdds = latestOdds.getMatchOdds();
	                    Optional<MatchOdds> matchingOdds = Optional.empty();
	                    if (matchOdds != null) {
	                        matchingOdds = matchOdds.stream()
	                                .filter(team -> bet.getTeamName().equals(team.getTeamName())).findFirst();
	                    }

	                    if (!matchingOdds.isPresent()) {
	                        // This is a one-day match scenario
	                        handleOneDayMatch(bet, currentUsername, latestOdds);
	                        Map<String, Object> response = new HashMap<>();
	                        response.put("message", "One day match handled successfully");
	                        response.put("bet", bet);
	                        return ResponseEntity.ok(response);
	                    } else {
	                        // This is the test match scenario
	                        handleMatchWithTestOdds(bet, currentUsername, latestOdds);
	                        Map<String, Object> response = new HashMap<>();
	                        response.put("message", "Test match handled successfully");
	                        response.put("bet", bet);
	                        return ResponseEntity.ok(response);
	                    }
	                }

	            }
	            catch (Exception e) {
	                // Handle any other exceptions that may occur during the asynchronous processing
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body(Collections.singletonMap("message", "Error during bet processing"));
	            }
	        });
	    } catch (Exception e) {
	        // Handle any exception that may occur during the initial transaction
	        return CompletableFuture.completedFuture(
	                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body(Collections.singletonMap("message", "Error during bet processing")));
	    }
	}

	private void handleMatchWithTestOdds(Bets bet, String currentUsername, CricketDataDTO latestOdds) {
		// handle similarly how the processMultiTeamBets
		boolean confirmBet = confirmTestBet(bet, latestOdds);

		if (confirmBet) {
			List<Bets> allBetsForMatch = getBetsForMatchForUser(bet.getMatchUrl(), bet.getUser().getId());

			// Group bets by team to handle multi-team logic if necessary
			Map<String, List<Bets>> betsByTeam = allBetsForMatch.stream()
					.collect(Collectors.groupingBy(Bets::getTeamName));

			// Process logic for multi-team bets similar to a one-day match, but possibly
			// with additional logic
			// for test matches depending on the specific requirements or outcomes being
			// handled.
			if (betsByTeam.size() > 1 || (betsByTeam.size() == 1 && betsByTeam.containsKey(bet.getTeamName()))) {
				processTestMatchBets(bet, currentUsername, betsByTeam);
			} else {
				// If the above conditions are not met, handle as an edge case or unexpected
				// scenario
				// This might involve logging a warning, notifying the user, or taking other
				// appropriate actions.
				// For example, cancelBet(bet); or some specialized handling for test matches.
			}
		} else {
			cancelBet(bet);
		}
	}

	private boolean confirmTestBet(Bets bet, CricketDataDTO latestOdds) {
		boolean confirmBet = false;
		BigDecimal hundred = BigDecimal.valueOf(100);
		BigDecimal one = BigDecimal.ONE;

		// Find the MatchOdds instance for the team specified in the bet
		MatchOdds matchingOdds = latestOdds.getMatchOdds().stream()
				.filter(odds -> odds.getTeamName().equalsIgnoreCase(bet.getTeamName())).findFirst().orElse(null);

		// If a matching odds instance is found, compare the odds
		if (matchingOdds != null) {
			if ("back".equals(bet.getBetType())
					&& bet.getOdd().compareTo(new BigDecimal(matchingOdds.getOdds().getBackOdds())) <= 0) {
				confirmBet = true;
			} else if ("lay".equals(bet.getBetType()) && bet.getOdd().subtract(one).multiply(hundred)
					.compareTo(new BigDecimal(matchingOdds.getOdds().getBackOdds())) >= 0) {
				confirmBet = true;
			}
		}

		return confirmBet;
	}

	@org.springframework.transaction.annotation.Transactional
	private CricketDataDTO fetchLatestOdds(Bets bet, CricketDataDTO latestOdds) {
		String completeUrl = liveMatchService.findAll().stream()
				.filter(liveMatch -> liveMatch.getUrl().contains(bet.getMatchUrl())).findFirst()
				.map(liveMatch -> liveMatchService.appendBaseUrl(liveMatch.getUrl())).orElse(null);
		if (completeUrl != null) {
			latestOdds = cricketDataService.getLastUpdatedData(completeUrl); // Adjust based on
		}
		return latestOdds;
	}

	private void handleOneDayMatch(Bets bet, String currentUsername, CricketDataDTO latestOdds) {

		boolean confirmBet = false;
		BigDecimal hundred = BigDecimal.valueOf(100);
		BigDecimal one = BigDecimal.ONE;
		CricketDataDTO fetchedLatestOdds = fetchLatestOdds(bet, latestOdds);
		if (fetchedLatestOdds == null) {
			cricketDataService.notifyBetStatus(cancelBet(bet));
			return;
		}

		String betTeamName = bet.getTeamName();
		String favTeam = fetchedLatestOdds.getFavTeam(); // Use real-time odds for fav team

		// Scenario 1: Bet is back on team 1 and favorite team is team 2 -> accept bet
		// Scenario 2: Bet is back on team 2 and favorite team is team 1 -> accept bet
		if ("back".equals(bet.getBetType()) && !betTeamName.equalsIgnoreCase(favTeam)) {
			confirmBet = true;
			BigDecimal fetchedBackOdd = new BigDecimal(fetchedLatestOdds.getTeamOdds().getLayOdds()).setScale(1,
					RoundingMode.HALF_UP);
			fetchedBackOdd = fetchedBackOdd.add(one);
			BigDecimal newBackOdds = BigDecimal.valueOf(100).divide(fetchedBackOdd).add(BigDecimal.ONE);
			bet.setOdd(newBackOdds);
			
			
		}
		// Scenario 3: Bet is lay on team 1 and favorite team is team 2 -> reject bet
		// Scenario 4: Bet is lay on team 2 and favorite team is team 1 -> reject bet
		else if ("lay".equals(bet.getBetType()) && !betTeamName.equalsIgnoreCase(favTeam)) {
			confirmBet = false;
		}

		// Check for bet on favorite team
		else if ("back".equals(bet.getBetType()) && betTeamName.equalsIgnoreCase(favTeam)) {
			BigDecimal betOddAdjusted = bet.getOdd().subtract(one).multiply(hundred).setScale(1, RoundingMode.HALF_UP);
			BigDecimal fetchedBackOdd = new BigDecimal(fetchedLatestOdds.getTeamOdds().getBackOdds()).setScale(1,
					RoundingMode.HALF_UP);
			if (betOddAdjusted.compareTo(fetchedBackOdd) <= 0) {
				confirmBet = true;
				if (betOddAdjusted.compareTo(fetchedBackOdd) < 0) {					
					bet.setOdd(fetchedBackOdd.subtract(one).divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE));
				}
			}
		} else if ("lay".equals(bet.getBetType()) && betTeamName.equalsIgnoreCase(favTeam)) {
			BigDecimal betOddAdjusted = bet.getOdd().subtract(one).multiply(hundred).setScale(1, RoundingMode.HALF_UP);
			BigDecimal fetchedLayOdd = new BigDecimal(fetchedLatestOdds.getTeamOdds().getLayOdds()).setScale(1,
					RoundingMode.HALF_UP);
			if (betOddAdjusted.compareTo(fetchedLayOdd) >= 0) {
				confirmBet = true;
				if (betOddAdjusted.compareTo(fetchedLayOdd) > 0) {					
					bet.setOdd(fetchedLayOdd.add(one).divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE));
				}
			}
		}

		if (confirmBet) {

			List<Bets> allBetsForMatch = getBetsForMatchForUser(bet.getMatchUrl(), bet.getUser().getId());

			Map<String, List<Bets>> betsByTeam = allBetsForMatch.stream()
					.filter(matchBet -> !matchBet.getIsSessionBet()).collect(Collectors.groupingBy(Bets::getTeamName));

			if (betsByTeam.size() > 1) {
				// Adjust logic to handle bets on both teams
				processMultiTeamBets(bet, currentUsername, betsByTeam);
			} else if (betsByTeam.size() == 1) {
				// Logic when bets are only on one team
				betsByTeam.get(bet.getTeamName()).remove(bet);
				singleTeamBetProcessing(bet, currentUsername, betsByTeam.get(bet.getTeamName()));
			} else {
				// Handle any other unexpected scenario (e.g., no bets or bets on an unexpected
				// team)
				// cancelBet(bet); // Or some other appropriate handling
			}

			// group them by teamName
			// findout teamNames how many are there
			// if 1 is there this existing logic is fine
			// filter all by teamName
		} else {

			// if the bet is not confirmed directly cancel the bet and notify
			cricketDataService.notifyBetStatus(cancelBet(bet));

		}

	}

	private void singleTeamBetProcessing(Bets bet, String currentUsername, List<Bets> allBetsForMatch) {
		Bets updatedBet;
		// This filters the list of bets to include only those that are for the same
		// team as the current bet.
		allBetsForMatch = allBetsForMatch.stream().filter(matchBet -> matchBet.getTeamName().equals(bet.getTeamName()))
				.collect(Collectors.toList());
		// This calculates the total exposure for the user before adding the current
		// bet.
		BigDecimal maxOverallExposurePrevious = calculateNetExposuresInWinLoseCase(allBetsForMatch);
		bet.setStatus("Confirmed");
		// Temporarily add the current bet to the list for exposure calculation also
		allBetsForMatch.add(bet);
		// This calculates the total exposure for the user after adding the current bet.
		BigDecimal maxOverallExposure = calculateNetExposuresInWinLoseCase(allBetsForMatch);
		// Remove the current bet from the list if not intended to be permanently added
		// at this stage
		allBetsForMatch.remove(bet);

		BigDecimal previousAndCurrentExposureDiff = maxOverallExposure.abs().subtract(maxOverallExposurePrevious.abs());
		User user = userService.findOne(currentUsername);
		BigDecimal totalPotentialExposure = BigDecimal.ZERO;
		if (previousAndCurrentExposureDiff.compareTo(BigDecimal.ZERO) >= 0) {
			// exposure has reduced
			totalPotentialExposure = user.getExposure().add(previousAndCurrentExposureDiff.abs());
		} else {
			// exposure has increased
			totalPotentialExposure = user.getExposure().subtract(previousAndCurrentExposureDiff.abs());
		}
		// Check if user balance covers the maximum overall exposure
		if (user.getBalance().compareTo(totalPotentialExposure) >= 0) {
			user.setExposure(totalPotentialExposure);
			User updateUser = userService.updateUser(user);
			updatedBet = betRepository.save(bet);
			updatedBet.setUser(updateUser);
		} else {
			// Handle insufficient balance case
			// I will simply cancell the bet and update the bets table
			updatedBet = cancelBet(bet);
		}

		// Notify frontend about the bet confirmations
		cricketDataService.notifyBetStatus(updatedBet);
	}

	private void processTestMatchBets(Bets bet, String currentUsername, Map<String, List<Bets>> betsByTeam) {
		// Preliminary checks and setup
		List<Bets> allBetsForMatch = getBetsForMatchForUser(bet.getMatchUrl(), bet.getUser().getId());

		Map<String, BigDecimal> initialAdjustedExposures = calculateAdjustedExposures(allBetsForMatch);

		bet.setStatus("Confirmed");
		// Add the current bet to its respective outcome list before calculating
		// exposures
		allBetsForMatch.add(bet);

		// Calculate adjusted exposures for all outcomes, including the draw
		Map<String, BigDecimal> adjustedExposures = calculateAdjustedExposures(allBetsForMatch);

		// Determine the maximum exposure from initial and updated exposures
		BigDecimal initialMaxExposure = calculateMaxExposure(initialAdjustedExposures);
		BigDecimal updatedMaxExposure = calculateMaxExposure(adjustedExposures);

		BigDecimal exposureDifference = updatedMaxExposure.subtract(initialMaxExposure);

		User user = userService.findOne(currentUsername);
		BigDecimal updatedPotentialDifference = calculateTotalPotentialExposure(user, exposureDifference);

		// Decision making based on the total potential exposure
		if (user.getBalance().compareTo(updatedPotentialDifference) >= 0) {
			// Update the user's exposure and confirm the bet if the user's balance covers
			// it
			user.setExposure(updatedPotentialDifference); // Example update, adjust as necessary
			User updateUser = userService.updateUser(user);
			bet.setStatus("Confirmed");
			Bets savedBet = betRepository.save(bet);
			savedBet.setUser(updateUser);
			cricketDataService.notifyBetStatus(savedBet);
		} else {
			// Cancel the bet if the user's balance does not cover the potential exposure
			cancelBet(bet);
		}
	}

	private BigDecimal calculateMaxExposure(Map<String, BigDecimal> adjustedExposures) {
		// This method calculates the maximum exposure from the adjusted exposures of
		// all outcomes
		return adjustedExposures.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
	}

	private Map<String, BigDecimal> calculateAdjustedExposures(List<Bets> bets) {
		Map<String, BigDecimal> adjustedExposures = new HashMap<>();

		// Initialize containers for payouts and stakes by team
		Map<String, BigDecimal> backBetPayoutsByTeam = new HashMap<>();
		Map<String, BigDecimal> layBetPayoutByTeam = new HashMap<>();
		Map<String, BigDecimal> totalBackStakesByTeam = new HashMap<>();
		Map<String, BigDecimal> totalLayStakesByTeam = new HashMap<>();

		// Populate back and lay bets payouts and stakes by team
		bets.forEach(bet -> {
			if ("Confirmed".equalsIgnoreCase(bet.getStatus())) {
				BigDecimal stake = bet.getAmount();
				BigDecimal odds = bet.getOdd();
				BigDecimal payout = stake.multiply(odds.subtract(BigDecimal.ONE));
				String teamName = bet.getTeamName();

				if ("back".equals(bet.getBetType())) {
					backBetPayoutsByTeam.merge(teamName, payout, BigDecimal::add);
					totalBackStakesByTeam.merge(teamName, stake, BigDecimal::add);
				} else if ("lay".equals(bet.getBetType())) {
					layBetPayoutByTeam.merge(teamName, payout, BigDecimal::add);
					totalLayStakesByTeam.merge(teamName, stake, BigDecimal::add);
				}
			}
		});

		// Calculate adjusted exposure for each unique team, including "Draw"
		totalBackStakesByTeam.keySet().forEach(teamName -> {
			BigDecimal totalBackStakesForOtherTeams = totalBackStakesByTeam.entrySet().stream()
					.filter(entry -> !entry.getKey().equals(teamName)).map(Map.Entry::getValue)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			BigDecimal totalLayStakesForOtherTeams = totalLayStakesByTeam.entrySet().stream()
					.filter(entry -> !entry.getKey().equals(teamName)).map(Map.Entry::getValue)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			BigDecimal backBetPayout = backBetPayoutsByTeam.getOrDefault(teamName, BigDecimal.ZERO);
			BigDecimal layBetPayout = layBetPayoutByTeam.getOrDefault(teamName, BigDecimal.ZERO);
			BigDecimal adjustedWinExposure = backBetPayout.subtract(layBetPayout).subtract(totalBackStakesForOtherTeams)
					.add(totalLayStakesForOtherTeams);

			if (!adjustedWinExposure.equals(BigDecimal.ZERO)) {
				adjustedExposures.put(teamName + " Adjusted Win", adjustedWinExposure);
			}
		});

		return adjustedExposures;
	}

	private void processMultiTeamBets(Bets bet, String currentUsername, Map<String, List<Bets>> betsByTeam) {

		Map<String, Map<String, BigDecimal>> initialExposures = calculateMatchExposures(betsByTeam);
		Map<String, BigDecimal> adjustedExposures = adjustExposuresForAllTeams(initialExposures);
		updateExposuresWithCurrentBet(bet, betsByTeam); // Adds current bet and recalculates exposures
		// again calculated adjusted exposure with added bet
		Map<String, Map<String, BigDecimal>> postBetExposures = calculateMatchExposures(betsByTeam);
		Map<String, BigDecimal> postBetadjustedExposures = adjustExposuresForAllTeams(postBetExposures);
		User user = userService.findOne(currentUsername);
		// user the again calculated adjusted exposure instead of initalExposure here
		// inside this function
		adjustUserExposureBasedOnBet(user, bet, adjustedExposures, postBetadjustedExposures);

	}

	private void adjustUserExposureBasedOnBet(User user, Bets bet, Map<String, BigDecimal> adjustedExposures,
			Map<String, BigDecimal> postBetadjustedExposures) {
		BigDecimal prvWinExposure = adjustedExposures.get(bet.getTeamName() + " Adjusted Win");
		BigDecimal prvLoseExposure = adjustedExposures.get(bet.getTeamName() + " Adjusted Lose");
		BigDecimal maxPrvExposure = BigDecimal.ZERO;
//
//		// Negative values of exposure indicate a potential loss. By focusing on the
//		// most negative value (i.e., the maximum potential loss), the system ensures
//		// that the user's risk is properly managed.
//		// Goal: Find the maximum potential loss (maxPrvExposure) before placing the new
//		// bet.
		if (prvWinExposure.compareTo(BigDecimal.ZERO) < 0 && prvLoseExposure.compareTo(BigDecimal.ZERO) < 0) {
			// Both are negative, compare to find the more negative value
			maxPrvExposure = prvWinExposure.min(prvLoseExposure);
		} else if (prvWinExposure.compareTo(BigDecimal.ZERO) < 0) {
			// Only prvWinExposure is negative
			maxPrvExposure = prvWinExposure;
		} else if (prvLoseExposure.compareTo(BigDecimal.ZERO) < 0) {
			// Only prvLoseExposure is negative
			maxPrvExposure = prvLoseExposure;
		}

		// here
		BigDecimal updtWinExposure = postBetadjustedExposures.get(bet.getTeamName() + " Adjusted Win");
		BigDecimal updtLoseExposure = postBetadjustedExposures.get(bet.getTeamName() + " Adjusted Lose");
		BigDecimal updtMaxExposure = BigDecimal.ZERO;

		// Negative values of exposure indicate a potential loss. By focusing on the
		// most negative value (i.e., the maximum potential loss), the system ensures
		// that the user's risk is properly managed.
		// Goal: Find the maximum potential loss (updtMaxExposure) after placing the new
		// bet.
		if (updtWinExposure.compareTo(BigDecimal.ZERO) < 0 && updtLoseExposure.compareTo(BigDecimal.ZERO) < 0) {
			// Both are negative, compare to find the more negative value
			// When both prvWinExposure and prvLoseExposure are negative, it indicates that
			// in both win and lose scenarios, the user will incur a loss. The goal is to
			// identify which scenario leads to a greater loss.
			updtMaxExposure = updtWinExposure.min(updtLoseExposure);
		} else if (updtWinExposure.compareTo(BigDecimal.ZERO) < 0) {
			// Only prvWinExposure is negative
			updtMaxExposure = updtWinExposure;
		} else if (updtLoseExposure.compareTo(BigDecimal.ZERO) < 0) {
			// Only prvLoseExposure is negative
			updtMaxExposure = updtLoseExposure;
		} else {
			// Both are positive or zero, indicating no potential loss
			BigDecimal updatedExposureDiff = updtMaxExposure.subtract(maxPrvExposure.abs());
			BigDecimal latestExposure = user.getExposure().subtract(updatedExposureDiff.abs());
			user.setExposure(latestExposure);
			User updateUser = userService.updateUser(user);
			bet.setStatus("Confirmed");
			Bets savedBet = betRepository.save(bet);
			savedBet.setUser(updateUser);
			cricketDataService.notifyBetStatus(savedBet);

			return;

		}

		// By calculating the difference between the new and old maximum potential
		// losses, the system can determine how the user's risk profile has changed due
		// to the new bet
		// Goal: Calculate the difference in the worst-case scenario of potential loss
		// due to the new bet.
		BigDecimal updatedExposureDiff = updtMaxExposure.abs().subtract(maxPrvExposure.abs());

		// Goal: Update the user's total potential exposure and decide whether to
		// confirm or cancel the bet based on this updated exposure.
//		BigDecimal totalPotentialExposure = calculateTotalPotentialExposure(user, updtMaxExposure);
		confirmOrCancelBetAndUpdateUser(user, bet, updatedExposureDiff);
	}

	public Map<String, BigDecimal> adjustExposuresForAllTeams(Map<String, Map<String, BigDecimal>> initialExposures) {
		Map<String, BigDecimal> adjustedExposures = new HashMap<>();

		initialExposures.forEach((team, exposures) -> {
			AtomicReference<BigDecimal> winExposure = new AtomicReference<>(exposures.get("WinExposure"));
			AtomicReference<BigDecimal> loseExposure = new AtomicReference<>(exposures.get("LoseExposure"));

			initialExposures.keySet().stream().filter(otherTeam -> !otherTeam.equals(team)).forEach(otherTeam -> {
				Map<String, BigDecimal> otherExposures = initialExposures.get(otherTeam);
				winExposure.set(winExposure.get().add(otherExposures.get("LoseExposure")));
				loseExposure.set(loseExposure.get().add(otherExposures.get("WinExposure")));
			});

			adjustedExposures.put(team + " Adjusted Win", winExposure.get());
			adjustedExposures.put(team + " Adjusted Lose", loseExposure.get());
		});

		return adjustedExposures;
	}

	private BigDecimal calculateTotalPotentialExposure(User user, BigDecimal exposureDifference) {
		if (exposureDifference.compareTo(BigDecimal.ZERO) > 0) {
			return user.getExposure().add(exposureDifference);
		} else {
			return user.getExposure().subtract(exposureDifference.abs());
		}
	}

	private void confirmOrCancelBetAndUpdateUser(User user, Bets bet, BigDecimal totalPotentialExposure) {
		// not adding the absolte value of totalPotentialExposure as it can also be
		// negative which means the exposure decreased
		BigDecimal latestExposure = user.getExposure().add(totalPotentialExposure);
		if (user.getBalance().compareTo(latestExposure) >= 0) {
			user.setExposure(latestExposure);
			User updateUser = userService.updateUser(user);
			bet.setStatus("Confirmed");
			Bets savedBet = betRepository.save(bet);
			savedBet.setUser(updateUser);
			cricketDataService.notifyBetStatus(savedBet);
		} else {
			cancelBet(bet);
		}
	}

	private void updateExposuresWithCurrentBet(Bets bet, Map<String, List<Bets>> betsByTeam) {
		// This method assumes betsByTeam is mutable and directly updates it
		List<Bets> teamBets = betsByTeam.getOrDefault(bet.getTeamName(), new ArrayList<>());

		// Change the status of all pending bets to "Confirmed" if they match the
		// received bet
		teamBets.forEach(existingBet -> {
			if ("Pending".equals(existingBet.getStatus()) && areBetsEqual(bet, existingBet)) {
				existingBet.setStatus("Confirmed");
			}
		});

		bet.setStatus("Confirmed");
	}

	private boolean areBetsEqual(Bets bet, Bets existingBet) {
		return bet.getBetType().equals(existingBet.getBetType())
				&& bet.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(existingBet.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)) == 0
				&& bet.getOdd().setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(existingBet.getOdd().setScale(2, BigDecimal.ROUND_HALF_UP)) == 0
				&& bet.getTeamName().equals(existingBet.getTeamName())
				&& bet.getBetId().compareTo(existingBet.getBetId()) == 0
				&& bet.getUser().getId() == (existingBet.getUser().getId());
	}

	public Bets cancelBet(Bets bet) {
		bet.setStatus("Cancelled");
		// for safer side saving the cancelled bet
		return betRepository.save(bet);
	}

	public List<Bets> getBetsForMatchForUser(String matchUrl, long userId) {
		return betRepository.findByMatchUrlAndUserId(matchUrl, userId);
	}
	
	public List<Bets> getBetsForMatch(String matchUrl) {
		return betRepository.findByMatchUrlContainingAndConfirmed(matchUrl);
	}

	public Map<String, Map<String, BigDecimal>> calculateMatchExposures(Map<String, List<Bets>> betsByTeam) {
		Map<String, Map<String, BigDecimal>> matchExposures = new HashMap<>();

		betsByTeam.forEach((teamName, teamBets) -> {
			Map<String, BigDecimal> teamExposures = calculateExposuresForTeam(teamBets);
			matchExposures.put(teamName, teamExposures);
		});

		return matchExposures;
	}

	private Map<String, BigDecimal> calculateExposuresForTeam(List<Bets> teamBets) {

		BigDecimal netLayStake = BigDecimal.ZERO;
		BigDecimal netBackStake = BigDecimal.ZERO;

		BigDecimal netLayExposure = BigDecimal.ZERO;
		BigDecimal netBackExposure = BigDecimal.ZERO;

		for (Bets bet : teamBets) {
			if ("Confirmed".equalsIgnoreCase(bet.getStatus())) {
				BigDecimal stake = bet.getAmount(); // Declare stake at the beginning
				BigDecimal odds = bet.getOdd();
				BigDecimal liability = stake.multiply(odds.subtract(BigDecimal.ONE)); // Calculate liability after stake
																						// is declared

				if ("back".equals(bet.getBetType())) {
					netBackStake = netBackStake.add(stake);
					netBackExposure = netBackExposure.add(stake.multiply(odds).subtract(stake)); // Potential Profit
				} else if ("lay".equals(bet.getBetType())) {
					netLayStake = netLayStake.add(stake);
					netLayExposure = netLayExposure.add(liability); // Potential Liability
				}
			}
		}

		BigDecimal winExposure = netBackExposure.subtract(netLayExposure); // Positive if back bets' wins > lay bets'
																			// liabilities
		BigDecimal loseExposure = netLayStake.subtract(netBackStake);

		Map<String, BigDecimal> winLoseExposureMap = new HashMap<String, BigDecimal>();

		winLoseExposureMap.put("WinExposure", winExposure);
		winLoseExposureMap.put("LoseExposure", loseExposure);

		return winLoseExposureMap;

	}

	public BigDecimal calculateNetExposuresInWinLoseCase(List<Bets> bets) {
		// Total stake for all lay bets.
		BigDecimal netLayStake = BigDecimal.ZERO;
		// Total stake for all back bets.
		BigDecimal netBackStake = BigDecimal.ZERO;

		// Total exposure (potential liability) for all lay bets.
		BigDecimal netLayExposure = BigDecimal.ZERO;
		// Total exposure (potential profit) for all back bets.
		BigDecimal netBackExposure = BigDecimal.ZERO;

		// The function iterates over all the bets in the provided list.
		// It only considers bets with the status "Confirmed".
		for (Bets bet : bets) {
			if ("Confirmed".equalsIgnoreCase(bet.getStatus())) {
				BigDecimal stake = bet.getAmount(); // For each bet, it extracts the stake and odds.
				BigDecimal odds = bet.getOdd();
				BigDecimal liability = stake.multiply(odds.subtract(BigDecimal.ONE)); // Calculate liability after stake
																						// is declared

				if ("back".equals(bet.getBetType())) {
					netBackStake = netBackStake.add(stake);
					netBackExposure = netBackExposure.add(stake.multiply(odds).subtract(stake)); // Potential Profit
				} else if ("lay".equals(bet.getBetType())) {
					netLayStake = netLayStake.add(stake);
					netLayExposure = netLayExposure.add(liability); // Potential Liability
				}
			}
		}

		BigDecimal winExposure = netBackExposure.subtract(netLayExposure); // Positive if back bets' wins > lay bets'
																			// liabilities
		BigDecimal loseExposure = netLayStake.subtract(netBackStake);

		// Determine which exposure is smaller (more negative) and return it
		BigDecimal moreNegativeExposure = winExposure.min(loseExposure); // Find the smaller (more negative) value

		// If you specifically want to ensure the value returned is negative, you can
		// add an additional check
		if (moreNegativeExposure.compareTo(BigDecimal.ZERO) > 0) {
			// If the more negative exposure is actually positive, you can choose to return
			// 0 or some indicator value
			return BigDecimal.ZERO; // or any other value you deem appropriate to indicate no negative exposure
		} else {
			return moreNegativeExposure;
		}
	}

	private BigDecimal calculatePotentialWin(Bets bet) {
		return bet.getAmount().multiply(bet.getOdd().subtract(BigDecimal.ONE));
	}

	public Map<String, ProfitLossDTO> calculateOverallProfitLoss(Long userId, Date startDate, Date endDate) {
	    List<Transaction> transactions = transactionRepository.findTransactionsBetweenDates(startDate, endDate, userId);

	    // Group transactions by match key
	    Map<String, List<Transaction>> groupedTransactions = transactions.stream()
	            .collect(Collectors.groupingBy(transaction -> extractMatchKey(transaction.getRemark())));

	    Map<String, ProfitLossDTO> profitLossMap = new HashMap<>();

	    for (Map.Entry<String, List<Transaction>> entry : groupedTransactions.entrySet()) {
	        String matchKey = entry.getKey();
	        List<Transaction> matchTransactions = entry.getValue();

	        // Sort transactions by date
	        matchTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));

	        BigDecimal totalProfit = BigDecimal.ZERO;
	        BigDecimal totalLoss = BigDecimal.ZERO;
	        BigDecimal initialBalance = BigDecimal.ZERO;
	        BigDecimal finalBalance = BigDecimal.ZERO;
	        String remark = "";
	        String status = "";
	        Date transactionDate = new Date();

	        if (!matchTransactions.isEmpty()) {
	            initialBalance = matchTransactions.get(0).getBalanceAfterTransaction().subtract(matchTransactions.get(0).getAmount());
	            finalBalance = matchTransactions.get(matchTransactions.size() - 1).getBalanceAfterTransaction();
	        }

	        for (Transaction transaction : matchTransactions) {
	            remark = transaction.getRemark();
	            status = transaction.getStatus();
	            transactionDate = transaction.getTransactionDate();

	            BigDecimal amount = transaction.getAmount();

	            if ("Credit".equalsIgnoreCase(transaction.getTransactionType())) {
	                totalProfit = totalProfit.add(amount);
	            } else {
	                totalLoss = totalLoss.add(amount);
	            }
	        }

	        // Calculate net profit or loss
	        BigDecimal netResult = finalBalance.subtract(initialBalance);
	        String transactionType = netResult.compareTo(BigDecimal.ZERO) > 0 ? "Credit" : "Debit";

	        // Create the ProfitLossDTO
	        ProfitLossDTO profitLossDTO = new ProfitLossDTO(
	                netResult,
	                finalBalance,
	                "cricket / " + matchKey,
	                status,
	                transactionDate,
	                transactionType
	        );

	        profitLossMap.put(matchKey, profitLossDTO);
	    }

	    return profitLossMap;
	}
	
	private String extractMatchKey(String remark) {
        // Implement your logic to extract the match key from the remark
        // For example, you might split the remark by "/" and take the relevant part
        String[] parts = remark.split("/");
        return parts[parts.length - 3]; // Adjust this based on your actual remark format
    }

	private String getWinningTeamForMatch(String matchUrl) {
		// Logic to retrieve the winning team for the match based on the match URL
		// This could involve querying your match repository or another data source
		LiveMatch match = liveMatchService.findByUrl(matchUrl);
		return match != null ? match.getWinningTeam() : null;
	}

	private void handleSessionBet(Bets bet, String currentUsername, CricketDataDTO latestOdds) {

		boolean confirmBet = false;
		CricketDataDTO fetchedLatestOdds = fetchLatestOdds(bet, latestOdds);
		if (fetchedLatestOdds == null) {
			cricketDataService.notifyBetStatus(cancelBet(bet));
			return;
		}

		SessionOdds fetchedSessionOdds = fetchedLatestOdds.getSessionOdds();

		if (fetchedSessionOdds == null) {
			cricketDataService.notifyBetStatus(cancelBet(bet));
			return;
		}

		// Session bets are either YES or NO bets
		// YES bet on session over: accept if bet odds <= fetched back odds
		// NO bet on session over: accept if bet odds >= fetched lay odds
		if ("yes".equals(bet.getBetType())) {
			BigDecimal betOddAdjusted = bet.getOdd().setScale(1, RoundingMode.HALF_UP);
			BigDecimal fetchedLayOdd = new BigDecimal(fetchedSessionOdds.getSessionLayOdds()).setScale(1,
					RoundingMode.HALF_UP);
			if (fetchedLayOdd.compareTo(betOddAdjusted) <= 0) {
				confirmBet = true;
			}
		} else if ("no".equals(bet.getBetType())) {
			BigDecimal betOddAdjusted = bet.getOdd().setScale(1, RoundingMode.HALF_UP);
			BigDecimal fetchedBackOdd = new BigDecimal(fetchedSessionOdds.getSessionBackOdds()).setScale(1,
					RoundingMode.HALF_UP);
			if (fetchedBackOdd.compareTo(betOddAdjusted) >= 0) {
				confirmBet = true;
			}
		}

		if (confirmBet) {
			List<Bets> allBetsForMatch = getBetsForMatchForUser(bet.getMatchUrl(), bet.getUser().getId());

			Map<String, List<Bets>> betsBySession = allBetsForMatch.stream()
					.filter(b -> b.getIsSessionBet() != null && b.getIsSessionBet()
							&& b.getSessionName().equalsIgnoreCase(bet.getSessionName())
							&& "Confirmed".equalsIgnoreCase(b.getStatus())
							&& b.getTeamName().equalsIgnoreCase(bet.getTeamName()))
					.collect(Collectors.groupingBy(Bets::getSessionName));

			/*
			 * if (betsBySession.size() > 1) { // Adjust logic to handle bets on multiple
			 * sessions processMultiSessionBets(bet, currentUsername, betsBySession); } else
			 * if (betsBySession.size() == 1) { // Logic when bets are only on one session
			 * singleSessionBetProcessing(bet, currentUsername,
			 * betsBySession.get(bet.getSessionName())); } else { // Handle any other
			 * unexpected scenario (e.g., no bets or bets on an unexpected session) //
			 * cancelBet(bet); // Or some other appropriate handling }
			 */
			singleSessionBetProcessing(bet, currentUsername, betsBySession.get(bet.getSessionName()));
		} else {
			// If the bet is not confirmed directly cancel the bet and notify
			cricketDataService.notifyBetStatus(cancelBet(bet));
		}
	}

	private void singleSessionBetProcessing(Bets bet, String currentUsername, List<Bets> existingBets) {
		if (existingBets == null || existingBets.isEmpty()) {
			// Handle the first bet scenario
			processFirstSessionBet(bet, currentUsername);
			return;
		}

		BigDecimal previousExposure = calculateSessionExposure(existingBets);

		// Add the new bet to the existing bets
		Bets newBetCopy = deepCopyBet(bet);
		List<Bets> newExistingBetsCopy = existingBets.stream().map(this::deepCopyBet).collect(Collectors.toList());
		newExistingBetsCopy.add(newBetCopy);

		// Calculate new exposure after adding the new bet
		BigDecimal newExposure = calculateSessionExposure(newExistingBetsCopy);

		// Calculate the change in exposure
		BigDecimal exposureChange = newExposure.subtract(previousExposure);

		// Check if the user has enough balance to cover the total exposure
		User user = userService.findOne(currentUsername);
		BigDecimal userBalance = user.getBalance();
		if (userBalance.compareTo(exposureChange.abs()) >= 0) {
			// Accept the bet and update user balance and exposure
			if (exposureChange.compareTo(BigDecimal.ZERO) >= 0) {
				user.setExposure(user.getExposure().add(exposureChange.abs()));
			} else {
				user.setExposure(user.getExposure().subtract(exposureChange.abs()));
			}
			userService.updateUser(user);
			bet.setStatus("Confirmed");
			
			cricketDataService.notifyBetStatus(betRepository.save(bet));
		} else {

			// Reject the bet due to insufficient balance
			bet.setStatus("Cancelled");
			cricketDataService.notifyBetStatus(betRepository.save(bet));
		}
	}

	public BigDecimal calculateSessionExposure(List<Bets> existingBets) {
		// Create deep copies of existing bets
		List<Bets> existingBetsCopy = existingBets.stream().map(this::deepCopyBet).collect(Collectors.toList());

		// Separate Yes and No bets
		List<Bets> yesBets = existingBetsCopy.stream().map(this::deepCopyBet)
				.filter(b -> "yes".equalsIgnoreCase(b.getBetType())).collect(Collectors.toList());

		List<Bets> noBets = existingBetsCopy.stream().map(this::deepCopyBet).filter(b -> "no".equalsIgnoreCase(b.getBetType()))
				.collect(Collectors.toList());

		// Calculate previous exposure considering overlapping bets
		BigDecimal previousExposure = calculateAdustedSessionExposure(yesBets, noBets);
		return previousExposure;
	}
	
	public ExposureResult calculateSessionExposureExtended(List<Bets> existingBets) {
		// Create deep copies of existing bets
		List<Bets> existingBetsCopy = existingBets.stream().map(this::deepCopyBet).collect(Collectors.toList());

		// Separate Yes and No bets
		List<Bets> yesBets = existingBetsCopy.stream().map(this::deepCopyBet)
				.filter(b -> "yes".equalsIgnoreCase(b.getBetType())).collect(Collectors.toList());

		List<Bets> noBets = existingBetsCopy.stream().map(this::deepCopyBet).filter(b -> "no".equalsIgnoreCase(b.getBetType()))
				.collect(Collectors.toList());

		// Calculate previous exposure considering overlapping bets
		ExposureResult previousExposure = calculateAdjustedSessionExposureExtended(yesBets, noBets);
		return previousExposure;
	}
	
	public ExposureResult calculateAdjustedSessionExposureExtended(List<Bets> yesBets, List<Bets> noBets) {
	    List<Bets> sortedYesBets = yesBets.stream().map(this::deepCopyBet)
	            .sorted(Comparator.comparing(Bets::getOdd)).collect(Collectors.toList());

	    List<Bets> sortedNoBets = noBets.stream().map(this::deepCopyBet)
	            .sorted(Comparator.comparing(Bets::getOdd).reversed()).collect(Collectors.toList());

	    BigDecimal adjustedYesExposure = sortedYesBets.stream().map(Bets::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	    BigDecimal adjustedNoExposure = sortedNoBets.stream().map(Bets::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

	    for (Bets yesBet : sortedYesBets) {
	        for (Bets noBet : sortedNoBets) {
	            if (yesBet.getAmount().compareTo(BigDecimal.ZERO) > 0 && noBet.getAmount().compareTo(BigDecimal.ZERO) > 0) {
	                if (yesBet.getOdd().compareTo(noBet.getOdd()) < 0) {
	                    BigDecimal arbitrageAmount = yesBet.getAmount().min(noBet.getAmount());
	                    adjustedYesExposure = adjustedYesExposure.subtract(arbitrageAmount);
	                    adjustedNoExposure = adjustedNoExposure.subtract(arbitrageAmount);

	                    yesBet.setAmount(yesBet.getAmount().subtract(arbitrageAmount));
	                    noBet.setAmount(noBet.getAmount().subtract(arbitrageAmount));
	                }
	            }
	        }
	    }

	    String higherExposure = adjustedYesExposure.compareTo(adjustedNoExposure) > 0 ? "Yes" : "No";

	    return new ExposureResult(adjustedYesExposure, adjustedNoExposure, higherExposure);
	}

	private boolean isBetWinning(Bets bet, SessionOverData sessionResult) {
		int sessionScore = Integer.parseInt(sessionResult.getPass());
		if ("yes".equalsIgnoreCase(bet.getBetType()) && sessionScore >= Integer.valueOf(bet.getOdd().intValue())) {
			return true;
		} else if ("no".equalsIgnoreCase(bet.getBetType()) && sessionScore < Integer.valueOf(bet.getOdd().intValue())) {
			return true;
		}
		return false;
	}

	private BigDecimal calculateAdustedSessionExposure(List<Bets> yesBets, List<Bets> noBets) {
		List<Bets> sortedYesBets = yesBets.stream().map(this::deepCopyBet).sorted(Comparator.comparing(Bets::getOdd))
				.collect(Collectors.toList());

		List<Bets> sortedNoBets = noBets.stream().map(this::deepCopyBet).sorted(Comparator.comparing(Bets::getOdd).reversed())
				.collect(Collectors.toList());

		BigDecimal adjustedYesExposure = sortedYesBets.stream().map(Bets::getAmount).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		BigDecimal adjustedNoExposure = sortedNoBets.stream().map(Bets::getAmount).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		for (Bets yesBet : sortedYesBets) {
			for (Bets noBet : sortedNoBets) {
				if (yesBet.getAmount().compareTo(BigDecimal.ZERO) > 0
						&& noBet.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					if (yesBet.getOdd().compareTo(noBet.getOdd()) < 0) {
						BigDecimal arbitrageAmount = yesBet.getAmount().min(noBet.getAmount());
						adjustedYesExposure = adjustedYesExposure.subtract(arbitrageAmount);
						adjustedNoExposure = adjustedNoExposure.subtract(arbitrageAmount);

						yesBet.setAmount(yesBet.getAmount().subtract(arbitrageAmount));
						noBet.setAmount(noBet.getAmount().subtract(arbitrageAmount));
					}
				}
			}
		}

		return adjustedYesExposure.add(adjustedNoExposure);
	}

	private Bets deepCopyBet(Bets bet) {
		Bets copy = new Bets();
		copy.setBetId(bet.getBetId());
		copy.setAmount(bet.getAmount());
		copy.setBetType(bet.getBetType());
		copy.setIsSessionBet(bet.getIsSessionBet());
		copy.setMatchUrl(bet.getMatchUrl());
		copy.setOdd(bet.getOdd());
		copy.setPlacedAt(bet.getPlacedAt());
		copy.setPotentialWin(bet.getPotentialWin());
		copy.setSessionName(bet.getSessionName());
		copy.setStatus(bet.getStatus());
		copy.setTeamName(bet.getTeamName());
		copy.setUser(bet.getUser());
		return copy;
	}

	private void processFirstSessionBet(Bets bet, String currentUsername) {
		User user = userService.findOne(currentUsername);
		BigDecimal totalExposure = bet.getAmount();
		BigDecimal userBalance = user.getBalance();

		if (userBalance.compareTo(totalExposure) >= 0) {
			// Accept the bet and update user balance and exposure
			user.setExposure(user.getExposure().add(totalExposure));
			userService.updateUser(user);
			bet.setStatus("Confirmed");
			Bets save = betRepository.save(bet);
			cricketDataService.notifyBetStatus(save);
		} else {
			// Reject the bet due to insufficient balance
			bet.setStatus("Cancelled");
			Bets save = betRepository.save(bet);
			cricketDataService.notifyBetStatus(save);
		}
	}

}
