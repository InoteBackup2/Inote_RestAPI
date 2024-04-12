package fr.inote.inoteApi.crossCutting.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Encryption password configuration
 *
 * @author Atsuhiko Mochizuki
 * @date 26/03/2024
 */

/* Nota : The @Configuration annotation in Spring is used to denote a class that
 * declares one or more @Bean methods and may be processed by the Spring IoC
 * container to generate bean definitions and service requests for those beans
 * at runtime.
 */
@Configuration
public class EncryptionPasswordConfiguration {
    
    /**
     * Create a BCryptPasswordEncoder bean
     *
     * @return BCryptPasswordEncoder bean
     * @author Atsuhiko Mochizuki
     * @date 26/03/2024
     * <p>
     * Nota : The @Bean annotation on a method in Spring indicates that the method
     * is a factory method for a Spring bean. When the application context loads, it
     * will call the method and register the returned object as a bean in the
     * context
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}