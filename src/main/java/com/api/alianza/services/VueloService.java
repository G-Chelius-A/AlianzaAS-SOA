package com.api.alianza.services;

import com.api.alianza.entities.Reserva;
import com.api.alianza.entities.Vuelo;
import com.api.alianza.repositories.ReservaRepository;
import com.api.alianza.repositories.VueloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VueloService {

    @Autowired
    private VueloRepository vueloRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Vuelo crear(String idReserva, Vuelo vuelo) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            throw new IllegalArgumentException("El idReserva es obligatorio para registrar un vuelo.");
        }
        if (vuelo == null) {
            throw new IllegalArgumentException("Los datos del vuelo no pueden ser nulos.");
        }
        if (vuelo.getFechaHoraSalida() == null) {
            throw new IllegalArgumentException("La fecha y hora de salida del vuelo es obligatoria.");
        }

        Optional<Reserva> optReserva = reservaRepository.findById(idReserva.trim());
        if (!optReserva.isPresent()) {
            throw new ResourceNotFoundException("No se encontró la reserva con ID: " + idReserva);
        }

        Reserva reserva = optReserva.get();
        if (vuelo.getIdVuelo() == null || vuelo.getIdVuelo().trim().isEmpty()) {
            vuelo.setIdVuelo(UUID.randomUUID().toString());
        }
        if (vuelo.getCompletado() == null) {
            vuelo.setCompletado(false);
        }
        vuelo.setReserva(reserva);

        return vueloRepository.save(vuelo);
    }

    public List<Vuelo> listarTodos() {
        return vueloRepository.findAll();
    }

    public Optional<Vuelo> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return vueloRepository.findById(id);
    }

    public List<Vuelo> listarPorReserva(String idReserva) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return vueloRepository.findByReservaIdReserva(idReserva.trim());
    }

    public Vuelo actualizar(String id, Vuelo datosActualizados) {
        Optional<Vuelo> optVuelo = vueloRepository.findById(id);
        if (!optVuelo.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el vuelo con ID: " + id);
        }

        Vuelo vueloExistente = optVuelo.get();

        if (datosActualizados.getFechaHoraSalida() != null) {
            vueloExistente.setFechaHoraSalida(datosActualizados.getFechaHoraSalida());
        }
        if (datosActualizados.getFechaHoraLlegada() != null) {
            vueloExistente.setFechaHoraLlegada(datosActualizados.getFechaHoraLlegada());
        }
        if (datosActualizados.getOrigen() != null) {
            vueloExistente.setOrigen(datosActualizados.getOrigen());
        }
        if (datosActualizados.getDestino() != null) {
            vueloExistente.setDestino(datosActualizados.getDestino());
        }
        if (datosActualizados.getCompletado() != null) {
            vueloExistente.setCompletado(datosActualizados.getCompletado());
        }

        return vueloRepository.save(vueloExistente);
    }

    public void eliminar(String id) {
        Optional<Vuelo> optVuelo = vueloRepository.findById(id);
        if (!optVuelo.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el vuelo con ID: " + id);
        }
        vueloRepository.deleteById(id);
    }
}

