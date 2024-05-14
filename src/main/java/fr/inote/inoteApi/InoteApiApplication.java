package fr.inote.inoteApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info=@Info(title="Inote API documentation", version ="1.0", description = "Product functionnalities"))
public class InoteApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InoteApiApplication.class, args);
    }
}





