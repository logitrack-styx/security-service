package com.logitrack.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Datos necesarios para iniciar sesión")
public class LoginRequest {

	@Schema(
        description = "Nombre de usuario registrado en el sistema",
        example = "usuario123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
	private String username;

    @jakarta.validation.constraints.Email
    @Schema(description = "Email de login según contrato Sprint 2; alternativo a username")
    private String email;

    /** @return true si se indicó exactamente una identidad de login. */
    @jakarta.validation.constraints.AssertTrue(message = "Indique username o email, exclusivamente uno")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isIdentityValid() {
        return (username != null && !username.isBlank()) ^ (email != null && !email.isBlank());
    }

	@NotBlank
	@Schema(
        description = "Contraseña del usuario",
        example = "MiPassword123!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "password"
    )
	private String password;

}
