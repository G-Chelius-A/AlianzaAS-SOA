package com.api.alianza.repositories;

import com.api.alianza.entities.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad Vuelo.
 */
@Repository
public interface VueloRepository extends JpaRepository<Vuelo, String> {
    List<Vuelo> findByReservaIdReserva(String idReserva);
}

