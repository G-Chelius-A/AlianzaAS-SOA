package com.api.alianza.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Infante que extiende de Pasajero.
 * Requiere la referencia obligatoria a un Adulto como tutor.
 */
@Entity
@Table(name = "infante")
@Getter
@Setter
@NoArgsConstructor
public class Infante extends Pasajero {

    @Column(name = "id_tutor", length = 36, nullable = false)
    private String idTutor;

    @Column(name = "requiere_asiento", nullable = false)
    private Boolean requiereAsiento = false;

    @Column(name = "requiere_servicio_especial", nullable = false)
    private Boolean requiereServicioEspecial = false;

    @Override
    public TipoPasajero getTipoPasajero() {
        return TipoPasajero.INFANTE;
    }
}
