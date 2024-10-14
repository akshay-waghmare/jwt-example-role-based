package com.devglan.dao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "playing_xi")
public class PlayingXIEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;
    private String playerRole;

    // Many to one relationship with MatchInfoEntity
    @ManyToOne
    @JoinColumn(name = "match_info_url")
    private MatchInfoEntity matchInfo;

    private String teamName;  // Store the team name for each player

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerRole() {
        return playerRole;
    }

    public void setPlayerRole(String playerRole) {
        this.playerRole = playerRole;
    }

    public MatchInfoEntity getMatchInfo() {
        return matchInfo;
    }

    public void setMatchInfo(MatchInfoEntity matchInfo) {
        this.matchInfo = matchInfo;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
