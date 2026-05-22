package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.TipoAtencionCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoAtencionCatalogRepository extends JpaRepository<TipoAtencionCatalog, Long> {
    boolean existsByCode(String code);
    List<TipoAtencionCatalog> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
