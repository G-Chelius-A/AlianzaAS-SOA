package com.api.alianza.controllers;

import com.api.alianza.entities.Vuelo;
import com.api.alianza.services.VueloService;
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
@RequestMapping(value = "/api/v1/vuelos", produces = MediaType.APPLICATION_JSON_VALUE)
public class VueloController {

    @Autowired
    private VueloService vueloService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Vuelo> crear(@RequestParam("idReserva") String idReserva, @RequestBody Vuelo vuelo) {
        Vuelo nuevo = vueloService.crear(idReserva, vuelo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<Vuelo>> listar(@RequestParam(name = "idReserva", required = false) String idReserva) {
        if (idReserva != null && !idReserva.trim().isEmpty()) {
            return ResponseEntity.ok(vueloService.listarPorReserva(idReserva));
        }
        return ResponseEntity.ok(vueloService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vuelo> buscarPorId(@PathVariable("id") String id) {
        Optional<Vuelo> optVuelo = vueloService.buscarPorId(id);
        if (optVuelo.isPresent()) {
            return ResponseEntity.ok(optVuelo.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Vuelo> actualizar(@PathVariable("id") String id, @RequestBody Vuelo vuelo) {
        Vuelo actualizado = vueloService.actualizar(id, vuelo);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        vueloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

