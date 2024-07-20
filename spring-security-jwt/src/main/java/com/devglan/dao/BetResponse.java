package com.devglan.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.devglan.model.Bets;

public class BetResponse {
    private List<Bets> bets;
    private Map<String, BigDecimal> adjustedExposures;
    private Map<String, BigDecimal> sessionExposures;

    // Constructors, getters, and setters

    public BetResponse(List<Bets> bets, Map<String, BigDecimal> adjustedExposures, Map<String, BigDecimal> sessionExposures) {
        this.bets = bets;
        this.adjustedExposures = adjustedExposures;
        this.sessionExposures = sessionExposures;
    }

    public List<Bets> getBets() {
        return bets;
    }

    public Map<String, BigDecimal> getSessionExposures() {
		return sessionExposures;
	}

	public void setSessionExposures(Map<String, BigDecimal> sessionExposures) {
		this.sessionExposures = sessionExposures;
	}

	public void setBets(List<Bets> bets) {
        this.bets = bets;
    }

    public Map<String, BigDecimal> getAdjustedExposures() {
        return adjustedExposures;
    }

    public void setAdjustedExposures(Map<String, BigDecimal> adjustedExposures) {
        this.adjustedExposures = adjustedExposures;
    }
}