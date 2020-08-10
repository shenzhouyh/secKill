package com.imooc.miaoshaproject.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @description : 生产者
 * @since : 10.7.0
 */
@Component
public class Producer {
    private DefaultMQProducer mqProducer;
    @Value("${mq.nameserver.addr}")
    private String nameSrvAddr;
    @Value("${mq.topicname}")
    private String topic;


    @PostConstruct
    public void init() throws MQClientException {
        //初始化producer
        mqProducer = new DefaultMQProducer("producer_group");
        mqProducer.setNamesrvAddr(nameSrvAddr);
        mqProducer.start();

    }

    public Boolean asyncReduceStock(Integer orderId, Integer amount) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("orderId", orderId);
        map.put("amount", amount);
        Message message = new Message(nameSrvAddr, "decrease", JSON.toJSON(map).toString().getBytes(StandardCharsets.UTF_8));
        try {
            mqProducer.send(message);
        } catch (MQClientException e) {
            return false;
        } catch (RemotingException e) {
            return false;
        } catch (MQBrokerException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }
}
