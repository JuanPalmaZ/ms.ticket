package cl.paris.marketplace.ms.ticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId; // ID de la orden de compra en disputa

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId; // Cliente que abre el ticket

    @Column(name = "vendedor_id", nullable = false)
    private UUID vendedorId; // Vendedor involucrado en la disputa

    @Column(nullable = false)
    private String asunto;

    @Column(nullable = false, length = 2000)
    private String mensajeInicial;

    @Column(nullable = false)
    private String estado; // ABIERTO, EN_PROCESO, RESUELTO, CERRADO

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "ABIERTO"; // Todo reclamo inicia en estado ABIERTO
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}