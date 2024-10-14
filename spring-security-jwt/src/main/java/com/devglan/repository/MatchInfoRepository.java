package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.dao.MatchInfoEntity;

@Repository
public interface MatchInfoRepository extends JpaRepository<MatchInfoEntity, String> {
	
}