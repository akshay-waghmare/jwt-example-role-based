package com.devglan.model;

import java.math.BigInteger;

public class Selection {

	
	private BigInteger id;
	private String name;
	private Double lastPriceTraded;
	private Double totalMatched;
	private String status;
	
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Double getLastPriceTraded() {
		return lastPriceTraded;
	}
	public void setLastPriceTraded(Double lastPriceTraded) {
		this.lastPriceTraded = lastPriceTraded;
	}
	public Double getTotalMatched() {
		return totalMatched;
	}
	public void setTotalMatched(Double totalMatched) {
		this.totalMatched = totalMatched;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	

}