package com.api.alianza.services;

/**
 * Excepción lanzada cuando se intenta registrar una reserva con un (aerolineaOrigen, origenPNR)
 * ya existente en el sistema. Mapea a código HTTP 409 Conflict.
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String mensaje) {
        super(mensaje);
    }
}
