package com.api.alianza.services.transformador;

import com.api.alianza.entities.Adulto;
import com.api.alianza.entities.AerolineaOrigen;
import com.api.alianza.entities.Equipaje;
import com.api.alianza.entities.Menor;
import com.api.alianza.entities.Pasajero;
import com.api.alianza.entities.Reserva;
import com.api.alianza.entities.Tarifa;
import com.api.alianza.entities.UnidadPeso;
import com.api.alianza.entities.Vuelo;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Transformador nativo para las reservas provenientes de la aerolínea AeroNorte.
 *
 * JUSTIFICACIÓN ARQUITECTURAL (Excepción de servicio según restricciones):
 * Este componente es lógica de negocio/servicio especializada y no un DTO ni mapper genérico.
 * Se justifica porque la estructura XML nativa de AeroNorte es completamente heterogénea e incompatible
 * con el grafo de entidades JPA canónico.
 *
 * CONVENCIONES DE ORIGEN Y REGLAS DE TRANSFORMACIÓN IMPLEMENTADAS:
 * 1. Formato de entrada: XML plano con raíz <reserva>.
 * 2. Identificador: <pnr> debe tener longitud exacta de 6 caracteres alfanuméricos. Se mapea a origenPNR.
 * 3. Aerolínea: Fija en AerolineaOrigen.AERONORTE.
 * 4. Nombre de pasajero: Cadena con formato "APELLIDO/NOMBRE" en mayúsculas sin acentos.
 *    Se separa por la barra diagonal "/", capitalizando cada parte (ej. GARCIA/ANA -> Nombres: Ana, Apellidos: Garcia).
 *    DECISIÓN DE NEGOCIO DOCUMENTADA: Los acentos perdidos en el sistema de origen NO se reconstruyen artificialmente.
 * 5. Fecha de vuelo: Cadena en formato "DD/MM/AAAA". Se asume la hora 00:00:00 en la zona "America/Mexico_City".
 *    DECISIÓN DE NEGOCIO: El modelo canónico almacena LocalDateTime sin zona horaria explícita.
 * 6. Tarifa: Cadena con valor numérico entero en <tarifa>. Se convierte a BigDecimal con escala 2.
 *    Moneda fija por convención en "MXN" (el sistema AeroNorte siempre opera en pesos mexicanos sin indicarlo).
 * 7. Equipaje: Cadena con valor numérico en <equipaje> que representa kilogramos. Se mapea a peso,
 *    con unidadPeso = KG y cantidadPiezas = null.
 * 8. Subtipo de Pasajero: Elemento <menor> con valores "S" o "N".
 *    - "N" indica Adulto.
 *    - "S" indica menor de 12 años (Menor).
 *    LIMITACIÓN DOCUMENTADA: AeroNorte no distingue infantes (0 a 1 año); toda 'S' se clasifica como Menor.
 *    Si es Menor, se requiere el idTutor pasado como parámetro de consulta. Si idTutor es nulo, se lanza excepción 422.
 * 9. Hardening XML (XXE): Se configuran de manera estricta las banderas de seguridad en DocumentBuilderFactory
 *    para prevenir ataques de inyección de entidades externas (XML External Entity - XXE).
 */
