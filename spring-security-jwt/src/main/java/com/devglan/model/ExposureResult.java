package com.devglan.model;

import java.math.BigDecimal;

public class ExposureResult {
    private BigDecimal adjustedYesExposure;
    private BigDecimal adjustedNoExposure;
    private String higherExposure;

    public ExposureResult(BigDecimal adjustedYesExposure, BigDecimal adjustedNoExposure, String higherExposure) {
        this.adjustedYesExposure = adjustedYesExposure;
        this.adjustedNoExposure = adjustedNoExposure;
        this.higherExposure = higherExposure;
    }

    // Getters and Setters
    public BigDecimal getAdjustedYesExposure() {
        return adjustedYesExposure;
    }

    public BigDecimal getAdjustedNoExposure() {
        return adjustedNoExposure;
    }

    public String getHigherExposure() {
        return higherExposure;
    }
    
    public BigDecimal getTotalExposure() {
        return adjustedYesExposure.add(adjustedNoExposure);
    }
}

