package com.api.alianza.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Equipaje en el modelo canónico.
 * Relación Reserva 0..1 : 1 Equipaje.
 * Cada registro de Equipaje pertenece obligatoriamente a una única Reserva (FK reserva_id NOT NULL y UNIQUE).
 */
@Entity
@Table(name = "equipaje")
@Getter
@Setter
@NoArgsConstructor
public class Equipaje {

    @Id
    @Column(name = "id_equipaje", length = 36, nullable = false)
    private String idEquipaje;

    @Column(name = "cantidad_piezas")
    private Integer cantidadPiezas;

    @Column(name = "peso")
    private Integer peso;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_peso", length = 2)
    private UnidadPeso unidadPeso;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;
}
