package com.api.alianza.services;

import com.api.alianza.entities.Adulto;
import com.api.alianza.entities.Infante;
import com.api.alianza.entities.Menor;
import com.api.alianza.entities.Pasajero;
import com.api.alianza.repositories.AdultoRepository;
import com.api.alianza.repositories.InfanteRepository;
import com.api.alianza.repositories.MenorRepository;
import com.api.alianza.repositories.PasajeroRepository;
import com.api.alianza.repositories.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PasajeroService {

    @Autowired
    private PasajeroRepository pasajeroRepository;

    @Autowired
    private AdultoRepository adultoRepository;

    @Autowired
    private MenorRepository menorRepository;

    @Autowired
    private InfanteRepository infanteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public List<Pasajero> listarTodos() {
        return pasajeroRepository.findAll();
    }

    public Optional<Pasajero> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return pasajeroRepository.findById(id);
    }

    public Map<String, Object> listarTuteladosPorIdTutor(String idTutor) {
        if (idTutor == null || idTutor.trim().isEmpty()) {
            throw new IllegalArgumentException("El id del tutor no puede ser nulo o vacío.");
        }

        Optional<Pasajero> optPasajero = pasajeroRepository.findById(idTutor);
        if (!optPasajero.isPresent()) {
            throw new ResourceNotFoundException("No se encontró ningún pasajero con el ID: " + idTutor);
        }

        Optional<Adulto> optAdulto = adultoRepository.findById(idTutor);
        if (!optAdulto.isPresent()) {
            throw new BusinessRuleValidationException("El pasajero con ID '" + idTutor + "' no es un Adulto y no puede ejercer tutela.");
        }

        List<Menor> menores = menorRepository.findByIdTutor(idTutor);
        List<Infante> infantes = infanteRepository.findByIdTutor(idTutor);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("tutor", optAdulto.get());
        respuesta.put("menores", menores);
        respuesta.put("infantes", infantes);
        respuesta.put("totalTutelados", menores.size() + infantes.size());

        return respuesta;
    }

    public void eliminar(String id) {
        Optional<Pasajero> optPasajero = pasajeroRepository.findById(id);
        if (!optPasajero.isPresent()) {
            throw new ResourceNotFoundException("No se encontró el pasajero con ID: " + id);
        }

        if (!reservaRepository.findByPasajeroIdPasajero(id).isEmpty()) {
            throw new ResourceConflictException("No se puede eliminar el pasajero porque tiene reservas asociadas.");
        }

        if (adultoRepository.existsById(id)) {
            if (!menorRepository.findByIdTutor(id).isEmpty() || !infanteRepository.findByIdTutor(id).isEmpty()) {
                throw new ResourceConflictException("No se puede eliminar el pasajero porque es tutor de menores o infantes.");
            }
        }

        pasajeroRepository.deleteById(id);
    }
}

