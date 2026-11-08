package com.logitrack.security.dto.response;

import java.util.UUID;

import com.logitrack.security.enums.RolUsuario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de un usuario registrado en LogiTrack")
public class UsuarioResponse {
    /** @return nombre contractual, conservando username para clientes existentes. */
    public String getNombre() { return username; }

	@Schema(
        description = "Identificador único del usuario",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID id;

    @Schema(
        description = "Nombre de usuario",
        example = "repartidor01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @Schema(
        description = "Correo electrónico del usuario",
        example = "repartidor@logitrack.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Schema(
        description = "Rol asignado al usuario dentro del sistema",
        example = "REPARTIDOR",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private RolUsuario role;

    @Schema(
        description = "Indica si la cuenta del usuario está habilitada",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean eneable;

}
