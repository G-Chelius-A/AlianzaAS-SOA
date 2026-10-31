package com.api.alianza.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidad Tarifa en el modelo canónico.
 * Relación 1:1 con Reserva (cada reserva tiene una tarifa única asociada).
 */
@Entity
@Table(name = "tarifa")
@Getter
@Setter
@NoArgsConstructor
public class Tarifa {

    @Id
    @Column(name = "id_tarifa", length = 36, nullable = false)
    private String idTarifa;

    @Column(name = "importe", precision = 12, scale = 2, nullable = false)
    private BigDecimal importe;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;
}
