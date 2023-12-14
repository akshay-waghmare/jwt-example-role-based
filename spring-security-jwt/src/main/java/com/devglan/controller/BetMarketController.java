package com.devglan.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.devglan.model.BetMarket;
import com.devglan.model.BetMarketIn;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/market")
public class BetMarketController {
	
	
	private static final Logger log = LoggerFactory.getLogger(BetMarketController.class);

	
	
	@RequestMapping(value = "/{marketId}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<BetMarket> getBetMarketByMarketId(@PathVariable("marketId") double marketId) {
		
		OkHttpClient client = new OkHttpClient();
		 ObjectMapper objectMapper = new ObjectMapper();
		 Request request = new Request.Builder()
				    .url("https://sport-data.p.rapidapi.com/api/betMarket/" + marketId)
					.get()
					.addHeader("x-rapidapi-host", "sport-data.p.rapidapi.com")
					.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e")
					.build();

		//implement global error controller advice
		try {
			ResponseBody responseBody = client.newCall(request).execute().body();
			BetMarketIn entity = objectMapper.readValue(responseBody.string(), BetMarketIn.class);


			return new ResponseEntity<BetMarket>(entity.getResult(),HttpStatus.OK);
		} catch (IOException e) {
			log.info("failed fetching events" + e.getMessage());
		}
		return null;
	}
	
	@RequestMapping(value = "/cricket-data", method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<BetMarket> cricketData(@PathVariable("marketId") double marketId) {
		
		OkHttpClient client = new OkHttpClient();
		 ObjectMapper objectMapper = new ObjectMapper();
		 Request request = new Request.Builder()
				    .url("https://sport-data.p.rapidapi.com/api/betMarket/" + marketId)
					.get()
					.addHeader("x-rapidapi-host", "sport-data.p.rapidapi.com")
					.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e")
					.build();

		//implement global error controller advice
		try {
			ResponseBody responseBody = client.newCall(request).execute().body();
			BetMarketIn entity = objectMapper.readValue(responseBody.string(), BetMarketIn.class);


			return new ResponseEntity<BetMarket>(entity.getResult(),HttpStatus.OK);
		} catch (IOException e) {
			log.info("failed fetching events" + e.getMessage());
		}
		return null;
	}

}
