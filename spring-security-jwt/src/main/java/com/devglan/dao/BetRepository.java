package com.devglan.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devglan.model.Bets;

public interface BetRepository extends JpaRepository<Bets, Long> {

	List<Bets> findByUserId(Long userId);

	List<Bets> findByMatchUrl(String matchUrl);

	List<Bets> findByMatchUrlAndUserId(String matchUrl, long userId);

	@Query("SELECT b FROM Bets b WHERE :matchUrl LIKE CONCAT('%', b.matchUrl, '%') AND b.status = 'Confirmed'")
	List<Bets> findByMatchUrlContainingAndConfirmed(@Param("matchUrl") String matchUrl);
	
	@Query("SELECT b FROM Bets b WHERE b.user.id = :userId AND :matchUrl LIKE CONCAT('%', b.matchUrl, '%') AND b.status = 'Confirmed'")
    List<Bets> findConfirmedBetsByUserIdAndMatchUrl(@Param("userId") long userId, @Param("matchUrl") String matchUrl);

	List<Bets> findByMatchUrlContaining(String url);

}
