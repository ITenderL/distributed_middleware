package com.itender.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: ITender
 * @CreateTime: 2021-12-24 16:22
 */
@Configuration
public class RabbitConfig {
    private static final String QUEUE_NAME = "test_queue";
    private static final String EXCHANGE_NAME = "test_exchange";
    private static final String CONFIRM_QUEUE_NAME = "confirm_queue";
    private static final String CONFIRM_EXCHANGE_NAME = "confirm_exchange";

    @Autowired
    private CachingConnectionFactory cachingConnectionFactory;

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cachingConnectionFactory) {
        return new RabbitTemplate(cachingConnectionFactory);
    }

    /*****************************延时队列************************************/
    @Bean
    public Queue delayQueue() {
        return QueueBuilder
                .durable("delayQueue")
                // delayQueue数据会过期，过期后会进入到死信队列，死信队列数据绑定到其他交换机
                .withArgument("x-dead-letter-exchange", "delayExchange")
                .withArgument("x-dead-letter-queue", "realQueue")
                .build();
    }

    @Bean
    public Queue realQueue() {
        return new Queue("realQueue");
    }

    @Bean
    public Exchange delayExchange() {
        return new DirectExchange("delayExchange");
    }

    @Bean
    public Binding delayAndRealBinding(Exchange delayExchange, Queue realQueue) {
        return BindingBuilder.bind(realQueue).to(delayExchange).with("delayQueue").noargs();
    }

    /*******************************测试队列********************************/
    @Bean
    public Queue testQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Exchange testExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Binding testBinding(Queue testQueue, Exchange testExchange) {
        return BindingBuilder.bind(testQueue).to(testExchange).with("testKey").noargs();
    }

    /**************************************确认队列**********************************/
    @Bean
    public Queue confirmQueue() {
        return QueueBuilder.durable(CONFIRM_QUEUE_NAME).build();
    }

    @Bean
    public Exchange confirmExchange() {
        return ExchangeBuilder.directExchange(CONFIRM_EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Binding confirmBinding(Queue confirmQueue, Exchange confirmExchange) {
        return BindingBuilder.bind(confirmQueue).to(confirmExchange).with("confirmKey").noargs();
    }

}
