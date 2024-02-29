package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devglan.model.LiveMatch;

public interface LiveMatchRepository extends JpaRepository<LiveMatch, Long> , LiveMatchRepositoryCustom {

	boolean existsByUrl(String url);
}
