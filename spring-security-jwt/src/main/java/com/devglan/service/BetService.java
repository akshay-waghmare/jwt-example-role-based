package com.devglan.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

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
import com.devglan.model.User;
import com.devglan.websocket.service.CricketDataService;

@Service
public class BetService {

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
	@Scheduled(fixedRate = 60000) // Runs every 60 seconds
	public void checkMatchResults() {
		List<LiveMatch> liveMatches = liveMatchService.findAll();
		for (LiveMatch match : liveMatches) {
			if (match.isFinished()) {
				distributeExposureAndWinnings(match);
			}
		}
	}

	private void distributeExposureAndWinnings(LiveMatch match) {

		List<Bets> betsForMatch = betRepository.findByMatchUrl(match.getUrl());

		for (Bets bet : betsForMatch) {
			User user = bet.getUser();
			String winningTeam = match.getWinningTeam();
	        if (winningTeam == null) {
	            return; // No winning team found, skip processing
	        }
	        
			if ("Confirmed".equalsIgnoreCase(bet.getStatus())) {
				/*
				 * if (bet.getTeamName().equalsIgnoreCase(match.getWinningTeam())) { // User won
				 * the bet BigDecimal winnings =
				 * bet.getAmount().multiply(bet.getOdd()).subtract(bet.getAmount());
				 * user.setBalance(user.getBalance().add(winnings).add(bet.getAmount())); //
				 * Adding bet amount back bet.setStatus("Won"); } else { // User lost the bet
				 * BigDecimal exposure = bet.getAmount();
				 * user.setBalance(user.getBalance().subtract(exposure)); // Deducting exposure
				 * bet.setStatus("Lost"); } betRepository.save(bet);
				 * userService.updateUser(user);
				 */
			}
		}
	}

	@Async("taskExecutor")
	@Transactional
	public void checkAndConfirmBet(Bets bet, String currentUsername) {
		try {
			// 2-second delay
			Thread.sleep(2000);

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
			bet.setStatus("Confirmed");
			List<Bets> allBetsForMatch = getBetsForMatch(bet.getMatchUrl(), bet.getUser().getId());

			Map<String, List<Bets>> betsByTeam = allBetsForMatch.stream()
					.collect(Collectors.groupingBy(Bets::getTeamName));

			if ((betsByTeam.size() == 1) && betsByTeam.containsKey(bet.getTeamName())) {
				// Logic when bets are only on one team (existing logic applies)
				singleTeamBetProcessing(bet, currentUsername, betsByTeam.get(bet.getTeamName()));
			} else if (betsByTeam.size() > 1) {
				// Adjust logic to handle bets on both teams
				processMultiTeamBets(bet, currentUsername, betsByTeam);
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
		allBetsForMatch = allBetsForMatch.stream().filter(matchBet -> matchBet.getTeamName().equals(bet.getTeamName()))
				.collect(Collectors.toList());
		// Temporarily add the current bet to the list for exposure calculation
		BigDecimal maxOverallExposurePrevious = calculateNetExposuresInWinLoseCase(allBetsForMatch);
		bet.setStatus("Confirmed");
		allBetsForMatch.add(bet);
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
			userService.updateUser(user);
			updatedBet = betRepository.save(bet);
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

		// Group bets by outcome (Team A, Team B, Draw) to process logic for multiple
		// outcomes
		Map<String, List<Bets>> betsByOutcome = allBetsForMatch.stream()
				.collect(Collectors.groupingBy(Bets::getTeamName));

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
			userService.updateUser(user);
			bet.setStatus("Confirmed");
			betRepository.save(bet);
			cricketDataService.notifyBetStatus(bet);
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
		BigDecimal maxPrvExposure = prvWinExposure.abs().max(prvLoseExposure.abs());

		// here
		BigDecimal updtWinExposure = postBetadjustedExposures.get(bet.getTeamName() + " Adjusted Win");
		BigDecimal updtLoseExposure = postBetadjustedExposures.get(bet.getTeamName() + " Adjusted Lose");
		BigDecimal updtMaxExposure = updtWinExposure.abs().max(updtLoseExposure.abs());

		BigDecimal exposureDifference = updtMaxExposure.subtract(maxPrvExposure);// if positive means exposure increased

		BigDecimal totalPotentialExposure = calculateTotalPotentialExposure(user, exposureDifference);
		confirmOrCancelBetAndUpdateUser(user, bet, totalPotentialExposure);
	}

	private Map<String, BigDecimal> adjustExposuresForAllTeams(Map<String, Map<String, BigDecimal>> initialExposures) {
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
		if (user.getBalance().compareTo(totalPotentialExposure) >= 0) {
			user.setExposure(totalPotentialExposure);
			userService.updateUser(user);
			bet.setStatus("Confirmed");
			betRepository.save(bet);
			cricketDataService.notifyBetStatus(bet);
		} else {
			cancelBet(bet);
		}
	}

	private void updateExposuresWithCurrentBet(Bets bet, Map<String, List<Bets>> betsByTeam) {
		// This method assumes betsByTeam is mutable and directly updates it
		List<Bets> teamBets = betsByTeam.getOrDefault(bet.getTeamName(), new ArrayList<>());
		bet.setStatus("Confirmed");
		teamBets.add(bet);
		betsByTeam.put(bet.getTeamName(), teamBets);
	}

	public Bets cancelBet(Bets bet) {
		bet.setStatus("Cancelled");
		// for safer side saving the cancelled bet
		return betRepository.save(bet);
	}

	public List<Bets> getBetsForMatch(String matchUrl, long userId) {
		return betRepository.findByMatchUrlAndUserId(matchUrl, userId);
	}

	Map<String, Map<String, BigDecimal>> calculateMatchExposures(Map<String, List<Bets>> betsByTeam) {
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
		BigDecimal netLayStake = BigDecimal.ZERO;
		BigDecimal netBackStake = BigDecimal.ZERO;

		BigDecimal netLayExposure = BigDecimal.ZERO;
		BigDecimal netBackExposure = BigDecimal.ZERO;

		for (Bets bet : bets) {
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

}
