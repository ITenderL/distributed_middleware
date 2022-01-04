package com.itender.rabbitmq.consumer;

import cn.hutool.core.date.DateUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Author: ITender
 * @CreateTime: 2022-01-04 11:15
 */
@Slf4j
@Component
public class DelayListener implements ChannelAwareMessageListener {
    // @RabbitListener(queues = {"realQueue","test_queue"})
    // public void getMessage(String message) {
    //
    // }

    @RabbitListener(queues = {"realQueue","test_queue"})
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        log.info("消息接收时间：{}", DateUtil.format(new Date(), "yyyyMMdd HH:mm:ss"));
        try {
            System.out.println("接收消息为：" + message);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        } catch (Exception e) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), true, true);
        }
    }
}
