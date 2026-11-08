package com.logitrack.security.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.logitrack.security.model.Usuario;
import com.logitrack.security.service.interfaces.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImp implements JwtService {

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.access-token-ttl:3600}")
    private long accessTokenTtl;

    /** @return segundos de vigencia utilizados en exp. */
    @Override
    public long expiresIn() { return accessTokenTtl; }

    /** Rechaza una configuración que emitiría tokens sin vigencia útil. */
    @jakarta.annotation.PostConstruct
    public void validateTtl() {
        if (accessTokenTtl <= 0) {
            throw new IllegalArgumentException("security.jwt.access-token-ttl must be positive seconds");
        }
    }

    private final JwtEncoder jwtEncoder;

    @Override
    public String generateToken(Usuario user) throws Exception {
    	List<String> roles = List.of(user.getRole().toString());

    	Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

}
