package com.ai.slp.order.service.business.impl;

import com.ai.opt.sdk.components.mcs.MCSClientFactory;
import com.ai.opt.sdk.util.BeanUtils;
import com.ai.paas.ipaas.mcs.interfaces.ICacheClient;
import com.ai.slp.order.api.shopcart.param.CartProd;
import com.ai.slp.order.api.shopcart.param.CartProdOptRes;
import com.ai.slp.order.constants.MallIPassConstants;
import com.ai.slp.order.constants.ShopCartConstants;
import com.ai.slp.order.dao.mapper.bo.OrdOdCartProd;
import com.ai.slp.order.service.atom.interfaces.IOrdOdCartProdAtomSV;
import com.ai.slp.order.service.business.interfaces.IShopCartBusiSV;
import com.ai.slp.order.util.DateUtils;
import com.ai.slp.order.util.IPassMcsUtils;
import com.ai.slp.order.vo.ShopCartCachePointsVo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by jackieliu on 16/5/17.
 */
@Service
@Transactional
public class ShopCartBusiSVImpl implements IShopCartBusiSV {
    private static Logger logger = LoggerFactory.getLogger(ShopCartBusiSVImpl.class);
    @Autowired
    IOrdOdCartProdAtomSV cartProdAtomSV;
    /**
     * 查询用户购物车概览
     *
     * @param tenantId
     * @param userId
     * @return
     */
    @Override
    public CartProdOptRes queryCartOptions(String tenantId, String userId) {
        ICacheClient iCacheClient = MCSClientFactory.getCacheClient(MallIPassConstants.SHOP_CART_MCS);
        CartProdOptRes cartProdOptRes = new CartProdOptRes();
        ShopCartCachePointsVo pointsVo = queryCartPoints(iCacheClient,tenantId,userId);
        BeanUtils.copyProperties(cartProdOptRes,pointsVo);
        return cartProdOptRes;
    }

    /**
     * 购物车添加商品
     *
     * @param cartProd
     * @return
     */
    @Override
    public CartProdOptRes addCartProd(CartProd cartProd) {
        //若购买数量为空,或小于0,则设置默认为1
        if (cartProd.getBuyNum() == null
                || cartProd.getBuyNum()<=0)
            cartProd.setBuyNum(1l);
        String tenantId = cartProd.getTenantId(),userId = cartProd.getUserId();
        ICacheClient iCacheClient = MCSClientFactory.getCacheClient(MallIPassConstants.SHOP_CART_MCS);
        String cartUserId = IPassMcsUtils.genShopCartUserId(tenantId,userId);

        //查询用户购物车概览
        ShopCartCachePointsVo pointsVo = queryCartPoints(iCacheClient,tenantId,userId);
        String cartProdStr = iCacheClient.hget(cartUserId,cartProd.getSkuId());
        OrdOdCartProd odCartProd;
        //若已经存在
        if (StringUtils.isNotBlank(cartProdStr)){
            odCartProd = JSON.parseObject(cartProdStr,OrdOdCartProd.class);
            //更新购买数量
            odCartProd.setBuySum(odCartProd.getBuySum()+cartProd.getBuyNum());
        }else {
            odCartProd = new OrdOdCartProd();
            odCartProd.setInsertTime(DateUtils.currTimeStamp());
            odCartProd.setBuySum(cartProd.getBuyNum());
            odCartProd.setSkuId(cartProd.getSkuId());
            odCartProd.setTenantId(tenantId);
            odCartProd.setUserId(userId);
            //若是新商品,则需要将概览中加1
            pointsVo.setProdNum(pointsVo.getProdNum()+1);
        }
        //添加/更新商品信息
        iCacheClient.hset(cartUserId,odCartProd.getSkuId(),JSON.toJSONString(odCartProd));
        //更新购物车上商品总数量
        pointsVo.setProdTotal(pointsVo.getProdTotal()+cartProd.getBuyNum());
        //更新概览
        iCacheClient.hset(cartUserId,ShopCartConstants.CacheParams.CART_POINTS,JSON.toJSONString(pointsVo));

        CartProdOptRes cartProdOptRes = new CartProdOptRes();
        BeanUtils.copyProperties(cartProdOptRes,pointsVo);
        return cartProdOptRes;
    }

