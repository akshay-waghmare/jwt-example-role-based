package com.devglan.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devglan.model.BatsmanData;
import com.devglan.model.BowlerData;
import com.devglan.model.PlayingXI;
import com.devglan.model.TeamComparison;
import com.devglan.model.TeamForm;
import com.devglan.model.VenueStats;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CricketDataDTO {

	@JsonProperty("match_odds")
	private List<MatchOdds> matchOdds; // Updated to use MatchOdds test match case
	@JsonProperty("team_odds")
	private TeamOdds teamOdds;
	@JsonProperty("batting_team")
	private String battingTeamName;
	@JsonProperty("session_odds")
	private Set<SessionOdds> sessionOddsList;
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
	private String url;
	@JsonProperty("crr")
    private String currentRunRate;
    @JsonProperty("final_result_text")
    private String finalResultText;
    @JsonProperty("overs_data")
    private List<OversData> oversData;
    @JsonProperty("batsman_data")
    private List<BatsmanData> batsmanData;
	@JsonProperty("bowler_data")
    private List<BowlerData> bowlerData;
    
    private Map<String, List<SessionOverData>> teamWiseSessionData;
    
//	@JsonProperty("team_player_info")
//	//@JsonDeserialize(using = TeamPlayerInfoDeserializer.class)
//	private Map<String, List<String>> team_player_info;

	


    public List<BatsmanData> getBatsmanData() {
		return batsmanData;
	}

	public void setBatsmanData(List<BatsmanData> batsmanData) {
		this.batsmanData = batsmanData;
	}

	public List<BowlerData> getBowlerData() {
		return bowlerData;
	}

	public void setBowlerData(List<BowlerData> bowlerData) {
		this.bowlerData = bowlerData;
	}

	public long getUpdatedTimeStamp() {
		return updatedTimeStamp;
	}

	public Map<String, List<SessionOverData>> getTeamWiseSessionData() {
		return teamWiseSessionData;
	}

	public void setTeamWiseSessionData(Map<String, List<SessionOverData>> teamWiseSessionData) {
		this.teamWiseSessionData = teamWiseSessionData;
	}

	public void setUpdatedTimeStamp(long updatedTimeStamp) {
		this.updatedTimeStamp = updatedTimeStamp;
	}

	private String toss_won_country;
	private String bat_or_ball_selected;
	private long updatedTimeStamp;
	private long lastOddsUpdated;
	
	
	private String matchDate;
    private String venue;
    private String matchName;
    private Set<TeamForm> teamForm;
    private Map<String, TeamComparison> teamComparison;
    private VenueStats venueStats;
    private Map<String, Set<PlayingXI>> playingXI;
    private String tossInfo;

	/*
	 * public Map<String, List<String>> getTeam_player_info() { return
	 * team_player_info; }
	 * 
	 * public void setTeam_player_info(Map<String, List<String>> team_player_info) {
	 * this.team_player_info = team_player_info; }
	 */

	public String getToss_won_country() {
		return toss_won_country;
	}

	public String getMatchDate() {
		return matchDate;
	}

	public void setMatchDate(String matchDate) {
		this.matchDate = matchDate;
	}

	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public String getMatchName() {
		return matchName;
	}

	public void setMatchName(String matchName) {
		this.matchName = matchName;
	}

	

	public Map<String, TeamComparison> getTeamComparison() {
		return teamComparison;
	}

	public void setTeamComparison(Map<String, TeamComparison> teamComparison) {
		this.teamComparison = teamComparison;
	}

	public VenueStats getVenueStats() {
		return venueStats;
	}

	public void setVenueStats(VenueStats venueStats) {
		this.venueStats = venueStats;
	}

	

	public Set<TeamForm> getTeamForm() {
		return teamForm;
	}

	public void setTeamForm(Set<TeamForm> teamForm) {
		this.teamForm = teamForm;
	}

	public Map<String, Set<PlayingXI>> getPlayingXI() {
		return playingXI;
	}

	public void setPlayingXI(Map<String, Set<PlayingXI>> playingXI) {
		this.playingXI = playingXI;
	}

	public String getTossInfo() {
		return tossInfo;
	}

	public void setTossInfo(String tossInfo) {
		this.tossInfo = tossInfo;
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

	public CricketDataDTO() {}

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

	
	public Set<SessionOdds> getSessionOddsList() {
		return sessionOddsList;
	}

	public void setSessionOddsList(Set<SessionOdds> sessionOddsList) {
		this.sessionOddsList = sessionOddsList;
	}

	public void setFavTeam(String favTeam) {
		this.favTeam = favTeam;
	}

	public String getBattingTeamName() {
		return battingTeamName;
	}

	public void setBattingTeamName(String battingTeamName) {
		this.battingTeamName = battingTeamName;
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setLastUpdated(Long currentTimeMillis) {
		this.lastOddsUpdated = currentTimeMillis;
		
	}
	
	public Long getLastUpdated() {
		return this.lastOddsUpdated;
	}

	public List<MatchOdds> getMatchOdds() {
		return matchOdds;
	}

	public void setMatchOdds(List<MatchOdds> matchOdds) {
		this.matchOdds = matchOdds;
	}

	public String getCurrentRunRate() {
		return currentRunRate;
	}

	public void setCurrentRunRate(String currentRunRate) {
		this.currentRunRate = currentRunRate;
	}

	public String getFinalResultText() {
		return finalResultText;
	}

	public void setFinalResultText(String finalResultText) {
		this.finalResultText = finalResultText;
	}

	public List<OversData> getOversData() {
		return oversData;
	}

	public void setOversData(List<OversData> list) {
		this.oversData = list;
	}

	
	
	
	
	
	

}
