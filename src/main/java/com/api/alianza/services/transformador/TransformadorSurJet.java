package com.api.alianza.services.transformador;

import com.api.alianza.entities.Adulto;
import com.api.alianza.entities.AerolineaOrigen;
import com.api.alianza.entities.Equipaje;
import com.api.alianza.entities.Infante;
import com.api.alianza.entities.Menor;
import com.api.alianza.entities.Pasajero;
import com.api.alianza.entities.Reserva;
import com.api.alianza.entities.Tarifa;
import com.api.alianza.entities.UnidadPeso;
import com.api.alianza.entities.Vuelo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Transformador nativo para las reservas provenientes de la aerolínea SurJet.
 *
 * JUSTIFICACIÓN ARQUITECTURAL (Excepción de servicio según restricciones):
 * Componente de servicio que traduce la estructura JSON nativa propia de SurJet
 * al grafo canónico de entidades de la alianza, sin intermediación de DTOs ni mappers genéricos.
 *
 * CONVENCIONES DE ORIGEN Y REGLAS DE TRANSFORMACIÓN IMPLEMENTADAS:
 * 1. Identificador: campo 'booking' con formato estricto 'SJ-\\d{5}' (ej. 'SJ-88412'). Mapeado a origenPNR.
 * 2. Aerolínea: Fija en AerolineaOrigen.SURJET.
 * 3. Nombres y apellidos: Provistos en campos separados 'firstName' y 'lastName' con acentos originales intactos.
 * 4. Fecha de vuelo (flightDate): Formato ISO 8601 con zona horaria/offset (ej. '2026-04-03T08:30:00-06:00').
 *    NORMALIZACIÓN OBLIGATORIA: Se parsea con OffsetDateTime, se conserva el instante exacto, se convierte mediante
 *    atZoneSameInstant(ZoneId.of("America/Mexico_City")) y se extrae el LocalDateTime canónico.
 * 5. Tarifa: Objeto 'fare' con campos 'amount' y 'currency'. NO se realiza conversión de divisas.
 *    Se preserva el monto con escala 2 y la moneda ISO enviada (ej. USD).
 * 6. Equipaje: campo 'bagsLb'. Se mapea a peso, con unidadPeso = LB y cantidadPiezas = null.
 * 7. Subtipo de Pasajero según 'age':
 *    - 0 a 1 año: Infante.
 *    - 2 a 11 años: Menor.
 *    - >= 12 años: Adulto.
 *    Si es Menor o Infante, se exige el parámetro ?idTutor.
 * 8. Estimación de fechaNacimiento:
 *    DECISIÓN DE NEGOCIO DOCUMENTADA: SurJet no envía fecha de nacimiento real, solo la edad entera en 'age'.
 *    Se estima de forma aproximada restando 'age' años a la fecha del vuelo (la fecha de referencia disponible).
 */
@Component
public class TransformadorSurJet {

    private static final Pattern PATRON_BOOKING = Pattern.compile("^SJ-\\d{5}$");
    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    public Reserva transformar(JsonNode json, String idTutor) {
        if (json == null || json.isNull() || !json.isObject()) {
            throw new IllegalArgumentException("El cuerpo JSON de SurJet debe ser un objeto JSON válido.");
        }

        // 1. Validar Booking
        JsonNode bookingNode = json.get("booking");
        if (bookingNode == null || !bookingNode.isTextual()) {
            throw new IllegalArgumentException("El campo 'booking' en SurJet es obligatorio y debe ser una cadena.");
        }
        String booking = bookingNode.asText().trim();
        if (!PATRON_BOOKING.matcher(booking).matches()) {
            throw new IllegalArgumentException("El campo 'booking' de SurJet debe cumplir el patrón 'SJ-XXXXX' (5 dígitos). Recibido: " + booking);
        }

        // 2. Nombres y apellidos
        JsonNode firstNameNode = json.get("firstName");
        JsonNode lastNameNode = json.get("lastName");
        if (firstNameNode == null || !firstNameNode.isTextual() || firstNameNode.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'firstName' en SurJet es obligatorio.");
        }
        if (lastNameNode == null || !lastNameNode.isTextual() || lastNameNode.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'lastName' en SurJet es obligatorio.");
        }
        String nombres = firstNameNode.asText().trim();
        String apellidos = lastNameNode.asText().trim();

        // 3. Fecha con normalización de zona horaria
        JsonNode flightDateNode = json.get("flightDate");
        if (flightDateNode == null || !flightDateNode.isTextual()) {
            throw new IllegalArgumentException("El campo 'flightDate' en SurJet es obligatorio con formato ISO 8601 con zona horaria.");
        }
        LocalDateTime fechaHoraSalidaNormalizada;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(flightDateNode.asText().trim());
            fechaHoraSalidaNormalizada = odt.atZoneSameInstant(ZONA_MEXICO).toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Error al parsear 'flightDate' en SurJet: debe ser un ISO-8601 con offset válido. Recibido: " + flightDateNode.asText(), e);
        }

