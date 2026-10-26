package com.ayan.shop.config;

import com.ayan.shop.model.Customer;
import com.ayan.shop.repository.Customers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(Customers customers) {
        return email -> {
            Customer customer = customers.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Unknown email"));
            boolean active = customer.active == null || customer.active;
            return User.withUsername(customer.email)
                    .password(customer.password)
                    .authorities("ROLE_" + customer.role)
                    .disabled(!active)
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/error", "/403", "/css/**", "/img/**",
                                "/uploads/**", "/products/**", "/actuator/health", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", false).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/403"))
                .build();
    }
}
