package com.api.alianza.services;

import java.util.List;

/**
 * Excepción lanzada cuando una representación de entrada no supera
 * las validaciones de esquema (XSD o JSON Schema).
 * Mapea a código HTTP 422 Unprocessable Entity.
 */
public class SchemaValidationException extends RuntimeException {

    private final List<String> errores;

    public SchemaValidationException(String mensaje, List<String> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public List<String> getErrores() {
        return errores;
    }
}
