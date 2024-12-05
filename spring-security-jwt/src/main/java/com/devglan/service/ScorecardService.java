package com.devglan.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devglan.dao.ScorecardEntity;
import com.devglan.repository.ScorecardRepository;

@Service
public class ScorecardService {

    
    @Autowired
    private ScorecardRepository scorecardRepository;

    public void saveMatchInfo(String url, String data) {
    	ScorecardEntity scorecard = new ScorecardEntity();
        scorecard.setUrl(url);
        scorecard.setData(data);
        scorecardRepository.save(scorecard);
    }

    public String getMatchInfo(String url) {
        
    	 Optional<ScorecardEntity> scorecardnfo = scorecardRepository.findFirstByUrlContaining(url);
    	    if (scorecardnfo.isPresent()) {
    	        ScorecardEntity scorecard = scorecardnfo.get();
    	        return scorecard.getData();
    	    } else {
    	        return null;
    	    }
    }

    public boolean existsByUrl(String url) {
        return scorecardRepository.existsById(url);
    }
}
