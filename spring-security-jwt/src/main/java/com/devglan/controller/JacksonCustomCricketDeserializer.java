package com.devglan.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devglan.dao.CricketDataDTO;
import com.devglan.dao.MatchOdds;
import com.devglan.dao.OversData;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.SessionOverData;
import com.devglan.dao.TeamOdds;
import com.devglan.model.BatsmanData;
import com.devglan.model.BowlerData;
import com.devglan.model.PlayingXI;
import com.devglan.model.TeamComparison;
import com.devglan.model.TeamForm;
import com.devglan.model.TeamScore;
import com.devglan.model.VenueStats;
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

        // Extract match date, venue, match name, toss info, and URL
        if (node.has("match_date")) {
            cricketData.setMatchDate(node.get("match_date").asText());
        }
        if (node.has("venue")) {
            cricketData.setVenue(node.get("venue").asText());
        }
        if (node.has("match_name")) {
            cricketData.setMatchName(node.get("match_name").asText());
        }
        if (node.has("toss_info")) {
            cricketData.setTossInfo(node.get("toss_info").asText());
        }
        if (node.has("url")) {
            cricketData.setUrl(node.get("url").asText());
        }

        // Extract team form data
        if (node.has("team_form")) {
            Set<TeamForm> teamFormList = new HashSet<>();
            for (JsonNode teamFormNode : node.get("team_form")) {
                TeamForm teamForm = new TeamForm();
                teamForm.setMatchName(teamFormNode.get("match_name").asText());
                teamForm.setSeriesName(teamFormNode.get("series_name").asText());

                List<TeamScore> teamScores = new ArrayList<>();
                for (JsonNode teamNode : teamFormNode.get("teams")) {
                    TeamScore teamScore = new TeamScore();
                    teamScore.setTeamName(teamNode.get("team_name").asText());
                    teamScore.setTeamScore(teamNode.get("team_score").asText());
                    teamScore.setTeamOver(teamNode.get("team_over").asText());
                    teamScores.add(teamScore);
                }
                teamForm.setTeams(teamScores);
                teamFormList.add(teamForm);
            }
            cricketData.setTeamForm(teamFormList);
        }

        // Extract team comparison data
        if (node.has("team_comparison")) {
            Map<String, TeamComparison> teamComparisonMap = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> teamComparisonFields = node.get("team_comparison").fields();
            while (teamComparisonFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = teamComparisonFields.next();
                TeamComparison teamComparison = new TeamComparison();
                teamComparison.setMatchesPlayed(entry.getValue().get("matches_played").asText());
                teamComparison.setWinPercentage(entry.getValue().get("win_percentage").asText());
                teamComparison.setAvgScore(entry.getValue().get("avg_score").asText());
                teamComparison.setHighestScore(entry.getValue().get("highest_score").asText());
                teamComparison.setLowestScore(entry.getValue().get("lowest_score").asText());
                teamComparisonMap.put(entry.getKey(), teamComparison);
            }
            cricketData.setTeamComparison(teamComparisonMap);
        }

        // Extract venue stats data
        if (node.has("venue_stats")) {
            VenueStats venueStats = new VenueStats();
            JsonNode venueStatsNode = node.get("venue_stats");
            venueStats.setMatches(venueStatsNode.get("matches").asText());
            venueStats.setWinBatFirst(venueStatsNode.get("win_bat_first").asText());
            venueStats.setWinBowlFirst(venueStatsNode.get("win_bowl_first").asText());
            venueStats.setAvg1stInns(venueStatsNode.get("avg_1st_inns").asText());
            venueStats.setAvg2ndInns(venueStatsNode.get("avg_2nd_inns").asText());
            cricketData.setVenueStats(venueStats);
        }

        // Extract playing XI
        if (node.has("playing_xi")) {
            Map<String, Set<PlayingXI>> playingXIMap = new HashMap<>();
            JsonNode playingXINode = node.get("playing_xi");
            Iterator<Map.Entry<String, JsonNode>> teamXIFields = playingXINode.fields();
            while (teamXIFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = teamXIFields.next();
                Set<PlayingXI> teamPlayingXI = new HashSet<>();
                for (JsonNode playerNode : entry.getValue()) {
                    PlayingXI player = new PlayingXI();
                    player.setPlayerName(playerNode.get("playerName").asText());
                    player.setPlayerRole(playerNode.get("playerRole").asText());
                    teamPlayingXI.add(player);
                }
                playingXIMap.put(entry.getKey(), teamPlayingXI);
            }
            cricketData.setPlayingXI(playingXIMap);
        }
        
        if (node.has("batsman_data") && node.has("bowler_data")) {
            // Handle Batsman Data
            JsonNode batsmanDataNode = node.get("batsman_data");
            List<BatsmanData> batsmanDataList = new ArrayList<>();

            if (batsmanDataNode.isArray()) {
                for (JsonNode batsmanNode : batsmanDataNode) {
                    BatsmanData batsman = new BatsmanData();
                    batsman.setName(getTextValue(batsmanNode, "name", ""));
                    batsman.setScore(getTextValue(batsmanNode, "runs", "")); // Assuming score = runs
                    batsman.setBallsFaced(getTextValue(batsmanNode, "balls_faced", "0"));
                    batsman.setFours(getTextValue(batsmanNode, "fours", "0"));
                    batsman.setSixes(getTextValue(batsmanNode, "sixes", "0"));
                    // Add strike rate if available
                    if (batsmanNode.has("strike_rate")) {
                        batsman.setStrikeRate(batsmanNode.get("strike_rate").asText());
                    }
                 // Handle boolean "on_strike"
                    if (batsmanNode.has("on_strike")) {
                        batsman.setOnStrike(batsmanNode.get("on_strike").asBoolean()); // This will parse the boolean value
                    }
                    batsmanDataList.add(batsman);
                }
            }

            // Handle Bowler Data
            JsonNode bowlerDataNode = node.get("bowler_data");
            List<BowlerData> bowlerDataList = new ArrayList<>();

            if (bowlerDataNode.isObject()) {
                BowlerData bowler = new BowlerData();
                bowler.setName(getTextValue(bowlerDataNode, "name", ""));
                bowler.setScore(getTextValue(bowlerDataNode, "runs_conceded", "")); // Assuming score = runs_conceded
                bowler.setBallsBowled(getTextValue(bowlerDataNode, "balls_bowled", ""));
                bowler.setWicketsTaken(getTextValue(bowlerDataNode, "wickets_taken", ""));
                bowler.setDotBalls(getTextValue(bowlerDataNode, "dot_balls", ""));
                bowlerDataList.add(bowler);
            }

            // Set the lists to the cricketData object
            cricketData.setBatsmanData(batsmanDataList);
            cricketData.setBowlerData(bowlerDataList);
        }
        
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
                if (over != null) {
                    double overValue;
                    if (over.matches(".*[a-zA-Z]+.*")) {
                        // If the over string contains letters, extract the numeric part
                        String overNumericPart = over.replaceAll("[^0-9]", "");
                        overValue = Double.parseDouble(overNumericPart);
                    } else {
                        // Otherwise, parse it as a double directly
                        overValue = Double.parseDouble(over);
                    }
                    cricketData.setOver(overValue);
                }
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

     // Extract team-wise session data
        Map<String, List<SessionOverData>> teamWiseSessionData = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> teamNodes = node.fields();
        while (teamNodes.hasNext()) {
            Map.Entry<String, JsonNode> teamEntry = teamNodes.next();
            String teamName = teamEntry.getKey();
            JsonNode teamNode = teamEntry.getValue();

            if (teamNode.has("innings") && teamNode.has("Session")) {
                String innings = teamNode.get("innings").asText();
                List<SessionOverData> sessionList = new ArrayList<>();

                JsonNode sessions = teamNode.get("Session");
                if (sessions.isArray()) {
                    for (JsonNode sessionNode : sessions) {
                        SessionOverData sessionData = new SessionOverData();
                        sessionData.setName(sessionNode.get("name").asText());
                        sessionData.setOpen(sessionNode.get("open").asText());
                        sessionData.setPass(sessionNode.get("pass").asText());
                        sessionList.add(sessionData);
                    }
                }
                teamWiseSessionData.put(teamName + " - " + innings, sessionList);
            }
        }
        cricketData.setTeamWiseSessionData(teamWiseSessionData);

        
        // Extract fields from the new JSON structure
        if (node.has("firstTeamData")) {
            JsonNode firstTeamDataNode = node.get("firstTeamData").get(0); // Assuming only one team
            if (firstTeamDataNode.has("teamName")) {              
                cricketData.setFavTeam(firstTeamDataNode.get("teamName").asText());
                
                TeamOdds teamOdds = new TeamOdds();
                
                int backOdds = firstTeamDataNode.get("backOdds").asInt();
                int layOdds = firstTeamDataNode.get("layOdds").asInt();
                
                if (backOdds > 30) {
                    backOdds -= 1;
                }
                
                if (layOdds > 30) {
                    layOdds += 1;
                } else {
                    layOdds += 1;
                }
                
                teamOdds.setBackOdds(String.valueOf(backOdds));
                teamOdds.setLayOdds(String.valueOf(layOdds));
                cricketData.setTeamOdds(teamOdds);
            }
        }
        
        if (node.has("sessionData")) {
            // Assuming sessionData is an array with multiple session entries
            Set<SessionOdds> sessionOddsList = new HashSet<>();

            for (JsonNode sessionDataNode : node.get("sessionData")) { // Iterate over all session entries
                if (sessionDataNode != null && sessionDataNode.has("sessionName")) {
                    SessionOdds sessionOdds = new SessionOdds();
                    JsonNode oddsNode = sessionDataNode.get("odds").get(0);
                    int sessionBackOdds = oddsNode.get("value").asInt();
                    oddsNode = sessionDataNode.get("odds").get(1);
                    int sessionLayOdds = oddsNode.get("value").asInt();

                    // Check if sessionBackOdds is valid and adjust lay odds if necessary
                    if (sessionBackOdds != 0) {
                        if (sessionBackOdds == sessionLayOdds) {
                            sessionLayOdds += 1;
                        }
                        sessionOdds.setSessionBackOdds(String.valueOf(sessionBackOdds));
                        sessionOdds.setSessionLayOdds(String.valueOf(sessionLayOdds));
                    } else {
                        // In case sessionBackOdds is 0, fallback to string values from the node
                        sessionOdds.setSessionBackOdds(sessionDataNode.get("odds").get(0).get("value").asText());
                        sessionOdds.setSessionLayOdds(sessionDataNode.get("odds").get(1).get("value").asText());
                    }

                    sessionOdds.setSessionOver(sessionDataNode.get("sessionName").asText());

                    // Add the session odds object to the list
                    sessionOddsList.add(sessionOdds);
                }
            }

            // Store the list of session odds in the DTO
            cricketData.setSessionOddsList(sessionOddsList);
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
    
    private String getTextValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return defaultValue;
    }

}
