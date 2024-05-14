package com.devglan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devglan.model.LiveMatch;

public interface LiveMatchRepository extends JpaRepository<LiveMatch, Long> , LiveMatchRepositoryCustom {

	List<LiveMatch> findByIsDeletedFalse();
	boolean existsByUrlAndIsDeletedFalse(String url);
	List<LiveMatch> findByIsDeletedTrue();
}