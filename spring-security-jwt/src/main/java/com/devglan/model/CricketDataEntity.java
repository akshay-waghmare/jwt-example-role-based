package com.devglan.model;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.devglan.dao.MatchOdds;
import com.devglan.dao.OversData;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.TeamOdds;

@Entity
@Table(name = "cricket_data")
public class CricketDataEntity {

    @Id
    private String url;
    
    @ElementCollection
    private List<MatchOdds> matchOdds;

    @Embedded
    private TeamOdds teamOdds;

    private String battingTeamName;
    private Double over;
    private String score;
    private String currentBall;
    private Integer runsOnBall;
    private String favTeam;

    @Embedded
    private SessionOdds sessionOdds;

    private String currentRunRate;
    private String finalResultText;
    
    @OneToMany
    private List<OversData> oversData;

    private String tossWonCountry;
    private String batOrBallSelected;
    private long updatedTimeStamp;

    // Getters and setters

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MatchOdds> getMatchOdds() {
        return matchOdds;
    }

    public void setMatchOdds(List<MatchOdds> matchOdds) {
        this.matchOdds = matchOdds;
    }

    public TeamOdds getTeamOdds() {
        return teamOdds;
    }

    public void setTeamOdds(TeamOdds teamOdds) {
        this.teamOdds = teamOdds;
    }

    public String getBattingTeamName() {
        return battingTeamName;
    }

    public void setBattingTeamName(String battingTeamName) {
        this.battingTeamName = battingTeamName;
    }

    public Double getOver() {
        return over;
    }

    public void setOver(Double over) {
        this.over = over;
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

    public void setFavTeam(String favTeam) {
        this.favTeam = favTeam;
    }

    public SessionOdds getSessionOdds() {
        return sessionOdds;
    }

    public void setSessionOdds(SessionOdds sessionOdds) {
        this.sessionOdds = sessionOdds;
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

    public void setOversData(List<OversData> oversData) {
        this.oversData = oversData;
    }

    public String getTossWonCountry() {
        return tossWonCountry;
    }

    public void setTossWonCountry(String tossWonCountry) {
        this.tossWonCountry = tossWonCountry;
    }

    public String getBatOrBallSelected() {
        return batOrBallSelected;
    }

    public void setBatOrBallSelected(String batOrBallSelected) {
        this.batOrBallSelected = batOrBallSelected;
    }

    public long getUpdatedTimeStamp() {
        return updatedTimeStamp;
    }

    public void setUpdatedTimeStamp(long updatedTimeStamp) {
        this.updatedTimeStamp = updatedTimeStamp;
    }
}
