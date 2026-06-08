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

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
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
    public ResponseEntity<TicketResponse> cambiarEstado(
            @PathVariable UUID id, 
            @Valid @RequestBody ActualizarEstadoTicketRequest request) {
        TicketResponse response = ticketService.cambiarEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponse>> listarTodos() {
        return ResponseEntity.ok(ticketService.listarTodosLosTickets());
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENTE') and #clienteId.toString() == authentication.credentials)")
    public ResponseEntity<List<TicketResponse>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorCliente(clienteId));
    }

    @GetMapping("/vendedor/{vendedorId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROVEEDOR') and #vendedorId.toString() == authentication.credentials)")
    public ResponseEntity<List<TicketResponse>> listarPorVendedor(@PathVariable UUID vendedorId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorVendedor(vendedorId));
    }
}