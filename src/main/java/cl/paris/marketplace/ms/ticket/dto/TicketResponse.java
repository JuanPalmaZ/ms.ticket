package cl.paris.marketplace.ms.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    UUID pedidoId,
    UUID clienteId,
    UUID vendedorId,
    String asunto,
    String mensajeInicial,
    String estado,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion
) {}