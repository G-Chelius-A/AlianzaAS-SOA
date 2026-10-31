package com.api.alianza.services.transformador;

import com.api.alianza.entities.Adulto;
import com.api.alianza.entities.AerolineaOrigen;
import com.api.alianza.entities.Equipaje;
import com.api.alianza.entities.Infante;
import com.api.alianza.entities.Menor;
import com.api.alianza.entities.Pasajero;
import com.api.alianza.entities.Reserva;
import com.api.alianza.entities.Tarifa;
import com.api.alianza.entities.Vuelo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformador nativo para las reservas provenientes de la aerolínea CaribeAir.
 *
 * JUSTIFICACIÓN ARQUITECTURAL (Excepción de servicio según restricciones):
 * Componente de servicio que implementa la lógica de parsing y unificación de datos
 * heterogéneos provenientes de CaribeAir hacia el modelo canónico de entidades.
 *
 * CONVENCIONES DE ORIGEN Y REGLAS DE TRANSFORMACIÓN IMPLEMENTADAS:
 * 1. Identificador: campo 'id' entero consecutivo (ej. 99812). Se convierte a String y se almacena en origenPNR.
 * 2. Aerolínea: Fija en AerolineaOrigen.CARIBEAIR.
 * 3. Nombre completo: Cadena sin acentos en 'nombre' (ej. "Ana Garcia").
 *    HEURÍSTICA Y AMBIGÜEDAD DOCUMENTADA: Debido a que CaribeAir no envía nombres y apellidos estructurados,
 *    se aplica la regla: el primer token separado por espacio se asigna a 'nombres' y el resto concatenado a 'apellidos'.
 *    Esta heurística es inherentemente ambigua para personas con nombres compuestos (ej. "Juan Pablo Pérez"),
 *    pero es la mejor aproximación posible sin enriquecimiento manual externo.
 * 4. Fecha de vuelo: Cadena en formato "MM/DD/AAAA" (notación estadounidense, ej. '04/03/2026').
 *    Se interpreta asumiendo las 00:00:00 en 'America/Mexico_City', generándose un LocalDateTime canónico.
 * 5. Precio/Tarifa: Formato texto con separador de miles y coma (ej. '4,500.00').
 *    Se eliminan las comas de miles y se parsea como BigDecimal. Moneda fija por convención en "MXN".
 * 6. Equipaje: Cadena descriptiva (ej. "1 pieza").
 *    Se extrae el número entero mediante expresión regular y se almacena en 'cantidadPiezas'.
 *    Los campos 'peso' y 'unidadPeso' quedan en null, tal como estipulan las reglas de negocio.
 * 7. Tipo de pasajero: campo 'tipoPasajero' con valores de código IATA estándar:
 *    - 'ADT' -> Adulto.
 *    - 'CHD' -> Menor (Child). Requiere ?idTutor.
 *    - 'INF' -> Infante (Infant). Requiere ?idTutor.
 *    Cualquier valor fuera de estos tres se rechaza con 422.
 */
@Component
public class TransformadorCaribeAir {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Pattern PATRON_PIEZAS = Pattern.compile("(\\d+)\\s*pieza", Pattern.CASE_INSENSITIVE);

