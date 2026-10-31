package com.api.alianza.services;

import com.api.alianza.entities.Menor;
import com.api.alianza.repositories.AdultoRepository;
import com.api.alianza.repositories.MenorRepository;
import com.api.alianza.repositories.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MenorService {

    @Autowired
    private MenorRepository menorRepository;

    @Autowired
    private AdultoRepository adultoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Menor crear(Menor menor) {
        if (menor == null) {
            throw new IllegalArgumentException("Los datos del menor no pueden ser nulos.");
        }
        if (menor.getNombres() == null || menor.getNombres().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del menor es obligatorio.");
        }
        if (menor.getApellidos() == null || menor.getApellidos().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido del menor es obligatorio.");
        }
        if (menor.getIdTutor() == null || menor.getIdTutor().trim().isEmpty()) {
            throw new IllegalArgumentException("El menor requiere obligatoriamente un idTutor.");
        }
        if (!adultoRepository.existsById(menor.getIdTutor().trim())) {
            throw new ResourceNotFoundException("El tutor especificado no existe o no es un adulto registrado: " + menor.getIdTutor());
        }

        if (menor.getIdPasajero() == null || menor.getIdPasajero().trim().isEmpty()) {
            menor.setIdPasajero(UUID.randomUUID().toString());
        }
        if (menor.getRequiereAcompanante() == null) {
            menor.setRequiereAcompanante(false);
        }
        if (menor.getAutorizacionViaje() == null) {
            menor.setAutorizacionViaje(false);
        }

        return menorRepository.save(menor);
    }

    public List<Menor> listarTodos() {
        return menorRepository.findAll();
    }

    public Optional<Menor> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return menorRepository.findById(id);
    }

    public List<Menor> listarPorTutor(String idTutor) {
        if (idTutor == null || idTutor.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return menorRepository.findByIdTutor(idTutor);
    }

    public Menor actualizar(String id, Menor datosActualizados) {
        Optional<Menor> optMenor = menorRepository.findById(id);
        if (!optMenor.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el menor con ID: " + id);
        }

        Menor menorExistente = optMenor.get();

        if (datosActualizados.getNombres() != null && !datosActualizados.getNombres().trim().isEmpty()) {
            menorExistente.setNombres(datosActualizados.getNombres());
        }
        if (datosActualizados.getApellidos() != null && !datosActualizados.getApellidos().trim().isEmpty()) {
            menorExistente.setApellidos(datosActualizados.getApellidos());
        }
        if (datosActualizados.getNacionalidad() != null) {
            menorExistente.setNacionalidad(datosActualizados.getNacionalidad());
        }
        if (datosActualizados.getFechaNacimiento() != null) {
            menorExistente.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        }
        if (datosActualizados.getIdTutor() != null && !datosActualizados.getIdTutor().trim().isEmpty()) {
            String nuevoTutor = datosActualizados.getIdTutor().trim();
            if (!adultoRepository.existsById(nuevoTutor)) {
                throw new ResourceNotFoundException("El nuevo tutor especificado no existe como adulto: " + nuevoTutor);
            }
            menorExistente.setIdTutor(nuevoTutor);
        }
        if (datosActualizados.getRequiereAcompanante() != null) {
            menorExistente.setRequiereAcompanante(datosActualizados.getRequiereAcompanante());
        }
        if (datosActualizados.getAutorizacionViaje() != null) {
            menorExistente.setAutorizacionViaje(datosActualizados.getAutorizacionViaje());
        }

        return menorRepository.save(menorExistente);
    }

    public void eliminar(String id) {
        Optional<Menor> optMenor = menorRepository.findById(id);
        if (!optMenor.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el menor con ID: " + id);
        }

        // Validación de integridad: no eliminar si tiene reservas
        if (!reservaRepository.findByPasajeroIdPasajero(id).isEmpty()) {
            throw new ResourceConflictException("No se puede eliminar el menor porque tiene reservas asociadas.");
        }

        menorRepository.deleteById(id);
    }
}

