package com.devglan.service;

import com.devglan.model.Poll;
import com.devglan.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PollService {
	private final PollRepository pollRepository;

    public PollService(PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    public List<Poll> getAllPolls() {
        return pollRepository.findAll();
    }

    public Poll savePoll(Poll poll) {
        return pollRepository.save(poll);
    }
    
    public List<Poll> getPollsForMatch(Long liveMatchId) {
        return pollRepository.findByLiveMatchId(liveMatchId)
				        		.stream()
				                .map(poll -> {
				                    poll.setVotes(null); // Exclude votes to prevent nesting issues
				                    return poll;
				                })
				                .collect(Collectors.toList());
    }
}
