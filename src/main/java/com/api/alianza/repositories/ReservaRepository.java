package com.api.alianza.repositories;

import com.api.alianza.entities.AerolineaOrigen;
import com.api.alianza.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Reserva.
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, String> {

    boolean existsByAerolineaOrigenAndOrigenPNR(AerolineaOrigen aerolineaOrigen, String origenPNR);

    Optional<Reserva> findByAerolineaOrigenAndOrigenPNR(AerolineaOrigen aerolineaOrigen, String origenPNR);

    List<Reserva> findByAerolineaOrigen(AerolineaOrigen aerolineaOrigen);

    List<Reserva> findByPasajeroIdPasajero(String idPasajero);
}
