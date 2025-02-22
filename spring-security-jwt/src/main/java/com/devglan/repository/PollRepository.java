package com.devglan.repository;

import com.devglan.model.Poll;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
	List<Poll> findByLiveMatchId(Long liveMatchId);
}

