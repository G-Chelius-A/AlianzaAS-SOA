package com.api.alianza.services;

/**
 * Excepción lanzada cuando una regla de negocio o convención de datos es violada
 * a pesar de que el formato estructural básico sea legible.
 * Mapea a código HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleValidationException extends RuntimeException {
    public BusinessRuleValidationException(String mensaje) {
        super(mensaje);
    }
}
