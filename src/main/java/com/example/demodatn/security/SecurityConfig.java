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
    private final AdminAccess access;

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
                .requestMatchers("/admin/**").access((authentication,ctx)->new org.springframework.security.authorization.AuthorizationDecision(access.allowed(authentication.get(),ctx.getRequest().getMethod(),ctx.getRequest().getRequestURI().substring(ctx.getRequest().getContextPath().length()))))
                .requestMatchers("/orders/**", "/checkout/**", "/profile", "/profile/**").authenticated()
                .requestMatchers("/api/support/**", "/ws/support/**").permitAll()
                .requestMatchers("/payment/**", "/api/payment/**", "/api/shop/payments/**", "/vnpay-demo", "/vnpay-demo.html").permitAll()
                .requestMatchers("/", "/products/**", "/category/**", "/search", "/cart/**", "/login", "/register", "/policy", "/api/chatbot/**", "/css/**", "/js/**", "/images/**", "/anh/**", "/uploads/products/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri != null && (uri.startsWith("/admin/chatbot") || uri.startsWith("/admin/chat") || uri.startsWith("/api/") || uri.equals("/checkout/vouchers"))) {
                        response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        response.sendRedirect("/login");
                    }
                })
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
                    
                    if(access.hasRole(authentication,"ROLE_ADMIN")||access.hasRole(authentication,"ROLE_EMPLOYEE")) {
                        response.sendRedirect(access.home(authentication));return;
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
        
        // SockJS transport POSTs and payment webhook/simulation endpoints ignore CSRF.
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/ws/support/**", "/api/payment/**", "/api/shop/payments/**", "/payment/vnpay/**"));
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
