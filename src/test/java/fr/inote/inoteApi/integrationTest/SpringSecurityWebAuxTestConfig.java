package fr.inote.inoteApi.integrationTest;

import java.util.List;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static fr.inote.inoteApi.ConstantsForTests.*;
@TestConfiguration
public class SpringSecurityWebAuxTestConfig {

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .roles(String.valueOf(Role.builder().name(RoleEnum.ADMIN).build()))
                .build();

        return new InMemoryUserDetailsManager(List.of(
                admin
        ));
    }
}
