package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devglan.model.CricketDataEntity;
import com.devglan.model.TeamSessionData;

public interface TeamSessionDataRepository extends JpaRepository<TeamSessionData, Long> {

	TeamSessionData findByTeamNameAndCricketDataEntity(String key, CricketDataEntity entity);
}
