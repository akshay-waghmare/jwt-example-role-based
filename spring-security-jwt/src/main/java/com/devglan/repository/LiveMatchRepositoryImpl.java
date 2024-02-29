package com.devglan.repository;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;

import com.devglan.model.LiveMatch;

public class LiveMatchRepositoryImpl implements LiveMatchRepositoryCustom {

	private final EntityManager entityManager;

	@Autowired
	public LiveMatchRepositoryImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public void saveLiveMatches(List<String> urls) {
		for (String url : urls) {
			LiveMatch liveMatch = new LiveMatch(url);
			entityManager.persist(liveMatch);
		}

	}

}
