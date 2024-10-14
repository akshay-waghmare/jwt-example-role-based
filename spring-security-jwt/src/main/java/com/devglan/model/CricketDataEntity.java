package com.devglan.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.devglan.dao.MatchInfoEntity;
import com.devglan.dao.MatchOdds;
import com.devglan.dao.OversData;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.TeamOdds;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "cricket_data", indexes = {
    @Index(name = "cricket_url", columnList = "url"),
})
public class CricketDataEntity {

    @Id
    private String url;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url", referencedColumnName = "url", insertable = false, updatable = false)
    private MatchInfoEntity matchInfo;
    
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

    @OneToMany(mappedBy = "cricketDataEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference // This will handle the forward part of the relationship
    private Set<SessionOdds> sessionOddsSet;  // Using Set instead of List for better handling
    
    @OneToMany(mappedBy = "cricketDataEntity", fetch = FetchType.EAGER, cascade = javax.persistence.CascadeType.ALL)
    private List<TeamSessionData> teamWiseSessionData;

    private String currentRunRate;
    private String finalResultText;
    
    @OneToMany(mappedBy = "cricketDataEntity", fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.ALL)
    private List<OversData> oversData;

    private String tossWonCountry;
    private String batOrBallSelected;
    
    @Column(name = "UPDATED_TIME_STAMP")
    private Long updatedTimeStamp;
    
    @Column(name = "LAST_ODDS_UPDATED_TIME_STAMP")
    private Long lastOddsUpdatedTimeStamp;

    // Getters and setters

    public String getUrl() {
        return url;
    }

   

	public MatchInfoEntity getMatchInfo() {
		return matchInfo;
	}



	public void setMatchInfo(MatchInfoEntity matchInfo) {
		this.matchInfo = matchInfo;
	}



	public Long getLastOddsUpdatedTimeStamp() {
		return lastOddsUpdatedTimeStamp;
	}



	public void setLastOddsUpdatedTimeStamp(Long lastOddsUpdatedTimeStamp) {
		if (lastOddsUpdatedTimeStamp == null) {
            this.lastOddsUpdatedTimeStamp = 0L;
        } else {
            this.lastOddsUpdatedTimeStamp = lastOddsUpdatedTimeStamp;
        }
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

    
    public Set<SessionOdds> getSessionOddsSet() {
		return sessionOddsSet;
	}

	public void setSessionOddsSet(Set<SessionOdds> sessionOddsSet) {
		this.sessionOddsSet = sessionOddsSet;
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

    public List<TeamSessionData> getTeamWiseSessionData() {
        return teamWiseSessionData;
    }

    public void setTeamWiseSessionData(List<TeamSessionData> teamWiseSessionData) {
        this.teamWiseSessionData = teamWiseSessionData;
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

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedTimeStamp = Instant.now().toEpochMilli();
    }

	public Long getUpdatedTimeStamp() {
		return updatedTimeStamp;
	}

	public void setUpdatedTimeStamp(Long updatedTimeStamp) {
		if (updatedTimeStamp == null) {
			this.updatedTimeStamp = 0L;
		} else {
			this.updatedTimeStamp = updatedTimeStamp;

		}
	}
}