package cl.paris.marketplace.ms.ticket.dto;

import java.util.List;
import java.util.UUID;

public record VentaResponse(
        UUID idVenta,
        List<DetalleVentaDTO> detalles
) {
    public record DetalleVentaDTO(
            UUID proveedorId
    ) {}
}