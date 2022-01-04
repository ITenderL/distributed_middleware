package com.itender.rabbitmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @Author: ITender
 * @CreateTime: 2021-12-24 16:18
 */
@Slf4j
@Component
public class MessageConsumer {

    /**
     * 消息监听
     *
     * @param message
     */
    @RabbitListener(queues = "test_queue")
    public void messageListener(String message) {
        log.info(message);
    }
}
