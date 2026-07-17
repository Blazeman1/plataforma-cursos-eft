package com.duocuc.cursos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ — Semana 8 (Sumativa).
 *
 * Cola 1: cursos-cola-inscripciones
 *   - Recibe todas las cursos en línea creadas (productor).
 *   - El consumidor lee de aquí y persiste en BD tabla inscripciones_procesadas_mq.
 *   - Si el procesamiento falla → el mensaje va automáticamente a Cola 2 (DLX).
 *
 * Cola 2: cursos-cola-errores (Dead Letter Queue)
 *   - Recibe los mensajes que fallaron en cursos-cola-inscripciones.
 *   - Actúa como buffer de errores para análisis y reintento posterior.
 *
 * Exchange principal: cursos-exchange (Direct)
 *   - Enruta mensajes a cursos-cola-inscripciones con routing key "cursos-key".
 *
 * Exchange DLX: cursos-dlx-exchange (Direct)
 *   - Recibe mensajes fallidos y los enruta a cursos-cola-errores.
 */
@Configuration
public class RabbitMQConfig {

    // Cola 1 — principal
    public static final String COLA_PRINCIPAL    = "cursos-cola-inscripciones";
    public static final String EXCHANGE_PRINCIPAL = "cursos-exchange";
    public static final String ROUTING_KEY        = "cursos-key";

    // Cola 2 — Dead Letter Queue (errores)
    public static final String COLA_ERRORES      = "cursos-cola-errores";
    public static final String DLX_EXCHANGE      = "cursos-dlx-exchange";
    public static final String DLX_ROUTING_KEY   = "cursos-error-key";

    // ── Cola 2: Dead Letter Queue (se declara primero porque Cola 1 la referencia) ──

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue colaErrores() {
        return QueueBuilder.durable(COLA_ERRORES).build();
    }

    @Bean
    public Binding dlxBinding(Queue colaErrores, DirectExchange dlxExchange) {
        return BindingBuilder.bind(colaErrores)
                .to(dlxExchange)
                .with(DLX_ROUTING_KEY);
    }

    // ── Cola 1: principal con DLX configurado ──

    @Bean
    public DirectExchange exchangePrincipal() {
        return new DirectExchange(EXCHANGE_PRINCIPAL);
    }

    /**
     * Cola principal con Dead Letter Exchange configurado.
     * Si el consumidor lanza una excepción no recuperable,
     * RabbitMQ reenvía el mensaje automáticamente a cursos-dlx-exchange
     * → cursos-cola-errores.
     */
    @Bean
    public Queue colaPrincipal() {
        return QueueBuilder.durable(COLA_PRINCIPAL)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindingPrincipal(Queue colaPrincipal, DirectExchange exchangePrincipal) {
        return BindingBuilder.bind(colaPrincipal)
                .to(exchangePrincipal)
                .with(ROUTING_KEY);
    }

    // ── Conversor JSON y RabbitTemplate ──

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Configura el contenedor de listeners con defaultRequeueRejected=false.
     *
     * Cuando el consumidor lanza una excepción no controlada, Spring AMQP
     * por defecto reencola el mensaje (requeue=true), causando un loop infinito.
     *
     * Con defaultRequeueRejected=false, al fallar el procesamiento el mensaje
     * es rechazado sin reencolarse, lo que activa el x-dead-letter-exchange
     * configurado en la Cola 1 y redirige el mensaje a cursos-cola-errores (DLQ).
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // Rechazar sin reencolar al fallar → activa DLX → mensaje va a DLQ
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
