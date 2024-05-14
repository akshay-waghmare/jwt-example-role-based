package com.devglan.model;

import java.util.ArrayList;
import java.util.List;

public class BetMarket {

	Market market = new Market();
	
	List<Selection> selections = new ArrayList<>();
	
	public Market getMarket() {
		return market;
	}
	public void setMarket(Market market) {
		this.market = market;
	}
	public List<Selection> getSelections() {
		return selections;
	}
	public void setSelections(List<Selection> selections) {
		this.selections = selections;
	}
	
	

	

}