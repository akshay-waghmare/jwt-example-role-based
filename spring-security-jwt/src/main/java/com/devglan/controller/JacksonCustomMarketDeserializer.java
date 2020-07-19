package com.devglan.controller;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.devglan.model.Market;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JacksonCustomMarketDeserializer extends StdDeserializer<Market> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 735617266948166428L;

	public JacksonCustomMarketDeserializer() {
		this(null);
	}

	public JacksonCustomMarketDeserializer(Class<Market> t) {
		super(t);
	}

	@Override
	public Market deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Market market = new Market();

		JsonNode treeNode = parser.getCodec().readTree(parser);
		Double id = treeNode.get("id").asDouble();
		Integer eventTypeId = treeNode.get("eventTypeId").asInt();
		Long asLong = treeNode.get("eventId").asLong();
		BigInteger eventId = BigInteger.valueOf(asLong);
		String sTime = treeNode.get("startTime").asText();
		LocalDateTime ssTime = LocalDateTime.parse(sTime, formatter);
		LocalDateTime startTime = ssTime.plusHours(5l).plusMinutes(30l);
		String eventDetails = treeNode.get("eventDetails") != null?treeNode.get("eventDetails").asText(" "):null;
		String eventName = treeNode.get("eventName").asText();
		String marketName = treeNode.get("marketName").asText();
		String countryCode = treeNode.get("countryCode").asText();
		Double totalMatched = treeNode.get("totalMatched").asDouble();
		Boolean inPlay = treeNode.get("inPlay").asBoolean();
		String status = treeNode.get("status").asText();
		String uTime = treeNode.get("updateTime").asText();
		ZonedDateTime parse = ZonedDateTime.parse(uTime);
		LocalDateTime updateTime = parse.toLocalDateTime();
//		LocalDateTime updateTime = LocalDateTime.parse(uTime, formatter);
		
		market.setId(id);
		market.setEventId(eventId);
		market.setEventTypeId(eventTypeId);
		market.setStartTime(startTime);
		market.setEventDetails(eventDetails);
		market.setEventName(eventName);
		market.setMarketName(marketName);
		market.setCountryCode(countryCode);
		market.setTotalMatched(totalMatched);
		market.setInPlay(inPlay);
		market.setStatus(status);
		market.setUpdateTime(updateTime);
		
		 
		return market;
	}

}