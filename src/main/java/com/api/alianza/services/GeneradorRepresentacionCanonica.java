package com.api.alianza.services;

import com.api.alianza.entities.Adulto;
import com.api.alianza.entities.Equipaje;
import com.api.alianza.entities.Infante;
import com.api.alianza.entities.Menor;
import com.api.alianza.entities.Pasajero;
import com.api.alianza.entities.Reserva;
import com.api.alianza.entities.Tarifa;
import com.api.alianza.entities.Vuelo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;

/**
 * Generador de representaciones canónicas oficiales (XML con DOM nativo y JSON con Jackson ObjectNode).
 *
 * JUSTIFICACIÓN ARQUITECTURAL (Excepción estipulada de la capa de servicio):
 * Este componente pertenece a la lógica de servicio y no es un DTO ni mapper genérico.
 * Se justifica porque los esquemas formales de intercambio ('alianza.xsd' y 'reserva.schema.json')
 * exigen una estructura de presentación con discriminadores explícitos (xs:choice en XML / oneOf con tipoPasajero en JSON),
 * atributos nulos emitidos con 'xsi:nil="true"' o 'null', y anidamiento estricto de vuelos y equipaje,
 * los cuales difieren de la estructura plana/asociativa de las entidades JPA.
 *
 * DETALLE DE DECISIONES DE GENERACIÓN XML Y HARDENING:
 * 1. Uso de DocumentBuilderFactory configurado con soporte de espacios de nombres (setNamespaceAware(true))
 *    para emitir correctamente el prefijo 'xsi:nil="true"' del namespace 'http://www.w3.org/2001/XMLSchema-instance'.
 * 2. Emisión explícita de nulos: todo campo opcional o nulable en el modelo canónico se emite con xsi:nil="true"
 *    en lugar de omitirse, asegurando paridad semántica exacta con el JSON ('"campo": null').
 * 3. Fechas y horas: formateo ISO-8601 sin zona ('yyyy-MM-dd' para fechas y 'yyyy-MM-dd'T'HH:mm:ss' para fechashoras).
 */
@Component
public class GeneradorRepresentacionCanonica {

    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ObjectMapper objectMapper;

