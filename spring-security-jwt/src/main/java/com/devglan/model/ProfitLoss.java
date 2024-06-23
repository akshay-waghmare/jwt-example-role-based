package com.devglan.model;


import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfitLoss {

    private String matchUrl;
    private BigDecimal profitOrLoss;
    private String matchName; // New field for formatted match name

    public ProfitLoss(String matchUrl, BigDecimal profitOrLoss) {
        this.matchUrl = matchUrl;
        this.profitOrLoss = profitOrLoss;
        this.matchName = extractMatchNameFromUrl(matchUrl);
    }

    private String extractMatchNameFromUrl(String matchUrl) {
        // Regex pattern to match different parts of the URL
        Pattern pattern = Pattern.compile("^([^-]+)-vs-([^-]+)-");
        Matcher matcher = pattern.matcher(matchUrl);

        if (matcher.find()) {
            String firstTeam = matcher.group(1).toUpperCase();
            String secondTeam = matcher.group(2).toUpperCase();
            return firstTeam + " vs " + secondTeam;
        } else {
            return matchUrl; // Fallback to the full URL if the format is unexpected
        }
    }

    // Getters and setters
    public String getMatchUrl() {
        return matchUrl;
    }

    public void setMatchUrl(String matchUrl) {
        this.matchUrl = matchUrl;
        this.matchName = extractMatchNameFromUrl(matchUrl);
    }

    public BigDecimal getProfitOrLoss() {
        return profitOrLoss;
    }

    public void setProfitOrLoss(BigDecimal profitOrLoss) {
        this.profitOrLoss = profitOrLoss;
    }

    public String getMatchName() {
        return matchName;
    }
}
