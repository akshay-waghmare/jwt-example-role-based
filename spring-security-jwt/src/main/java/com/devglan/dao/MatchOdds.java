package com.devglan.dao;

public class MatchOdds {
	
	private String teamName; // or use a specific enumeration for type (Team, Draw)
    private TeamOdds odds;

    // Constructors, getters, and setters
    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public TeamOdds getOdds() {
        return odds;
    }

    public void setOdds(TeamOdds odds) {
        this.odds = odds;
    }

}