@Component
public class TransformadorAeroNorte {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Transforma una carga XML nativa de AeroNorte a la entidad canónica en memoria Reserva.
     *
     * @param xmlPayload Contenido XML nativo en formato texto.
     * @param idTutor    Identificador UUID opcional del tutor adulto (obligatorio si menor = 'S').
     * @return Entidad Reserva en memoria lista para su validación canónica y posterior persistencia.
     */
    public Reserva transformar(String xmlPayload, String idTutor) {
        if (xmlPayload == null || xmlPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("El cuerpo XML de AeroNorte no puede ser nulo ni vacío.");
        }

        Document document = parsearXmlConHardening(xmlPayload);
        Element raiz = document.getDocumentElement();
        if (!"reserva".equalsIgnoreCase(raiz.getTagName())) {
            throw new IllegalArgumentException("La etiqueta raíz del XML de AeroNorte debe ser <reserva>.");
        }

        String pnr = obtenerTextoRequerido(raiz, "pnr");
        if (pnr.length() != 6) {
            throw new IllegalArgumentException("El PNR de AeroNorte debe tener exactamente 6 caracteres. Recibido: " + pnr);
        }

        String pasajeroRaw = obtenerTextoRequerido(raiz, "pasajero");
        if (!pasajeroRaw.contains("/")) {
            throw new IllegalArgumentException("El formato del nombre de pasajero en AeroNorte debe ser 'APELLIDO/NOMBRE'. Recibido: " + pasajeroRaw);
        }

        String[] partesNombre = pasajeroRaw.split("/", 2);
        String apellidos = capitalizar(partesNombre[0].trim());
        String nombres = capitalizar(partesNombre[1].trim());

        String fechaStr = obtenerTextoRequerido(raiz, "fecha");
        LocalDate fechaVuelo;
        try {
            fechaVuelo = LocalDate.parse(fechaStr, FECHA_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de vuelo en AeroNorte debe tener el formato DD/MM/AAAA válido. Recibido: " + fechaStr, ex);
        }

        String tarifaStr = obtenerTextoRequerido(raiz, "tarifa");
        BigDecimal importeTarifa;
        try {
            importeTarifa = new BigDecimal(tarifaStr).setScale(2, java.math.RoundingMode.HALF_UP);
            if (importeTarifa.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El importe de la tarifa no puede ser negativo.");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El importe de la tarifa en AeroNorte debe ser un valor numérico. Recibido: " + tarifaStr, ex);
        }

        String menorFlag = obtenerTextoRequerido(raiz, "menor").toUpperCase();
        if (!"S".equals(menorFlag) && !"N".equals(menorFlag)) {
            throw new IllegalArgumentException("El valor de la etiqueta <menor> en AeroNorte debe ser 'S' o 'N'. Recibido: " + menorFlag);
        }

        Integer pesoEquipaje = null;
        String equipajeStr = obtenerTextoOpcional(raiz, "equipaje");
        if (equipajeStr != null && !equipajeStr.trim().isEmpty()) {
            try {
                pesoEquipaje = Integer.parseInt(equipajeStr.trim());
                if (pesoEquipaje < 0) {
                    throw new IllegalArgumentException("El peso del equipaje no puede ser negativo.");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El peso del equipaje en AeroNorte debe ser un número entero. Recibido: " + equipajeStr, ex);
            }
        }

        // Construir Pasajero canónico según clasificación
        Pasajero pasajero;
        String idPasajero = UUID.randomUUID().toString();
        if ("S".equals(menorFlag)) {
            if (idTutor == null || idTutor.trim().isEmpty()) {
                throw new IllegalArgumentException("La reserva corresponde a un menor de edad. Se requiere el parámetro '?idTutor' con el identificador de un adulto tutor.");
            }
            Menor menor = new Menor();
            menor.setIdPasajero(idPasajero);
            menor.setNombres(nombres);
            menor.setApellidos(apellidos);
            menor.setNacionalidad(null);
            menor.setFechaNacimiento(null); // AeroNorte no envía fecha de nacimiento
            menor.setIdTutor(idTutor.trim());
            menor.setRequiereAcompanante(false);
            menor.setAutorizacionViaje(true);
            pasajero = menor;
        } else {
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

        // Construir Reserva Canónica
        Reserva reserva = new Reserva();
        reserva.setIdReserva(UUID.randomUUID().toString());
        reserva.setOrigenPNR(pnr);
        reserva.setAerolineaOrigen(AerolineaOrigen.AERONORTE);
        reserva.setPasajero(pasajero);

        // Tarifa
        Tarifa tarifa = new Tarifa();
        tarifa.setIdTarifa(UUID.randomUUID().toString());
        tarifa.setImporte(importeTarifa);
        tarifa.setMoneda("MXN");
        reserva.setTarifa(tarifa);

        // Vuelo (1:N, salida a las 00:00:00 en America/Mexico_City)
        Vuelo vuelo = new Vuelo();
        vuelo.setIdVuelo(UUID.randomUUID().toString());
        vuelo.setFechaHoraSalida(fechaVuelo.atStartOfDay());
        vuelo.setOrigen(null);
        vuelo.setDestino(null);
        vuelo.setCompletado(false);
        vuelo.setFechaHoraLlegada(null);
        reserva.addVuelo(vuelo);

        // Equipaje opcional
        if (pesoEquipaje != null) {
            Equipaje equipaje = new Equipaje();
            equipaje.setIdEquipaje(UUID.randomUUID().toString());
            equipaje.setCantidadPiezas(null);
            equipaje.setPeso(pesoEquipaje);
            equipaje.setUnidadPeso(UnidadPeso.KG);
            reserva.setEquipaje(equipaje);
        } else {
            reserva.setEquipaje(null);
        }

        return reserva;
    }

    /**
     * Parsea la cadena XML aplicando hardening estricto contra ataques XXE (XML External Entity)
     * e inclusión maliciosa de DTDs externas.
     */
    private Document parsearXmlConHardening(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);

            // Desactivar DTDs por completo para neutralizar ataques XXE y 'Billion Laughs'
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            // Restricción de acceso a protocolos externos (CWE-611)
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder builder = dbf.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Error al parsear el XML de AeroNorte: formato mal formado o no procesable. " + e.getMessage(), e);
        }
    }

    private String obtenerTextoRequerido(Element raiz, String tag) {
        NodeList nl = raiz.getElementsByTagName(tag);
        if (nl.getLength() == 0 || nl.item(0).getTextContent() == null || nl.item(0).getTextContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Falta la etiqueta requerida <" + tag + "> en el XML de AeroNorte.");
        }
        return nl.item(0).getTextContent().trim();
    }

    private String obtenerTextoOpcional(Element raiz, String tag) {
        NodeList nl = raiz.getElementsByTagName(tag);
        if (nl.getLength() == 0 || nl.item(0).getTextContent() == null) {
            return null;
        }
        return nl.item(0).getTextContent().trim();
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        String[] palabras = texto.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < palabras.length; i++) {
            if (!palabras[i].isEmpty()) {
                sb.append(Character.toUpperCase(palabras[i].charAt(0)));
                if (palabras[i].length() > 1) {
                    sb.append(palabras[i].substring(1));
                }
            }
            if (i < palabras.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
