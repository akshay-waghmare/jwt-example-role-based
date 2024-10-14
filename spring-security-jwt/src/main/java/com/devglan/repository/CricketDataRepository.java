package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.devglan.model.CricketDataEntity;

@Repository
public interface CricketDataRepository extends JpaRepository<CricketDataEntity, String> {

    @Query("SELECT e FROM CricketDataEntity e LEFT JOIN FETCH e.teamWiseSessionData WHERE e.url = :url")
    CricketDataEntity findByUrlWithTeamWiseSessionData(String url);
    
    CricketDataEntity findByUrlContaining(String url);
    
	/*
	 * @Query("SELECT e.matchOdds as matchOdds, e.teamOdds as teamOdds, e.battingTeamName as battingTeamName, "
	 * +
	 * "e.over as over, e.score as score, e.currentBall as currentBall, e.runsOnBall as runsOnBall, "
	 * +
	 * "e.favTeam as favTeam, e.sessionOdds as sessionOdds, e.currentRunRate as currentRunRate, "
	 * +
	 * "e.finalResultText as finalResultText, e.oversData as oversData, e.teamWiseSessionData as teamWiseSessionData, "
	 * +
	 * "e.tossWonCountry as tossWonCountry, e.batOrBallSelected as batOrBallSelected, e.updatedTimeStamp as updatedTimeStamp "
	 * + "FROM CricketDataEntity e WHERE e.url = :url") CricketDataProjection
	 * findByUrlWithTeamWiseSessionData(@Param("url") String url);
	 */
	
}