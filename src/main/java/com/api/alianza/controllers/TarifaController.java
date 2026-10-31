package com.api.alianza.controllers;

import com.api.alianza.entities.Tarifa;
import com.api.alianza.services.TarifaService;
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
@RequestMapping(value = "/api/v1/tarifas", produces = MediaType.APPLICATION_JSON_VALUE)
public class TarifaController {

    @Autowired
    private TarifaService tarifaService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Tarifa> crear(@RequestParam("idReserva") String idReserva, @RequestBody Tarifa tarifa) {
        Tarifa nueva = tarifaService.crear(idReserva, tarifa);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @GetMapping
    public ResponseEntity<List<Tarifa>> listarTodas(@RequestParam(name = "idReserva", required = false) String idReserva) {
        if (idReserva != null && !idReserva.trim().isEmpty()) {
            Optional<Tarifa> optTarifa = tarifaService.buscarPorIdReserva(idReserva);
            if (optTarifa.isPresent()) {
                return ResponseEntity.ok(java.util.Collections.singletonList(optTarifa.get()));
            } else {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
        }
        return ResponseEntity.ok(tarifaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tarifa> buscarPorId(@PathVariable("id") String id) {
        Optional<Tarifa> optTarifa = tarifaService.buscarPorId(id);
        if (optTarifa.isPresent()) {
            return ResponseEntity.ok(optTarifa.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Tarifa> actualizar(@PathVariable("id") String id, @RequestBody Tarifa tarifa) {
        Tarifa actualizada = tarifaService.actualizar(id, tarifa);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        tarifaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

