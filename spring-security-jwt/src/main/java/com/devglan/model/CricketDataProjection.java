package com.devglan.model;

import java.util.List;
import java.util.Map;

import com.devglan.dao.MatchOdds;
import com.devglan.dao.OversData;
import com.devglan.dao.SessionOdds;
import com.devglan.dao.SessionOverData;
import com.devglan.dao.TeamOdds;

public interface CricketDataProjection {
    List<MatchOdds> getMatchOdds();
    TeamOdds getTeamOdds();
    String getBattingTeamName();
    Double getOver();
    String getScore();
    String getCurrentBall();
    Integer getRunsOnBall();
    String getFavTeam();
    SessionOdds getSessionOdds();
    String getCurrentRunRate();
    String getFinalResultText();
    List<OversData> getOversData();
    Map<String, List<SessionOverData>> getTeamWiseSessionData();
    String getTossWonCountry();
    String getBatOrBallSelected();
    long getUpdatedTimeStamp();
}
