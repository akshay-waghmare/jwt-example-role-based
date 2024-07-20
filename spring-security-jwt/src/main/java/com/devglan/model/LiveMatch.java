package com.devglan.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "LIVE_MATCH", indexes = {
	    @Index(name = "idx_is_deleted", columnList = "isDeleted")
	})
public class LiveMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String url;
	
	private boolean isDeleted = false; // Soft delete flag
    private String lastKnownState; // JSON string to store the last known state
    private int deletionAttempts = 0; // Counter for deletion attempts
    
    @Column(name="isDistributionDone")
    private Boolean distributionDone=false;

	public Boolean isDistributionDone() {
		 return Boolean.TRUE.equals(distributionDone);
	}

	public void setDistributionDone(Boolean distributionDone) {
		this.distributionDone = distributionDone;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public String getLastKnownState() {
		return lastKnownState;
	}

	public void setLastKnownState(String lastKnownState) {
		this.lastKnownState = lastKnownState;
	}

	public int getDeletionAttempts() {
		return deletionAttempts;
	}

	public void setDeletionAttempts(int deletionAttempts) {
		this.deletionAttempts = deletionAttempts;
	}

	public LiveMatch() {
	}

	public LiveMatch(String url) {
		this.url = url;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isFinished() {
		return isDeleted();
	}

	public String getWinningTeam() {
		if (lastKnownState != null && lastKnownState.contains("won by")) {
            String[] parts = lastKnownState.split(" won by");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return null;
	}


}
