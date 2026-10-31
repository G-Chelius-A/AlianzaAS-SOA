package com.api.alianza.repositories;

import com.api.alianza.entities.Equipaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Equipaje.
 */
@Repository
public interface EquipajeRepository extends JpaRepository<Equipaje, String> {
    Optional<Equipaje> findByReservaIdReserva(String idReserva);
    boolean existsByReservaIdReserva(String idReserva);
}

