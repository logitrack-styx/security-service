package com.logitrack.security.exception;

import java.time.OffsetDateTime;

/**
 * Error público uniforme. No serializa rutas internas ni detalles de infraestructura.
 * @param timestamp instante UTC
 * @param status código HTTP
 * @param error frase HTTP estable
 * @param message explicación controlada, publicada como mensaje
 * @param path contexto interno omitido de JSON por compatibilidad de constructores
 * @author dominox45
 */
public record ErrorResponse(
		OffsetDateTime timestamp,
        int status,
        String error,
        @com.fasterxml.jackson.annotation.JsonProperty("mensaje") String message,
        @com.fasterxml.jackson.annotation.JsonIgnore String path
) {
    /** Normaliza la frase de error al código HTTP, sin repetir mensajes internos. */
    public ErrorResponse {
        error = org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase();
    }
}
