package com.devglan.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devglan.dao.BetRepository;
import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.MatchOdds;
import com.devglan.model.Bets;
import com.devglan.model.LiveMatch;
import com.devglan.model.ProfitLoss;
import com.devglan.model.User;
import com.devglan.websocket.service.CricketDataService;

@Service
public class BetService {
	
    private static final Logger logger = LoggerFactory.getLogger(BetService.class);


	@Autowired
	private BetRepository betRepository;
	@Autowired
	private CricketDataService cricketDataService;
	@Autowired
	private UserService userService;
	@Autowired
	private LiveMatchService liveMatchService;

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
			List<LiveMatch> liveMatches = liveMatchService.findAllFinishedMatches();
			logger.debug("Found {} finished matches", liveMatches.size());

			for (LiveMatch match : liveMatches) {
				if (match.isFinished()) {
					logger.debug("Processing finished match: {}", match.getId());
					try {
						distributeExposureAndWinnings(match);
					} catch (Exception e) {
						logger.error("Error distributing exposure and winnings for match: {}", match.getId(), e);
					}
				} else {
					logger.debug("Match {} is not finished", match.getId());
				}
			}
		} catch (Exception e) {
			logger.error("Error in scheduled task checkMatchResults", e);
		}

		logger.info("Scheduled task checkMatchResults finished");
	}

	@Transactional
	public void distributeExposureAndWinnings(LiveMatch match) {
        logger.info("Distributing exposure and winnings for match: {}", match.getId());
		String winningTeam = match.getWinningTeam();
		if (winningTeam == null) {
            logger.warn("No winning team found for match: {}, skipping processing", match.getId());
			return; // No winning team found, skip processing
		}

		List<Bets> betsForMatch = betRepository.findByMatchUrlContainingAndConfirmed(match.getUrl());
        logger.debug("Winning team for match {}: {}", match.getId(), winningTeam);
		// Group bets by user for exposure reversal
		Map<User, List<Bets>> userBetsMap = betsForMatch.stream().collect(Collectors.groupingBy(Bets::getUser));

		for (Entry<User, List<Bets>> entry : userBetsMap.entrySet()) { // Line 49
			User user = entry.getKey();
			List<Bets> userBets = entry.getValue();
            logger.debug("Processing bets for user: {}", user.getId());

			// Group bets by team to handle multi-team logic if necessary
			Map<String, List<Bets>> betsByTeam = userBets.stream().collect(Collectors.groupingBy(Bets::getTeamName));

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

			if (user.getExposure().compareTo(BigDecimal.ZERO) < 0) { // Line 76
				user.setExposure(BigDecimal.ZERO);
			}

			userService.updateUser(user); // Line 78
            logger.debug("User: {}, updated exposure: {}", user.getId(), user.getExposure());

		}

		for (Bets bet : betsForMatch) {
            User user = bet.getUser();
            logger.debug("Processing bet: {} for user: {}", bet.getBetId(), user.getId());

            BigDecimal stake = bet.getAmount();
            BigDecimal odds = bet.getOdd();
            BigDecimal liability = stake.multiply(odds.subtract(BigDecimal.ONE));
            BigDecimal winnings = stake.multiply(odds).subtract(stake);

            // Ensure exposure does not go negative
            if (user.getExposure().compareTo(BigDecimal.ZERO) < 0) {
                user.setExposure(BigDecimal.ZERO);
                logger.debug("User: {}, exposure set to zero before processing bet: {}", user.getId(), bet.getBetId());
            }

            if (bet.getTeamName().equalsIgnoreCase(winningTeam)) {
                // User won the bet
                if ("back".equalsIgnoreCase(bet.getBetType())) {
                    user.setBalance(user.getBalance().add(winnings)); // Add winnings and return stake
                    bet.setStatus("Won");
                    logger.debug("User: {}, bet: {} won, new balance: {}", user.getId(), bet.getBetId(), user.getBalance());
                } else if ("lay".equalsIgnoreCase(bet.getBetType())) {
                    user.setBalance(user.getBalance().subtract(liability)); // Subtract liability
                    bet.setStatus("Lost");
                    logger.debug("User: {}, bet: {} lost, new balance: {}", user.getId(), bet.getBetId(), user.getBalance());
                }
            } else {
                // User lost the bet
                if ("back".equalsIgnoreCase(bet.getBetType())) {
                    user.setBalance(user.getBalance().subtract(stake)); // Subtract stake
                    bet.setStatus("Lost");
                    logger.debug("User: {}, bet: {} lost, new balance: {}", user.getId(), bet.getBetId(), user.getBalance());
                } else if ("lay".equalsIgnoreCase(bet.getBetType())) {
                    user.setBalance(user.getBalance().add(stake)); // Return stake
                    bet.setStatus("Won");
                    logger.debug("User: {}, bet: {} won, new balance: {}", user.getId(), bet.getBetId(), user.getBalance());
                }
            }

            betRepository.save(bet);
            userService.updateUser(user);
            logger.debug("Bet: {}, updated status: {}, user: {}, updated balance: {}", bet.getBetId(), bet.getStatus(), user.getId(), user.getBalance());
        }

	}

	@Async("taskExecutor")
	@Transactional
	public void checkAndConfirmBet(Bets bet, String currentUsername) {
		try {
			// 5-second delay
			Thread.sleep(5000);

			// Fetch latest odds for the event
			CricketDataDTO latestOdds = null;
			latestOdds = fetchLatestOdds(bet, latestOdds);

			if (latestOdds == null) {
				cancelBet(bet);
			}
			List<MatchOdds> matchOdds = latestOdds.getMatchOdds();
			Optional<MatchOdds> matchingOdds = java.util.Optional.empty();
			if (matchOdds != null) {

				// This is the test match odds scenario if the matchOdds is not null for a match
				// url
				matchingOdds = latestOdds.getMatchOdds().stream()
						.filter(team -> bet.getTeamName().equals(team.getTeamName())).findFirst();
			}

			if (!matchingOdds.isPresent()) {
				// this is a one day match scenario
				handleOneDayMatch(bet, currentUsername, latestOdds);
			} else {
				// this is the test match scenario
				handleMatchWithTestOdds(bet, currentUsername, latestOdds);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// Handle the interruption accordingly
		}
	}

	private void handleMatchWithTestOdds(Bets bet, String currentUsername, CricketDataDTO latestOdds) {
		// handle similarly how the processMultiTeamBets
		boolean confirmBet = confirmTestBet(bet, latestOdds);

		if (confirmBet) {
			List<Bets> allBetsForMatch = getBetsForMatch(bet.getMatchUrl(), bet.getUser().getId());

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
		if ("back".equals(bet.getBetType()) && bet.getOdd().subtract(one).multiply(hundred)
				.compareTo(BigDecimal.valueOf(Double.parseDouble(latestOdds.getTeamOdds().getBackOdds()))) <= 0) {
			confirmBet = true;
		} else if (bet.getOdd().subtract(one).multiply(hundred)
				.compareTo(BigDecimal.valueOf(Double.parseDouble(latestOdds.getTeamOdds().getLayOdds()))) >= 0) {
			confirmBet = true;
		}

		if (confirmBet) {

			List<Bets> allBetsForMatch = getBetsForMatch(bet.getMatchUrl(), bet.getUser().getId());

			Map<String, List<Bets>> betsByTeam = allBetsForMatch.stream()
					.collect(Collectors.groupingBy(Bets::getTeamName));

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
		List<Bets> allBetsForMatch = getBetsForMatch(bet.getMatchUrl(), bet.getUser().getId());

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
			updtMaxExposure = BigDecimal.ZERO;
			BigDecimal updatedExposureDiff = updtMaxExposure.abs().subtract(maxPrvExposure.abs());
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
		// not adding the absolte value of totalPotentialExposure as it can also be negative which means the exposure decreased
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
				&& bet.getAmount().compareTo(existingBet.getAmount()) == 0
				&& bet.getOdd().compareTo(existingBet.getOdd()) == 0
				&& bet.getTeamName().equals(existingBet.getTeamName())
				&& bet.getUser().getId() == (existingBet.getUser().getId());
	}

	public Bets cancelBet(Bets bet) {
		bet.setStatus("Cancelled");
		// for safer side saving the cancelled bet
		return betRepository.save(bet);
	}

	public List<Bets> getBetsForMatch(String matchUrl, long userId) {
		return betRepository.findByMatchUrlAndUserId(matchUrl, userId);
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
	
	public List<ProfitLoss> calculateProfitLoss(Long userId) {
	    List<Bets> bets = getBetsByUserId(userId);
	    Map<String, List<Bets>> betsByMatch = bets.stream()
	            .collect(Collectors.groupingBy(Bets::getMatchUrl));

	    return betsByMatch.entrySet().stream().map(entry -> {
	        String matchUrl = entry.getKey();
	        List<Bets> matchBets = entry.getValue();
	        String winningTeam = getWinningTeamForMatch(matchUrl); // Method to get the winning team for the match
	        BigDecimal netProfitOrLoss = BigDecimal.ZERO;

	        for (Bets bet : matchBets) {
	            BigDecimal potentialWin = calculatePotentialWin(bet);
	            if (bet.getTeamName().equalsIgnoreCase(winningTeam)) {
	                if ("back".equalsIgnoreCase(bet.getBetType())) {
	                    netProfitOrLoss = netProfitOrLoss.add(potentialWin);
	                } else if ("lay".equalsIgnoreCase(bet.getBetType())) {
	                    BigDecimal liability = bet.getAmount().multiply(bet.getOdd().subtract(BigDecimal.ONE));
	                    netProfitOrLoss = netProfitOrLoss.subtract(liability);
	                }
	            } else {
	                if ("back".equalsIgnoreCase(bet.getBetType())) {
	                    netProfitOrLoss = netProfitOrLoss.subtract(bet.getAmount());
	                } else if ("lay".equalsIgnoreCase(bet.getBetType())) {
	                    netProfitOrLoss = netProfitOrLoss.add(bet.getAmount());
	                }
	            }
	        }

	        return new ProfitLoss(matchUrl, netProfitOrLoss);
	    }).collect(Collectors.toList());
	}

	private String getWinningTeamForMatch(String matchUrl) {
	    // Logic to retrieve the winning team for the match based on the match URL
	    // This could involve querying your match repository or another data source
	    LiveMatch match = liveMatchService.findByUrl(matchUrl);
	    return match != null ? match.getWinningTeam() : null;
	}
}
