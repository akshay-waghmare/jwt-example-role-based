package com.devglan.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@EnableScheduling
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	
	
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableStompBrokerRelay("/topic")
//        .setRelayHost("127.0.0.1")
//        .setClientLogin("guest")
//        .setClientPasscode("guest")
//        .setRelayPort(61613);
    	
    	//messages having this prefix will be interpreted by @MessageMapping() annotation 
    	//eg. /topic/add call from client side will be interpreted by @MessageMapping("topic")
        config.enableSimpleBroker("/topic");
    	//messages having this prefix will be interpreted by @MessageMapping() annotation 
    	//eg. /app/add call from client side will be interpreted by @MessageMapping("add")
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
//    	registry.setSendBufferSizeLimit(0);
    	registry.setMessageSizeLimit(500 * 1024); // default : 64 * 1024
    	registry.setSendTimeLimit(20 * 10000); // default : 10 * 10000
    	registry.setSendBufferSizeLimit(1024 * 1024); // default : 512 * 1024
	}
    

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
    	//client will use this to connect to server
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }
}