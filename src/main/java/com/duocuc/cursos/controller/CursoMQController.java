package com.duocuc.cursos.controller;

import com.duocuc.cursos.config.RabbitMQConfig;
import com.duocuc.cursos.model.InscripcionProcesadaMQ;
import com.duocuc.cursos.repository.InscripcionMQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador del Consumidor RabbitMQ — Semana 8.
 *
 * GET  /api/cursos/procesadas-mq → lista guías procesadas por consumidor Cola 1
 * POST /api/cursos/test-error    → envía mensaje malformado para demostrar DLQ
 */
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoMQController {

    private final InscripcionMQRepository repository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * GET /api/cursos/procesadas-mq
     * Lista todas las guías procesadas por el consumidor de la Cola 1.
     * Requiere JWT con rol "admin".
     */
    @GetMapping("/procesadas-mq")
    public ResponseEntity<List<InscripcionProcesadaMQ>> listarGuiasProcesadas() {
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * POST /api/cursos/test-error
     * Endpoint de prueba para demostrar el funcionamiento de la DLQ (Cola 2).
     *
     * Envía un mensaje intencionalmente malformado a la Cola 1:
     * - Sin el campo "codigoCurso" que el consumidor requiere obligatoriamente.
     * - Sin el campo "fechaInicio" que el consumidor intenta parsear.
     *
     * Cuando el consumidor recibe este mensaje, lanza una NullPointerException
     * al intentar procesar los campos faltantes. RabbitMQ detecta el fallo y
     * redirige automáticamente el mensaje a cursos-cola-errores (DLQ) gracias
     * al x-dead-letter-exchange configurado en RabbitMQConfig.
     *
     * Para verificar: abrir RabbitMQ Management → Queues → cursos-cola-errores
     * → debería aparecer 1 mensaje con estado "Ready".
     *
     * Requiere JWT con rol "admin".
     */
    @PostMapping("/test-error")
    public ResponseEntity<Map<String, Object>> testDlq() {
        // Mensaje malformado: faltan campos obligatorios que el consumidor espera
        Map<String, Object> mensajeMalformado = new HashMap<>();
        mensajeMalformado.put("campo_invalido", "este mensaje no tiene codigoCurso ni fechaInicio");
        mensajeMalformado.put("test", true);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PRINCIPAL,
                RabbitMQConfig.ROUTING_KEY,
                mensajeMalformado
        );

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Mensaje malformado enviado a cursos-cola-inscripciones");
        response.put("explicacion", "El consumidor fallará al procesar este mensaje "
                + "y RabbitMQ lo redirigirá automáticamente a cursos-cola-errores (DLQ)");
        response.put("verificar", "Abrir http://44.215.15.32:15672 → Queues → "
                + "cursos-cola-errores → debe aparecer 1 mensaje Ready");

        return ResponseEntity.ok(response);
    }
}
