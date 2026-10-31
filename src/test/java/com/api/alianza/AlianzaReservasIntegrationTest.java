package com.api.alianza;

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
import com.api.alianza.repositories.AdultoRepository;
import com.api.alianza.repositories.PasajeroRepository;
import com.api.alianza.repositories.ReservaRepository;
import com.api.alianza.services.BusinessRuleValidationException;
import com.api.alianza.services.GeneradorRepresentacionCanonica;
import com.api.alianza.services.ResourceConflictException;
import com.api.alianza.services.ResourceNotFoundException;
import com.api.alianza.services.ReservaService;
import com.api.alianza.services.SchemaValidationException;
import com.api.alianza.services.ValidadorEsquemasService;
import com.api.alianza.services.transformador.TransformadorAeroNorte;
import com.api.alianza.services.transformador.TransformadorCaribeAir;
import com.api.alianza.services.transformador.TransformadorSurJet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite integral de pruebas para el sistema de reservas de la alianza Alas del Sur.
 * Cobertura obligatoria según especificaciones:
 * 1. Transformación, generación XML/JSON y validación contra AMBOS esquemas para las tres muestras.
 * 2. Persistencia y recuperación completa de relaciones JPA.
 * 3. Casos inválidos por esquema (XSD y JSON Schema) y códigos HTTP (400, 404, 409, 422).
 * 4. Reglas de tutoría: Menor/Infante sin tutor (422), tutor inexistente (404), tutor no adulto (422).
 * 5. Rollback atómico: operaciones fallidas no dejan registros huérfanos.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AlianzaReservasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PasajeroRepository pasajeroRepository;

    @Autowired
    private AdultoRepository adultoRepository;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private TransformadorAeroNorte transformadorAeroNorte;

    @Autowired
    private TransformadorSurJet transformadorSurJet;

    @Autowired
    private TransformadorCaribeAir transformadorCaribeAir;

    @Autowired
    private GeneradorRepresentacionCanonica generadorRepresentacionCanonica;

    @Autowired
    private ValidadorEsquemasService validadorEsquemasService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        pasajeroRepository.deleteAll();
    }

    // =========================================================================
    // BLOQUE 1: LAS TRES MUESTRAS OFICIALES (AeroNorte, SurJet, CaribeAir)
    // =========================================================================
    @Nested
    @DisplayName("1. Pruebas de las Tres Muestras Oficiales (Ana García)")
    class TresMuestrasTests {

        private final String AERONORTE_XML =
                "<reserva>\n" +
                "  <pnr>QX7Z2A</pnr>\n" +
                "  <pasajero>GARCIA/ANA</pasajero>\n" +
                "  <fecha>03/04/2026</fecha>\n" +
                "  <tarifa>4500</tarifa>\n" +
                "  <equipaje>23</equipaje>\n" +
                "  <menor>N</menor>\n" +
                "</reserva>";

        private final String SURJET_JSON =
                "{\n" +
                "  \"booking\": \"SJ-88412\",\n" +
                "  \"firstName\": \"Ana\",\n" +
                "  \"lastName\": \"García\",\n" +
                "  \"flightDate\": \"2026-04-03T08:30:00-06:00\",\n" +
                "  \"fare\": { \"amount\": 260, \"currency\": \"USD\" },\n" +
                "  \"bagsLb\": 50,\n" +
                "  \"age\": 34\n" +
                "}";

        private final String CARIBEAIR_JSON =
                "{\n" +
                "  \"id\": 99812,\n" +
                "  \"nombre\": \"Ana Garcia\",\n" +
                "  \"fecha\": \"04/03/2026\",\n" +
                "  \"precio\": \"4,500.00\",\n" +
                "  \"maleta\": \"1 pieza\",\n" +
                "  \"tipoPasajero\": \"ADT\"\n" +
                "}";

        @Test
        @DisplayName("Muestra AeroNorte: transformación, generación XML/JSON y validación contra ambos esquemas")
        void testMuestraAeroNorte() {
            Reserva reserva = transformadorAeroNorte.transformar(AERONORTE_XML, null);
            assertNotNull(reserva);
            assertEquals("QX7Z2A", reserva.getOrigenPNR());
            assertEquals(AerolineaOrigen.AERONORTE, reserva.getAerolineaOrigen());
            assertEquals("Ana", reserva.getPasajero().getNombres());
            assertEquals("Garcia", reserva.getPasajero().getApellidos());
            assertEquals(new BigDecimal("4500.00"), reserva.getTarifa().getImporte());
            assertEquals("MXN", reserva.getTarifa().getMoneda());
            assertEquals(23, reserva.getEquipaje().getPeso());
            assertEquals(UnidadPeso.KG, reserva.getEquipaje().getUnidadPeso());
            assertNull(reserva.getEquipaje().getCantidadPiezas());

            // Generación y validación de representaciones canónicas
            String xml = generadorRepresentacionCanonica.generarXml(reserva);
            ObjectNode json = generadorRepresentacionCanonica.generarJson(reserva);

            List<String> erroresXsd = validadorEsquemasService.validarXml(xml);
            List<String> erroresJson = validadorEsquemasService.validarJson(json);

            assertTrue(erroresXsd.isEmpty(), "El XML canónico de AeroNorte debe ser válido contra alianza.xsd: " + erroresXsd);
            assertTrue(erroresJson.isEmpty(), "El JSON canónico de AeroNorte debe ser válido contra reserva.schema.json: " + erroresJson);
        }

        @Test
        @DisplayName("Muestra SurJet: transformación con huso horario, estimación de fechaNacimiento y validación dual")
        void testMuestraSurJet() throws Exception {
            JsonNode jsonNode = objectMapper.readTree(SURJET_JSON);
            Reserva reserva = transformadorSurJet.transformar(jsonNode, null);
            assertNotNull(reserva);
            assertEquals("SJ-88412", reserva.getOrigenPNR());
            assertEquals(AerolineaOrigen.SURJET, reserva.getAerolineaOrigen());
            assertEquals("Ana", reserva.getPasajero().getNombres());
            assertEquals("García", reserva.getPasajero().getApellidos());
            assertEquals(new BigDecimal("260.00"), reserva.getTarifa().getImporte());
            assertEquals("USD", reserva.getTarifa().getMoneda());
            assertEquals(50, reserva.getEquipaje().getPeso());
            assertEquals(UnidadPeso.LB, reserva.getEquipaje().getUnidadPeso());
            assertNull(reserva.getEquipaje().getCantidadPiezas());
            // Verificación de fecha nacimiento estimada (2026 - 34 = 1992)
            assertEquals(LocalDate.of(1992, 4, 3), reserva.getPasajero().getFechaNacimiento());

            String xml = generadorRepresentacionCanonica.generarXml(reserva);
            ObjectNode json = generadorRepresentacionCanonica.generarJson(reserva);

            List<String> erroresXsd = validadorEsquemasService.validarXml(xml);
            List<String> erroresJson = validadorEsquemasService.validarJson(json);

            assertTrue(erroresXsd.isEmpty(), "El XML canónico de SurJet debe ser válido contra alianza.xsd: " + erroresXsd);
            assertTrue(erroresJson.isEmpty(), "El JSON canónico de SurJet debe ser válido contra reserva.schema.json: " + erroresJson);
        }

        @Test
        @DisplayName("Muestra CaribeAir: transformación, extracción de piezas, heurística de nombres y validación dual")
        void testMuestraCaribeAir() throws Exception {
            JsonNode jsonNode = objectMapper.readTree(CARIBEAIR_JSON);
            Reserva reserva = transformadorCaribeAir.transformar(jsonNode, null);
            assertNotNull(reserva);
            assertEquals("99812", reserva.getOrigenPNR());
            assertEquals(AerolineaOrigen.CARIBEAIR, reserva.getAerolineaOrigen());
            assertEquals("Ana", reserva.getPasajero().getNombres());
            assertEquals("Garcia", reserva.getPasajero().getApellidos());
            assertEquals(new BigDecimal("4500.00"), reserva.getTarifa().getImporte());
            assertEquals("MXN", reserva.getTarifa().getMoneda());
            assertEquals(1, reserva.getEquipaje().getCantidadPiezas());
            assertNull(reserva.getEquipaje().getPeso());
            assertNull(reserva.getEquipaje().getUnidadPeso());

            String xml = generadorRepresentacionCanonica.generarXml(reserva);
            ObjectNode json = generadorRepresentacionCanonica.generarJson(reserva);

            List<String> erroresXsd = validadorEsquemasService.validarXml(xml);
            List<String> erroresJson = validadorEsquemasService.validarJson(json);

            assertTrue(erroresXsd.isEmpty(), "El XML canónico de CaribeAir debe ser válido contra alianza.xsd: " + erroresXsd);
            assertTrue(erroresJson.isEmpty(), "El JSON canónico de CaribeAir debe ser válido contra reserva.schema.json: " + erroresJson);
        }

        @Test
        @DisplayName("Procesamiento REST completo vía endpoints para las 3 aerolíneas (POST 201 Created)")
        void testEndpointsCreacionTresMuestras() throws Exception {
            // 1. AeroNorte
            String payloadAeroNorte = objectMapper.writeValueAsString(Map.of("xml", AERONORTE_XML));
            mockMvc.perform(post("/api/v1/reservas/aeronorte")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payloadAeroNorte))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.origenPNR").value("QX7Z2A"))
                    .andExpect(jsonPath("$.aerolineaOrigen").value("AERONORTE"))
                    .andExpect(jsonPath("$.pasajero.tipoPasajero").value("ADULTO"))
                    .andExpect(jsonPath("$.pasajero.nombres").value("Ana"));

            // 2. SurJet
            mockMvc.perform(post("/api/v1/reservas/surjet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SURJET_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.origenPNR").value("SJ-88412"))
                    .andExpect(jsonPath("$.aerolineaOrigen").value("SURJET"))
                    .andExpect(jsonPath("$.tarifa.moneda").value("USD"));

            // 3. CaribeAir
            mockMvc.perform(post("/api/v1/reservas/caribeair")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARIBEAIR_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.origenPNR").value("99812"))
                    .andExpect(jsonPath("$.aerolineaOrigen").value("CARIBEAIR"))
                    .andExpect(jsonPath("$.equipaje.cantidadPiezas").value(1));

            assertEquals(3, reservaRepository.count(), "Deben haberse persistido exactamente 3 reservas.");
        }
    }

    // =========================================================================
    // BLOQUE 2: PERSISTENCIA Y RECUPERACIÓN DE RELACIONES JPA
    // =========================================================================
    @Nested
    @DisplayName("2. Persistencia y Recuperación de Relaciones")
    class RelacionesPersistenciaTests {

        @Test
        @DisplayName("Recuperación de relaciones completas: Pasajero, Reserva, Tarifa, Vuelos y Equipaje")
        void testRecuperacionRelacionesCompletas() {
            Adulto adulto = new Adulto();
            adulto.setIdPasajero(UUID.randomUUID().toString());
            adulto.setNombres("Carlos");
            adulto.setApellidos("Perez");
            adulto.setDocumentoIdentidad("DOC-7788");
            adulto.setTelefonoContacto("+5215512345678");

            Reserva reserva = new Reserva();
            reserva.setIdReserva(UUID.randomUUID().toString());
            reserva.setOrigenPNR("TEST01");
            reserva.setAerolineaOrigen(AerolineaOrigen.SURJET);
            reserva.setPasajero(adulto);

            Tarifa tarifa = new Tarifa();
            tarifa.setIdTarifa(UUID.randomUUID().toString());
            tarifa.setImporte(new BigDecimal("180.50"));
            tarifa.setMoneda("USD");
            reserva.setTarifa(tarifa);

            Vuelo v1 = new Vuelo();
            v1.setIdVuelo(UUID.randomUUID().toString());
            v1.setFechaHoraSalida(LocalDateTime.of(2026, 5, 10, 14, 0, 0));
            v1.setOrigen("MEX");
            v1.setDestino("CUN");
            v1.setCompletado(false);
            reserva.addVuelo(v1);

            Equipaje eq = new Equipaje();
            eq.setIdEquipaje(UUID.randomUUID().toString());
            eq.setPeso(20);
            eq.setUnidadPeso(UnidadPeso.KG);
            reserva.setEquipaje(eq);

            reservaRepository.save(reserva);

            Reserva recuperada = reservaRepository.findById(reserva.getIdReserva()).orElseThrow();
            assertNotNull(recuperada);
            assertEquals("Carlos", recuperada.getPasajero().getNombres());
            assertEquals("DOC-7788", ((Adulto) recuperada.getPasajero()).getDocumentoIdentidad());
            assertEquals(new BigDecimal("180.50"), recuperada.getTarifa().getImporte());
            assertEquals(1, recuperada.getVuelos().size());
            assertEquals("MEX", recuperada.getVuelos().get(0).getOrigen());
            assertNotNull(recuperada.getEquipaje());
            assertEquals(20, recuperada.getEquipaje().getPeso());
        }

        @Test
        @DisplayName("Eliminación de reserva no elimina al Pasajero ni al Tutor")
        void testEliminacionReservaNoEliminaPasajero() throws Exception {
            Adulto adulto = new Adulto();
            adulto.setIdPasajero(UUID.randomUUID().toString());
            adulto.setNombres("Mariana");
            adulto.setApellidos("Lopez");
            adultoRepository.save(adulto);

            Reserva reserva = new Reserva();
            String idReserva = UUID.randomUUID().toString();
            reserva.setIdReserva(idReserva);
            reserva.setOrigenPNR("DEL001");
            reserva.setAerolineaOrigen(AerolineaOrigen.CARIBEAIR);
            reserva.setPasajero(adulto);

            Tarifa tarifa = new Tarifa();
            tarifa.setIdTarifa(UUID.randomUUID().toString());
            tarifa.setImporte(new BigDecimal("100.00"));
            tarifa.setMoneda("MXN");
            reserva.setTarifa(tarifa);

            Vuelo v = new Vuelo();
            v.setIdVuelo(UUID.randomUUID().toString());
            v.setFechaHoraSalida(LocalDateTime.of(2026, 6, 1, 10, 0));
            reserva.addVuelo(v);

            reservaRepository.save(reserva);

            mockMvc.perform(delete("/api/v1/reservas/" + idReserva))
                    .andExpect(status().isOk());

            assertFalse(reservaRepository.existsById(idReserva), "La reserva debió eliminarse");
            assertTrue(pasajeroRepository.existsById(adulto.getIdPasajero()), "El pasajero NO debe ser eliminado");
        }
    }

    // =========================================================================
    // BLOQUE 3: CASOS INVÁLIDOS POR ESQUEMA Y CONVENCIÓN (400, 404, 409, 422)
    // =========================================================================
    @Nested
    @DisplayName("3. Casos Inválidos y Códigos HTTP")
    class CasosInvalidosTests {

        @Test
        @DisplayName("400 Bad Request: Cuerpo JSON mal formado")
        void test400BadRequestBody() throws Exception {
            mockMvc.perform(post("/api/v1/reservas/surjet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ booking: no-es-json-valido }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Cuerpo JSON Mal Formado"));
        }

        @Test
        @DisplayName("404 Not Found: Reserva o Pasajero inexistente")
        void test404RecursoInexistente() throws Exception {
            String fakeId = UUID.randomUUID().toString();
            mockMvc.perform(get("/api/v1/reservas/" + fakeId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            mockMvc.perform(get("/api/v1/pasajeros/" + fakeId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("409 Conflict: Duplicación de (aerolineaOrigen, origenPNR)")
        void test409DuplicacionPNR() throws Exception {
            String payload = objectMapper.writeValueAsString(Map.of("xml",
                    "<reserva><pnr>QX7Z2A</pnr><pasajero>GARCIA/ANA</pasajero><fecha>03/04/2026</fecha><tarifa>4500</tarifa><equipaje>23</equipaje><menor>N</menor></reserva>"));

            // Primer registro exitoso
            mockMvc.perform(post("/api/v1/reservas/aeronorte")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated());

            // Segundo registro con mismo PNR y aerolínea -> 409
            mockMvc.perform(post("/api/v1/reservas/aeronorte")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflicto de Integridad"));
        }

        @Test
        @DisplayName("422 Unprocessable Entity: Violación de convención de origen (PNR != 6 caracteres en AeroNorte)")
        void test422AeroNortePnrInvalido() throws Exception {
            String xmlInvalido = "<reserva><pnr>123</pnr><pasajero>GARCIA/ANA</pasajero><fecha>03/04/2026</fecha><tarifa>4500</tarifa><equipaje>23</equipaje><menor>N</menor></reserva>";
            String payload = objectMapper.writeValueAsString(Map.of("xml", xmlInvalido));

            mockMvc.perform(post("/api/v1/reservas/aeronorte")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }

        @Test
        @DisplayName("422 Unprocessable Entity: Violación de convención SurJet (booking no cumple SJ-\\d{5})")
        void test422SurJetBookingInvalido() throws Exception {
            String jsonInvalido = "{\"booking\":\"INVALIDO-123\",\"firstName\":\"Ana\",\"lastName\":\"Garcia\",\"flightDate\":\"2026-04-03T08:30:00-06:00\",\"fare\":{\"amount\":260,\"currency\":\"USD\"},\"age\":30}";

            mockMvc.perform(post("/api/v1/reservas/surjet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonInvalido))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }

        @Test
        @DisplayName("Validación directa XSD con errores descriptivos (POST /api/v1/validaciones/xml)")
        void testValidacionXsdDirectaConErrores() throws Exception {
            // XML con elemento desconocido o importe con más de 2 decimales
            String xmlInvalido =
                    "<reserva xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                    "<idReserva>no-es-uuid</idReserva>" +
                    "<origenPNR>123</origenPNR>" +
                    "<aerolineaOrigen>AEROLINEA_FANTASMA</aerolineaOrigen>" +
                    "</reserva>";

            mockMvc.perform(post("/api/v1/validaciones/xml")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("xml", xmlInvalido))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valido").value(false))
                    .andExpect(jsonPath("$.errores", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Validación directa JSON Schema con errores descriptivos (POST /api/v1/validaciones/json)")
        void testValidacionJsonSchemaDirectaConErrores() throws Exception {
            // JSON sin campos requeridos y con UUID inválido
            String jsonInvalido = "{\"idReserva\":\"invalido\",\"origenPNR\":\"123\"}";

            mockMvc.perform(post("/api/v1/validaciones/json")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonInvalido))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valido").value(false))
                    .andExpect(jsonPath("$.errores", hasSize(greaterThan(0))));
        }
    }

    // =========================================================================
    // BLOQUE 4: REGLAS DE TUTORÍA
    // =========================================================================
    @Nested
    @DisplayName("4. Pruebas de Reglas de Tutoría")
    class TutoriaTests {

        @Test
        @DisplayName("422 si Menor o Infante no envía idTutor")
        void testMenorSinTutor() throws Exception {
            // AeroNorte con menor = 'S' sin parámetro ?idTutor
            String xmlMenor = "<reserva><pnr>QX7Z2S</pnr><pasajero>GARCIA/PEDRO</pasajero><fecha>03/04/2026</fecha><tarifa>2000</tarifa><equipaje>10</equipaje><menor>S</menor></reserva>";
            String payload = objectMapper.writeValueAsString(Map.of("xml", xmlMenor));

            mockMvc.perform(post("/api/v1/reservas/aeronorte")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("tutor")));
        }

        @Test
        @DisplayName("404 si el idTutor no existe en el sistema")
        void testTutorInexistente() throws Exception {
            String fakeTutorId = UUID.randomUUID().toString();
            String xmlMenor = "<reserva><pnr>QX7Z2M</pnr><pasajero>GARCIA/PEDRO</pasajero><fecha>03/04/2026</fecha><tarifa>2000</tarifa><equipaje>10</equipaje><menor>S</menor></reserva>";
            String payload = objectMapper.writeValueAsString(Map.of("xml", xmlMenor));

            mockMvc.perform(post("/api/v1/reservas/aeronorte?idTutor=" + fakeTutorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("no existe en el sistema")));
        }

        @Test
        @DisplayName("422 si el tutor referenciado existe pero NO es un Adulto (es otro Menor)")
        void testTutorNoEsAdulto() throws Exception {
            // 1. Crear un tutor adulto legítimo para el primer menor
            Adulto adultoTutor = new Adulto();
            adultoTutor.setIdPasajero(UUID.randomUUID().toString());
            adultoTutor.setNombres("Tutor");
            adultoTutor.setApellidos("Mayor");
            adultoRepository.save(adultoTutor);

            // 2. Crear un menor en base de datos
            Menor primerMenor = new Menor();
            primerMenor.setIdPasajero(UUID.randomUUID().toString());
            primerMenor.setNombres("Menor");
            primerMenor.setApellidos("Uno");
            primerMenor.setIdTutor(adultoTutor.getIdPasajero());
            primerMenor.setRequiereAcompanante(true);
            primerMenor.setAutorizacionViaje(true);
            pasajeroRepository.save(primerMenor);

            // 3. Intentar registrar un nuevo menor asignando como tutor al menor creado
            String xmlSegundoMenor = "<reserva><pnr>QX7Z2K</pnr><pasajero>GARCIA/LUCIA</pasajero><fecha>03/04/2026</fecha><tarifa>2000</tarifa><equipaje>10</equipaje><menor>S</menor></reserva>";
            String payload = objectMapper.writeValueAsString(Map.of("xml", xmlSegundoMenor));

            mockMvc.perform(post("/api/v1/reservas/aeronorte?idTutor=" + primerMenor.getIdPasajero())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("no es un Adulto válido")));
        }

        @Test
        @DisplayName("Consulta de tutelados: 404 si tutor no existe, 422 si no es adulto, 200 con lista si es adulto")
        void testConsultaTutelados() throws Exception {
            // Tutor inexistente -> 404
            mockMvc.perform(get("/api/v1/pasajeros/" + UUID.randomUUID() + "/tutelados"))
                    .andExpect(status().isNotFound());

            // Crear tutor adulto
            Adulto tutor = new Adulto();
            String idTutor = UUID.randomUUID().toString();
            tutor.setIdPasajero(idTutor);
            tutor.setNombres("Roberto");
            tutor.setApellidos("Gomez");
            adultoRepository.save(tutor);

            // Registrar menor y asociarlo
            String caribeMenor = "{\"id\":77112,\"nombre\":\"Luis Gomez\",\"fecha\":\"04/03/2026\",\"precio\":\"3,000.00\",\"tipoPasajero\":\"CHD\"}";
            mockMvc.perform(post("/api/v1/reservas/caribeair?idTutor=" + idTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(caribeMenor))
                    .andExpect(status().isCreated());

            // Consultar tutelados -> 200 con lista de 1 menor
            mockMvc.perform(get("/api/v1/pasajeros/" + idTutor + "/tutelados"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTutelados").value(1))
                    .andExpect(jsonPath("$.menores", hasSize(1)))
                    .andExpect(jsonPath("$.menores[0].nombres").value("Luis"));
        }
    }

    // =========================================================================
    // BLOQUE 5: ROLLBACK TRANSACCIONAL
    // =========================================================================
    @Nested
    @DisplayName("5. Pruebas de Transaccionalidad y Rollback")
    class RollbackTests {

        @Test
        @DisplayName("Una operación fallida durante el procesamiento no deja registros parciales (rollback)")
        void testRollbackEnOperacionFallida() {
            long reservasAntes = reservaRepository.count();
            long pasajerosAntes = pasajeroRepository.count();

            // Enviamos un menor con un tutor inexistente
            // La entidad Reserva y Pasajero se crean en memoria durante la transformación,
            // pero la validación de tutoría en el servicio debe fallar con 404 antes de comprometer la transacción.
            String fakeTutor = UUID.randomUUID().toString();
            String xmlMenor = "<reserva><pnr>QX7Z99</pnr><pasajero>LOPEZ/DIEGO</pasajero><fecha>03/04/2026</fecha><tarifa>2000</tarifa><equipaje>10</equipaje><menor>S</menor></reserva>";

            assertThrows(ResourceNotFoundException.class, () -> {
                reservaService.procesarReservaAeroNorte(xmlMenor, fakeTutor);
            });

            // Verificar que la base de datos se mantiene intacta
            assertEquals(reservasAntes, reservaRepository.count(), "No deben haberse persistido reservas parciales.");
            assertEquals(pasajerosAntes, pasajeroRepository.count(), "No deben haberse persistido pasajeros huérfanos.");
        }
    }
}
