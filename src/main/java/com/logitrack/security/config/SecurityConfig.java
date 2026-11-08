package com.logitrack.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
        org.springframework.security.web.AuthenticationEntryPoint entry = (request, response, ex) ->
                writeError(response, mapper, 401, "Unauthorized", "Token ausente, inválido o expirado");
        org.springframework.security.web.access.AccessDeniedHandler denied = (request, response, ex) ->
                writeError(response, mapper, 403, "Forbidden", "No tiene permisos para realizar esta operación");
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(
							"/actuator/**",
							"/swagger-ui/**",
		                    "/swagger-ui.html",
		                    "/v3/api-docs/**"
	                ).permitAll()

					.requestMatchers(
							HttpMethod.POST,
							"/auth/register",
							"/auth/login"
					).permitAll()

					.requestMatchers(
							HttpMethod.POST,
							"/users"
	                ).hasRole("ADMIN")

					.requestMatchers(
							HttpMethod.GET,
							"/users/me"
	                ).authenticated()
                    .requestMatchers(HttpMethod.GET, "/users/*").hasRole("ADMIN")

					.anyRequest().denyAll()
			)
            .exceptionHandling(errors -> errors.authenticationEntryPoint(entry).accessDeniedHandler(denied))
			.oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(entry).accessDeniedHandler(denied)
	                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

    private static void writeError(jakarta.servlet.http.HttpServletResponse response,
            com.fasterxml.jackson.databind.ObjectMapper mapper, int status, String error, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(response.getOutputStream(), java.util.Map.of("status", status, "error", error,
                "mensaje", message, "timestamp", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString()));
    }

	@Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");

        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

}
