package com.itender.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author: ITender
 * @CreateTime: 2021-12-27 17:08
 * <p>
 * consumer ack 机制：
 * 1.设置手动签收acknowledge=true
 * 2.监听类实现ChannelAwareMessageListener接口，重写onMessage方法
 * 3.如果消息处理成功调用channel的basicAck()方法签收
 * 4.如果消息处失败调用channel的basicNack()方法拒收
 */
@Slf4j
@Component
public class AckListener implements ChannelAwareMessageListener {

    // @RabbitListener(queues = "confirm_queue")
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // int i = 1/0;
            log.info("consumer ack message:{}", message.getBody());
            channel.basicAck(deliveryTag, true);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, true, true);
        }
    }


}
