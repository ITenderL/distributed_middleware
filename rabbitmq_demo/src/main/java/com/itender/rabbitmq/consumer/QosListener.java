package com.itender.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

/**
 * @Author: ITender
 * @CreateTime: 2021-12-27 17:38
 */
@Slf4j
@Component
public class QosListener implements ChannelAwareMessageListener {

    /**
     * consumer限流机制：
     *  1.ack模式为手动确认
     *  2.设置prefetch = 1表示消费端每次拉去一条消息来消费，知道手动确认后才会拉取下一条消息
     * @param message
     * @param channel
     * @throws Exception
     */
    @RabbitListener(queues = "confirm_queue")
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        // log.info("consumer limit message:{} ", message.getBody());
        System.out.println("consumer limit message: " + new String(message.getBody()));
        // 签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
    }
}
