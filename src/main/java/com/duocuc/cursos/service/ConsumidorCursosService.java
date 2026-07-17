package com.duocuc.cursos.service;

import com.duocuc.cursos.config.RabbitMQConfig;
import com.duocuc.cursos.model.InscripcionProcesadaMQ;
import com.duocuc.cursos.repository.InscripcionMQRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Consumidor RabbitMQ — Semana 8.
 *
 * Escucha la Cola 1 (cursos-cola-inscripciones) y persiste cada guía
 * recibida en la tabla inscripciones_procesadas_mq de la base de datos.
 *
 * Si el procesamiento falla (excepción no controlada), RabbitMQ
 * reenvía automáticamente el mensaje a la Cola 2 (cursos-cola-errores)
 * gracias al Dead Letter Exchange configurado en RabbitMQConfig.
 *
 * También escucha la Cola 2 para registrar en logs los mensajes
 * de error para análisis y depuración posterior.
 */
@Service
public class ConsumidorCursosService {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorCursosService.class);

    private final InscripcionMQRepository repository;

    public ConsumidorCursosService(InscripcionMQRepository repository) {
        this.repository = repository;
    }

    /**
     * Consumidor de Cola 1: cursos-cola-inscripciones.
     * Procesa el mensaje y persiste en la tabla inscripciones_procesadas_mq.
     * Si lanza excepción → el mensaje va a cursos-cola-errores automáticamente.
     */
    @RabbitListener(queues = RabbitMQConfig.COLA_PRINCIPAL)
    public void procesarGuia(Map<String, Object> mensaje) {
        log.info("Mensaje recibido desde {}: codigoCurso={}",
                RabbitMQConfig.COLA_PRINCIPAL, mensaje.get("codigoCurso"));

        // Si codigoCurso es null (mensaje malformado), lanza excepción
        // → RabbitMQ detecta el fallo y redirige a cursos-cola-errores (DLQ)
        String codigoCurso = (String) mensaje.get("codigoCurso");
        if (codigoCurso == null) {
            throw new IllegalArgumentException(
                "Mensaje malformado: falta el campo 'codigoCurso'. "
                + "Redirigiendo a DLQ cursos-cola-errores.");
        }

        String instructor    = (String) mensaje.get("instructor");
        String estudiante     = (String) mensaje.get("estudiante");
        String tematica = (String) mensaje.get("tematica");
        String descripcion = (String) mensaje.get("descripcion");
        Double duracionHoras = mensaje.get("duracionHoras") != null
                ? Double.parseDouble(mensaje.get("duracionHoras").toString()) : null;
        String claveS3 = (String) mensaje.get("claveS3");

        LocalDate fechaInicio = null;
        if (mensaje.get("fechaInicio") != null) {
            fechaInicio = LocalDate.parse(mensaje.get("fechaInicio").toString());
        }

        InscripcionProcesadaMQ guiaMQ = new InscripcionProcesadaMQ(
                codigoCurso, instructor, estudiante,
                tematica, descripcion, duracionHoras,
                fechaInicio, claveS3
        );

        InscripcionProcesadaMQ guardada = repository.save(guiaMQ);
        log.info("Guía {} guardada en BD (inscripciones_procesadas_mq) con ID: {}",
                codigoCurso, guardada.getId());
    }

    /**
     * Consumidor de Cola 2: cursos-cola-errores (Dead Letter Queue).
     * Registra en logs los mensajes que fallaron en la Cola 1
     * para análisis y depuración posterior.
     */
    @RabbitListener(queues = RabbitMQConfig.COLA_ERRORES)
    public void procesarError(Map<String, Object> mensajeError) {
        log.error("Mensaje en COLA DE ERRORES (DLQ): codigoCurso={}, datos={}",
                mensajeError.get("codigoCurso"), mensajeError);
    }
}
