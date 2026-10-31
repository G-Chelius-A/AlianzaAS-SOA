package com.api.alianza.services;

/**
 * Excepción lanzada cuando no se encuentra un recurso solicitado (Reserva, Pasajero, Tutor).
 * Mapea a código HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
