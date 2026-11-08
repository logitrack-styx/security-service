package com.logitrack.security.service.interfaces;

import com.logitrack.security.model.Usuario;

public interface JwtService {
    /** @return duración configurada del token de acceso en segundos. */
    long expiresIn();

	public String generateToken(Usuario user) throws Exception;

}
