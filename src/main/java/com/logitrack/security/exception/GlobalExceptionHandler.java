package com.logitrack.security.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * @param ex entrada inválida, sin exponer su contenido
     * @return error 400 con formato común
     */
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> invalidInput(Exception ex) {
        return common(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos");
    }

    /** @param ex conflicto de persistencia; @return 409 sin SQL ni datos sensibles. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> duplicate(org.springframework.dao.DataIntegrityViolationException ex) {
        return common(HttpStatus.CONFLICT, "La identidad ya está registrada");
    }

    private ResponseEntity<ErrorResponse> common(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), status.value(), status.getReasonPhrase(), message, null));
    }
	
	@ExceptionHandler(CredencialesInvalidasException.class)
	public ResponseEntity<ErrorResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.UNAUTHORIZED.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(error);
	}
	
	@ExceptionHandler(PermisosInsuficientesException.class)
	public ResponseEntity<ErrorResponse> handlePermisosInsuficientes(PermisosInsuficientesException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.FORBIDDEN.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.FORBIDDEN)
				.body(error);
	}
	
	@ExceptionHandler(RolInvalidoException.class)
	public ResponseEntity<ErrorResponse> handleRolInvalido(RolInvalidoException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.BAD_REQUEST.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(error);
	}
	
	@ExceptionHandler(TokenExpiradoException.class)
	public ResponseEntity<ErrorResponse> handleTokenExpirado(TokenExpiradoException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.UNAUTHORIZED.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(error);
	}
	
	@ExceptionHandler(TokenInvalidoException.class)
	public ResponseEntity<ErrorResponse> handleTokenInvalido(TokenInvalidoException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.UNAUTHORIZED.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(error);
	}
	
	@ExceptionHandler(UsuarioNoEncontradoException.class)
	public ResponseEntity<ErrorResponse> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.NOT_FOUND.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(error);
	}
	
	@ExceptionHandler(UsuarioYaExistenteException.class)
	public ResponseEntity<ErrorResponse> handleUsuarioYaExistente(UsuarioYaExistenteException ex, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(
				OffsetDateTime.now(java.time.ZoneOffset.UTC),
				HttpStatus.CONFLICT.value(),
				"Credenciales Invalidas",
				ex.getMessage(),
				request.getRequestURI()
		);
		
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(error);
	}

}
