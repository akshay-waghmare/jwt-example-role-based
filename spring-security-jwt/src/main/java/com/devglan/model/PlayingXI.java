package com.devglan.model;

import javax.persistence.Embeddable;

@Embeddable
public class PlayingXI {
    private String playerName;
    private String playerRole;

    // Getters and Setters
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
}
