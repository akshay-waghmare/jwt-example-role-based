package com.devglan.dao;

import java.util.List;
import java.util.Map;

import com.devglan.controller.TeamPlayerInfoDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class CricketDataDTO {

	@JsonProperty("team_odds")
	private TeamOdds teamOdds;
	@JsonProperty("team_odds")
	private SessionOdds sessionOdds;
	@JsonProperty("over")
	private Double over;
	@JsonProperty("score")
	private String score;
	@JsonProperty("current_ball")
	private String currentBall;
	@JsonProperty("runs_on_ball")
	private Integer runsOnBall;
	@JsonProperty("fav_team")
	private String favTeam;
	@JsonProperty("team_player_info")
	@JsonDeserialize(using = TeamPlayerInfoDeserializer.class)
	private Map<String, List<String>> team_player_info;

	private String toss_won_country;
	private String bat_or_ball_selected;

	public Map<String, List<String>> getTeam_player_info() {
		return team_player_info;
	}

	public void setTeam_player_info(Map<String, List<String>> team_player_info) {
		this.team_player_info = team_player_info;
	}

	public String getToss_won_country() {
		return toss_won_country;
	}

	public void setToss_won_country(String toss_won_country) {
		this.toss_won_country = toss_won_country;
	}

	public String getBat_or_ball_selected() {
		return bat_or_ball_selected;
	}

	public void setBat_or_ball_selected(String bat_or_ball_selected) {
		this.bat_or_ball_selected = bat_or_ball_selected;
	}

	public CricketDataDTO() {
		// Default constructor
	}

	public Map<String, List<String>> getTeamPlayerInfo() {
		return team_player_info;
	}

	public void setTeamPlayerInfo(Map<String, List<String>> teamPlayerInfo) {
		this.team_player_info = teamPlayerInfo;
	}

	public Double getOver() {
		return over;
	}

	public TeamOdds getTeamOdds() {
		return teamOdds;
	}

	public void setTeamOdds(TeamOdds teamOdds) {
		this.teamOdds = teamOdds;
	}

	public void setOver(Double d) {
		this.over = d;
	}

	public String getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = score;
	}

	public String getCurrentBall() {
		return currentBall;
	}

	public void setCurrentBall(String currentBall) {
		this.currentBall = currentBall;
	}

	public Integer getRunsOnBall() {
		return runsOnBall;
	}

	public void setRunsOnBall(Integer runsOnBall) {
		this.runsOnBall = runsOnBall;
	}

	public String getFavTeam() {
		return favTeam;
	}

	public SessionOdds getSessionOdds() {
		return sessionOdds;
	}

	public void setSessionOdds(SessionOdds sessionOdds) {
		this.sessionOdds = sessionOdds;
	}

	public void setFavTeam(String favTeam) {
		this.favTeam = favTeam;
	}

}
