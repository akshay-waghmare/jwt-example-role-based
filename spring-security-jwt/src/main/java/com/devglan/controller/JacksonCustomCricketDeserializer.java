package com.devglan.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.TeamOdds;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JacksonCustomCricketDeserializer extends StdDeserializer<CricketDataDTO> {

	protected JacksonCustomCricketDeserializer(Class<CricketDataDTO> vc) {
		super(vc);
	}

	public JacksonCustomCricketDeserializer() {
		this(null);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 735617266948166428L;

	@Override
	public CricketDataDTO deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);
		CricketDataDTO cricketData = new CricketDataDTO();
		Map<String, List<String>> teamPlayerInfo = new HashMap<>();

		// Extract the fields from the JSON node and set them in the DTO
		if (node != null && node.has("team_odds")) {
			JsonNode teamOddsNode = node.get("team_odds");
			String backOdds = teamOddsNode.get("backOdds").asText();
			String layOdds = teamOddsNode.get("layOdds").asText();

			TeamOdds team_odds = new TeamOdds();
			team_odds.setBackOdds(backOdds);
			team_odds.setLayOdds(layOdds);

			cricketData.setTeamOdds(team_odds);
		}

		if (node != null && node.has("session_odds")) {
			JsonNode sessionOddsNode = node.get("session_odds");

			SessionOdds session_odds = new SessionOdds();
			session_odds.setSessionBackOdds(sessionOddsNode.get("session_back_odds").asText());
			session_odds.setSessionLayOdds(sessionOddsNode.get("session_lay_odds").asText());
			session_odds.setSessionOver(sessionOddsNode.get("over").asText());

			cricketData.setSessionOdds(session_odds);

		}

		if (node.has("over"))
			cricketData.setOver(node.get("over").asDouble());
		if (node.has("score"))
			cricketData.setScore(node.get("score").asText());
		if (node.has("current_ball")) {
			cricketData.setCurrentBall(node.get("current_ball").asText());
		}
		if (node.has("runs_on_ball"))
			cricketData.setRunsOnBall(node.get("runs_on_ball").asInt());
		if (node.has("fav_team"))
			cricketData.setFavTeam(node.get("fav_team").asText());

		if (node != null && node.has("team_player_info")) {
			JsonNode teamPlayerInfoNode = node.get("team_player_info").get("team_player_info");
			Iterator<String> fieldNames = teamPlayerInfoNode.fieldNames();
			while (fieldNames.hasNext()) {
				String teamName = fieldNames.next();
				JsonNode playersNode = teamPlayerInfoNode.get(teamName);
				List<String> players = new ArrayList<>();
				for (JsonNode playerNode : playersNode) {
					players.add(playerNode.asText());
				}
				teamPlayerInfo.put(teamName, players);
			}

			cricketData.setTeamPlayerInfo(teamPlayerInfo);
		}

		if (node != null && node.has("toss_won_country")) {
			cricketData.setToss_won_country(node.get("toss_won_country").asText());
		}
		if (node != null && node.has("bat_or_ball_selected")) {
			cricketData.setBat_or_ball_selected(node.get("bat_or_ball_selected").asText());
		}
		return cricketData;
	}

}