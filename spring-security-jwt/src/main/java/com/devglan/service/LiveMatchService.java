package com.devglan.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.devglan.dao.CricketDataDTO;
import com.devglan.model.LiveMatch;

public interface LiveMatchService {
	void syncLiveMatches(String[] urls);

	public List<LiveMatch> findAllMatches();
    List<LiveMatch> findAll();
    public ResponseEntity<CricketDataDTO> fetchAndSendData(String url);
    public String appendBaseUrl(String url);

	public List<LiveMatch> findAllFinishedMatches();
	public LiveMatch findByUrl(String url);
	public List<LiveMatch> findAllLiveMatches();
	LiveMatch update(LiveMatch match);
    
}
