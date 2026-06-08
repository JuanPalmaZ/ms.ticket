package cl.paris.marketplace.ms.ticket.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.paris.marketplace.ms.ticket.dto.VentaResponse;

@FeignClient(
        name = "ms-venta", 
        configuration = FeignClientConfig.class
)
public interface VentaClient {

    @GetMapping("/api/ventas/{ventaId}")
    VentaResponse buscarPorId(@PathVariable("ventaId") UUID ventaId);
}