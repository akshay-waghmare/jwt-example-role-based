package com.devglan.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class TeamPlayerInfoDeserializer extends StdDeserializer<Map<String, List<String>>> {

	public TeamPlayerInfoDeserializer() {
		this(null);
	}

	protected TeamPlayerInfoDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Map<String, List<String>> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException {
		JsonNode node = jsonParser.getCodec().readTree(jsonParser);

		Map<String, List<String>> teamPlayerInfo = new HashMap<>();

		if (node != null && node.has("team_player_info")) {
			JsonNode teamPlayerInfoNode = node.get("team_player_info");
			for (JsonNode teamNode : teamPlayerInfoNode) {
				if (teamNode.has("team_name") && teamNode.has("players")) {
					String teamName = teamNode.get("team_name").asText();
					JsonNode playersNode = teamNode.get("players");
					List<String> players = new ArrayList<>();
					for (JsonNode playerNode : playersNode) {
						players.add(playerNode.asText());
					}
					teamPlayerInfo.put(teamName, players);
				}
			}
		}

		return teamPlayerInfo;
	}
}
