package com.api.alianza.controllers;

import com.api.alianza.entities.Menor;
import com.api.alianza.services.MenorService;
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
@RequestMapping(value = "/api/v1/menores", produces = MediaType.APPLICATION_JSON_VALUE)
public class MenorController {

    @Autowired
    private MenorService menorService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Menor> crear(@RequestBody Menor menor) {
        Menor nuevo = menorService.crear(menor);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<Menor>> listar(@RequestParam(name = "idTutor", required = false) String idTutor) {
        if (idTutor != null && !idTutor.trim().isEmpty()) {
            return ResponseEntity.ok(menorService.listarPorTutor(idTutor));
        }
        return ResponseEntity.ok(menorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menor> buscarPorId(@PathVariable("id") String id) {
        Optional<Menor> optMenor = menorService.buscarPorId(id);
        if (optMenor.isPresent()) {
            return ResponseEntity.ok(optMenor.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Menor> actualizar(@PathVariable("id") String id, @RequestBody Menor menor) {
        Menor actualizado = menorService.actualizar(id, menor);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        menorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

