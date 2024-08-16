package com.devglan.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
public class UserExposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private LiveMatch match;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "overall_match_exposure")
    private BigDecimal overallMatchExposure;

    @Column(name = "overall_session_exposure")
    private BigDecimal overallSessionExposre;
    
    @Column(name = "soft_deleted")
    private Boolean softDeleted = false;  // Default value

    public Boolean getSoftDeleted() {
		return softDeleted;
	}

	public void setSoftDeleted(Boolean softDeleted) {
		this.softDeleted = softDeleted;
	}

	// Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getOverallMatchExposure() {
		return overallMatchExposure;
	}

	public void setOverallMatchExposure(BigDecimal overallMatchExposure) {
		this.overallMatchExposure = overallMatchExposure;
	}

	public BigDecimal getOverallSessionExposre() {
		return overallSessionExposre;
	}

	public void setOverallSessionExposre(BigDecimal overallSessionExposre) {
		this.overallSessionExposre = overallSessionExposre;
	}

	public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LiveMatch getMatch() {
        return match;
    }

    public void setMatch(LiveMatch match) {
        this.match = match;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

}
