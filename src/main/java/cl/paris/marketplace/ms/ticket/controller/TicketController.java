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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.paris.marketplace.ms.ticket.dto.ActualizarEstadoTicketRequest;
import cl.paris.marketplace.ms.ticket.dto.TicketRequest;
import cl.paris.marketplace.ms.ticket.dto.TicketResponse;
import cl.paris.marketplace.ms.ticket.service.TicketService;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Operaciones de gestión y consulta de tickets de disputa")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Abrir un nuevo ticket")
    @ApiResponse(responseCode = "201", description = "Ticket creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Error de validación o formato incorrecto")
    @ApiResponse(responseCode = "500", description = "Error crítico interno")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = "{\n  \"pedidoId\": \"123e4567-e89b-12d3-a456-426614174000\",\n  \"asunto\": \"Producto defectuoso\",\n  \"mensajeInicial\": \"El producto llegó roto en una esquina y solicito reemplazo.\"\n}"
            )
        )
    )
    public ResponseEntity<?> abrirTicket(
            @Valid @RequestBody TicketRequest request,
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

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')") 
    @Operation(summary = "Cambiar el estado de un ticket")
    @ApiResponse(responseCode = "200", description = "Estado del ticket actualizado exitosamente")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = "{\n  \"estado\": \"EN_REVISION\"\n}"
            )
        )
    )
    public ResponseEntity<TicketResponse> cambiarEstado(
            @PathVariable UUID id, 
            @Valid @RequestBody ActualizarEstadoTicketRequest request) {
        TicketResponse response = ticketService.cambiarEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los tickets del sistema")
    @ApiResponse(responseCode = "200", description = "Lista de tickets obtenida correctamente")
    public ResponseEntity<List<TicketResponse>> listarTodos() {
        return ResponseEntity.ok(ticketService.listarTodosLosTickets());
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and #clienteId.toString() == authentication.credentials)")
    @Operation(summary = "Listar tickets por ID de cliente")
    @ApiResponse(responseCode = "200", description = "Lista de tickets asociada al cliente obtenida correctamente")
    public ResponseEntity<List<TicketResponse>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorCliente(clienteId));
    }

    @GetMapping("/vendedor/{vendedorId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROVEEDOR') and #vendedorId.toString() == authentication.credentials)")
    @Operation(summary = "Listar tickets por ID de vendedor")
    @ApiResponse(responseCode = "200", description = "Lista de tickets asociada al vendedor obtenida correctamente")
    public ResponseEntity<List<TicketResponse>> listarPorVendedor(@PathVariable UUID vendedorId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorVendedor(vendedorId));
    }
}
