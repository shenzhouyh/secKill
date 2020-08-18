package com.imooc.miaoshaproject.service;

import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.service.model.PromoModel;

/**
 * Created by hzllb on 2018/11/18.
 */
public interface PromoService {
    //根据itemid获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    /**
     * 发布指定活动
     *
     * @param id
     */
    void publishPromo(Integer id);

    /**
     * 根据活动信息生成对应的令牌
     *
     * @param id
     * @return
     */
    String generateSecKillToken(Integer promoId, Integer itemId, Integer userId) throws BusinessException;

}
