package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.CatalogItemResponse;
import com.tbtha.gespa_backend.services.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogs")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/tipos-atencion")
    public List<CatalogItemResponse> getTiposAtencion() {
        return catalogService.getTiposAtencion();
    }

    @GetMapping("/modalidades")
    public List<CatalogItemResponse> getModalidades() {
        return List.of(
            new CatalogItemResponse("PRESENCIAL", "Presencial"),
            new CatalogItemResponse("ONLINE", "Online")
        );
    }

    @GetMapping("/estados-cita")
    public List<CatalogItemResponse> getEstadosCita() {
        return List.of(
            new CatalogItemResponse("SCHEDULED", "Agendada"),
            new CatalogItemResponse("CONFIRMED", "Confirmada"),
            new CatalogItemResponse("COMPLETED", "Completada"),
            new CatalogItemResponse("CANCELLED", "Cancelada"),
            new CatalogItemResponse("NO_SHOW", "No asistió")
        );
    }

    @GetMapping("/previsiones")
    public List<CatalogItemResponse> getPrevisiones() {
        return List.of(
            new CatalogItemResponse("FONASA", "FONASA"),
            new CatalogItemResponse("ISAPRE", "ISAPRE"),
            new CatalogItemResponse("PARTICULAR", "Particular")
        );
    }

    @GetMapping("/estados-civiles")
    public List<CatalogItemResponse> getEstadosCiviles() {
        return List.of(
            new CatalogItemResponse("SOLTERO", "Soltero/a"),
            new CatalogItemResponse("CASADO", "Casado/a"),
            new CatalogItemResponse("CONVIVIENTE", "Conviviente"),
            new CatalogItemResponse("DIVORCIADO", "Divorciado/a"),
            new CatalogItemResponse("VIUDO", "Viudo/a")
        );
    }
}
