package com.devglan.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devglan.model.LiveMatch;
import com.devglan.model.Poll;
import com.devglan.model.PollResults;
import com.devglan.model.Vote;
import com.devglan.model.VoteRequest;
import com.devglan.repository.LiveMatchRepository;
import com.devglan.repository.PollRepository;
import com.devglan.repository.VoteRepository;
import com.devglan.service.PollService;

import java.util.*;

@RestController
@RequestMapping("/api/poll")
public class PollController {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private LiveMatchRepository liveMatchRepository;
    
    @Autowired
    private PollService pollService;

    @PostMapping("/create")
    public String createPoll(@RequestBody Poll poll, @RequestParam Long liveMatchId) {
    	 // Retrieve the associated LiveMatch entity
        LiveMatch liveMatch = liveMatchRepository.findById(liveMatchId).orElse(null);
        if (liveMatch == null) {
            return "LiveMatch not found with ID: " + liveMatchId;
        }

        // Associate the LiveMatch with the Poll
        poll.setLiveMatch(liveMatch);

        // Add votes for each answer
        poll.getAnswers().forEach(answer -> {
            Vote vote = new Vote();
            vote.setAnswer(answer);
            vote.setPoll(poll);
            vote.setCount(0);
            poll.getVotes().add(vote);
        });
        pollRepository.save(poll);
        return "Poll created successfully with ID: " + poll.getId()+ " for LiveMatch ID: " + liveMatchId;
    }

    @PostMapping("/vote/{pollId}")
    public ResponseEntity<PollResults> voteOnPoll(@PathVariable Long pollId, @RequestBody VoteRequest voteRequest) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found."));

        // Find the existing vote for the given answer
        Optional<Vote> voteOptional = poll.getVotes().stream()
                .filter(v -> v.getAnswer().equals(voteRequest.getAnswer()))
                .findFirst();

        if (!voteOptional.isPresent()) {
            return ResponseEntity.badRequest().body(null); // Return a 400 response if the answer is invalid
        }

        // Update the vote count
        Vote vote = voteOptional.get();
        vote.setCount(vote.getCount() + 1);
        voteRepository.save(vote);

        // Calculate total votes
        int totalVotes = poll.getVotes().stream().mapToInt(Vote::getCount).sum();

        // Calculate vote percentages
        Map<String, Double> percentages = new HashMap<>();
        for (Vote v : poll.getVotes()) {
            double percentage = totalVotes == 0 ? 0 : (v.getCount() * 100.0) / totalVotes;
            percentages.put(v.getAnswer(), percentage);
        }

        return ResponseEntity.ok(new PollResults(poll.getQuestion(), percentages));
    }


    @GetMapping("/results/{pollId}")
    public PollResults getPollResults(@PathVariable Long pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            throw new RuntimeException("Poll not found.");
        }
        int totalVotes = poll.getVotes().stream().mapToInt(Vote::getCount).sum();
        Map<String, Double> percentages = new HashMap<>();
        for (Vote vote : poll.getVotes()) {
            double percentage = totalVotes == 0 ? 0 : (vote.getCount() * 100.0) / totalVotes;
            percentages.put(vote.getAnswer(), percentage);
        }
        return new PollResults(poll.getQuestion(), percentages);
    }
    
    @GetMapping("/get")
    public ResponseEntity<List<Poll>> getPollsForMatch(@RequestParam Long liveMatchId) {
        List<Poll> polls = pollService.getPollsForMatch(liveMatchId);
        return ResponseEntity.ok(polls);
    }
    
    @GetMapping("/votes/{pollId}")
    public ResponseEntity<List<Vote>> getVotesForPoll(@PathVariable Long pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        
        if (poll == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(poll.getVotes());
    }


}
