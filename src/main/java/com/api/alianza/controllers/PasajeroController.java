package com.api.alianza.controllers;

import com.api.alianza.entities.Pasajero;
import com.api.alianza.services.PasajeroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para consultas y operaciones de Pasajeros y Tutelas.
 */
@RestController
@RequestMapping(value = "/api/v1/pasajeros", produces = MediaType.APPLICATION_JSON_VALUE)
public class PasajeroController {

    @Autowired
    private PasajeroService pasajeroService;

    /**
     * GET /api/v1/pasajeros
     * Lista todos los pasajeros con su subtipo polimórfico discriminado.
     */
    @GetMapping
    public ResponseEntity<List<Pasajero>> listarTodos() {
        return ResponseEntity.ok(pasajeroService.listarTodos());
    }

    /**
     * GET /api/v1/pasajeros/{id}
     * Obtiene el pasajero por identificador con su subtipo polimórfico (Adulto, Menor o Infante).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pasajero> obtenerPasajero(@PathVariable("id") String id) {
        Optional<Pasajero> optPasajero = pasajeroService.buscarPorId(id);
        if (optPasajero.isPresent()) {
            return ResponseEntity.ok(optPasajero.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * GET /api/v1/pasajeros/{idTutor}/tutelados
     * Devuelve los menores e infantes supervisados por el tutor especificado.
     */
    @GetMapping("/{idTutor}/tutelados")
    public ResponseEntity<Map<String, Object>> listarTutelados(@PathVariable("idTutor") String idTutor) {
        Map<String, Object> tutelados = pasajeroService.listarTuteladosPorIdTutor(idTutor);
        return ResponseEntity.ok(tutelados);
    }

    /**
     * DELETE /api/v1/pasajeros/{id}
     * Elimina el pasajero si no posee reservas ni tutelados asociados.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        pasajeroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
