package com.devglan.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.devglan.model.Event;
import com.devglan.model.Market;
import com.devglan.model.Markets;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/events")
public class EventController {

	private static final Logger log = LoggerFactory.getLogger(EventController.class);

	@RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<List<Event>> getListOfEvents() {

		// abstract this logic to service layer later on

		OkHttpClient client = new OkHttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		Request request = new Request.Builder().url("https://pinnacle-odds.p.rapidapi.com/v2/sports").get()
				.addHeader("x-rapidapi-host", "pinnacle-odds.p.rapidapi.com")
				.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e").build();

		// implement global error controller advice
		try {
		ResponseBody responseBody = client.newCall(request).execute().body();
			// for development purposes avoiding calls as calls are limited so hard coding
			String events = "[{\"id\":1,\"name\":\"Football\"},{\"id\":2,\"name\":\"Tennis\"},{\"id\":3,\"name\":\"Basketball\"}]";
			List<Event> entity = objectMapper.readValue(events, new TypeReference<List<Event>>() {
			});

			return new ResponseEntity<List<Event>>(entity, HttpStatus.OK);
		} catch (Exception e) {
			log.info("failed fetching events" + e.getMessage());
		}
		return null;

	}

	// upcoming matches here for new football betting odds api
	@RequestMapping(value = "/football/upcoming", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getUpcomingEventsForFootball() {

		OkHttpClient client = new OkHttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		Request request = new Request.Builder()
				.url("https://football-betting-odds1.p.rapidapi.com/provider1/live/upcoming").get()
				.addHeader("x-rapidapi-host", "football-betting-odds1.p.rapidapi.com")
				.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e").build();

		// implement global error controller advice
		try {
			ResponseBody responseBody = client.newCall(request).execute().body();
			System.out.println(request.toString());
//			Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

//			List<Market> result = entity.getResult();

			return new ResponseEntity<String>(responseBody.string(), HttpStatus.OK);
		} catch (IOException e) {
			log.info("failed fetching events" + e.getMessage());
		}
		return null;
	}

	// in-play matches here for new football betting odds api
	@RequestMapping(value = "/football/inplay", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getInplayEventsForFootball() {

		OkHttpClient client = new OkHttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		Request request = new Request.Builder()
				.url("https://football-betting-odds1.p.rapidapi.com/provider1/live/inplaying").get()
				.addHeader("x-rapidapi-host", "football-betting-odds1.p.rapidapi.com")
				.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e").build();

		// implement global error controller advice
		try {
			ResponseBody responseBody = client.newCall(request).execute().body();
			System.out.println(request.toString());
//				Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

//				List<Market> result = entity.getResult();

			return new ResponseEntity<String>(responseBody.string(), HttpStatus.OK);
		} catch (IOException e) {
			log.info("failed fetching events" + e.getMessage());
		}
		return null;
	}

	@RequestMapping(value = "/{eventId}/inplay/{isInplay}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<Markets> getBetMarketsByEvent(@PathVariable("eventId") int eventId,
			@PathVariable("isInplay") boolean isInplay) {

		OkHttpClient client = new OkHttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		Request request = new Request.Builder()
				.url("https://sport-data.p.rapidapi.com/api/listBetMarkets/" + eventId + "/" + isInplay).get()
				.addHeader("x-rapidapi-host", "sport-data.p.rapidapi.com")
				.addHeader("x-rapidapi-key", "927875fad7mshc0dd3c20a97f03ap1854f7jsnc0b1a557da8e").build();

		// implement global error controller advice
		try {
			ResponseBody responseBody = client.newCall(request).execute().body();
			Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

			List<Market> result = entity.getResult();

			return new ResponseEntity<Markets>(entity, HttpStatus.OK);
		} catch (IOException e) {
			log.info("failed fetching events" + e.getMessage());
		}
		return null;
	}

	
	
	/**
	 * This function retrieves inplay cricket data
	 */
	@RequestMapping(value = "/cricket/inplay", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getCricketInplayInfo() {
		String cricketInplayInfo = "Akash is playing cricket";

		System.out.println(cricketInplayInfo);
		OkHttpClient client = new OkHttpClient();// client communicates with the server eg - browser
		ObjectMapper objectMapper = new ObjectMapper();
		Request request = new Request.Builder()
		.url("https://api.sofascore.com/api/v1/sport/cricket/events/live").build();
		 try {
		        Response responseBody = client.newCall(request).execute();
		        if (responseBody.isSuccessful()) {
		            String rBody = responseBody.body().string();
		            System.out.println(rBody);
		            return new ResponseEntity<String>(rBody, HttpStatus.OK);
		        } else {
		            System.out.println("Error: " + responseBody.code());
		        }
		    } catch (IOException e)
		 {
		        log.info("Failed fetching in-play events: " + e.getMessage());
		    }
	    return null;
	}


	@RequestMapping(value = "/cricket/upcoming", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getCricketUpcomingInfo() {
		String cricketUpcomingInfo = "Exciting upcoming cricket are";

		System.out.println(cricketUpcomingInfo);
		OkHttpClient client = new OkHttpClient();// client communicates with the server eg - browser
		ObjectMapper objectMapper = new ObjectMapper();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		 String todayDate = LocalDate.now().format(formatter);

		        Request request = new Request.Builder()		
		.url("https://api.sofascore.com/api/v1/sport/cricket/scheduled-events/" + todayDate).build();
		try {
			Response responseBody = client.newCall(request).execute();// actualling sending a request
			if (responseBody.isSuccessful()) {
                String rBody =  responseBody.body().string();
                responseBody.body().string(); 
                System.out.println(rBody);
                return new ResponseEntity<String>(rBody, HttpStatus.OK); // returning this data to caller
            } else {
                System.out.println("Error: " + responseBody.code());
            }
			
		} catch (IOException e) {
			log.info("failed fetching events" + e.getMessage());
		}
		
	
         return null;
	}
}