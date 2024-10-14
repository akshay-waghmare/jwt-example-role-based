package com.devglan.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devglan.dao.MatchInfoDetailEntity;
import com.devglan.repository.MatchInfoDetailRepository;

@Service
public class MatchInfoService {

    @Autowired
    private MatchInfoDetailRepository matchInfoDetailRepository;

    public void saveMatchInfo(String url, String data) {
        MatchInfoDetailEntity matchInfo = new MatchInfoDetailEntity();
        matchInfo.setUrl(url);
        matchInfo.setData(data);
        matchInfoDetailRepository.save(matchInfo);
    }

    public String getMatchInfo(String url) {
        
    	 Optional<MatchInfoDetailEntity> optionalMatchInfo = matchInfoDetailRepository.findFirstByUrlContaining(url);
    	    if (optionalMatchInfo.isPresent()) {
    	        MatchInfoDetailEntity matchInfo = optionalMatchInfo.get();
    	        return matchInfo.getData();
    	    } else {
    	        return null;
    	    }
    }

    public boolean existsByUrl(String url) {
        return matchInfoDetailRepository.existsById(url);
    }
}
