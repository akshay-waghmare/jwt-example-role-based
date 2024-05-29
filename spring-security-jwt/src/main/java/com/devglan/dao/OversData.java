package com.devglan.dao;

import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class OversData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String overNumber;

    @ElementCollection
    private List<String> balls;

    private String totalRuns;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOverNumber() {
        return overNumber;
    }

    public void setOverNumber(String overNumber) {
        this.overNumber = overNumber;
    }

    public List<String> getBalls() {
        return balls;
    }

    public void setBalls(List<String> balls) {
        this.balls = balls;
    }

    public String getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(String totalRuns) {
        this.totalRuns = totalRuns;
    }
}