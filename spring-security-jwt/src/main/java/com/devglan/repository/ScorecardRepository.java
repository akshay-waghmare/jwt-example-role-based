package com.devglan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.dao.ScorecardEntity;

@Repository
public interface ScorecardRepository extends JpaRepository<ScorecardEntity, String> {
	
    Optional<ScorecardEntity> findFirstByUrlContaining(String urlFragment);

	
}