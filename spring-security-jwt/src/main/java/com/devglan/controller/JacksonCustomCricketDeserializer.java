package com.devglan.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.MatchOdds;
import com.devglan.dao.OversData;
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

        if (node.has("match_update")) {
            JsonNode matchUpdateNode = node.get("match_update");

            // Extracting score information
            if (matchUpdateNode.has("score") && matchUpdateNode.get("score").isObject()) {
                JsonNode scoreNode = matchUpdateNode.get("score");
                String teamName = scoreNode.has("teamName") ? scoreNode.get("teamName").asText() : null;
                String score = scoreNode.has("score") ? scoreNode.get("score").asText() : null;
                String over = scoreNode.has("over") ? scoreNode.get("over").asText() : null;
                
                cricketData.setBattingTeamName(teamName); // Assume you have a setter for team name
                cricketData.setScore(score); // Reuse existing field for score
                cricketData.setOver(Double.parseDouble(over)); // Assuming over is a string that needs conversion
            }

            // Extracting CRR
            if (matchUpdateNode.has("crr")) {
                String crr = matchUpdateNode.get("crr").asText();
                cricketData.setCurrentRunRate(crr); // Assuming you have a setter for CRR
            }

            // Extracting final result text
            if (matchUpdateNode.has("final_result_text")) {
                String finalResultText = matchUpdateNode.get("final_result_text").asText();
                cricketData.setFinalResultText(finalResultText); // Assuming you have a setter for final result text
            }
        }
        
        if (node.has("overs_data")) {
            JsonNode oversDataNode = node.get("overs_data");
            List<OversData> oversDataList = new ArrayList<>();

            if (oversDataNode.isArray()) {
                for (JsonNode overNode : oversDataNode) {
                    OversData oversData = new OversData();
                    oversData.setOverNumber(overNode.get("overNumber").asText());
                    
                    List<String> balls = new ArrayList<>();
                    for (JsonNode ballNode : overNode.get("balls")) {
                        balls.add(ballNode.asText());
                    }
                    oversData.setBalls(balls);

                    oversData.setTotalRuns(overNode.get("totalRuns").asText());
                    oversDataList.add(oversData);
                }
            }
            cricketData.setOversData(oversDataList);
        }
        
        if (node.has("data")) {
            JsonNode matchOddsListNode = node.get("data");
            List<MatchOdds> matchOddsList = new ArrayList<>();
            if (matchOddsListNode.isArray()) {
                for (JsonNode oddsNode : matchOddsListNode) {
                    MatchOdds matchOdds = new MatchOdds();
                    TeamOdds teamOdds = new TeamOdds();
                    matchOdds.setTeamName(oddsNode.get("teamName").asText());
                    teamOdds.setBackOdds(oddsNode.get("backOdds").asText());
                    teamOdds.setLayOdds(oddsNode.get("layOdds").asText());
                    matchOdds.setOdds(teamOdds);
                    matchOddsList.add(matchOdds);
                }
            }
            cricketData.setMatchOdds(matchOddsList);
        }

        
        // Extract fields from the new JSON structure
        if (node.has("firstTeamData")) {
            JsonNode firstTeamDataNode = node.get("firstTeamData").get(0); // Assuming only one team
            if (firstTeamDataNode.has("teamName")) {              
                cricketData.setFavTeam(firstTeamDataNode.get("teamName").asText());
                
                TeamOdds teamOdds = new TeamOdds();
                teamOdds.setBackOdds(firstTeamDataNode.get("backOdds").asText());
                teamOdds.setLayOdds(firstTeamDataNode.get("layOdds").asText());
                cricketData.setTeamOdds(teamOdds);
            }
        }
        
        if (node.has("sessionData")) {
            JsonNode sessionDataNode = node.get("sessionData").get(0); // Assuming only one session

            if (sessionDataNode != null && sessionDataNode.has("sessionName")) {              
                SessionOdds sessionOdds = new SessionOdds();
                JsonNode oddsNode = sessionDataNode.get("odds").get(0);
                sessionOdds.setSessionBackOdds(oddsNode.get("value").asText());
                sessionOdds.setSessionLayOdds(oddsNode.get("value").asText());
                sessionOdds.setSessionOver(sessionDataNode.get("sessionName").asText());
                cricketData.setSessionOdds(sessionOdds);
            }
        }
        
        // Extract the URL from the JSON node and set it in the DTO
        if (node.has("url")) {
            cricketData.setUrl(node.get("url").asText());
        }

        // Extract the fields from the JSON node and set them in the DTO
        if (node.has("team_odds")) {
            JsonNode teamOddsNode = node.get("team_odds");
            String backOdds = teamOddsNode.get("backOdds").asText();
            String layOdds = teamOddsNode.get("layOdds").asText();

            TeamOdds teamOdds = new TeamOdds();
            teamOdds.setBackOdds(backOdds);
            teamOdds.setLayOdds(layOdds);

            cricketData.setTeamOdds(teamOdds);
        }
        
        if (node.has("teamName")) {
            cricketData.setBattingTeamName(node.get("teamName").asText());
        }

        if (node.has("session_odds")) {
            JsonNode sessionOddsNode = node.get("session_odds");

            SessionOdds sessionOdds = new SessionOdds();
            sessionOdds.setSessionBackOdds(sessionOddsNode.get("session_back_odds").asText());
            sessionOdds.setSessionLayOdds(sessionOddsNode.get("session_lay_odds").asText());
            sessionOdds.setSessionOver(sessionOddsNode.get("over").asText());

            cricketData.setSessionOdds(sessionOdds);
        }
        
        if (node.has("over"))
            cricketData.setOver(node.get("over").asDouble());
        if (node.has("score"))
            cricketData.setScore(node.get("score").asText());
        if (node.has("score_update")) {
            cricketData.setCurrentBall(node.get("score_update").asText());
        }
        if (node.has("runs_on_ball"))
            cricketData.setRunsOnBall(node.get("runs_on_ball").asInt());
        if (node.has("fav_team"))
            cricketData.setFavTeam(node.get("fav_team").asText());

        if (node.has("team_player_info")) {
            JsonNode teamPlayerInfoNode = node.get("team_player_info").get("team_player_info");
            Iterator<Map.Entry<String, JsonNode>> fields = teamPlayerInfoNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String teamName = entry.getKey();
                JsonNode playersNode = entry.getValue();
                List<String> players = new ArrayList<>();
                for (JsonNode playerNode : playersNode) {
                    players.add(playerNode.asText());
                }
                teamPlayerInfo.put(teamName, players);
            }

            //cricketData.setTeamPlayerInfo(teamPlayerInfo);
        }

        if (node.has("toss_won_country")) {
            cricketData.setToss_won_country(node.get("toss_won_country").asText());
        }
        if (node.has("bat_or_ball_selected")) {
            cricketData.setBat_or_ball_selected(node.get("bat_or_ball_selected").asText());
        }
        return cricketData;
    }

}