        // 4. Tarifa
        JsonNode fareNode = json.get("fare");
        if (fareNode == null || !fareNode.isObject()) {
            throw new IllegalArgumentException("El objeto 'fare' en SurJet es obligatorio.");
        }
        JsonNode amountNode = fareNode.get("amount");
        JsonNode currencyNode = fareNode.get("currency");
        if (amountNode == null || !amountNode.isNumber()) {
            throw new IllegalArgumentException("El campo 'fare.amount' debe ser un número.");
        }
        if (currencyNode == null || !currencyNode.isTextual() || currencyNode.asText().trim().length() != 3) {
            throw new IllegalArgumentException("El campo 'fare.currency' debe ser un código ISO de 3 letras.");
        }
        BigDecimal importe = BigDecimal.valueOf(amountNode.asDouble()).setScale(2, java.math.RoundingMode.HALF_UP);
        String moneda = currencyNode.asText().trim().toUpperCase();

        // 5. Equipaje
        Integer pesoLb = null;
        if (json.hasNonNull("bagsLb")) {
            JsonNode bagsNode = json.get("bagsLb");
            if (!bagsNode.isInt()) {
                throw new IllegalArgumentException("El campo 'bagsLb' debe ser un entero.");
            }
            pesoLb = bagsNode.asInt();
            if (pesoLb < 0) {
                throw new IllegalArgumentException("El peso de equipaje en SurJet no puede ser negativo.");
            }
        }

        // 6. Edad y clasificación de Pasajero
        JsonNode ageNode = json.get("age");
        if (ageNode == null || !ageNode.isInt()) {
            throw new IllegalArgumentException("El campo 'age' en SurJet es obligatorio y debe ser un número entero.");
        }
        int age = ageNode.asInt();
        if (age < 0) {
            throw new IllegalArgumentException("La edad del pasajero no puede ser negativa.");
        }

        LocalDate fechaNacimientoAproximada = fechaHoraSalidaNormalizada.toLocalDate().minusYears(age);
        String idPasajero = UUID.randomUUID().toString();
        Pasajero pasajero;

        if (age <= 1) {
            // Infante (0 a 1 año)
            if (idTutor == null || idTutor.trim().isEmpty()) {
                throw new IllegalArgumentException("La reserva corresponde a un infante (edad: " + age + "). Se requiere el parámetro '?idTutor' con el identificador de un adulto tutor.");
            }
            Infante infante = new Infante();
            infante.setIdPasajero(idPasajero);
            infante.setNombres(nombres);
            infante.setApellidos(apellidos);
            infante.setNacionalidad(null);
            infante.setFechaNacimiento(fechaNacimientoAproximada);
            infante.setIdTutor(idTutor.trim());
            infante.setRequiereAsiento(false);
            infante.setRequiereServicioEspecial(false);
            pasajero = infante;
        } else if (age <= 11) {
            // Menor (2 a 11 años)
            if (idTutor == null || idTutor.trim().isEmpty()) {
                throw new IllegalArgumentException("La reserva corresponde a un menor de edad (edad: " + age + "). Se requiere el parámetro '?idTutor' con el identificador de un adulto tutor.");
            }
            Menor menor = new Menor();
            menor.setIdPasajero(idPasajero);
            menor.setNombres(nombres);
            menor.setApellidos(apellidos);
            menor.setNacionalidad(null);
            menor.setFechaNacimiento(fechaNacimientoAproximada);
            menor.setIdTutor(idTutor.trim());
            menor.setRequiereAcompanante(false);
            menor.setAutorizacionViaje(true);
            pasajero = menor;
        } else {
            // Adulto (>= 12 años)
            Adulto adulto = new Adulto();
            adulto.setIdPasajero(idPasajero);
            adulto.setNombres(nombres);
            adulto.setApellidos(apellidos);
            adulto.setNacionalidad(null);
            adulto.setFechaNacimiento(fechaNacimientoAproximada);
            adulto.setDocumentoIdentidad(null);
            adulto.setTelefonoContacto(null);
            pasajero = adulto;
        }

        // Construir Reserva Canónica
        Reserva reserva = new Reserva();
        reserva.setIdReserva(UUID.randomUUID().toString());
        reserva.setOrigenPNR(booking);
        reserva.setAerolineaOrigen(AerolineaOrigen.SURJET);
        reserva.setPasajero(pasajero);

        Tarifa tarifa = new Tarifa();
        tarifa.setIdTarifa(UUID.randomUUID().toString());
        tarifa.setImporte(importe);
        tarifa.setMoneda(moneda);
        reserva.setTarifa(tarifa);

        Vuelo vuelo = new Vuelo();
        vuelo.setIdVuelo(UUID.randomUUID().toString());
        vuelo.setFechaHoraSalida(fechaHoraSalidaNormalizada);
        vuelo.setOrigen(null);
        vuelo.setDestino(null);
        vuelo.setCompletado(false);
        vuelo.setFechaHoraLlegada(null);
        reserva.addVuelo(vuelo);

        if (pesoLb != null) {
            Equipaje equipaje = new Equipaje();
            equipaje.setIdEquipaje(UUID.randomUUID().toString());
            equipaje.setCantidadPiezas(null);
            equipaje.setPeso(pesoLb);
            equipaje.setUnidadPeso(UnidadPeso.LB);
            reserva.setEquipaje(equipaje);
        } else {
            reserva.setEquipaje(null);
        }

        return reserva;
    }
}
