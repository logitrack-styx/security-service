package com.logitrack.security.config;

import com.logitrack.security.enums.RolUsuario;
import com.logitrack.security.model.Usuario;
import com.logitrack.security.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer administrador de forma idempotente cuando se configura su contraseña.
 * No restablece contraseñas ni eleva cuentas existentes. Los secretos nunca se registran.
 * @author dominox45
 */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UsuarioRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String email;
    private final String password;

    /**
     * @param users almacén de identidades
     * @param encoder hash de contraseña
     * @param username identificador de login
     * @param email correo administrativo
     * @param password secreto externo; vacío desactiva el bootstrap
     */
    public AdminBootstrap(UsuarioRepository users, PasswordEncoder encoder,
            @Value("${ADMIN_USERNAME:admin}") String username,
            @Value("${ADMIN_EMAIL:}") String email,
            @Value("${ADMIN_PASSWORD:}") String password) {
        this.users = users; this.encoder = encoder;
        this.username = username; this.email = email; this.password = password;
    }

    /**
     * @param args argumentos de arranque no utilizados
     * @throws IllegalStateException ante conflicto con una cuenta existente
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (password.isBlank()) return;
        var existing = users.findByUsername(username);
        if (existing.isPresent()) {
            if (existing.get().getRole() != RolUsuario.ADMIN)
                throw new IllegalStateException("La identidad de bootstrap ya existe con otro rol");
            return;
        }
        if (email.isBlank() || password.length() < 8)
            throw new IllegalStateException("Bootstrap requiere email y contraseña de al menos 8 caracteres");
        users.save(Usuario.builder().username(username).email(email)
                .password(encoder.encode(password)).role(RolUsuario.ADMIN).eneable(true).build());
    }
}
