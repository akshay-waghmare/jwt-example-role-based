package com.devglan.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.devglan.model.CricketDataEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "session_odds", uniqueConstraints = {
	    @UniqueConstraint(columnNames = {"session_over", "cricket_data_url"})
	})
public class SessionOdds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cricket_data_url", nullable = false)
    @JsonIgnore // This will ignore this field during serialization
    private CricketDataEntity cricketDataEntity;
    
    @Column(name = "session_back_odds")
    private String sessionBackOdds;
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSessionBackOdds() {
		return sessionBackOdds;
	}

	public void setSessionBackOdds(String sessionBackOdds) {
		this.sessionBackOdds = sessionBackOdds;
	}

	public String getSessionLayOdds() {
		return sessionLayOdds;
	}

	public void setSessionLayOdds(String sessionLayOdds) {
		this.sessionLayOdds = sessionLayOdds;
	}

	public String getSessionOver() {
		return sessionOver;
	}

	public void setSessionOver(String sessionOver) {
		this.sessionOver = sessionOver;
	}
	
	@Column(name = "session_lay_odds")
	private String sessionLayOdds;
	@Column(name = "session_over")
    private String sessionOver;

    // Getters and setters remain unchanged

    public CricketDataEntity getCricketDataEntity() {
        return cricketDataEntity;
    }

    public void setCricketDataEntity(CricketDataEntity cricketDataEntity) {
        this.cricketDataEntity = cricketDataEntity;
    }
}
