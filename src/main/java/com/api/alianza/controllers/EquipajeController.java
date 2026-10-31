package com.api.alianza.controllers;

import com.api.alianza.entities.Equipaje;
import com.api.alianza.services.EquipajeService;
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
@RequestMapping(value = "/api/v1/equipajes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EquipajeController {

    @Autowired
    private EquipajeService equipajeService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Equipaje> crear(@RequestParam("idReserva") String idReserva, @RequestBody Equipaje equipaje) {
        Equipaje nuevo = equipajeService.crear(idReserva, equipaje);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<Equipaje>> listarTodos(@RequestParam(name = "idReserva", required = false) String idReserva) {
        if (idReserva != null && !idReserva.trim().isEmpty()) {
            Optional<Equipaje> optEquipaje = equipajeService.buscarPorIdReserva(idReserva);
            if (optEquipaje.isPresent()) {
                return ResponseEntity.ok(java.util.Collections.singletonList(optEquipaje.get()));
            } else {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
        }
        return ResponseEntity.ok(equipajeService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipaje> buscarPorId(@PathVariable("id") String id) {
        Optional<Equipaje> optEquipaje = equipajeService.buscarPorId(id);
        if (optEquipaje.isPresent()) {
            return ResponseEntity.ok(optEquipaje.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Equipaje> actualizar(@PathVariable("id") String id, @RequestBody Equipaje equipaje) {
        Equipaje actualizado = equipajeService.actualizar(id, equipaje);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        equipajeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

