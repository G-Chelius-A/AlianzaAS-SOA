package com.api.alianza.controllers;

import com.api.alianza.services.ValidadorEsquemasService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para validaciones de esquemas sin persistir entidades.
 * Endpoints:
 * - POST /api/v1/validaciones/xml: Valida XML contra alianza.xsd.
 * - POST /api/v1/validaciones/json: Valida JSON contra reserva.schema.json.
 */
@RestController
@RequestMapping(value = "/api/v1/validaciones", produces = MediaType.APPLICATION_JSON_VALUE)
public class ValidacionController {

    private final ValidadorEsquemasService validadorEsquemasService;

    public ValidacionController(ValidadorEsquemasService validadorEsquemasService) {
        this.validadorEsquemasService = validadorEsquemasService;
    }

    /**
     * POST /api/v1/validaciones/xml
     * Recibe cuerpo JSON: {"xml": "<reserva>...</reserva>"}.
     * Valida contra el XSD canónico sin persistir.
     */
    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> validarXml(@RequestBody Map<String, String> body) {
        if (body == null || !body.containsKey("xml")) {
            throw new IllegalArgumentException("El cuerpo JSON debe contener la propiedad 'xml'.");
        }
        String xml = body.get("xml");
        List<String> errores = validadorEsquemasService.validarXml(xml);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("valido", errores.isEmpty());
        resp.put("esquema", "alianza.xsd");
        resp.put("errores", errores);
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/v1/validaciones/json
     * Recibe JSON canónico en el cuerpo.
     * Valida contra reserva.schema.json sin persistir.
     */
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> validarJson(@RequestBody JsonNode body) {
        List<String> errores = validadorEsquemasService.validarJson(body);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("valido", errores.isEmpty());
        resp.put("esquema", "reserva.schema.json");
        resp.put("errores", errores);
        return ResponseEntity.ok(resp);
    }
}