    /**
     * 更新购物车中商品数量
     *
     * @param cartProd
     * @return
     */
    @Override
    public CartProdOptRes updateCartProd(CartProd cartProd) {
        String tenantId = cartProd.getTenantId(),userId = cartProd.getUserId();
        ICacheClient iCacheClient = MCSClientFactory.getCacheClient(MallIPassConstants.SHOP_CART_MCS);
        String cartUserId = IPassMcsUtils.genShopCartUserId(tenantId,userId);
        //若不存在,则直接进行添加操作
        if (!iCacheClient.hexists(cartUserId,cartProd.getSkuId())){
            return addCartProd(cartProd);
        }
        //若购买数量为空,或小于0,则设置默认为1
        if (cartProd.getBuyNum() == null
                || cartProd.getBuyNum()<=0)
            cartProd.setBuyNum(1l);
        String cartProdStr = iCacheClient.hget(cartUserId,cartProd.getSkuId());
        //更新商品数量
        OrdOdCartProd odCartProd = JSON.parseObject(cartProdStr, OrdOdCartProd.class);
        //此商品变化的数量,若为负数,则表示减少
        long addNum = cartProd.getBuyNum() - odCartProd.getBuySum();
        //更新购买数量
        odCartProd.setBuySum(cartProd.getBuyNum());
        //添加/更新商品信息
        iCacheClient.hset(cartUserId,odCartProd.getSkuId(),JSON.toJSONString(odCartProd));
        //查询用户购物车概览
        ShopCartCachePointsVo pointsVo = queryCartPoints(iCacheClient,tenantId,userId);
        pointsVo.setProdTotal(pointsVo.getProdTotal()+addNum);//更新商品总数量
        //更新概览
        iCacheClient.hset(cartUserId,ShopCartConstants.CacheParams.CART_POINTS,JSON.toJSONString(pointsVo));
        CartProdOptRes cartProdOptRes = new CartProdOptRes();
        BeanUtils.copyProperties(cartProdOptRes,pointsVo);
        return cartProdOptRes;
    }

    /**
     * 查询用户购物车的概览
     * @param iCacheClient
     * @param tenantId
     * @param userId
     * @return
     */
    private ShopCartCachePointsVo queryCartPoints(ICacheClient iCacheClient,String tenantId,String userId){
        //查询缓存中是否存在
        String cartUserId = IPassMcsUtils.genShopCartUserId(tenantId,userId);
        //若不存在购物车信息缓存,则建立缓存
        if (!iCacheClient.exists(cartUserId)){
            //从数据库中查询,建立缓存
            addShopCartCache(tenantId,userId);
        }
        //查询概览信息
        String cartPrefix = iCacheClient.hget(IPassMcsUtils.genShopCartUserId(tenantId,userId)
                ,ShopCartConstants.CacheParams.CART_POINTS);
        ShopCartCachePointsVo pointsVo = JSON.parseObject(cartPrefix, ShopCartCachePointsVo.class);
        return pointsVo;
    }

    /**
     * 将数据库中数据加载到缓存中
     *
     * @param tenantId
     * @param userId
     */
    private void addShopCartCache(String tenantId,String userId){
        ICacheClient iCacheClient = MCSClientFactory.getCacheClient(MallIPassConstants.SHOP_CART_MCS);
        String cartUserId = IPassMcsUtils.genShopCartUserId(tenantId,userId);
        //查询用户购物车商品列表
        List<OrdOdCartProd> cartProdList = cartProdAtomSV.queryCartProdsOfUser(tenantId,userId);
        int prodTotal = 0;
        //循环建立购物车单品缓存
        for (OrdOdCartProd cartProd:cartProdList){
            iCacheClient.hset(cartUserId, cartProd.getSkuId(),JSON.toJSONString(cartProd));
            prodTotal += cartProd.getBuySum();
        }
        //添加概览信息
        ShopCartCachePointsVo cartProdPoints = new ShopCartCachePointsVo();
        cartProdPoints.setProdNum(cartProdList.size());
        cartProdPoints.setProdTotal(prodTotal);
        iCacheClient.hset(cartUserId, ShopCartConstants.CacheParams.CART_POINTS,JSON.toJSONString(cartProdPoints));
    }
}
