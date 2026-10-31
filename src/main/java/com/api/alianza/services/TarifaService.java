package com.api.alianza.services;

import com.api.alianza.entities.Reserva;
import com.api.alianza.entities.Tarifa;
import com.api.alianza.repositories.ReservaRepository;
import com.api.alianza.repositories.TarifaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TarifaService {

    @Autowired
    private TarifaRepository tarifaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Tarifa crear(String idReserva, Tarifa tarifa) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            throw new IllegalArgumentException("El idReserva es obligatorio para registrar la tarifa.");
        }
        if (tarifa == null) {
            throw new IllegalArgumentException("Los datos de la tarifa no pueden ser nulos.");
        }
        if (tarifa.getImporte() == null || tarifa.getImporte().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El importe de la tarifa debe ser un valor no negativo.");
        }
        if (tarifa.getMoneda() == null || tarifa.getMoneda().trim().isEmpty()) {
            throw new IllegalArgumentException("La moneda de la tarifa es obligatoria (ej. MXN, USD).");
        }

        Optional<Reserva> optReserva = reservaRepository.findById(idReserva.trim());
        if (!optReserva.isPresent()) {
            throw new ResourceNotFoundException("No se encontró la reserva con ID: " + idReserva);
        }

        if (tarifaRepository.existsByReservaIdReserva(idReserva.trim())) {
            throw new ResourceConflictException("La reserva " + idReserva + " ya tiene una tarifa asignada (relación 1:1).");
        }

        Reserva reserva = optReserva.get();
        if (tarifa.getIdTarifa() == null || tarifa.getIdTarifa().trim().isEmpty()) {
            tarifa.setIdTarifa(UUID.randomUUID().toString());
        }
        tarifa.setReserva(reserva);

        return tarifaRepository.save(tarifa);
    }

    public List<Tarifa> listarTodas() {
        return tarifaRepository.findAll();
    }

    public Optional<Tarifa> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return tarifaRepository.findById(id);
    }

    public Optional<Tarifa> buscarPorIdReserva(String idReserva) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            return Optional.empty();
        }
        return tarifaRepository.findByReservaIdReserva(idReserva.trim());
    }

    public Tarifa actualizar(String id, Tarifa datosActualizados) {
        Optional<Tarifa> optTarifa = tarifaRepository.findById(id);
        if (!optTarifa.isPresent()) {
            throw new ResourceNotFoundException("No se encontró la tarifa con ID: " + id);
        }

        Tarifa tarifaExistente = optTarifa.get();

        if (datosActualizados.getImporte() != null) {
            if (datosActualizados.getImporte().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El importe de la tarifa debe ser no negativo.");
            }
            tarifaExistente.setImporte(datosActualizados.getImporte());
        }
        if (datosActualizados.getMoneda() != null && !datosActualizados.getMoneda().trim().isEmpty()) {
            tarifaExistente.setMoneda(datosActualizados.getMoneda().trim().toUpperCase());
        }

        return tarifaRepository.save(tarifaExistente);
    }

    public void eliminar(String id) {
        Optional<Tarifa> optTarifa = tarifaRepository.findById(id);
        if (!optTarifa.isPresent()) {
            throw new ResourceNotFoundException("No se encontró la tarifa con ID: " + id);
        }
        tarifaRepository.deleteById(id);
    }
}

