package com.devglan.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "match_info_detail")
public class MatchInfoDetailEntity {

    @Id
    private String url; // Same as the URL in CricketDataEntity, used to link the two tables
    
    @Lob
    @Column(columnDefinition = "CLOB")
    private String data;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	


}
