package com.logitrack.security.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.security.dto.request.LoginRequest;
import com.logitrack.security.dto.request.RegisterRequest;
import com.logitrack.security.dto.response.AuthResponse;
import com.logitrack.security.dto.response.UsuarioResponse;
import com.logitrack.security.enums.RolUsuario;
import com.logitrack.security.exception.CredencialesInvalidasException;
import com.logitrack.security.exception.UsuarioNoEncontradoException;
import com.logitrack.security.exception.UsuarioYaExistenteException;
import com.logitrack.security.model.Usuario;
import com.logitrack.security.notification.NotificationOutbox;
import com.logitrack.security.repository.UsuarioRepository;
import com.logitrack.security.service.interfaces.AuthService;
import com.logitrack.security.service.interfaces.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final NotificationOutbox notificationOutbox;

	@Override
	@Transactional
	public UsuarioResponse register(RegisterRequest request) {
	    if (usuarioRepository.existsByUsername(request.getUsername())) {
	        throw new UsuarioYaExistenteException("username", request.getUsername());
	    }

	    if (usuarioRepository.existsByEmail(request.getEmail())) {
	        throw new UsuarioYaExistenteException("email", request.getEmail());
	    }

		Usuario usuario = Usuario.builder()
	    		.username(request.getUsername())
	    		.email(request.getEmail())
	    		.password(passwordEncoder.encode(request.getPassword()))
	    		.role(RolUsuario.CLIENTE)
	    		.eneable(true)
	    		.build();

		usuario = usuarioRepository.save(usuario);
		notificationOutbox.enqueueWelcome(usuario.getId(), usuario.getEmail());

		log.info("Se ha registrado el Usuario: " + usuario.getId());
	    return mapToResponse(usuario);
	}

	@Override
	@Transactional
	public AuthResponse login(LoginRequest request) throws Exception {
		Usuario usuario = (request.getEmail() != null && !request.getEmail().isBlank()
                ? usuarioRepository.findByEmail(request.getEmail())
                : usuarioRepository.findByUsername(request.getUsername()))
	            .orElseThrow(() -> {
                	log.info("No se ha encontrado el Usuario: " + request.getUsername());
                    return new CredencialesInvalidasException("usuario");
	            });

	    if (!usuario.isEneable() || !passwordEncoder.matches(
	            request.getPassword(),
	            usuario.getPassword()
	    )) {
	    	log.info("Contraseña incorrecta para el Usuario: " + usuario.getId());
	        throw new CredencialesInvalidasException("password");
	    }

	    String token = jwtService.generateToken(usuario);

	    log.info("Se ha logeadoc el Usuario: " + usuario.getId());
	    return AuthResponse.builder()
	            .expiresIn(jwtService.expiresIn())
	    		.accessToken(token)
	    		.tokenType("Bearer")
	    		.build();
	}

	private UsuarioResponse mapToResponse(Usuario usuario) {
		return UsuarioResponse.builder()
				.id(usuario.getId())
				.username(usuario.getUsername())
				.email(usuario.getEmail())
				.role(usuario.getRole())
				.eneable(usuario.isEneable())
				.build();
	}

}
