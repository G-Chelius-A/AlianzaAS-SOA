package com.api.alianza.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Servicio encargado de la validación formal de representaciones contra esquemas:
 * 1. Validación XML contra 'alianza.xsd' utilizando javax.xml.validation del JDK.
 * 2. Validación JSON contra 'reserva.schema.json' utilizando com.networknt:json-schema-validator (Draft 2020-12).
 *
 * DETALLES TÉCNICOS Y DOCUMENTACIÓN OBLIGATORIA:
 * - javax.xml.validation: Se instancia SchemaFactory con W3C_XML_SCHEMA_NS_URI. Se restringe el acceso
 *   a protocolos externos (ACCESS_EXTERNAL_DTD y ACCESS_EXTERNAL_SCHEMA) para blindar el validador contra XXE.
 * - ErrorHandler personalizado: Captura advertencias, errores y errores fatales recopilando la ubicación
 *   exacta (línea, columna y mensaje) en lugar de abortar en el primer fallo.
 * - JSON Schema Draft 2020-12: Captura todas las violaciones del schema reportando el JSON Pointer exacto
 *   y la regla vulnerada.
 */
@Service
public class ValidadorEsquemasService {

    private Schema xsdSchema;
    private JsonSchema jsonSchema;

    @PostConstruct
    public void inicializarEsquemas() {
        inicializarXsd();
        inicializarJsonSchema();
    }

    private void inicializarXsd() {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // Hardening de seguridad contra XXE en la carga del XSD
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            ClassPathResource resource = new ClassPathResource("alianza.xsd");
            try (InputStream is = resource.getInputStream()) {
                this.xsdSchema = factory.newSchema(new StreamSource(is));
            }
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Error crítico al cargar y compilar el esquema XSD 'alianza.xsd': " + e.getMessage(), e);
        }
    }

    private void inicializarJsonSchema() {
        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            ClassPathResource resource = new ClassPathResource("reserva.schema.json");
            try (InputStream is = resource.getInputStream()) {
                this.jsonSchema = factory.getSchema(is);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error crítico al cargar y compilar el JSON Schema 'reserva.schema.json': " + e.getMessage(), e);
        }
    }

    /**
     * Valida una representación XML contra el esquema alianza.xsd.
     *
     * @param xml Contenido XML a validar.
     * @return Lista de mensajes descriptivos de error. Si la lista está vacía, el XML es válido.
     */
    public List<String> validarXml(String xml) {
        List<String> errores = new ArrayList<>();
        if (xml == null || xml.trim().isEmpty()) {
            errores.add("El contenido XML a validar es nulo o vacío.");
            return errores;
        }

        try {
            Validator validator = xsdSchema.newValidator();
            // Hardening del validador XML contra XXE
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // Advertencias opcionales
                }

                @Override
                public void error(SAXParseException exception) {
                    errores.add(formatearSaxError("Error de validación XSD", exception));
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    errores.add(formatearSaxError("Error fatal XSD / XML mal formado", exception));
                }
            });

            validator.validate(new StreamSource(new StringReader(xml)));

        } catch (SAXException e) {
            if (errores.isEmpty()) {
                errores.add("Error de estructura XML: " + e.getMessage());
            }
        } catch (IOException e) {
            errores.add("Error de E/S al leer el XML: " + e.getMessage());
        }

        return errores;
    }

    private String formatearSaxError(String prefijo, SAXParseException e) {
        return String.format("%s en línea %d, columna %d: %s",
                prefijo, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
    }

    /**
     * Valida un árbol JsonNode contra el esquema reserva.schema.json.
     *
     * @param jsonNode Árbol JSON a validar.
     * @return Lista de mensajes descriptivos con JSON Pointer. Si la lista está vacía, es válido.
     */
    public List<String> validarJson(JsonNode jsonNode) {
        List<String> errores = new ArrayList<>();
        if (jsonNode == null || jsonNode.isNull()) {
            errores.add("El nodo JSON a validar es nulo.");
            return errores;
        }

        Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode);
        for (ValidationMessage vm : validationMessages) {
            String path = vm.getInstanceLocation() != null ? vm.getInstanceLocation().toString() : "";
            errores.add(String.format("Error en JSON Schema [ruta: '%s']: %s", path, vm.getMessage()));
        }

        return errores;
    }
}
