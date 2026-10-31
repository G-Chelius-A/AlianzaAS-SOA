package com.api.alianza.controllers;

import com.api.alianza.entities.Infante;
import com.api.alianza.services.InfanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1/infantes", produces = MediaType.APPLICATION_JSON_VALUE)
public class InfanteController {

    @Autowired
    private InfanteService infanteService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Infante> crear(@RequestBody Infante infante) {
        Infante nuevo = infanteService.crear(infante);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<Infante>> listar(@RequestParam(name = "idTutor", required = false) String idTutor) {
        if (idTutor != null && !idTutor.trim().isEmpty()) {
            return ResponseEntity.ok(infanteService.listarPorTutor(idTutor));
        }
        return ResponseEntity.ok(infanteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Infante> buscarPorId(@PathVariable("id") String id) {
        Optional<Infante> optInfante = infanteService.buscarPorId(id);
        if (optInfante.isPresent()) {
            return ResponseEntity.ok(optInfante.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Infante> actualizar(@PathVariable("id") String id, @RequestBody Infante infante) {
        Infante actualizado = infanteService.actualizar(id, infante);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        infanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

