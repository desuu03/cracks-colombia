package co.uniquindio.crackscolombia.cracks_colombia_api.shared.security;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.response.ApiErrorResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.AccountUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

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
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtFilter,
                                            ObjectMapper objectMapper,
                                            Clock clock) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
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
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/athletes/search")
                            .hasRole("OJEADOR_ENTRENADOR")
                        .requestMatchers(HttpMethod.POST, "/athletes/*/interest", "/requests")
                            .hasRole("OJEADOR_ENTRENADOR")
                        .requestMatchers(HttpMethod.POST, "/athletes/*/videos")
                            .hasRole("DEPORTISTA")
                        .requestMatchers(HttpMethod.DELETE, "/videos/*")
                            .hasRole("DEPORTISTA")
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
