package com.devglan.dao;

import java.util.List;

public class OversData {
    private String overNumber;
    private List<String> balls;
    private String totalRuns;
    
	public String getOverNumber() {
		return overNumber;
	}
	public void setOverNumber(String overNumber) {
		this.overNumber = overNumber;
	}
	public List<String> getBalls() {
		return balls;
	}
	public void setBalls(List<String> balls) {
		this.balls = balls;
	}
	public String getTotalRuns() {
		return totalRuns;
	}
	public void setTotalRuns(String totalRuns) {
		this.totalRuns = totalRuns;
	}
    
}