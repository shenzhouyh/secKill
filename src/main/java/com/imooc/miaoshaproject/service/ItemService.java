package com.imooc.miaoshaproject.service;

import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.service.model.ItemModel;

import java.util.List;

/**
 * Created by hzllb on 2018/11/18.
 */
public interface ItemService {

    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);

    /**
     * @param id 获取商品的缓存信息
     * @return 商品信息
     */
    ItemModel getItemByIdInCache(Integer id);

    //库存扣减
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    /**
     * 异步扣减库存
     *
     * @param itemId 商品ID
     * @param amount 数量
     * @return
     */
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

    boolean increaseStock(Integer itemId, Integer amount);

    //商品销量增加
    void increaseSales(Integer itemId, Integer amount) throws BusinessException;

    /**
     * 记录指定商品的库存流水信息
     *
     * @param itemId 商品ID
     * @param amount 商品当前库存
     */
    String initStockLog(Integer itemId, Integer amount);

}
