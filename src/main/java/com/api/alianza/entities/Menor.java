package com.api.alianza.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Menor que extiende de Pasajero.
 * Requiere la referencia obligatoria a un Adulto como tutor.
 */
@Entity
@Table(name = "menor")
@Getter
@Setter
@NoArgsConstructor
public class Menor extends Pasajero {

    @Column(name = "id_tutor", length = 36, nullable = false)
    private String idTutor;

    @Column(name = "requiere_acompanante", nullable = false)
    private Boolean requiereAcompanante = false;

    @Column(name = "autorizacion_viaje", nullable = false)
    private Boolean autorizacionViaje = false;

    @Override
    public TipoPasajero getTipoPasajero() {
        return TipoPasajero.MENOR;
    }
}
