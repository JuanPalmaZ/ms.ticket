package cl.paris.marketplace.ms.ticket.repository;

import cl.paris.marketplace.ms.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // Permite al cliente ver sus disputas activas o pasadas
    List<Ticket> findByClienteIdOrderByFechaCreacionDesc(UUID clienteId);

    // Permite a un vendedor ver los reclamos que le han abierto los usuarios
    List<Ticket> findByVendedorIdOrderByFechaCreacionDesc(UUID vendedorId);

    // Permite buscar disputas directas vinculadas a una transacción
    List<Ticket> findByPedidoIdOrderByFechaCreacionDesc(UUID pedidoId);
}