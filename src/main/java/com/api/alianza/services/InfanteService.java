package com.api.alianza.services;

import com.api.alianza.entities.Infante;
import com.api.alianza.repositories.AdultoRepository;
import com.api.alianza.repositories.InfanteRepository;
import com.api.alianza.repositories.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InfanteService {

    @Autowired
    private InfanteRepository infanteRepository;

    @Autowired
    private AdultoRepository adultoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Infante crear(Infante infante) {
        if (infante == null) {
            throw new IllegalArgumentException("Los datos del infante no pueden ser nulos.");
        }
        if (infante.getNombres() == null || infante.getNombres().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del infante es obligatorio.");
        }
        if (infante.getApellidos() == null || infante.getApellidos().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido del infante es obligatorio.");
        }
        if (infante.getIdTutor() == null || infante.getIdTutor().trim().isEmpty()) {
            throw new IllegalArgumentException("El infante requiere obligatoriamente un idTutor.");
        }
        if (!adultoRepository.existsById(infante.getIdTutor().trim())) {
            throw new ResourceNotFoundException("El tutor especificado no existe o no es un adulto registrado: " + infante.getIdTutor());
        }

        if (infante.getIdPasajero() == null || infante.getIdPasajero().trim().isEmpty()) {
            infante.setIdPasajero(UUID.randomUUID().toString());
        }
        if (infante.getRequiereAsiento() == null) {
            infante.setRequiereAsiento(false);
        }
        if (infante.getRequiereServicioEspecial() == null) {
            infante.setRequiereServicioEspecial(false);
        }

        return infanteRepository.save(infante);
    }

    public List<Infante> listarTodos() {
        return infanteRepository.findAll();
    }

    public Optional<Infante> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return infanteRepository.findById(id);
    }

    public List<Infante> listarPorTutor(String idTutor) {
        if (idTutor == null || idTutor.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return infanteRepository.findByIdTutor(idTutor);
    }

    public Infante actualizar(String id, Infante datosActualizados) {
        Optional<Infante> optInfante = infanteRepository.findById(id);
        if (!optInfante.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el infante con ID: " + id);
        }

        Infante infanteExistente = optInfante.get();

        if (datosActualizados.getNombres() != null && !datosActualizados.getNombres().trim().isEmpty()) {
            infanteExistente.setNombres(datosActualizados.getNombres());
        }
        if (datosActualizados.getApellidos() != null && !datosActualizados.getApellidos().trim().isEmpty()) {
            infanteExistente.setApellidos(datosActualizados.getApellidos());
        }
        if (datosActualizados.getNacionalidad() != null) {
            infanteExistente.setNacionalidad(datosActualizados.getNacionalidad());
        }
        if (datosActualizados.getFechaNacimiento() != null) {
            infanteExistente.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        }
        if (datosActualizados.getIdTutor() != null && !datosActualizados.getIdTutor().trim().isEmpty()) {
            String nuevoTutor = datosActualizados.getIdTutor().trim();
            if (!adultoRepository.existsById(nuevoTutor)) {
                throw new ResourceNotFoundException("El nuevo tutor especificado no existe como adulto: " + nuevoTutor);
            }
            infanteExistente.setIdTutor(nuevoTutor);
        }
        if (datosActualizados.getRequiereAsiento() != null) {
            infanteExistente.setRequiereAsiento(datosActualizados.getRequiereAsiento());
        }
        if (datosActualizados.getRequiereServicioEspecial() != null) {
            infanteExistente.setRequiereServicioEspecial(datosActualizados.getRequiereServicioEspecial());
        }

        return infanteRepository.save(infanteExistente);
    }

    public void eliminar(String id) {
        Optional<Infante> optInfante = infanteRepository.findById(id);
        if (!optInfante.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el infante con ID: " + id);
        }

        // Validación de integridad: no eliminar si tiene reservas
        if (!reservaRepository.findByPasajeroIdPasajero(id).isEmpty()) {
            throw new ResourceConflictException("No se puede eliminar el infante porque tiene reservas asociadas.");
        }

        infanteRepository.deleteById(id);
    }
}

