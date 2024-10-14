package com.devglan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.dao.MatchInfoDetailEntity;

@Repository
public interface MatchInfoDetailRepository extends JpaRepository<MatchInfoDetailEntity, String> {
	
    Optional<MatchInfoDetailEntity> findFirstByUrlContaining(String urlFragment);

	
}