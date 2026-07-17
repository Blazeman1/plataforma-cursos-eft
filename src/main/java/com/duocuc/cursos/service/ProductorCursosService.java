package com.duocuc.cursos.service;

import com.duocuc.cursos.config.RabbitMQConfig;
import com.duocuc.cursos.model.Curso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Productor RabbitMQ — Semana 8.
 *
 * Envía los datos de una curso en línea a la Cola 1 (cursos-cola-inscripciones)
 * a través del exchange cursos-exchange con routing key cursos-key.
 *
 * Si el consumidor falla al procesar el mensaje, RabbitMQ lo reenvía
 * automáticamente a la Cola 2 (cursos-cola-errores) mediante el DLX configurado.
 */
@Service
public class ProductorCursosService {

    private static final Logger log = LoggerFactory.getLogger(ProductorCursosService.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorCursosService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Envía los datos de la guía a la Cola 1.
     * Serializado automáticamente como JSON por Jackson2JsonMessageConverter.
     */
    public void enviarGuia(Curso guia) {
        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("codigoCurso",       guia.getNumeroGuia());
        mensaje.put("instructor",    guia.getTransportista());
        mensaje.put("estudiante",     guia.getDestinatario());
        mensaje.put("tematica", guia.getDireccionDestino());
        mensaje.put("descripcion", guia.getDescripcionCarga());
        mensaje.put("duracionHoras",           guia.getPesoKg());
        mensaje.put("fechaInicio",    guia.getFechaDespacho() != null
                ? guia.getFechaDespacho().toString() : null);
        mensaje.put("claveS3",          guia.getClaveS3());
        mensaje.put("estado",           guia.getEstado() != null
                ? guia.getEstado().name() : null);

        log.info("Enviando guía {} a la cola {}", guia.getNumeroGuia(), RabbitMQConfig.COLA_PRINCIPAL);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PRINCIPAL,
                RabbitMQConfig.ROUTING_KEY,
                mensaje
        );

        log.info("Guía {} enviada exitosamente a cursos-exchange -> cursos-cola-inscripciones",
                guia.getNumeroGuia());
    }
}
