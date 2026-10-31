package com.api.alianza.repositories;

import com.api.alianza.entities.Pasajero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la entidad Pasajero (y sus subtipos JOINED).
 */
@Repository
public interface PasajeroRepository extends JpaRepository<Pasajero, String> {
}
