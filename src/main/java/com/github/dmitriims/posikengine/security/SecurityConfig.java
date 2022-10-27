package com.github.dmitriims.posikengine.security;

import com.github.dmitriims.posikengine.dto.userprovaideddata.UserProvidedData;
import com.github.dmitriims.posikengine.model.User;
import com.github.dmitriims.posikengine.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
        return new BCryptPasswordEncoder(14);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
          User user = userRepository.findByUsername(username);
          if(user == null) {
              throw new UsernameNotFoundException("Пользователь с именем '" + username + "' не найден");
          }
          return user;
        };
    }

    @Bean
    public ForbiddenAccessHandler forbiddenAccessHandler() {
        return new ForbiddenAccessHandler();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
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
