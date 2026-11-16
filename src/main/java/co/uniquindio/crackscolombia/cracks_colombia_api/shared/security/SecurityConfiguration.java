package co.uniquindio.crackscolombia.cracks_colombia_api.shared.security;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.response.ApiErrorResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.AccountUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    AuthenticationManager authenticationManager(AccountUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtFilter,
                                            ObjectMapper objectMapper,
                                            Clock clock) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                                    "UNAUTHORIZED", "Debes iniciar sesión para acceder a este recurso.",
                                    clock.instant()));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                                    "FORBIDDEN", "Tu rol no tiene permiso para acceder a este recurso.",
                                    clock.instant()));
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/athletes/search")
                            .hasRole("OJEADOR_ENTRENADOR")
                        .requestMatchers(HttpMethod.POST, "/athletes/*/interest", "/requests")
                            .hasRole("OJEADOR_ENTRENADOR")
                        .requestMatchers(HttpMethod.POST, "/athletes/*/videos")
                            .hasRole("DEPORTISTA")
                        .requestMatchers(HttpMethod.PUT, "/athletes/*/photo")
                            .hasRole("DEPORTISTA")
                        .requestMatchers(HttpMethod.DELETE, "/videos/*")
                            .hasRole("DEPORTISTA")
                        .requestMatchers(HttpMethod.GET, "/users/me/videos")
                            .hasRole("DEPORTISTA")
                        .requestMatchers(HttpMethod.GET, "/videos/*/content", "/athletes/*/photo").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/requests/*/respond")
                            .hasRole("DEPORTISTA")
                        .requestMatchers("/moderation/**").hasRole("MODERADOR")
                        .requestMatchers("/users/me", "/requests/**", "/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/athletes/*", "/athletes/*/videos").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
