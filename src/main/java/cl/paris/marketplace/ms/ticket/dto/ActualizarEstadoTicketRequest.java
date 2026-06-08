package cl.paris.marketplace.ms.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarEstadoTicketRequest(
    @NotBlank(message = "El nuevo estado es obligatorio") String estado
) {}