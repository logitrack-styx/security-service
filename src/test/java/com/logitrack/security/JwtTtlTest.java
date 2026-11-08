package com.logitrack.security;

import com.logitrack.security.service.impl.JwtServiceImp;
import com.logitrack.security.model.Usuario;
import com.logitrack.security.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifica que el TTL configurado se refleja en los claims y rechaza valores no positivos.
 * @author dominox45
 */
class JwtTtlTest {
    @Test
    void configuredTtlControlsExpiration() throws Exception {
        JwtEncoder encoder = mock(JwtEncoder.class);
        JwtServiceImp service = new JwtServiceImp(encoder);
        ReflectionTestUtils.setField(service, "issuer", "logitrack-security");
        ReflectionTestUtils.setField(service, "accessTokenTtl", 120L);
        service.validateTtl();
        when(encoder.encode(any())).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            var claims = parameters.getClaims();
            assertEquals(120, Duration.between(claims.getIssuedAt(), claims.getExpiresAt()).toSeconds());
            assertEquals("RS256", parameters.getJwsHeader().getAlgorithm().getName());
            return Jwt.withTokenValue("test-token").header("alg", "RS256")
                    .claims(map -> map.putAll(claims.getClaims())).build();
        });
        service.generateToken(Usuario.builder().id(UUID.randomUUID()).role(RolUsuario.CLIENTE).build());
        verify(encoder).encode(any());
        for (long invalid : new long[]{0, -1}) {
            ReflectionTestUtils.setField(service, "accessTokenTtl", invalid);
            assertThrows(IllegalArgumentException.class, service::validateTtl);
        }
    }
}
