package cl.paris.marketplace.ms.ticket.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.paris.marketplace.ms.ticket.client.VentaClient;
import cl.paris.marketplace.ms.ticket.dto.ActualizarEstadoTicketRequest;
import cl.paris.marketplace.ms.ticket.dto.TicketRequest;
import cl.paris.marketplace.ms.ticket.dto.TicketResponse;
import cl.paris.marketplace.ms.ticket.dto.VentaResponse;
import cl.paris.marketplace.ms.ticket.mapper.TicketMapper;
import cl.paris.marketplace.ms.ticket.model.Ticket;
import cl.paris.marketplace.ms.ticket.repository.TicketRepository;
import feign.FeignException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final VentaClient ventaClient; 

    public TicketService(
            TicketRepository ticketRepository, 
            TicketMapper ticketMapper,
            VentaClient ventaClient) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.ventaClient = ventaClient;
    }

    // ==========================================
    // LÓGICA DE NEGOCIO: APERTURA DE DISPUTAS
    // ==========================================
    
    @Transactional
    public TicketResponse abrirTicket(TicketRequest request, UUID clienteIdFidedigno) {
        if (request.pedidoId() == null) {
            throw new RuntimeException("Debe asociar obligatoriamente un pedido para abrir una disputa.");
        }

        if (request.asunto() == null || request.asunto().trim().isEmpty()) {
            throw new RuntimeException("El asunto de la disputa no puede estar vacío.");
        }

        // ======================================================
        // PUENTE INTERNO: Validar pedido y AUTODESCUBRIR al Vendedor
        // ======================================================
        VentaResponse ventaInfo;
        try {
            ventaInfo = ventaClient.buscarPorId(request.pedidoId());
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("Error: El pedido especificado (" + request.pedidoId() + ") no existe en el sistema de ventas.");
        } catch (Exception e) {
            throw new RuntimeException("Error de comunicación al intentar validar el pedido con el servicio de ventas.");
        }

        // Extraemos el vendedor directamente de la boleta.
        if (ventaInfo.detalles() == null || ventaInfo.detalles().isEmpty()) {
            throw new RuntimeException("Error Crítico: El pedido no tiene detalles válidos.");
        }
        UUID vendedorAutodescubierto = ventaInfo.detalles().get(0).proveedorId();

        Ticket ticket = ticketMapper.toEntity(request, clienteIdFidedigno);
        ticket.setVendedorId(vendedorAutodescubierto); // Inyectamos el ID que el sistema descubrió solo
        
        Ticket ticketGuardado = ticketRepository.save(ticket);
        
        return ticketMapper.toResponse(ticketGuardado);
    }

    // ==========================================
    // LÓGICA DE NEGOCIO: RESOLUCIÓN DE DISPUTAS
    // ==========================================
    
    @Transactional
    public TicketResponse cambiarEstado(UUID ticketId, ActualizarEstadoTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("El ticket con el ID provisto no existe en la base de datos."));

        String nuevoEstado = request.estado().toUpperCase();
        if (!nuevoEstado.equals("ABIERTO") && !nuevoEstado.equals("EN_PROCESO") && 
            !nuevoEstado.equals("RESUELTO") && !nuevoEstado.equals("CERRADO")) {
            throw new RuntimeException("Estado inválido. Los estados válidos son: ABIERTO, EN_PROCESO, RESUELTO o CERRADO.");
        }

        ticket.setEstado(nuevoEstado);
        Ticket ticketActualizado = ticketRepository.save(ticket);
        
        return ticketMapper.toResponse(ticketActualizado);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listarTodosLosTickets() {
        return ticketRepository.findAll().stream()
                .map(ticketMapper::toResponse)
                .toList(); 
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsPorPedido(UUID pedidoId) {
        List<Ticket> tickets = ticketRepository.findByPedidoIdOrderByFechaCreacionDesc(pedidoId);
        if (tickets.isEmpty()) {
            throw new RuntimeException("No se encontraron tickets asociados al pedido especificado.");
        }
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsPorVendedor(UUID vendedorId) {
        List<Ticket> tickets = ticketRepository.findByVendedorIdOrderByFechaCreacionDesc(vendedorId);
        if (tickets.isEmpty()) {
            throw new RuntimeException("No se encontraron reclamos registrados para el vendedor especificado.");
        }
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsPorCliente(UUID clienteId) {
        List<Ticket> tickets = ticketRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId);
        if (tickets.isEmpty()) {
            throw new RuntimeException("El cliente especificado no registra tickets creados.");
        }
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }
}