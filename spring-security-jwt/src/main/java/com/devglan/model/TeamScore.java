package com.devglan.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "team_score")
public class TeamScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teamName;
    private String teamScore;
    private String teamOver;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getTeamName() {
		return teamName;
	}
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	public String getTeamScore() {
		return teamScore;
	}
	public void setTeamScore(String teamScore) {
		this.teamScore = teamScore;
	}
	public String getTeamOver() {
		return teamOver;
	}
	public void setTeamOver(String teamOver) {
		this.teamOver = teamOver;
	}

    // Getters and setters...
    
}
