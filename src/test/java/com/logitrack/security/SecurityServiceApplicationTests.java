package com.logitrack.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.security.repository.UsuarioRepository;

/**
 * Pruebas de API y JWT con H2 y un par RSA efimero, independientes de MySQL
 * y de los secretos del equipo. MySQL real se valida aparte en E2E.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:security_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "security.jwt.issuer=logitrack-security",
        "mail.notifications.enabled=true",
        "mail.notifications.poll-delay-ms=3600000"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityServiceApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void loginAndValidationErrorsUseCommonUtcEnvelope() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"missing\",\"password\":\"test-password-123\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.timestamp", org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(4)));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(4)));
    }

    @Test
    void bootstrapIsIdempotentAndEmailLoginRespectsDisabledAccounts() throws Exception {
        var bootstrap = new com.logitrack.security.config.AdminBootstrap(usuarios, encoder,
                "bootstrap-admin", "bootstrap@example.invalid", "test-password-123");
        bootstrap.run();
        bootstrap.run();
        org.junit.jupiter.api.Assertions.assertEquals(1, usuarios.count());
        var admin = usuarios.findByUsername("bootstrap-admin").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("test-password-123", admin.getPassword()));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bootstrap@example.invalid\",\"password\":\"test-password-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expiresIn").value(3600));
        admin.setEneable(false);
        usuarios.saveAndFlush(admin);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bootstrap-admin\",\"password\":\"test-password-123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"missing-user\",\"password\":\"test-password-123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/login']").exists());
    }

    @DynamicPropertySource
    static void jwtTestKeys(DynamicPropertyRegistry registry) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            Path dir = Files.createTempDirectory("logitrack-security-test-");
            Path privateKey = dir.resolve("private.pem");
            Path publicKey = dir.resolve("public.pem");
            Files.writeString(privateKey, pem("PRIVATE KEY", pair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
            Files.writeString(publicKey, pem("PUBLIC KEY", pair.getPublic().getEncoded()), StandardCharsets.UTF_8);
            privateKey.toFile().deleteOnExit();
            publicKey.toFile().deleteOnExit();
            dir.toFile().deleteOnExit();
            registry.add("security.jwt.private-key-path", () -> privateKey.toUri().toString());
            registry.add("security.jwt.public-key-path", () -> publicKey.toUri().toString());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron generar claves RSA de prueba", e);
        }
    }

    private static String pem(String type, byte[] encoded) {
        String data = Base64.getMimeEncoder(64, new byte[] { 10 }).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + data + "\n-----END " + type + "-----\n";
    }

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM outbox_notifications");
        usuarios.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void registroCreaSolamenteCliente() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("cliente-prueba"))
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andExpect(jsonPath("$.eneable").value(true));
        var cliente = usuarios.findByUsername("cliente-prueba").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("PENDING",
                jdbc.queryForObject("SELECT status FROM outbox_notifications WHERE id = ?",
                        String.class, cliente.getId().toString()));
    }

    @Test
    void loginEmiteJwtYPermiteConsultarSoloElPropioUsuario() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro())).andExpect(status().isCreated());

        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"cliente-prueba\",\"password\":\"clave-de-prueba-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = mapper.readTree(body).get("accessToken").asText();

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("cliente-prueba"));

        mvc.perform(get("/users/550e8400-e29b-41d4-a716-446655440000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private static String registro() {
        return "{\"username\":\"cliente-prueba\",\"email\":\"cliente-prueba@example.com\","
                + "\"password\":\"clave-de-prueba-123\"}";
    }
}
