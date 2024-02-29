package com.devglan.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devglan.model.Matches;

@Repository
public interface MatchRepository extends JpaRepository<Matches, Long> {
}

