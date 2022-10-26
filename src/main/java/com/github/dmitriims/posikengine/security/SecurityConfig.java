package com.github.dmitriims.posikengine.security;

import com.github.dmitriims.posikengine.dto.userprovaideddata.AuthDetails;
import com.github.dmitriims.posikengine.dto.userprovaideddata.UserProvidedData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private UserProvidedData userProvidedData;
    private String homePage;

    public SecurityConfig(@Qualifier("homePage") String homePage, UserProvidedData userProvidedData) {
        this.userProvidedData = userProvidedData;
        this.homePage = homePage;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        List<UserDetails> users = new ArrayList<>();
        for(AuthDetails authDetails : userProvidedData.getAuthorisations()) {
            users.add(new User(
                    authDetails.getUsername(),
                    encoder.encode(authDetails.getPassword()),
                    Collections.singleton(new SimpleGrantedAuthority(authDetails.getRole()))
                    )
            );
        }
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public ForbiddenAccessHandler forbiddenAccessHandler() {
        return new ForbiddenAccessHandler();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic();
        http.antMatcher("/api/**")
            .authorizeRequests()
                .antMatchers("/api/search").access("hasAnyRole('SEARCH_API_USER', 'ADMIN')")
                .antMatchers("/api/**").access("hasRole('ADMIN')");
        return http.build();
    }

    @Bean

    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic().disable();
        http.exceptionHandling().accessDeniedHandler(forbiddenAccessHandler());
        http.formLogin()
            .loginPage("/login")
            .defaultSuccessUrl(homePage)
                .and().logout();
        http
            .authorizeRequests()
                .antMatchers("/admin").access("hasRole('ADMIN')")
                .antMatchers("/", "/greetings", "/login").access("permitAll()");
        return http.build();
    }
}
