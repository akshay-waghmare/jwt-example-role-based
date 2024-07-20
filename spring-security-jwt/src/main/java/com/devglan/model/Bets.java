package com.devglan.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "bets", indexes = {
	    @Index(name = "idx_match_url", columnList = "matchUrl"),
	    @Index(name = "idx_user_id", columnList = "user_id"),
	    @Index(name = "idx_team_name", columnList = "teamName"),
	    @Index(name = "idx_status", columnList = "status")
	})
public class Bets {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "betId")
	private Long betId;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

	@Column(name = "betType")
	private String betType;
	
	@Column(name = "teamName")
	private String teamName;

	@Column(name = "amount")
	private BigDecimal amount;

	@Column(name = "odd")
	private BigDecimal odd;

	@Column(name = "potentialWin")
	private BigDecimal potentialWin;

	@Column(name = "status")
	private String status;
	
	@Column(name = "matchUrl")
	private String matchUrl;

	@Column(name = "placedAt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date placedAt;

    @Column(name = "isSessionBet")
    private Boolean isSessionBet = false; // Provide a default value


    @Column(name = "sessionName")
    private String sessionName;

	// Constructors, Getters, and Setters
	// Constructor
	public Bets() {
	}

	// Getters and Setters
	public Long getBetId() {
		return betId;
	}

	public void setBetId(Long betId) {
		this.betId = betId;
	}

	public String getBetType() {
		return betType;
	}

	public void setBetType(String betType) {
		this.betType = betType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getOdd() {
		return odd;
	}

	public void setOdd(BigDecimal odd) {
		this.odd = odd;
	}

	public BigDecimal getPotentialWin() {
		return potentialWin;
	}

	public void setPotentialWin(BigDecimal potentialWin) {
		this.potentialWin = potentialWin;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getPlacedAt() {
		return placedAt;
	}

	public void setPlacedAt(Date placedAt) {
		this.placedAt = placedAt;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public String getMatchUrl() {
		return matchUrl;
	}

	public void setMatchUrl(String matchUrl) {
		this.matchUrl = matchUrl;
	}

    public Boolean getIsSessionBet() {
        return isSessionBet;
    }

    public void setIsSessionBet(Boolean isSessionBet) {
        this.isSessionBet = isSessionBet;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
}
