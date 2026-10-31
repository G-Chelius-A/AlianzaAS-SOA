package com.api.alianza.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad Vuelo en el modelo canónico.
 * Relación Reserva 1 : N Vuelo.
 * Cada Vuelo pertenece a UNA única Reserva con FK 'reserva_id' NOT NULL.
 * El modelo no contempla que un vuelo se comparta entre reservas.
 */
@Entity
@Table(name = "vuelo")
@Getter
@Setter
@NoArgsConstructor
public class Vuelo {

    @Id
    @Column(name = "id_vuelo", length = 36, nullable = false)
    private String idVuelo;

    @Column(name = "fecha_hora_salida", nullable = false)
    private LocalDateTime fechaHoraSalida;

    @Column(name = "origen", length = 100)
    private String origen;

    @Column(name = "destino", length = 100)
    private String destino;

    @Column(name = "completado", nullable = false)
    private Boolean completado = false;

    @Column(name = "fecha_hora_llegada")
    private LocalDateTime fechaHoraLlegada;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;
}
