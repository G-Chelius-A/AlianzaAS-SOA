package com.api.alianza.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclase abstracta del Pasajero en el modelo canónico.
 * Implementa la estrategia de herencia JPA JOINED para persistir los atributos
 * comunes en la tabla 'pasajero' y los atributos específicos en las tablas hijas
 * ('adulto', 'menor', 'infante').
 */
@Entity
@Table(name = "pasajero")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tipoPasajero",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Adulto.class, name = "ADULTO"),
        @JsonSubTypes.Type(value = Menor.class, name = "MENOR"),
        @JsonSubTypes.Type(value = Infante.class, name = "INFANTE")
})
public abstract class Pasajero {

    @Id
    @Column(name = "id_pasajero", length = 36, nullable = false)
    private String idPasajero;

    @Column(name = "nombres", length = 40, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 40, nullable = false)
    private String apellidos;

    @Column(name = "nacionalidad", length = 40)
    private String nacionalidad;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @JsonIgnore
    @OneToMany(mappedBy = "pasajero")
    private List<Reserva> reservas = new ArrayList<>();

    public abstract TipoPasajero getTipoPasajero();
}
