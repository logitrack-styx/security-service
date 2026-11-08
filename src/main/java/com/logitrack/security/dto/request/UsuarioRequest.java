package com.logitrack.security.dto.request;

import com.logitrack.security.enums.RolUsuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Datos necesarios para crear un usuario desde la administración")
public class UsuarioRequest {

	@NotBlank
	@Schema(
        description = "Nombre de usuario",
        example = "repartidor01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @com.fasterxml.jackson.annotation.JsonAlias("nombre")
	private String username;

	@NotBlank
	@Email
	@Schema(
        description = "Correo electrónico del usuario",
        example = "repartidor@logitrack.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
	private String email;

	@NotBlank
	@Schema(
        description = "Contraseña inicial del usuario",
        example = "ClaveSegura123!",
        format = "password",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
	private String password;

	@NotNull
	@Schema(
        description = "Rol que tendrá el usuario",
        example = "REPARTIDOR",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
	private RolUsuario role;

}