    public GeneradorRepresentacionCanonica(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Genera la representación canónica en XML como String, con indentación y xsi:nil="true" explícito.
     */
    public String generarXml(Reserva reserva) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("reserva");
            root.setAttribute("xmlns:xsi", XSI_NS);
            doc.appendChild(root);

            agregarTexto(doc, root, "idReserva", reserva.getIdReserva());
            agregarTexto(doc, root, "origenPNR", reserva.getOrigenPNR());
            agregarTexto(doc, root, "aerolineaOrigen", reserva.getAerolineaOrigen().name());

            // Pasajero (xs:choice: adulto, menor o infante)
            Pasajero p = reserva.getPasajero();
            if (p instanceof Adulto adulto) {
                Element adultoElem = doc.createElement("adulto");
                root.appendChild(adultoElem);
                construirPasajeroBaseXml(doc, adultoElem, adulto);
                agregarElementoNillable(doc, adultoElem, "documentoIdentidad", adulto.getDocumentoIdentidad());
                agregarElementoNillable(doc, adultoElem, "telefonoContacto", adulto.getTelefonoContacto());
            } else if (p instanceof Menor menor) {
                Element menorElem = doc.createElement("menor");
                root.appendChild(menorElem);
                construirPasajeroBaseXml(doc, menorElem, menor);
                agregarTexto(doc, menorElem, "idTutor", menor.getIdTutor());
                agregarTexto(doc, menorElem, "requiereAcompanante", String.valueOf(menor.getRequiereAcompanante()));
                agregarTexto(doc, menorElem, "autorizacionViaje", String.valueOf(menor.getAutorizacionViaje()));
            } else if (p instanceof Infante infante) {
                Element infanteElem = doc.createElement("infante");
                root.appendChild(infanteElem);
                construirPasajeroBaseXml(doc, infanteElem, infante);
                agregarTexto(doc, infanteElem, "idTutor", infante.getIdTutor());
                agregarTexto(doc, infanteElem, "requiereAsiento", String.valueOf(infante.getRequiereAsiento()));
                agregarTexto(doc, infanteElem, "requiereServicioEspecial", String.valueOf(infante.getRequiereServicioEspecial()));
            }

            // Tarifa
            Tarifa tarifa = reserva.getTarifa();
            Element tarifaElem = doc.createElement("tarifa");
            root.appendChild(tarifaElem);
            agregarTexto(doc, tarifaElem, "idTarifa", tarifa.getIdTarifa());
            agregarTexto(doc, tarifaElem, "importe", tarifa.getImporte().toPlainString());
            agregarTexto(doc, tarifaElem, "moneda", tarifa.getMoneda());

            // Vuelos
            Element vuelosElem = doc.createElement("vuelos");
            root.appendChild(vuelosElem);
            for (Vuelo vuelo : reserva.getVuelos()) {
                Element vueloElem = doc.createElement("vuelo");
                vuelosElem.appendChild(vueloElem);
                agregarTexto(doc, vueloElem, "idVuelo", vuelo.getIdVuelo());
                agregarTexto(doc, vueloElem, "fechaHoraSalida", vuelo.getFechaHoraSalida().format(FORMATO_FECHA_HORA));
                agregarElementoNillable(doc, vueloElem, "origen", vuelo.getOrigen());
                agregarElementoNillable(doc, vueloElem, "destino", vuelo.getDestino());
                agregarTexto(doc, vueloElem, "completado", String.valueOf(vuelo.getCompletado()));
                if (vuelo.getFechaHoraLlegada() != null) {
                    agregarTexto(doc, vueloElem, "fechaHoraLlegada", vuelo.getFechaHoraLlegada().format(FORMATO_FECHA_HORA));
                } else {
                    agregarElementoNillable(doc, vueloElem, "fechaHoraLlegada", null);
                }
            }

            // Equipaje (nillable)
            Equipaje equipaje = reserva.getEquipaje();
            if (equipaje != null) {
                Element equipajeElem = doc.createElement("equipaje");
                root.appendChild(equipajeElem);
                agregarTexto(doc, equipajeElem, "idEquipaje", equipaje.getIdEquipaje());
                if (equipaje.getCantidadPiezas() != null) {
                    agregarTexto(doc, equipajeElem, "cantidadPiezas", String.valueOf(equipaje.getCantidadPiezas()));
                } else {
                    agregarElementoNillable(doc, equipajeElem, "cantidadPiezas", null);
                }
                if (equipaje.getPeso() != null) {
                    agregarTexto(doc, equipajeElem, "peso", String.valueOf(equipaje.getPeso()));
                } else {
                    agregarElementoNillable(doc, equipajeElem, "peso", null);
                }
                if (equipaje.getUnidadPeso() != null) {
                    agregarTexto(doc, equipajeElem, "unidadPeso", equipaje.getUnidadPeso().name());
                } else {
                    agregarElementoNillable(doc, equipajeElem, "unidadPeso", null);
                }
            } else {
                Element equipajeNilElem = doc.createElement("equipaje");
                equipajeNilElem.setAttributeNS(XSI_NS, "xsi:nil", "true");
                root.appendChild(equipajeNilElem);
            }

            // Transformar DOM a String XML
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();

        } catch (ParserConfigurationException | TransformerException e) {
            throw new IllegalStateException("Error al generar la representación XML canónica: " + e.getMessage(), e);
        }
    }

    private void construirPasajeroBaseXml(Document doc, Element parent, Pasajero p) {
        agregarTexto(doc, parent, "idPasajero", p.getIdPasajero());
        agregarTexto(doc, parent, "nombres", p.getNombres());
        agregarTexto(doc, parent, "apellidos", p.getApellidos());
        agregarElementoNillable(doc, parent, "nacionalidad", p.getNacionalidad());
        if (p.getFechaNacimiento() != null) {
            agregarTexto(doc, parent, "fechaNacimiento", p.getFechaNacimiento().format(FORMATO_FECHA));
        } else {
            agregarElementoNillable(doc, parent, "fechaNacimiento", null);
        }
    }

    private void agregarTexto(Document doc, Element parent, String tag, String texto) {
        Element el = doc.createElement(tag);
        el.setTextContent(texto != null ? texto : "");
        parent.appendChild(el);
    }

    private void agregarElementoNillable(Document doc, Element parent, String tag, String texto) {
        Element el = doc.createElement(tag);
        if (texto != null && !texto.isEmpty()) {
            el.setTextContent(texto);
        } else {
            el.setAttributeNS(XSI_NS, "xsi:nil", "true");
        }
        parent.appendChild(el);
    }

    /**
     * Genera la representación canónica en JSON como ObjectNode, con discriminador tipoPasajero
     * y campos nulos explícitos.
     */
    public ObjectNode generarJson(Reserva reserva) {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("idReserva", reserva.getIdReserva());
        root.put("origenPNR", reserva.getOrigenPNR());
        root.put("aerolineaOrigen", reserva.getAerolineaOrigen().name());

        // Pasajero
        ObjectNode pasajeroNode = objectMapper.createObjectNode();
        Pasajero p = reserva.getPasajero();
        pasajeroNode.put("tipoPasajero", p.getTipoPasajero().name());
        pasajeroNode.put("idPasajero", p.getIdPasajero());
        pasajeroNode.put("nombres", p.getNombres());
        pasajeroNode.put("apellidos", p.getApellidos());

        if (p.getNacionalidad() != null) {
            pasajeroNode.put("nacionalidad", p.getNacionalidad());
        } else {
            pasajeroNode.putNull("nacionalidad");
        }

        if (p.getFechaNacimiento() != null) {
            pasajeroNode.put("fechaNacimiento", p.getFechaNacimiento().format(FORMATO_FECHA));
        } else {
            pasajeroNode.putNull("fechaNacimiento");
        }

        if (p instanceof Adulto adulto) {
            if (adulto.getDocumentoIdentidad() != null) {
                pasajeroNode.put("documentoIdentidad", adulto.getDocumentoIdentidad());
            } else {
                pasajeroNode.putNull("documentoIdentidad");
            }
            if (adulto.getTelefonoContacto() != null) {
                pasajeroNode.put("telefonoContacto", adulto.getTelefonoContacto());
            } else {
                pasajeroNode.putNull("telefonoContacto");
            }
        } else if (p instanceof Menor menor) {
            pasajeroNode.put("idTutor", menor.getIdTutor());
            pasajeroNode.put("requiereAcompanante", menor.getRequiereAcompanante());
            pasajeroNode.put("autorizacionViaje", menor.getAutorizacionViaje());
        } else if (p instanceof Infante infante) {
            pasajeroNode.put("idTutor", infante.getIdTutor());
            pasajeroNode.put("requiereAsiento", infante.getRequiereAsiento());
            pasajeroNode.put("requiereServicioEspecial", infante.getRequiereServicioEspecial());
        }
        root.set("pasajero", pasajeroNode);

        // Tarifa
        Tarifa tarifa = reserva.getTarifa();
        ObjectNode tarifaNode = objectMapper.createObjectNode();
        tarifaNode.put("idTarifa", tarifa.getIdTarifa());
        tarifaNode.put("importe", tarifa.getImporte().doubleValue());
        tarifaNode.put("moneda", tarifa.getMoneda());
        root.set("tarifa", tarifaNode);

        // Vuelos
        ArrayNode vuelosArray = objectMapper.createArrayNode();
        for (Vuelo vuelo : reserva.getVuelos()) {
            ObjectNode vueloNode = objectMapper.createObjectNode();
            vueloNode.put("idVuelo", vuelo.getIdVuelo());
            vueloNode.put("fechaHoraSalida", vuelo.getFechaHoraSalida().format(FORMATO_FECHA_HORA));
            if (vuelo.getOrigen() != null) {
                vueloNode.put("origen", vuelo.getOrigen());
            } else {
                vueloNode.putNull("origen");
            }
            if (vuelo.getDestino() != null) {
                vueloNode.put("destino", vuelo.getDestino());
            } else {
                vueloNode.putNull("destino");
            }
            vueloNode.put("completado", vuelo.getCompletado());
            if (vuelo.getFechaHoraLlegada() != null) {
                vueloNode.put("fechaHoraLlegada", vuelo.getFechaHoraLlegada().format(FORMATO_FECHA_HORA));
            } else {
                vueloNode.putNull("fechaHoraLlegada");
            }
            vuelosArray.add(vueloNode);
        }
        root.set("vuelos", vuelosArray);

        // Equipaje
        Equipaje equipaje = reserva.getEquipaje();
        if (equipaje != null) {
            ObjectNode equipajeNode = objectMapper.createObjectNode();
            equipajeNode.put("idEquipaje", equipaje.getIdEquipaje());
            if (equipaje.getCantidadPiezas() != null) {
                equipajeNode.put("cantidadPiezas", equipaje.getCantidadPiezas());
            } else {
                equipajeNode.putNull("cantidadPiezas");
            }
            if (equipaje.getPeso() != null) {
                equipajeNode.put("peso", equipaje.getPeso());
            } else {
                equipajeNode.putNull("peso");
            }
            if (equipaje.getUnidadPeso() != null) {
                equipajeNode.put("unidadPeso", equipaje.getUnidadPeso().name());
            } else {
                equipajeNode.putNull("unidadPeso");
            }
            root.set("equipaje", equipajeNode);
        } else {
            root.putNull("equipaje");
        }

        return root;
    }
}
