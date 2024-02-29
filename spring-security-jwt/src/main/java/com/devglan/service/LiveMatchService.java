package com.devglan.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.devglan.dao.CricketDataDTO;
import com.devglan.model.LiveMatch;

public interface LiveMatchService {
	void syncLiveMatches(String[] urls);

    List<LiveMatch> findAll();
    public ResponseEntity<CricketDataDTO> fetchAndSendData(String url);
    public String appendBaseUrl(String url);
    
}
