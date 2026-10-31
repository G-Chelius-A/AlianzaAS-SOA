package com.api.alianza.services;

import com.api.alianza.entities.Adulto;
import com.api.alianza.entities.AerolineaOrigen;
import com.api.alianza.entities.Infante;
import com.api.alianza.entities.Menor;
import com.api.alianza.entities.Pasajero;
import com.api.alianza.entities.Reserva;
import com.api.alianza.repositories.AdultoRepository;
import com.api.alianza.repositories.InfanteRepository;
import com.api.alianza.repositories.MenorRepository;
import com.api.alianza.repositories.PasajeroRepository;
import com.api.alianza.repositories.ReservaRepository;
import com.api.alianza.services.transformador.TransformadorAeroNorte;
import com.api.alianza.services.transformador.TransformadorCaribeAir;
import com.api.alianza.services.transformador.TransformadorSurJet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio central de gestión y procesamiento de reservas para la alianza Alas del Sur.
 *
 * ORDEN ESTRICTO DEL FLUJO DE PROCESAMIENTO:
 * 1. Recibir la entrada original.
 * 2. Validar estructura y convenciones de origen (400 si mal formada; 422 si incumple convenciones).
 * 3. Transformar al modelo canónico (entidades en memoria, sin persistir).
 * 4. Generar representación XML canónica.
 * 5. Generar representación JSON canónica.
 * 6. Validar ambas contra sus respectivos esquemas formales (XSD y JSON Schema). Si alguna falla, 422 con errores.
 * 7. Persistir la reserva y sus relaciones (@Transactional, validando duplicidad 409 y reglas de tutoría 422/404).
 * 8. Devolver la respuesta JSON canónica.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final PasajeroRepository pasajeroRepository;
    private final AdultoRepository adultoRepository;
    private final MenorRepository menorRepository;
    private final InfanteRepository infanteRepository;

    private final TransformadorAeroNorte transformadorAeroNorte;
    private final TransformadorSurJet transformadorSurJet;
    private final TransformadorCaribeAir transformadorCaribeAir;
    private final GeneradorRepresentacionCanonica generadorRepresentacionCanonica;
    private final ValidadorEsquemasService validadorEsquemasService;

    public ReservaService(
            ReservaRepository reservaRepository,
            PasajeroRepository pasajeroRepository,
            AdultoRepository adultoRepository,
            MenorRepository menorRepository,
            InfanteRepository infanteRepository,
            TransformadorAeroNorte transformadorAeroNorte,
            TransformadorSurJet transformadorSurJet,
            TransformadorCaribeAir transformadorCaribeAir,
            GeneradorRepresentacionCanonica generadorRepresentacionCanonica,
            ValidadorEsquemasService validadorEsquemasService) {
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.adultoRepository = adultoRepository;
        this.menorRepository = menorRepository;
        this.infanteRepository = infanteRepository;
        this.transformadorAeroNorte = transformadorAeroNorte;
        this.transformadorSurJet = transformadorSurJet;
        this.transformadorCaribeAir = transformadorCaribeAir;
        this.generadorRepresentacionCanonica = generadorRepresentacionCanonica;
        this.validadorEsquemasService = validadorEsquemasService;
    }

    /**
     * Procesa una reserva proveniente de AeroNorte recibida en {"xml": "<reserva>..."}.
     */
    @Transactional
    public ObjectNode procesarReservaAeroNorte(String xmlPayload, String idTutor) {
        Reserva reserva = transformadorAeroNorte.transformar(xmlPayload, idTutor);
        return ejecutarPipelineValidacionYPersistencia(reserva);
    }

    /**
     * Procesa una reserva proveniente de SurJet en formato JSON nativo.
     */
    @Transactional
    public ObjectNode procesarReservaSurJet(JsonNode jsonPayload, String idTutor) {
        Reserva reserva = transformadorSurJet.transformar(jsonPayload, idTutor);
        return ejecutarPipelineValidacionYPersistencia(reserva);
    }

    /**
     * Procesa una reserva proveniente de CaribeAir en formato JSON nativo.
     */
    @Transactional
    public ObjectNode procesarReservaCaribeAir(JsonNode jsonPayload, String idTutor) {
        Reserva reserva = transformadorCaribeAir.transformar(jsonPayload, idTutor);
        return ejecutarPipelineValidacionYPersistencia(reserva);
    }

    /**
     * Pipeline obligatorio:
     * - Genera XML y JSON canónicos en memoria.
     * - Valida contra XSD y JSON Schema antes de tocar la base de datos.
     * - Valida tutoría y unicidad compuesta (aerolineaOrigen, origenPNR).
     * - Persiste entidad completa en una transacción atómica.
     * - Retorna la representación JSON canónica.
     */
    private ObjectNode ejecutarPipelineValidacionYPersistencia(Reserva reserva) {
        // 1. Generar XML canónico
        String xmlCanonico = generadorRepresentacionCanonica.generarXml(reserva);

        // 2. Generar JSON canónico
        ObjectNode jsonCanonico = generadorRepresentacionCanonica.generarJson(reserva);

        // 3. Validar XML contra XSD
        List<String> erroresXml = validadorEsquemasService.validarXml(xmlCanonico);

        // 4. Validar JSON contra JSON Schema
        List<String> erroresJson = validadorEsquemasService.validarJson(jsonCanonico);

        if (!erroresXml.isEmpty() || !erroresJson.isEmpty()) {
            List<String> todosErrores = new ArrayList<>(erroresXml);
            todosErrores.addAll(erroresJson);
            throw new SchemaValidationException("La representación canónica no supera la validación formal de esquemas.", todosErrores);
        }

        // 5. Validar unicidad compuesta (aerolineaOrigen, origenPNR) -> 409
        if (reservaRepository.existsByAerolineaOrigenAndOrigenPNR(reserva.getAerolineaOrigen(), reserva.getOrigenPNR())) {
            throw new ResourceConflictException(String.format(
                    "Ya existe una reserva registrada para la aerolínea '%s' con el identificador de origen PNR '%s'.",
                    reserva.getAerolineaOrigen(), reserva.getOrigenPNR()));
        }

        // 6. Validar reglas de tutoría para Menor e Infante en la capa de servicio
        validarReglasDeTutoria(reserva);

        // 7. Persistir en base de datos
        // Aseguramos el enlace bidireccional
        reservaRepository.save(reserva);

        return jsonCanonico;
    }

    /**
     * Validación obligatoria de negocio para tutoría:
     * - Únicamente un Adulto puede ser tutor.
     * - Menor/Infante nunca pueden ser tutores.
     * - Si el tutor no existe -> 404.
     * - Si existe pero no es Adulto -> 422.
     * - Comprobación de "misma reserva": dado que una Reserva alberga un único Pasajero en este modelo,
     *   se valida la condición comprobable más cercana: que el tutor posea al menos una reserva en la
     *   misma aerolineaOrigen (si aún no tiene reserva en dicha aerolínea, se permite pero se deja registro/validación).
     */
    private void validarReglasDeTutoria(Reserva reserva) {
        Pasajero pasajero = reserva.getPasajero();
        String idTutor = null;

        if (pasajero instanceof Menor menor) {
            idTutor = menor.getIdTutor();
        } else if (pasajero instanceof Infante infante) {
            idTutor = infante.getIdTutor();
        }

        if (idTutor != null) {
            final String tutorId = idTutor;
            // Verificar existencia del tutor
            Pasajero tutor = pasajeroRepository.findById(tutorId)
                    .orElseThrow(() -> new ResourceNotFoundException("El tutor especificado con id '" + tutorId + "' no existe en el sistema."));

            // Verificar que sea instancia de Adulto
            if (!(tutor instanceof Adulto)) {
                throw new BusinessRuleValidationException("El tutor con id '" + tutorId + "' no es un Adulto válido. Un menor o infante no puede ser tutor.");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ObjectNode> listarReservas(String aerolineaStr) {
        List<Reserva> reservas;
        if (aerolineaStr != null && !aerolineaStr.trim().isEmpty()) {
            try {
                AerolineaOrigen ao = AerolineaOrigen.valueOf(aerolineaStr.trim().toUpperCase());
                reservas = reservaRepository.findByAerolineaOrigen(ao);
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleValidationException("Aerolínea inválida para filtro: " + aerolineaStr + ". Valores permitidos: AERONORTE, SURJET, CARIBEAIR");
            }
        } else {
            reservas = reservaRepository.findAll();
        }

        List<ObjectNode> resultado = new ArrayList<>();
        for (Reserva r : reservas) {
            resultado.add(generadorRepresentacionCanonica.generarJson(r));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public ObjectNode obtenerReservaPorId(String idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + idReserva));
        return generadorRepresentacionCanonica.generarJson(reserva);
    }

    @Transactional(readOnly = true)
    public String obtenerXmlCanonicoPorId(String idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + idReserva));
        return generadorRepresentacionCanonica.generarXml(reserva);
    }

    @Transactional(readOnly = true)
    public Pasajero obtenerPasajeroPorId(String idPasajero) {
        return pasajeroRepository.findById(idPasajero)
                .orElseThrow(() -> new ResourceNotFoundException("Pasajero no encontrado con id: " + idPasajero));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listarTuteladosPorIdTutor(String idTutor) {
        Pasajero pasajero = pasajeroRepository.findById(idTutor)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún pasajero con el id: " + idTutor));

        if (!(pasajero instanceof Adulto adulto)) {
            throw new BusinessRuleValidationException("El id '" + idTutor + "' no corresponde a un Adulto. Solo los adultos pueden tener tutelados.");
        }

        List<Menor> menores = menorRepository.findByIdTutor(idTutor);
        List<Infante> infantes = infanteRepository.findByIdTutor(idTutor);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("idTutor", idTutor);
        respuesta.put("tutor", adulto);
        respuesta.put("menores", menores);
        respuesta.put("infantes", infantes);
        respuesta.put("totalTutelados", menores.size() + infantes.size());
        return respuesta;
    }

    @Transactional
    public void eliminarReserva(String idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada para eliminar con id: " + idReserva));

        // Desvincular pasajero para que cascade ALL no elimine al pasajero ni a su tutor
        reserva.setPasajero(null);
        reservaRepository.delete(reserva);
    }
}
