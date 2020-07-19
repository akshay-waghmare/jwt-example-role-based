package com.devglan.model;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.devglan.controller.JacksonCustomMarketDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = JacksonCustomMarketDeserializer.class)
public class Market {

	private Double id;
	private Integer eventTypeId;
	private BigInteger eventId;
	private LocalDateTime startTime;
	private String eventDetails;
	private String eventName;
	private String marketName;
	private String countryCode;
	private Double totalMatched;
	private Boolean inPlay;
	private String status;
	private LocalDateTime updateTime;

	public Double getId() {
		return id;
	}

	public void setId(Double id) {
		this.id = id;
	}

	public Integer getEventTypeId() {
		return eventTypeId;
	}

	public void setEventTypeId(Integer eventTypeId) {
		this.eventTypeId = eventTypeId;
	}

	public BigInteger getEventId() {
		return eventId;
	}

	public void setEventId(BigInteger eventId) {
		this.eventId = eventId;
	}

	

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public String getEventDetails() {
		return eventDetails;
	}

	public void setEventDetails(String eventDetails) {
		this.eventDetails = eventDetails;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getMarketName() {
		return marketName;
	}

	public void setMarketName(String marketName) {
		this.marketName = marketName;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public Double getTotalMatched() {
		return totalMatched;
	}

	public void setTotalMatched(Double totalMatched) {
		this.totalMatched = totalMatched;
	}

	public Boolean getInPlay() {
		return inPlay;
	}

	public void setInPlay(Boolean inPlay) {
		this.inPlay = inPlay;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	

}