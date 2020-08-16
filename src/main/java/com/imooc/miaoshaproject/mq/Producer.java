package com.imooc.miaoshaproject.mq;

import com.alibaba.fastjson.JSON;
import com.imooc.miaoshaproject.dao.StockLogDOMapper;
import com.imooc.miaoshaproject.dataobject.StockLogDO;
import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
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

    private TransactionMQProducer transactionMQProducer;
    @Value("${mq.nameserver.addr}")
    private String nameSrvAddr;
    @Value("${mq.topicname}")
    private String topic;

    @Autowired
    private OrderService orderService;
    @Autowired
    private StockLogDOMapper stockLogDOMapper;


    @PostConstruct
    public void init() throws MQClientException {
        //初始化producer
        mqProducer = new DefaultMQProducer("producer_group");
        mqProducer.setNamesrvAddr(nameSrvAddr);
        mqProducer.start();

        //初始化transactionProducer
        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameSrvAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {
                //真正要做的事儿（创建订单）
                //获取参数
                Integer userId = (Integer) ((Map) o).get("userId");
                Integer amount = (Integer) ((Map) o).get("amount");
                Integer itemId = (Integer) ((Map) o).get("itemId");
                Integer promoId = (Integer) ((Map) o).get("promoId");
                String stockLogId = (String) ((Map) o).get("stockLogId");
                try {
                    orderService.createOrder(userId, itemId, promoId, amount, stockLogId);
                } catch (BusinessException e) {
                    e.printStackTrace();
                    //需要更新库存流水记录的状态
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    //下单成功扣减库存
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKey(stockLogDO);
                    //如果发生异常,则将消息回滚
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                //订单创建成功，将消息进行二次提交（原消息状态为prepare状态，不会被消费）
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            //根据库存是否扣减成功，来判断是否返回COMMIT、ROLLBACK、UNKNOWN
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                String jsonString = new String(messageExt.getBody());
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId = (String) map.get("stockLogId");

                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if (stockLogDO == null) {
                    return LocalTransactionState.UNKNOW;
                }
                Integer status = stockLogDO.getStatus();
                if (status == 1) {
                    return LocalTransactionState.UNKNOW;
                } else if (status == 2) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }

            }
        });

    }

    public boolean transactionAsyncReduceStock(Integer userId, Integer itemId,
                                               Integer promoId, Integer amount, String stockLogId) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("amount", amount);
        map.put("itemId", itemId);
        map.put("stockLogId", stockLogId);

        Map<String, Object> argsMap = new HashMap<>(2);
        argsMap.put("userId", userId);
        argsMap.put("amount", amount);
        argsMap.put("itemId", itemId);
        argsMap.put("promoId", promoId);
        argsMap.put("stockLogId", stockLogId);
        Message message = new Message(nameSrvAddr, "decrease", JSON.toJSON(map).toString().getBytes(StandardCharsets.UTF_8));
        TransactionSendResult transactionSendResult = null;
        try {
            transactionSendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        if (transactionSendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
            return false;
        } else if (transactionSendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 异步扣减库存的producer
     *
     * @param orderId
     * @param amount
     * @return
     */
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
