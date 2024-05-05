package fr.inote.inoteApi.crossCutting.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;

@Configuration
public class WebConfig {
    
	@Value("${server.name}")
	private String serverName;

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping(Endpoint.REGISTER).allowedOrigins("http://localhost:8080");
			}
		};
	}

}
