package com.devglan.model;

public class BatsmanData {
    private String name;
    private String score;
    private String ballsFaced;
    private String fours;
    private String sixes;
    private String strikeRate;
    private Boolean onStrike;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getBallsFaced() {
        return ballsFaced;
    }

    public void setBallsFaced(String ballsFaced) {
        this.ballsFaced = ballsFaced;
    }

    public String getFours() {
        return fours;
    }

    public void setFours(String fours) {
        this.fours = fours;
    }

    public String getSixes() {
        return sixes;
    }

    public void setSixes(String sixes) {
        this.sixes = sixes;
    }

    public String getStrikeRate() {
        return strikeRate;
    }

    public void setStrikeRate(String strikeRate) {
        this.strikeRate = strikeRate;
    }

	public Boolean getOnStrike() {
		return onStrike;
	}

	public void setOnStrike(Boolean onStrike) {
		this.onStrike = onStrike;
	}
    

	
}
