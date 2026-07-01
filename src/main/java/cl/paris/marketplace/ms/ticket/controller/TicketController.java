package cl.paris.marketplace.ms.ticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.paris.marketplace.ms.ticket.dto.ActualizarEstadoTicketRequest;
import cl.paris.marketplace.ms.ticket.dto.TicketRequest;
import cl.paris.marketplace.ms.ticket.dto.TicketResponse;
import cl.paris.marketplace.ms.ticket.service.TicketService;
import jakarta.validation.Valid;

// Imports de la pauta OpenAPI
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Tickets", description = "Gestión de tickets de soporte y disputas en el marketplace")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Abrir un nuevo ticket", description = "Permite a un cliente autenticado abrir una disputa o ticket de soporte asociado a un pedido.")
    @ApiResponse(responseCode = "201", description = "Ticket creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Error de formato en el token o datos de entrada inválidos")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor o usuarioId no encontrado en el token")
    @RequestBody(description = "Detalles del ticket que se desea abrir")
    @ExampleObject(value = "{\n  \"pedidoId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n  \"asunto\": \"Producto dañado\",\n  \"mensajeInicial\": \"El artículo llegó con una fisura en la parte posterior.\"\n}")
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> abrirTicket(
            @Valid @org.springframework.web.bind.annotation.RequestBody TicketRequest request,
            Authentication authentication
    ) {
        try {
            if (authentication.getCredentials() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error Crítico: El usuarioId no se encontró en el token.");
            }

            String credencialesStr = authentication.getCredentials().toString();
            UUID clienteId;
            
            try {
                clienteId = UUID.fromString(credencialesStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error de Formato: El Token fue leído, pero el ID adentro no es un UUID válido.");
            }
            
            TicketResponse response = ticketService.abrirTicket(request, clienteId);
            return new ResponseEntity<>(response, HttpStatus.CREATED); 

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error procesando el ticket: " + e.getMessage());
        }
    }

    @Operation(summary = "Cambiar estado del ticket", description = "Permite a los roles ADMIN o PROVEEDOR actualizar el estado actual de un ticket por su ID.")
    @ApiResponse(responseCode = "200", description = "Estado del ticket actualizado exitosamente")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')") 
    public ResponseEntity<TicketResponse> cambiarEstado(
            @PathVariable UUID id, 
            @Valid @org.springframework.web.bind.annotation.RequestBody ActualizarEstadoTicketRequest request) {
        TicketResponse response = ticketService.cambiarEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todos los tickets", description = "Permite al rol ADMIN obtener una lista global con la totalidad de los tickets del sistema.")
    @ApiResponse(responseCode = "200", description = "Lista completa de tickets obtenida exitosamente")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponse>> listarTodos() {
        return ResponseEntity.ok(ticketService.listarTodosLosTickets());
    }

    @Operation(summary = "Listar tickets por cliente", description = "Permite obtener todos los tickets de un cliente específico. Protegido para ADMIN o el propio dueño del token.")
    @ApiResponse(responseCode = "200", description = "Lista de tickets del cliente obtenida exitosamente")
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and #clienteId.toString() == authentication.credentials)")
    public ResponseEntity<List<TicketResponse>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorCliente(clienteId));
    }

    @Operation(summary = "Listar tickets por vendedor", description = "Permite obtener todos los tickets asociados a un vendedor/proveedor específico. Protegido para ADMIN o el propio proveedor.")
    @ApiResponse(responseCode = "200", description = "Lista de tickets del vendedor obtenida exitosamente")
    @GetMapping("/vendedor/{vendedorId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROVEEDOR') and #vendedorId.toString() == authentication.credentials)")
    public ResponseEntity<List<TicketResponse>> listarPorVendedor(@PathVariable UUID vendedorId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorVendedor(vendedorId));
    }
}