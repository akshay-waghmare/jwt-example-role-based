package com.devglan.config;


import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Resource(name = "userService")
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtAuthenticationEntryPoint unauthorizedHandler;

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Autowired
	public void globalUserDetails(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(encoder());
	}

	@Bean
	public JwtAuthenticationFilter authenticationTokenFilterBean() throws Exception {
		return new JwtAuthenticationFilter();
	}

	@Bean
	public CORSFilter corsFilterBean() throws Exception {
		return new CORSFilter();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.cors().and().headers()
				.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
				.and().csrf().disable()

				.authorizeRequests()
				.antMatchers(
						"/api/**",
						"/vote/**",
				        "/users/search", "/users/search/**", 
				        "/ws/*", "/ws/**", 
				        "/h2-console/**", 
				        "/token/*", "/token", "/token/**",
						"/signup", "/football", "/football/**", 
						"/events", "/events/**", 
						"/market/**", 
						"/tennis", "/tennis/**", 
						"/cricket-data", "/cricket-data/**", 
						"/bet-history", "/bet-history/**",
						"/cricket-data/bets","cricket-data/bets/**",
						"/cricket-data/blog-posts","cricket-data/blog-posts/**",
						"cricket-data/match-info/get","cricket-data/match-info/get/**",
						"/cricket-data/update-winning-team", "/cricket-data/update-winning-team/**"
				).permitAll()
				.anyRequest().authenticated()
				.and().exceptionHandling()
				.authenticationEntryPoint(unauthorizedHandler)
				.and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(
		        "/users/search", "/users/search/**", 
		        "/h2-console/**", 
		        "/ws/*", "/ws/**", 
		        "/token", "/token/*", 
		        "/cricket-data/update-winning-team", "/cricket-data/update-winning-team/**"
		);
	}

	@Bean
	public BCryptPasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
