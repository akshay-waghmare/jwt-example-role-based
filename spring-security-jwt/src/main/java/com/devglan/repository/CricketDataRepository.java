package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.model.CricketDataEntity;

@Repository
public interface CricketDataRepository extends JpaRepository<CricketDataEntity, String> {
	
}