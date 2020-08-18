package com.imooc.miaoshaproject.service.impl;

import com.imooc.miaoshaproject.dao.PromoDOMapper;
import com.imooc.miaoshaproject.dataobject.PromoDO;
import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.error.EmBusinessError;
import com.imooc.miaoshaproject.service.ItemService;
import com.imooc.miaoshaproject.service.PromoService;
import com.imooc.miaoshaproject.service.UserService;
import com.imooc.miaoshaproject.service.model.ItemModel;
import com.imooc.miaoshaproject.service.model.PromoModel;
import com.imooc.miaoshaproject.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by hzllb on 2018/11/18.
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        //dataobject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel == null) {
            return null;
        }

        //判断当前时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        return promoModel;
    }
    private PromoModel convertFromDataObject(PromoDO promoDO){
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }

    /**
     * 发布指定活动
     *
     * @param id
     */
    @Override
    public void publishPromo(Integer id) {
        //获取活动详情
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(id);
        if (promoDO == null || promoDO.getItemId() == null) {
            return;
        }
        //获取商品信息
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        //将商品的库存同步到Redis中
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

    }

    /**
     * 根据活动信息生成对应的令牌
     *
     * @param promoId
     * @return
     */
    @Override
    public String generateSecKillToken(Integer promoId, Integer itemId, Integer userId) throws BusinessException {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = convertFromDataObject(promoDO);
        //判断当前时间是否秒杀活动即将开始或正在进行
        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        if (promoModel.getStatus() != 2) {
            return null;
        }
        //校验商品信息
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            return null;
        }
        //校验用户信息
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            return null;
        }
        //校验活动信息
        if (promoId != null) {
            //（1）校验对应活动是否存在这个适用商品
            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
                //（2）校验活动是否正在进行中
            } else if (itemModel.getPromoModel().getStatus().intValue() != 2) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息还未开始");
            }
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        //将生成的token存入redis
        redisTemplate.opsForValue().set("promo_token_" + promoId + "_userId" + userId + "_itemId" + itemId, token);
        //设置过期时间为五分钟
        redisTemplate.expire("promo_token_" + promoId + "_userId" + userId + "_itemId" + itemId, 5, TimeUnit.MINUTES);
        return token;
    }
}
