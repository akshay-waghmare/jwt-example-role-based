package com.devglan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.dao.OversData;

@Repository
public interface OversDataRepository extends JpaRepository<OversData, String> {
	
}