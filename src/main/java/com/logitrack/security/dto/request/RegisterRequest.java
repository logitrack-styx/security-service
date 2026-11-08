package com.logitrack.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Datos para registrar una nueva cuenta de usuario")
public class RegisterRequest {

	@NotBlank
	@Schema(
        description = "Nombre de usuario que se utilizará para identificarse",
        example = "nuevoUsuario",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @com.fasterxml.jackson.annotation.JsonAlias("nombre")
	private String username;

	@NotBlank
	@Email
	@Schema(
        description = "Dirección de correo electrónico válida del usuario",
        example = "usuario@ejemplo.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
	private String email;

	@NotBlank
	@Schema(
        description = "Contraseña para crear la cuenta",
        example = "MiPassword123!",
        format = "password",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
	private String password;

}
