package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CatalogItemResponse;
import com.tbtha.gespa_backend.entities.TipoAtencionCatalog;
import com.tbtha.gespa_backend.repositories.TipoAtencionCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final TipoAtencionCatalogRepository tipoAtencionCatalogRepository;

    public CatalogService(TipoAtencionCatalogRepository tipoAtencionCatalogRepository) {
        this.tipoAtencionCatalogRepository = tipoAtencionCatalogRepository;
    }

    @Transactional
    public List<CatalogItemResponse> getTiposAtencion() {
        bootstrapDefaultTiposAtencion();
        return tipoAtencionCatalogRepository.findByActiveTrueOrderBySortOrderAscLabelAsc().stream()
                .map(item -> new CatalogItemResponse(item.getCode(), item.getLabel()))
                .toList();
    }

    private void bootstrapDefaultTiposAtencion() {
        ensureType("CONTROL", "Control", 10);
        ensureType("PRIMERA_CONSULTA", "Primera consulta", 20);
        ensureType("SEGUIMIENTO", "Seguimiento", 30);
        ensureType("URGENCIA", "Urgencia", 40);
    }

    private void ensureType(String code, String label, int sortOrder) {
        if (tipoAtencionCatalogRepository.existsByCode(code)) {
            return;
        }
        TipoAtencionCatalog item = new TipoAtencionCatalog();
        item.setCode(code);
        item.setLabel(label);
        item.setSortOrder(sortOrder);
        item.setActive(true);
        tipoAtencionCatalogRepository.save(item);
    }
}
