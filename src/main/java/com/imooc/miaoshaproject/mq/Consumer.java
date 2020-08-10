package com.imooc.miaoshaproject.mq;

import com.alibaba.fastjson.JSON;
import com.imooc.miaoshaproject.dao.ItemStockDOMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @description :消费者
 * @since : 10.7.0
 */
@Component
public class Consumer {

    private DefaultMQPushConsumer mqPushConsumer;

    @Value("${mq.nameserver.addr}")
    private String nameSrvAddr;
    @Value("${mq.topicname}")
    private String topic;
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;


    @PostConstruct
    public void init() throws MQClientException {
        mqPushConsumer = new DefaultMQPushConsumer("consume_group");
        //设置nameServer地址信息
        mqPushConsumer.setNamesrvAddr(nameSrvAddr);
        //订阅指定的topic
        mqPushConsumer.subscribe(topic, "*");
        //设置监听器，处理消息
        mqPushConsumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                MessageExt messageExt = list.get(0);
                String jsonString = new String(messageExt.getBody());
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                Integer orderId = (Integer) map.get("orderId");
                Integer amount = (Integer) map.get("amount");
                //减库存
                itemStockDOMapper.decreaseStock(orderId, amount);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //启动consumer
        mqPushConsumer.start();
    }
}
