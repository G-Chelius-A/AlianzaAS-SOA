package com.api.alianza.services;

import com.api.alianza.entities.Adulto;
import com.api.alianza.repositories.AdultoRepository;
import com.api.alianza.repositories.InfanteRepository;
import com.api.alianza.repositories.MenorRepository;
import com.api.alianza.repositories.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdultoService {

    @Autowired
    private AdultoRepository adultoRepository;

    @Autowired
    private MenorRepository menorRepository;

    @Autowired
    private InfanteRepository infanteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Adulto crear(Adulto adulto) {
        if (adulto == null) {
            throw new IllegalArgumentException("Los datos del adulto no pueden ser nulos.");
        }
        if (adulto.getNombres() == null || adulto.getNombres().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del adulto es obligatorio.");
        }
        if (adulto.getApellidos() == null || adulto.getApellidos().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido del adulto es obligatorio.");
        }
        if (adulto.getDocumentoIdentidad() != null && !adulto.getDocumentoIdentidad().trim().isEmpty()) {
            if (adultoRepository.existsByDocumentoIdentidad(adulto.getDocumentoIdentidad().trim())) {
                throw new ResourceConflictException("Ya existe un adulto registrado con el documento: " + adulto.getDocumentoIdentidad());
            }
        }
        if (adulto.getIdPasajero() == null || adulto.getIdPasajero().trim().isEmpty()) {
            adulto.setIdPasajero(UUID.randomUUID().toString());
        }
        return adultoRepository.save(adulto);
    }

    public List<Adulto> listarTodos() {
        return adultoRepository.findAll();
    }

    public Optional<Adulto> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return adultoRepository.findById(id);
    }

    public Adulto actualizar(String id, Adulto datosActualizados) {
        Optional<Adulto> optAdulto = adultoRepository.findById(id);
        if (!optAdulto.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el adulto con ID: " + id);
        }

        Adulto adultoExistente = optAdulto.get();

        if (datosActualizados.getNombres() != null && !datosActualizados.getNombres().trim().isEmpty()) {
            adultoExistente.setNombres(datosActualizados.getNombres());
        }
        if (datosActualizados.getApellidos() != null && !datosActualizados.getApellidos().trim().isEmpty()) {
            adultoExistente.setApellidos(datosActualizados.getApellidos());
        }
        if (datosActualizados.getNacionalidad() != null) {
            adultoExistente.setNacionalidad(datosActualizados.getNacionalidad());
        }
        if (datosActualizados.getFechaNacimiento() != null) {
            adultoExistente.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        }
        if (datosActualizados.getTelefonoContacto() != null) {
            adultoExistente.setTelefonoContacto(datosActualizados.getTelefonoContacto());
        }
        if (datosActualizados.getDocumentoIdentidad() != null) {
            String nuevoDoc = datosActualizados.getDocumentoIdentidad().trim();
            if (!nuevoDoc.equalsIgnoreCase(adultoExistente.getDocumentoIdentidad())) {
                if (adultoRepository.existsByDocumentoIdentidad(nuevoDoc)) {
                    throw new ResourceConflictException("Ya existe otro adulto con el documento: " + nuevoDoc);
                }
                adultoExistente.setDocumentoIdentidad(nuevoDoc);
            }
        }

        return adultoRepository.save(adultoExistente);
    }

    public void eliminar(String id) {
        Optional<Adulto> optAdulto = adultoRepository.findById(id);
        if (!optAdulto.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el adulto con ID: " + id);
        }

        // Validación de integridad: no eliminar si tiene reservas
        if (!reservaRepository.findByPasajeroIdPasajero(id).isEmpty()) {
            throw new ResourceConflictException("No se puede eliminar el adulto porque tiene reservas asociadas.");
        }

        // Validación de integridad: no eliminar si tiene menores o infantes a su cargo
        if (!menorRepository.findByIdTutor(id).isEmpty() || !infanteRepository.findByIdTutor(id).isEmpty()) {
            throw new ResourceConflictException("No se puede eliminar el adulto porque funge como tutor de menores o infantes.");
        }

        adultoRepository.deleteById(id);
    }
}

