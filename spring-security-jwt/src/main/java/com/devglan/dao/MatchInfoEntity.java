package com.devglan.dao;

import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.devglan.model.TeamComparison;
import com.devglan.model.TeamForm;
import com.devglan.model.VenueStats;

@Entity
@Table(name = "match_info")
public class MatchInfoEntity {

    @Id
    private String url; // Same as the URL in CricketDataEntity, used to link the two tables

    private String matchDate;
    private String venue;
    private String matchName;
    private String tossInfo;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "match_info_url")
    private Set<TeamForm> teamForm;

	@ElementCollection
	@CollectionTable(name = "team_comparison", joinColumns = @JoinColumn(name = "match_info_url"))
	@MapKeyColumn(name = "team_name")
	private Map<String, TeamComparison> teamComparison;

    @Embedded
    private VenueStats venueStats;

    @OneToMany(mappedBy = "matchInfo", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<PlayingXIEntity> playingXI;
    

    public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMatchDate() {
		return matchDate;
	}

	public void setMatchDate(String matchDate) {
		this.matchDate = matchDate;
	}

	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public String getMatchName() {
		return matchName;
	}

	public void setMatchName(String matchName) {
		this.matchName = matchName;
	}

	public String getTossInfo() {
		return tossInfo;
	}

	public void setTossInfo(String tossInfo) {
		this.tossInfo = tossInfo;
	}


	public Map<String, TeamComparison> getTeamComparison() {
		return teamComparison;
	}

	public void setTeamComparison(Map<String, TeamComparison> teamComparison) {
		this.teamComparison = teamComparison;
	}

	public VenueStats getVenueStats() {
		return venueStats;
	}

	public void setVenueStats(VenueStats venueStats) {
		this.venueStats = venueStats;
	}

	public Set<TeamForm> getTeamForm() {
		return teamForm;
	}

	public void setTeamForm(Set<TeamForm> teamForm) {
		this.teamForm = teamForm;
	}

	public Set<PlayingXIEntity> getPlayingXI() {
		return playingXI;
	}

	public void setPlayingXI(Set<PlayingXIEntity> playingXI) {
		this.playingXI = playingXI;
	}

	


}
