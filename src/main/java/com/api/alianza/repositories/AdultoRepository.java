package com.api.alianza.repositories;

import com.api.alianza.entities.Adulto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA específico para la entidad Adulto.
 */
@Repository
public interface AdultoRepository extends JpaRepository<Adulto, String> {
    Optional<Adulto> findByDocumentoIdentidad(String documentoIdentidad);
    boolean existsByDocumentoIdentidad(String documentoIdentidad);
}

