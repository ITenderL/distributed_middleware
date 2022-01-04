import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

// @Configuration
@Slf4j
public class RabbitMQConfig {

    @Autowired
    private CachingConnectionFactory cachingConnectionFactory;

    /**
     * 开启confirm和return确认机制
     * @return RabbitTemplate
     */
    @Bean
    RabbitTemplate rabbitTemplate(){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);

        // 消息只要被rabbit broker接收到就会执行confirmCallback
        // 被broker执行只能保证消息到达服务器，并不能保证一定被投递到目标queue里
        rabbitTemplate.setConfirmCallback((data,ack,cause) -> {
            String msgId = data.getId();
            if (ack) {
                log.info(msgId+"消息发送成功!!!");
            }else{
                log.info(msgId+"消息发送失败!!!");
            }
        });

        // confirm 模式只能保证消息达到broker 不能保证消息准确投递到目标queuez中
        // 有些业务场景下，需要保证消息一定投递到目标queue中，此时需要用到return退回模式
        // 如果未能达到目前queue中将调用returnCallback,可以记录下详细投递数据，定期巡检或者纠错
        rabbitTemplate.setReturnCallback((message,replyCode,replyText,exchange,routingKey) -> {
            log.info("消息发送失败，ReturnCallback:{0},{1},{2},{3},{4}",message,replyCode,replyText,exchange,routingKey);
        });
        return rabbitTemplate;
    }

    //---------------------------业务队列常量定义-----------------------
    /**
     * 业务交换机标识符
     */
    public static final String BUSINESS_EXCHANGE_NAME="dead-letter-business-exchange";
    /**
     * 业务A队列
     */
    public static final String BUSINESS_QUEUEA_NAME = "dead-letter-business-queuea";
    /**
     * 业务B队列
     */
    public static final String BUSINESS_QUEUEB_NAME = "dead-letter-business-queueb";
    /**
     * 业务死信交换机标识符
     */
    public static final String DEAD_LETTER_EXCHANGE = "dead-letter-deadletter-exchange";
    /**
     * 业务A队列与死信队列交换机绑定键 标识符
     */
    public static final String DEAD_LETTER_QUEUEA_ROUTING_KEY = "dead-letter-deadletter-queuea-routingkey";
    /**
     * 业务B队列与死信队列交换机绑定键 标识符
     */
    public static final String DEAD_LETTER_QUEUEB_ROUTING_KEY = "dead-letter-deadletter-queueb-routingkey";
    /**
     * 业务A死信队列名称
     */
    public static final String DEAD_LETTER_QUEUEA_NAME = "dead-letter-deadletter-queuea";
    /**
     * 业务B死信队列名称
     */
    public static final String DEAD_LETTER_QUEUEB_NAME = "dead-letter-deadletter-queueb";
    //---------------------------订单私信队列常量定义-----------------------
    /**
     * 订单死信队列交换机标识符 属性值不能改 写死
     */
    private static final String ORDER_DEAD_LETTER_QUEUE_KEY = "x-dead-letter-exchange";

    /**
     * 订单死信队列交换机绑定键 标识符  属性值不能改，写死
     */
    private static final String ORDER_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    //----------------------------业务队列定义------------------------------
    /**
     * 声明业务交换机Exchange
     * Fanout类型
     */
    @Bean("businessExchange")
    public FanoutExchange businessExchange(){
        return new FanoutExchange(BUSINESS_EXCHANGE_NAME);
    }
    /**
     *  声明死信交换机Exchange
     *  direct类型
     */
    @Bean("deadLetterExchange")
    public DirectExchange deadLetterExchange(){
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }
    /**
     * 声明业务A队列
     */
    @Bean("businessQueueA")
    public Queue businessQueueA(){
        Map<String,Object> args = new HashMap<>(2);
        // 声明当前队列的绑定的死信交换机
        args.put(ORDER_DEAD_LETTER_QUEUE_KEY,DEAD_LETTER_EXCHANGE);
        // 声明当前队列死信路由key
        args.put(ORDER_DEAD_LETTER_ROUTING_KEY,DEAD_LETTER_QUEUEA_ROUTING_KEY);
        return QueueBuilder.durable(BUSINESS_QUEUEA_NAME).withArguments(args).build();
    }
    /**
     * 声明业务B队列
     */
    @Bean("businessQueueB")
    public Queue businessQueueB(){
        Map<String,Object> args = new HashMap<>(2);
        // 声明当前队列的绑定的死信交换机
        args.put(ORDER_DEAD_LETTER_QUEUE_KEY,DEAD_LETTER_EXCHANGE);
        // 声明当前队列死信路由key
        args.put(ORDER_DEAD_LETTER_ROUTING_KEY,DEAD_LETTER_QUEUEB_ROUTING_KEY);
        return QueueBuilder.durable(BUSINESS_QUEUEB_NAME).withArguments(args).build();
    }

    /**
     * 声明A死信队列
     */
    @Bean("deadLetterQueueA")
    public Queue deadLetterQueueA(){
        return new Queue(DEAD_LETTER_QUEUEA_NAME);
    }

    /**
     * 声明B死信队列
     */
    @Bean("deadLetterQueueB")
    public Queue deadLetterQueueB(){
        return new Queue(DEAD_LETTER_QUEUEB_NAME);
    }

    /**
     * 声明业务A绑定关系
     */
    @Bean
    public Binding businessBindingA(@Qualifier("businessQueueA")Queue queueA,
                                    @Qualifier("businessExchange")FanoutExchange exchange){
        return BindingBuilder.bind(queueA).to(exchange);
    }
    /**
     * 声明业务B绑定关系
     */
    @Bean
    public Binding businessBindingB(@Qualifier("businessQueueB")Queue queueB,
                                    @Qualifier("businessExchange")FanoutExchange exchange){
        return BindingBuilder.bind(queueB).to(exchange);
    }

    /**
     * 声明死信队列A绑定关系
     */
    @Bean
    public Binding deadLetterBindingA(@Qualifier("deadLetterQueueA")Queue queueA,
                                      @Qualifier("deadLetterExchange")DirectExchange exchange){
        return BindingBuilder.bind(queueA).to(exchange).with(DEAD_LETTER_QUEUEA_ROUTING_KEY);
    }
    /**
     * 声明死信队列B绑定关系
     */
    @Bean
    public Binding deadLetterBindingB(@Qualifier("deadLetterQueueB")Queue queueB,
                                      @Qualifier("deadLetterExchange")DirectExchange exchange){
        return BindingBuilder.bind(queueB).to(exchange).with(DEAD_LETTER_QUEUEB_ROUTING_KEY);
    }
    //----------------------------订单死信定义------------------------------
    /**
     * orderDeadLetterExchange（direct类型交换机）
     */
    @Bean("orderDeadLetterExchange")
    public Exchange orderDeadLetterExchange() {
        return ExchangeBuilder.directExchange("ORDER_DL_EXCHANGE").durable(true).build();
    }
    /**
     * 声明一个订单死信队列.
     * x-dead-letter-exchange   对应  死信交换机
     * x-dead-letter-routing-key  对应 死信队列
     */
    @Bean("orderDeadLetterQueue")
    public Queue orderDeadLetterQueue() {
        // 参数
        Map<String, Object> args = new HashMap<>(2);
        // 出现dead letter之后将dead letter重新发送到指定exchange
        args.put(ORDER_DEAD_LETTER_QUEUE_KEY, "ORDER_DL_EXCHANGE");
        // 出现dead letter之后将dead letter重新按照指定的routing-key发送
        args.put(ORDER_DEAD_LETTER_ROUTING_KEY, "RED_KEY");
        // name队列名字
        // durable是否持久化，true保证消息的不丢失,
        // exclusive是否排他队列，如果一个队列被声明为排他队列，该队列仅对首次申明它的连接可见，并在连接断开时自动删除,
        // autoDelete如果该队列没有任何订阅的消费者的话，该队列是否会被自动删除, arguments参数map
        return new Queue("ORDER_DL_QUEUE",true,false,false, args);
    }

    /**
     * 定义订单死信队列转发队列.
     */
    @Bean("orderRedirectQueue")
    public Queue orderRedirectQueue() {
        return new Queue("ORDER_REDIRECT_QUEUE",true,false,false);
    }

    /**
     * 死信路由通过 DL_KEY 绑定键绑定到订单死信队列上.
     */
    @Bean
    public Binding orderDeadLetterBinding() {
        return new Binding("ORDER_DL_QUEUE", Binding.DestinationType.QUEUE, "ORDER_DL_EXCHANGE", "DL_KEY", null);

    }

    /**
     * 死信路由通过 KEY_R 绑定键绑定到订单转发队列上.
     */
    @Bean
    public Binding orderRedirectBinding() {
        return new Binding("ORDER_REDIRECT_QUEUE", Binding.DestinationType.QUEUE, "ORDER_DL_EXCHANGE", "RED_KEY", null);
    }
    // ---------------------------------------延迟队列实现---------------------------------------
    /**
     * 现实中的一个需求分析：
     * 1.用户在系统中创建一个订单，如果超时用户没有进行支付操作，那么自动取消订单
     * 实现手段：
     * 采用延迟队列
     * rabbitmq中没有延迟队列，可以同过过期时间+死信队列来实现延迟队列
     * 过期时间可以通过队列中设置x-message-ttl参数实现
     * 死信队列在声明时，给队列设置x-dead-letter-exhcange参数，另外声明一个队列绑定x-dead-letter-exchange对应的交换器
     * 步骤：
     * 1.声明一个订单业务队列 business-order-queue
     * 2.声明一个业务交换器 business-order-exchnage
     * 3.绑定业务对应的交换器 binding: { business-order-queue:business-order-exchnage }
     * ----------------------------------------
     * 4.声明一个死信交换器 dead-letter-order-exchange
     * 5.声明一个死信队列 dead-letter-order-queue
     * 6.绑定死信队列对应交换器 binding: { dead-letter-order-queue:dead-letter-order-exchange }
     */
    public static final String BUSINESS_ORDER_EXCHANGE = "business-order-exchnage";
    public static final String BUSINESS_ORDER_QUEUE_NAME = "business-order-queue";
    public static final String DEAD_LETTER_ORDER_EXCHANGE = "dead-letter-order-exchange";
    public static final String DEAD_LETTER_ORDER_QUEUE_NAME = "dead-letter-order-queue";
    public static final String DEAD_LETTER_ORDER_ROUTING_KEY = "dead-letter-deadletter-order-routingkey";

    /**
     * 声明订单业务交换器
     */
    @Bean("businessOrderExchange")
    public FanoutExchange businessOrderExchange(){
        return new FanoutExchange(BUSINESS_ORDER_EXCHANGE);
    }

    /**
     * 声明订单业务死信交换器
     */
    @Bean("deadLetterOrderExchange")
    public DirectExchange deadLetterOrderExchange(){
        return new DirectExchange(DEAD_LETTER_ORDER_EXCHANGE);
    }

    /**
     * 声明订单业务队列
     * 设置超时时间
     * @return
     */
    @Bean("businessOrderQueue")
    public Queue  businessOrderQueue(){
        Map<String,Object> args = new HashMap<>();
        // 设定x-message-ttl过期时间
        args.put("x-message-ttl",30000);
        // 声明当前队列的绑定的死信交换机
        args.put(ORDER_DEAD_LETTER_QUEUE_KEY,DEAD_LETTER_ORDER_EXCHANGE);
        // 声明当前队列死信路由key
        args.put(ORDER_DEAD_LETTER_ROUTING_KEY,DEAD_LETTER_ORDER_ROUTING_KEY);
        return QueueBuilder.durable(BUSINESS_ORDER_QUEUE_NAME).withArguments(args).build();
    }

    /**
     * 声明订单死信队列
     * @return
     */
    @Bean("deadLetterOrderQueue")
    public Queue  deadLetterOrderQueue(){
        return new Queue(DEAD_LETTER_ORDER_QUEUE_NAME);
    }

    /**
     * 绑定业务交换器
     */
    @Bean
    public Binding businessOrderBinding(@Qualifier("businessOrderQueue")Queue queue,
                                        @Qualifier("businessOrderExchange")FanoutExchange exchange){
        return BindingBuilder.bind(queue).to(exchange);
    }

    /**
     * 绑定死信队列交换器
     */
    @Bean
    public Binding deadLetterOrderBinding(@Qualifier("deadLetterOrderQueue")Queue queue,
                                          @Qualifier("deadLetterOrderExchange")DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_ORDER_ROUTING_KEY);
    }


    /**
     * 实现多线程处理队列消息
     * @RabbitListener默认是单线程监听队列
     * 当线程消费消息容易引起消息处理缓慢，消息堆积，不能最大化利用硬件资源
     * 可以通过配置mq工厂参数，增加并发量处理数据即可实现多线程处理监听队列，实现多线程处理消息
     */

    @Bean("customContainerFactory")
    public SimpleRabbitListenerContainerFactory containerFactory(SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConcurrentConsumers(10);
        factory.setMaxConcurrentConsumers(10);
        configurer.configure(factory,connectionFactory);
        return factory;
    }

}

