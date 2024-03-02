package com.devglan.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.devglan.dao.BetRepository;
import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.MatchOdds;
import com.devglan.model.Bets;
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

	@Async("taskExecutor")
	@Transactional
	public void checkAndConfirmBet(Bets bet, String currentUsername) {
		try {
			// 2-second delay
			Thread.sleep(2000);

			// Fetch latest odds for the event
			CricketDataDTO latestOdds = null;
			String completeUrl = liveMatchService.findAll().stream()
				    .filter(liveMatch -> liveMatch.getUrl().contains(bet.getMatchUrl()))
				    .findFirst()
				    .map(liveMatch -> liveMatchService.appendBaseUrl(liveMatch.getUrl()))
				    .orElse(null);
			if(completeUrl != null) {				
				latestOdds = cricketDataService.getLastUpdatedData(completeUrl); // Adjust based on
			}
																								
			if (latestOdds == null) {
				bet.setStatus("Cancelled");
				// for safer side saving the cancelled bet
				betRepository.save(bet);
			}
			List<MatchOdds> matchOdds = latestOdds.getMatchOdds();
			Optional<MatchOdds> matchingOdds = java.util.Optional.empty();
			if (matchOdds != null) {

				// This is the test match odds scenario if the matchOdds is not null for a match
				// url
				matchingOdds = latestOdds.getMatchOdds().stream()
						.filter(team -> bet.getTeamName().equals(team.getTeamName())).findFirst();
			}
			matchingOdds.ifPresent(team -> {
				boolean confirmBet = false;
				if ("back".equals(bet.getBetType()) && bet.getOdd()
						.compareTo(BigDecimal.valueOf(Double.parseDouble(team.getOdds().getBackOdds()))) <= 0) {
					confirmBet = true;
				} else if ("lay".equals(bet.getBetType()) && bet.getOdd()
						.compareTo(BigDecimal.valueOf(Double.parseDouble(team.getOdds().getLayOdds()))) >= 0) {
					confirmBet = true;
				}
				if (confirmBet) {
					bet.setStatus("Confirmed");
					Bets updatedBet = betRepository.save(bet);
					// Notify frontend about the bet confirmation
					cricketDataService.notifyBetStatus(updatedBet);
				} else {
					
					User user = userService.findOne(currentUsername);
					BigDecimal originalBalance = user.getBalance();
					BigDecimal betAmount = bet.getAmount();
					BigDecimal updatedBalance = null;
					
					if ("back".equalsIgnoreCase(bet.getBetType())) {
		                updatedBalance = originalBalance.add(betAmount);
		            } else if ("lay".equalsIgnoreCase(bet.getBetType())) {
		                BigDecimal potentialPayout = bet.getOdd().subtract(BigDecimal.ONE).multiply(betAmount);
		                updatedBalance = originalBalance.add(potentialPayout);
		            }
					
					user.setBalance(updatedBalance);
		            userService.updateUser(user); // Assuming this method exists and updates the user in the database
		            
		            bet.setStatus("Cancelled");
		            Bets updatedBet = betRepository.save(bet); // Save the cancelled bet status
					// Optionally, notify the frontend about the bet cancellation
		            cricketDataService.notifyBetStatus(updatedBet);
		            
				}
			});

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// Handle the interruption accordingly
		}
	}

}
