package com.logitrack.security.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.logitrack.security.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    /** @param email identidad de login contractual; @return usuario registrado. */
    Optional<Usuario> findByEmail(String email);

	public Optional<Usuario> findByUsername(String username);

	public boolean existsByUsername(String username);

	public boolean existsByEmail(String email);

}
