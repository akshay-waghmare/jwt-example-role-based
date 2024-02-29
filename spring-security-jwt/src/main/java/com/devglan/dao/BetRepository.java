package com.devglan.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devglan.model.Bets;

public interface BetRepository extends JpaRepository<Bets, Long> {

	List<Bets> findByUserId(Long userId);
    // You can add custom query methods here if needed
}

