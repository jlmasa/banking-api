package com.jlmasa.banking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder, @Value("${spring.app.demo-password}") String demoPassword){
        String hash = encoder.encode(demoPassword);
        //passwords are stored hashed, never plain
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password(hash).roles("USER").build(),
                User.withUsername("bob").password(hash).roles("USER").build(),
                User.withUsername("admin").password(hash).roles("ADMIN").build());
    }
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()) //Stateless API with credentials in a header, no cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health" , "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().
                        requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasRole("ADMIN")
                        .anyRequest().authenticated()).httpBasic(Customizer.withDefaults()).build();
    }
}
