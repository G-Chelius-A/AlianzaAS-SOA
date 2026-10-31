package com.api.alianza.controllers;

import com.api.alianza.entities.Adulto;
import com.api.alianza.services.AdultoService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1/adultos", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdultoController {

    @Autowired
    private AdultoService adultoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Adulto> crear(@RequestBody Adulto adulto) {
        Adulto nuevo = adultoService.crear(adulto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<Adulto>> listarTodos() {
        List<Adulto> lista = adultoService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Adulto> buscarPorId(@PathVariable("id") String id) {
        Optional<Adulto> optAdulto = adultoService.buscarPorId(id);
        if (optAdulto.isPresent()) {
            return ResponseEntity.ok(optAdulto.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Adulto> actualizar(@PathVariable("id") String id, @RequestBody Adulto adulto) {
        Adulto actualizado = adultoService.actualizar(id, adulto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        adultoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

