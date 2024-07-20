package com.devglan.model;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.devglan.dao.SessionOverData;

@Entity
public class TeamSessionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teamName;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_session_data_id")
    private List<SessionOverData> sessionOverDataList;

    @ManyToOne
    @JoinColumn(name = "cricket_data_id")
    private CricketDataEntity cricketDataEntity;

    // Getters and setters
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

    public List<SessionOverData> getSessionOverDataList() {
        return sessionOverDataList;
    }

    public void setSessionOverDataList(List<SessionOverData> sessionOverDataList) {
        this.sessionOverDataList = sessionOverDataList;
    }

    public CricketDataEntity getCricketDataEntity() {
        return cricketDataEntity;
    }

    public void setCricketDataEntity(CricketDataEntity cricketDataEntity) {
        this.cricketDataEntity = cricketDataEntity;
    }
}
