package com.devglan.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "bets")
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

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "odd")
    private BigDecimal odd;

    @Column(name = "potentialWin")
    private BigDecimal potentialWin;

    @Column(name = "status")
    private String status;

    @Column(name = "placedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date placedAt;

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
}

