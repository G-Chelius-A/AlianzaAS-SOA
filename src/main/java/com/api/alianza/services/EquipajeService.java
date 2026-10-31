package com.api.alianza.services;

import com.api.alianza.entities.Equipaje;
import com.api.alianza.entities.Reserva;
import com.api.alianza.repositories.EquipajeRepository;
import com.api.alianza.repositories.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EquipajeService {

    @Autowired
    private EquipajeRepository equipajeRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Equipaje crear(String idReserva, Equipaje equipaje) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            throw new IllegalArgumentException("El idReserva es obligatorio para registrar equipaje.");
        }
        if (equipaje == null) {
            throw new IllegalArgumentException("Los datos del equipaje no pueden ser nulos.");
        }

        Optional<Reserva> optReserva = reservaRepository.findById(idReserva.trim());
        if (!optReserva.isPresent()) {
            throw new ResourceNotFoundException("No se encontró la reserva con ID: " + idReserva);
        }

        if (equipajeRepository.existsByReservaIdReserva(idReserva.trim())) {
            throw new ResourceConflictException("La reserva " + idReserva + " ya tiene un registro de equipaje (relación 0..1 : 1).");
        }

        Reserva reserva = optReserva.get();
        if (equipaje.getIdEquipaje() == null || equipaje.getIdEquipaje().trim().isEmpty()) {
            equipaje.setIdEquipaje(UUID.randomUUID().toString());
        }
        equipaje.setReserva(reserva);

        return equipajeRepository.save(equipaje);
    }

    public List<Equipaje> listarTodos() {
        return equipajeRepository.findAll();
    }

    public Optional<Equipaje> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return equipajeRepository.findById(id);
    }

    public Optional<Equipaje> buscarPorIdReserva(String idReserva) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            return Optional.empty();
        }
        return equipajeRepository.findByReservaIdReserva(idReserva.trim());
    }

    public Equipaje actualizar(String id, Equipaje datosActualizados) {
        Optional<Equipaje> optEquipaje = equipajeRepository.findById(id);
        if (!optEquipaje.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el equipaje con ID: " + id);
        }

        Equipaje equipajeExistente = optEquipaje.get();

        if (datosActualizados.getCantidadPiezas() != null) {
            equipajeExistente.setCantidadPiezas(datosActualizados.getCantidadPiezas());
        }
        if (datosActualizados.getPeso() != null) {
            equipajeExistente.setPeso(datosActualizados.getPeso());
        }
        if (datosActualizados.getUnidadPeso() != null) {
            equipajeExistente.setUnidadPeso(datosActualizados.getUnidadPeso());
        }

        return equipajeRepository.save(equipajeExistente);
    }

    public void eliminar(String id) {
        Optional<Equipaje> optEquipaje = equipajeRepository.findById(id);
        if (!optEquipaje.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el equipaje con ID: " + id);
        }
        equipajeRepository.deleteById(id);
    }
}

