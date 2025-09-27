package com.securevote.securevotebackend.Config;

import com.securevote.securevotebackend.Entity.User;
import com.securevote.securevotebackend.Repo.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:4200"); // Match Angular dev server
        configuration.addAllowedMethod("*"); // Allow all HTTP methods
        configuration.addAllowedHeader("*"); // Allow all headers (e.g., X-Wallet-Address)
        configuration.setAllowCredentials(true); // Allow credentials if needed
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all endpoints
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RoleFilter roleFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply CORS before security
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/voting/register", "/api/voting/user/**", "/api/voting/verify").permitAll()
                        .requestMatchers( "/api/candidates/getAllCandidates","/api/voting/{walletAddress}").permitAll()
                        .requestMatchers("/api/elections/getAllElection").hasRole("ADMIN")
                        .requestMatchers("/api/voting/vote").hasAnyRole("VOTER", "CANDIDATE")
                        .requestMatchers("/api/voting/results/**","/api/candidates/create","/api/candidates/{id}","/api/elections/createElection").hasRole("ADMIN")
                        .requestMatchers("/api/voting/{walletAddress}").authenticated()
                        .anyRequest().denyAll()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No session required
                .addFilterBefore(roleFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)));
        return http.build();
    }

    public static class RoleFilter extends OncePerRequestFilter {
        private final UserRepo userRepo;
        private static final Logger log = LoggerFactory.getLogger(RoleFilter.class);

        public RoleFilter(UserRepo userRepo) {
            this.userRepo = userRepo;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String path = request.getRequestURI();
            String method = request.getMethod();
            log.debug("Processing request: path={}, method={}, headers={}", path, method, request.getHeaderNames().toString());
            if (path.startsWith("/api/voting/register") || path.startsWith("/api/voting/user/") ||
                    path.startsWith("/api/voting/verify") || path.equals("/api/candidates/getAllCandidates")) {
                log.debug("Skipping RoleFilter for permitAll endpoint: {}, proceeding to chain", path);
                chain.doFilter(request, response);
                return;
            }

            String walletAddress = request.getHeader("X-Wallet-Address");
            if (walletAddress != null && !walletAddress.trim().isEmpty()) {
                User user = userRepo.findByWalletAddress(walletAddress).orElse(null);
                if (user != null) {
                    String prefixedRole = "ROLE_" + user.getRole().name();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(walletAddress, null,
                                    AuthorityUtils.createAuthorityList(prefixedRole))
                    );
                    log.info("Authenticated {} with role {}", walletAddress, prefixedRole);
                } else {
                    log.warn("No user found for wallet: {}", walletAddress);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            } else {
                log.warn("No X-Wallet-Address header provided for protected endpoint: {}", path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            chain.doFilter(request, response);
        }
    }

    @Bean
    public RoleFilter roleFilter(UserRepo userRepo) {
        return new RoleFilter(userRepo);
    }
}