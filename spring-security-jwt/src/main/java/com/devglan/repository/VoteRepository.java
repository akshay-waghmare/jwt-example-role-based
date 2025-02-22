package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.devglan.model.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
	@Modifying
    @Transactional
    @Query("UPDATE Vote v SET v.count = v.count + 1 WHERE v.poll.id = :pollId AND v.answer = :answer")
    int incrementVoteCount(@Param("pollId") Long pollId, @Param("answer") String answer);

}
