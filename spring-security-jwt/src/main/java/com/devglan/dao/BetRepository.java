package com.devglan.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devglan.model.Bets;

public interface BetRepository extends JpaRepository<Bets, Long> {

	List<Bets> findByUserId(Long userId);
	
	List<Bets> findByMatchUrl(String matchUrl);
	
	List<Bets> findByMatchUrlAndUserId(String matchUrl, long userId);
}
