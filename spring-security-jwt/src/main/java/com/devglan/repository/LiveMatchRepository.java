package com.devglan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devglan.model.LiveMatch;

public interface LiveMatchRepository extends JpaRepository<LiveMatch, Long> , LiveMatchRepositoryCustom {

	boolean existsByUrl(String url);
	List<LiveMatch> findByIsDeletedFalse();
	boolean existsByUrlAndIsDeletedFalse(String url);
	List<LiveMatch> findByIsDeletedTrue();
	@Query("SELECT lm FROM LiveMatch lm WHERE lm.url LIKE %:url%")
    LiveMatch findByUrlContaining(@Param("url") String url);
	List<LiveMatch> findByDeletionAttemptsLessThan(Integer attempts);
	List<LiveMatch> findByDeletionAttemptsLessThanAndIsDeletedFalse(Integer attempts);
}