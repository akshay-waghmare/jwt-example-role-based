package com.devglan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.dao.SessionOdds;

@Repository
public interface SessionOddsRepository extends JpaRepository<SessionOdds, String> {

    Optional<SessionOdds> findBySessionOverAndCricketDataEntityUrl(String sessionOver, String cricketDataUrl);

	
}