    public Reserva transformar(JsonNode json, String idTutor) {
        if (json == null || json.isNull() || !json.isObject()) {
            throw new IllegalArgumentException("El cuerpo JSON de CaribeAir debe ser un objeto JSON válido.");
        }

        // 1. Identificador entero -> String
        JsonNode idNode = json.get("id");
        if (idNode == null || (!idNode.isInt() && !idNode.isLong())) {
            throw new IllegalArgumentException("El campo 'id' de CaribeAir debe ser un número entero consecutivo.");
        }
        String origenPNR = String.valueOf(idNode.asLong());

        // 2. Nombre
        JsonNode nombreNode = json.get("nombre");
        if (nombreNode == null || !nombreNode.isTextual() || nombreNode.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'nombre' en CaribeAir es obligatorio.");
        }
        String nombreCompleto = nombreNode.asText().trim();
        String[] partesNombre = nombreCompleto.split("\\s+", 2);
        String nombres = partesNombre[0];
        String apellidos = (partesNombre.length > 1) ? partesNombre[1] : "";

        // 3. Fecha MM/DD/AAAA
        JsonNode fechaNode = json.get("fecha");
        if (fechaNode == null || !fechaNode.isTextual()) {
            throw new IllegalArgumentException("El campo 'fecha' en CaribeAir es obligatorio (formato MM/DD/AAAA).");
        }
        LocalDate fechaVuelo;
        try {
            fechaVuelo = LocalDate.parse(fechaNode.asText().trim(), FECHA_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha en CaribeAir debe tener el formato MM/DD/AAAA válido. Recibido: " + fechaNode.asText(), e);
        }

        // 4. Precio "4,500.00"
        JsonNode precioNode = json.get("precio");
        if (precioNode == null || !precioNode.isTextual()) {
            throw new IllegalArgumentException("El campo 'precio' en CaribeAir es obligatorio.");
        }
        String precioStr = precioNode.asText().replace(",", "").trim();
        BigDecimal importe;
        try {
            importe = new BigDecimal(precioStr).setScale(2, java.math.RoundingMode.HALF_UP);
            if (importe.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio en CaribeAir no puede ser negativo.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El formato del precio en CaribeAir no es numérico válido: " + precioNode.asText(), e);
        }

        // 5. Maleta "1 pieza"
        Integer cantidadPiezas = null;
        if (json.hasNonNull("maleta")) {
            String maletaStr = json.get("maleta").asText().trim();
            Matcher matcher = PATRON_PIEZAS.matcher(maletaStr);
            if (matcher.find()) {
                cantidadPiezas = Integer.parseInt(matcher.group(1));
            } else {
                try {
                    cantidadPiezas = Integer.parseInt(maletaStr.replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {
                    throw new IllegalArgumentException("No se pudo interpretar el equipaje en CaribeAir: " + maletaStr);
                }
            }
        }

        // 6. Tipo de Pasajero (ADT, CHD, INF)
        JsonNode tipoPasajeroNode = json.get("tipoPasajero");
        if (tipoPasajeroNode == null || !tipoPasajeroNode.isTextual()) {
            throw new IllegalArgumentException("El campo 'tipoPasajero' en CaribeAir es obligatorio.");
        }
        String tipoPasajeroStr = tipoPasajeroNode.asText().trim().toUpperCase();

        String idPasajero = UUID.randomUUID().toString();
        Pasajero pasajero;

        switch (tipoPasajeroStr) {
            case "ADT" -> {
                Adulto adulto = new Adulto();
                adulto.setIdPasajero(idPasajero);
                adulto.setNombres(nombres);
                adulto.setApellidos(apellidos);
                adulto.setNacionalidad(null);
                adulto.setFechaNacimiento(null);
                adulto.setDocumentoIdentidad(null);
                adulto.setTelefonoContacto(null);
                pasajero = adulto;
            }
            case "CHD" -> {
                if (idTutor == null || idTutor.trim().isEmpty()) {
                    throw new IllegalArgumentException("La reserva corresponde a un menor (CHD). Se requiere el parámetro '?idTutor' con el identificador de un adulto tutor.");
                }
                Menor menor = new Menor();
                menor.setIdPasajero(idPasajero);
                menor.setNombres(nombres);
                menor.setApellidos(apellidos);
                menor.setNacionalidad(null);
                menor.setFechaNacimiento(null);
                menor.setIdTutor(idTutor.trim());
                menor.setRequiereAcompanante(false);
                menor.setAutorizacionViaje(true);
                pasajero = menor;
            }
            case "INF" -> {
                if (idTutor == null || idTutor.trim().isEmpty()) {
                    throw new IllegalArgumentException("La reserva corresponde a un infante (INF). Se requiere el parámetro '?idTutor' con el identificador de un adulto tutor.");
                }
                Infante infante = new Infante();
                infante.setIdPasajero(idPasajero);
                infante.setNombres(nombres);
                infante.setApellidos(apellidos);
                infante.setNacionalidad(null);
                infante.setFechaNacimiento(null);
                infante.setIdTutor(idTutor.trim());
                infante.setRequiereAsiento(false);
                infante.setRequiereServicioEspecial(false);
                pasajero = infante;
            }
            default -> throw new IllegalArgumentException("El campo 'tipoPasajero' en CaribeAir debe ser ADT, CHD o INF. Recibido: " + tipoPasajeroStr);
        }

        // Construir Reserva Canónica
        Reserva reserva = new Reserva();
        reserva.setIdReserva(UUID.randomUUID().toString());
        reserva.setOrigenPNR(origenPNR);
        reserva.setAerolineaOrigen(AerolineaOrigen.CARIBEAIR);
        reserva.setPasajero(pasajero);

        Tarifa tarifa = new Tarifa();
        tarifa.setIdTarifa(UUID.randomUUID().toString());
        tarifa.setImporte(importe);
        tarifa.setMoneda("MXN");
        reserva.setTarifa(tarifa);

        Vuelo vuelo = new Vuelo();
        vuelo.setIdVuelo(UUID.randomUUID().toString());
        vuelo.setFechaHoraSalida(fechaVuelo.atStartOfDay());
        vuelo.setOrigen(null);
        vuelo.setDestino(null);
        vuelo.setCompletado(false);
        vuelo.setFechaHoraLlegada(null);
        reserva.addVuelo(vuelo);

        if (cantidadPiezas != null) {
            Equipaje equipaje = new Equipaje();
            equipaje.setIdEquipaje(UUID.randomUUID().toString());
            equipaje.setCantidadPiezas(cantidadPiezas);
            equipaje.setPeso(null);
            equipaje.setUnidadPeso(null);
            reserva.setEquipaje(equipaje);
        } else {
            reserva.setEquipaje(null);
        }

        return reserva;
    }
}
