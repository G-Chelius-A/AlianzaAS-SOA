package com.api.alianza.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad central Reserva en el modelo canónico.
 * Posee un identificador global UUID asignado programáticamente por la alianza.
 * Mantiene unicidad compuesta sobre (aerolineaOrigen, origenPNR) para evitar colisiones
 * y garantizar trazabilidad exacta del sistema de origen.
 */
@Entity
@Table(
        name = "reserva",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reserva_aerolinea_pnr",
                        columnNames = {"aerolinea_origen", "origen_pnr"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @Column(name = "id_reserva", length = 36, nullable = false)
    private String idReserva;

    @Column(name = "origen_pnr", nullable = false)
    private String origenPNR;

    @Enumerated(EnumType.STRING)
    @Column(name = "aerolinea_origen", length = 20, nullable = false)
    private AerolineaOrigen aerolineaOrigen;

    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "pasajero_id", nullable = false)
    private Pasajero pasajero;

    /**
     * Relación 1 : 1 con Tarifa. Cascade ALL y orphanRemoval true para
     * ciclo de vida acoplado a la Reserva.
     */
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private Tarifa tarifa;

    /**
     * Relación 1 : N con Vuelo. Cascade ALL y orphanRemoval true:
     * los vuelos pertenecen exclusivamente a esta reserva.
     * FetchType.EAGER para asegurar que la colección de vuelos esté siempre disponible
     * al consultar la reserva sin riesgo de LazyInitializationException.
     */
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<Vuelo> vuelos = new ArrayList<>();

    /**
     * Relación 0..1 : 1 con Equipaje. Cascade ALL y orphanRemoval true.
     */
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, optional = true)
    private Equipaje equipaje;

    // Métodos helper de conveniencia para mantener la consistencia bidireccional
    public void setTarifa(Tarifa tarifa) {
        this.tarifa = tarifa;
        if (tarifa != null) {
            tarifa.setReserva(this);
        }
    }

    public void setEquipaje(Equipaje equipaje) {
        this.equipaje = equipaje;
        if (equipaje != null) {
            equipaje.setReserva(this);
        }
    }

    public void addVuelo(Vuelo vuelo) {
        this.vuelos.add(vuelo);
        vuelo.setReserva(this);
    }
}
