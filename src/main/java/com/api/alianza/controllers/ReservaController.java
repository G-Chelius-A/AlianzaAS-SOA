package com.api.alianza.controllers;

import com.api.alianza.services.ReservaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de reservas en la alianza Alas del Sur.
 * Todos los endpoints consumen y producen estrictamente application/json.
 */
@RestController
@RequestMapping(value = "/api/v1/reservas", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * POST /api/v1/reservas/aeronorte
     * Recibe XML dentro de un cuerpo JSON: {"xml": "<reserva>...</reserva>"}.
     * Parámetro opcional: ?idTutor=<UUID>
     */
    @PostMapping(value = "/aeronorte", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> crearReservaAeroNorte(
            @RequestBody Map<String, String> body,
            @RequestParam(name = "idTutor", required = false) String idTutor) {

        if (body == null || !body.containsKey("xml")) {
            throw new IllegalArgumentException("El cuerpo JSON debe contener la propiedad 'xml'.");
        }
        String xml = body.get("xml");
        ObjectNode reservaCanonica = reservaService.procesarReservaAeroNorte(xml, idTutor);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaCanonica);
    }

    /**
     * POST /api/v1/reservas/surjet
     * Recibe cuerpo JSON nativo de SurJet.
     * Parámetro opcional: ?idTutor=<UUID>
     */
    @PostMapping(value = "/surjet", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> crearReservaSurJet(
            @RequestBody JsonNode jsonPayload,
            @RequestParam(name = "idTutor", required = false) String idTutor) {

        ObjectNode reservaCanonica = reservaService.procesarReservaSurJet(jsonPayload, idTutor);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaCanonica);
    }

    /**
     * POST /api/v1/reservas/caribeair
     * Recibe cuerpo JSON nativo de CaribeAir.
     * Parámetro opcional: ?idTutor=<UUID>
     */
    @PostMapping(value = "/caribeair", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> crearReservaCaribeAir(
            @RequestBody JsonNode jsonPayload,
            @RequestParam(name = "idTutor", required = false) String idTutor) {

        ObjectNode reservaCanonica = reservaService.procesarReservaCaribeAir(jsonPayload, idTutor);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaCanonica);
    }

    /**
     * GET /api/v1/reservas
     * Lista reservas registradas con filtro opcional por aerolínea.
     */
    @GetMapping
    public ResponseEntity<List<ObjectNode>> listarReservas(
            @RequestParam(name = "aerolinea", required = false) String aerolinea) {
        List<ObjectNode> reservas = reservaService.listarReservas(aerolinea);
        return ResponseEntity.ok(reservas);
    }

    /**
     * GET /api/v1/reservas/{idReserva}
     * Detalle de la reserva en formato JSON canónico.
     */
    @GetMapping("/{idReserva}")
    public ResponseEntity<ObjectNode> obtenerReserva(@PathVariable("idReserva") String idReserva) {
        ObjectNode detalle = reservaService.obtenerReservaPorId(idReserva);
        return ResponseEntity.ok(detalle);
    }

    /**
     * GET /api/v1/reservas/{idReserva}/xml-canonico
     * Devuelve JSON {"xml": "..."} con el XML canónico correspondiente.
     */
    @GetMapping("/{idReserva}/xml-canonico")
    public ResponseEntity<Map<String, String>> obtenerXmlCanonico(@PathVariable("idReserva") String idReserva) {
        String xml = reservaService.obtenerXmlCanonicoPorId(idReserva);
        return ResponseEntity.ok(Collections.singletonMap("xml", xml));
    }

    /**
     * DELETE /api/v1/reservas/{idReserva}
     * Elimina la reserva y sus vuelos, tarifa y equipaje sin eliminar al pasajero ni a su tutor.
     */
    @DeleteMapping("/{idReserva}")
    public ResponseEntity<Map<String, Object>> eliminarReserva(@PathVariable("idReserva") String idReserva) {
        reservaService.eliminarReserva(idReserva);
        Map<String, Object> resp = Map.of(
                "mensaje", "Reserva eliminada con éxito",
                "idReserva", idReserva
        );
        return ResponseEntity.ok(resp);
    }
}
