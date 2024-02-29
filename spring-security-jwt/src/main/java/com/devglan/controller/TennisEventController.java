package com.devglan.controller;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
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
import okhttp3.ResponseBody;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/tennis")
public class TennisEventController {
	
private static final Logger log = LoggerFactory.getLogger(TennisEventController.class);

	
	@RequestMapping(value = "/tennis/tournaments", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getListOfEvents() {

		// abstract this logic to service layer later on 
		
		 OkHttpClient client = new OkHttpClient();
		 ObjectMapper objectMapper = new ObjectMapper();
		 Request request = new Request.Builder()
					.url("https://api.sofascore.com/api/v1/sport/tennis/events/live")
					.get()
					.build();

		//implement global error controller advice
		 try {
				ResponseBody responseBody = client.newCall(request).execute().body();
				System.out.println(request.toString());
//				Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

//				List<Market> result = entity.getResult();

				return new ResponseEntity<String>(responseBody.string(),HttpStatus.OK);
			} catch (IOException e) {
				log.info("failed fetching events" + e.getMessage());
			}
		return null;
		
	}
//	https://api.sofascore.com/api/v1/odds/1/featured-events/tennis
	
	@RequestMapping(value = "/tennis/all", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getListAllOfEvents() {

		//	System.out.println(sDateFormat);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		 String todayDate = LocalDate.now().format(formatter);
		 OkHttpClient client = new OkHttpClient();
		 
		 Request request = new Request.Builder()		
					.url("https://api.sofascore.com/api/v1/sport/tennis/scheduled-events/" + todayDate).build();

		 
		//implement global error controller advice
		 try {
				ResponseBody responseBody = client.newCall(request).execute().body();
				System.out.println(request.toString());
//				Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

//				List<Market> result = entity.getResult();

				return new ResponseEntity<String>(responseBody.string(),HttpStatus.OK);
			} catch (IOException e) {
				log.info("failed fetching events" + e.getMessage());
			}
		return null;
		
	}
//ranking
//	https://api.sofascore.com/api/v1/rankings/type/5
	@RequestMapping(value = "/tennis/atp/ranking", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getListAtpRankOfEvents() {

		
		 OkHttpClient client = new OkHttpClient();
		 Request request = new Request.Builder()
					.url("https://api.sofascore.com/api/v1/rankings/type/5")
					.get()
					.build();

		//implement global error controller advice
		 try {
				ResponseBody responseBody = client.newCall(request).execute().body();
				System.out.println(request.toString());
//				Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

//				List<Market> result = entity.getResult();

				return new ResponseEntity<String>(responseBody.string(),HttpStatus.OK);
			} catch (IOException e) {
				log.info("failed fetching events" + e.getMessage());
			}
		return null;
		
	}
	
	@RequestMapping(value = "/tennis/wta/ranking", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getListWtaRankOfEvents() {

		
		 OkHttpClient client = new OkHttpClient();
		 Request request = new Request.Builder()
					.url("https://api.sofascore.com/api/v1/rankings/type/6")
					.get()
					.build();

		//implement global error controller advice
		 try {
				ResponseBody responseBody = client.newCall(request).execute().body();
				System.out.println(request.toString());
//				Markets entity = objectMapper.readValue(responseBody.string(), Markets.class);

//				List<Market> result = entity.getResult();

				return new ResponseEntity<String>(responseBody.string(),HttpStatus.OK);
			} catch (IOException e) {
				log.info("failed fetching events" + e.getMessage());
			}
		return null;
		
	}


}
