package com.api.alianza.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Adulto que extiende de Pasajero.
 * Puede actuar como tutor legal de Menores e Infantes.
 */
@Entity
@Table(name = "adulto")
@Getter
@Setter
@NoArgsConstructor
public class Adulto extends Pasajero {

    @Column(name = "documento_identidad", unique = true)
    private String documentoIdentidad;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Override
    public TipoPasajero getTipoPasajero() {
        return TipoPasajero.ADULTO;
    }
}
