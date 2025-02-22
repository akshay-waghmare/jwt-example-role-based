package com.devglan.model;

import java.util.Map;

public class PollResults {
    private String question;
    private Map<String, Double> percentages;

    public PollResults(String question, Map<String, Double> percentages) {
        this.question = question;
        this.percentages = percentages;
    }

    public String getQuestion() {
        return question;
    }

    public Map<String, Double> getPercentages() {
        return percentages;
    }
}
