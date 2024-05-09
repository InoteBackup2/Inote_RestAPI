package fr.inote.inoteApi.crossCutting.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(Endpoint.REGISTER)
                .allowedOrigins("*")
                .allowedMethods("POST")
                // .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");

                registry.addMapping(Endpoint.ACTIVATION)
                .allowedOrigins("*")
                .allowedMethods("POST")
                // .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");

                registry.addMapping(Endpoint.SIGN_IN)
                .allowedOrigins("*")
                .allowedMethods("POST")
                // .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");

                
    }
}
