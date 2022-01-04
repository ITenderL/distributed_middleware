package com.itender.rabbit;

import cn.hutool.core.date.DateUtil;
import com.itender.rabbitmq.RabbitMqApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @Author: ITender
 * @CreateTime: 2021-12-24 16:52
 */
@Slf4j
@SpringBootTest(classes = RabbitMqApplication.class)
@RunWith(SpringRunner.class)
public class RabbitTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void delayQueueTest() {
        log.info("消息发送时间：{}", DateUtil.format(new Date(), "yyyyMMdd HH:mm:ss"));
        String message = "I am a delay message!";
        rabbitTemplate.convertAndSend("delayQueue", (Object) message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setExpiration("10000");
                return message;
            }
        });
    }

    @Test
    public void sendMessageTest() {
        String message = "Hello RabbitMQ";
        rabbitTemplate.convertAndSend("test_exchange", "testKey", message);
    }

    /**
     * confirm模式
     */
    @Test
    public void testConfirm() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功！");
            } else {
                log.info("消息发送失败：重新发送！失败原因:{}", cause);
            }
        });
        String message = "Hello RabbitMQ please confirm!!!";
        rabbitTemplate.convertAndSend("confirm_exchange", "confirmKey", message);
    }

    /**
     * return回退模式
     * 消息发送到exchange后，exchange路由到queue失败时才会执行
     * 步骤：
     * 1.开启回退模式
     * 2.设置returnCallBack
     * 3.设置exchange处理消息模式
     * 1.如果没有路由到queue，则丢弃消息（默认）
     * 2.如果消息没有路由到queue，返回给消息发送方returnCallback
     */
    @Test
    public void testReturn() {
        // 交换机处理失败消息的模式
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routeKey) -> {
            log.info("message:{} replyCode:{} replyText:{} exchange:{} routeKey:{}", message, replyCode, replyText, exchange, routeKey);
        });
        String message = "Hello RabbitMQ please confirm!!!";
        rabbitTemplate.convertAndSend("confirm_exchange", "confirmKey11", message);
    }


    @Test
    public void testSend() {
        String message = "Hello RabbitMQ please confirm!!!";
        for (int i = 10; i < 20; i++) {
            rabbitTemplate.convertAndSend("confirm_exchange", "confirmKey", message + ": " + i);
        }
    }
}
