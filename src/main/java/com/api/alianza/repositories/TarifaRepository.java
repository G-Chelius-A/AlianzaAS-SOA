package com.api.alianza.repositories;

import com.api.alianza.entities.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Tarifa.
 */
@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, String> {
    Optional<Tarifa> findByReservaIdReserva(String idReserva);
    boolean existsByReservaIdReserva(String idReserva);
}

