package com.logitrack.security.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwtConfig {

	@Value("${security.jwt.public-key-path}")
    private Resource publicKeyResource;

    @Value("${security.jwt.private-key-path}")
    private Resource privateKeyResource;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
    	byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyResource.getURI()));

        String privateKeyPEM = new String(keyBytes, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decodeKey = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory kf = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decodeKey));
    }

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
    	byte[] keyBytes = Files.readAllBytes(Paths.get(publicKeyResource.getURI()));

        String publicKeyPEM = new String(keyBytes, StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decodeKey = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory kf = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decodeKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        var jwkSource = new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey));

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();


        OAuth2TokenValidator<Jwt> identity = jwt -> {
            try {
                String sub = jwt.getSubject();
                Object claim = jwt.getClaims().get("roles");
                if (sub == null || !java.util.UUID.fromString(sub).toString().equals(sub)
                        || jwt.getExpiresAt() == null
                        || !(claim instanceof java.util.List<?> roles) || roles.size() != 1
                        || !(roles.get(0) instanceof String role)
                        || !java.util.List.of("ADMIN", "CLIENTE", "REPARTIDOR").contains(role)) {
                    throw new IllegalArgumentException();
                }
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException ex) {
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
            }
        };

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), identity);

        decoder.setJwtValidator(validator);

        return decoder;
    }

}
