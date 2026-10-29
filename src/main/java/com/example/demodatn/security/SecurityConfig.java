package com.example.demodatn.security;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/orders/**", "/checkout/**", "/profile", "/profile/**").authenticated()
                .requestMatchers("/", "/products/**", "/category/**", "/search", "/cart/**", "/login", "/register", "/policy", "/css/**", "/js/**", "/images/**", "/anh/**", "/uploads/products/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                })
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    if(authentication.getPrincipal() instanceof CustomUserDetails user && user.isMustChangePassword()) {response.sendRedirect("/account/change-required");return;}
                    org.springframework.security.web.savedrequest.SavedRequest savedRequest =
                        new org.springframework.security.web.savedrequest.HttpSessionRequestCache().getRequest(request, response);
                    
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            
                    if (isAdmin) {
                        response.sendRedirect("/admin");
                        return;
                    }
                    
                    if (savedRequest != null) {
                        response.sendRedirect(savedRequest.getRedirectUrl());
                    } else {
                        response.sendRedirect("/");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(new AccountStatusFilter(userDetailsService),
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
