package com.ai.slp.order.service.business.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.base.exception.SystemException;
import com.ai.opt.sdk.constants.ExceptCodeConstants;
import com.ai.opt.sdk.dubbo.util.DubboConsumerFactory;
import com.ai.opt.sdk.util.BeanUtils;
import com.ai.opt.sdk.util.CollectionUtil;
import com.ai.opt.sdk.util.DateUtil;
import com.ai.platform.common.api.cache.interfaces.ICacheSV;
import com.ai.slp.order.api.delivergoods.param.DeliverGoodsPrintInfoVo;
import com.ai.slp.order.api.delivergoods.param.DeliverGoodsPrintInfosRequest;
import com.ai.slp.order.api.delivergoods.param.DeliverGoodsPrintRequest;
import com.ai.slp.order.api.delivergoods.param.DeliverGoodsPrintResponse;
import com.ai.slp.order.api.delivergoods.param.DeliverGoodsPrintVo;
import com.ai.slp.order.constants.OrdersConstants;
import com.ai.slp.order.constants.OrdersConstants.OrdOdStateChg;
import com.ai.slp.order.dao.mapper.bo.DeliverInfoProd;
import com.ai.slp.order.dao.mapper.bo.DeliverInfoProdCriteria;
import com.ai.slp.order.dao.mapper.bo.OrdOdDeliverInfo;
import com.ai.slp.order.dao.mapper.bo.OrdOdDeliverInfoCriteria;
import com.ai.slp.order.dao.mapper.bo.OrdOdLogistics;
import com.ai.slp.order.dao.mapper.bo.OrdOdLogisticsCriteria;
import com.ai.slp.order.dao.mapper.bo.OrdOdProd;
import com.ai.slp.order.dao.mapper.bo.OrdOdProdCriteria;
import com.ai.slp.order.dao.mapper.bo.OrdOrder;
import com.ai.slp.order.dao.mapper.bo.OrdOrderCriteria;
import com.ai.slp.order.service.atom.interfaces.IDeliveryOrderPrintAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOdLogisticsAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOdProdAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOrderAtomSV;
import com.ai.slp.order.service.business.interfaces.IDeliverGoodsPrintBusiSV;
import com.ai.slp.order.service.business.interfaces.IOrderFrameCoreSV;
import com.ai.slp.order.util.CommonCheckUtils;
import com.ai.slp.order.util.SequenceUtil;

@Service
@Transactional
public class DeliverGoodsPrintBusiSVImpl implements IDeliverGoodsPrintBusiSV {
	
	private static final Logger logger=LoggerFactory.getLogger(DeliveryOrderPrintBusiSVImpl.class);

	@Autowired
	private IOrdOrderAtomSV ordOrderAtomSV;
	
	@Autowired
	private IOrdOdLogisticsAtomSV ordOdLogisticsAtomSV;
	
	@Autowired
	private IOrderFrameCoreSV orderFrameCoreSV;
	
	@Autowired
	private IDeliveryOrderPrintAtomSV deliveryOrderPrintAtomSV;
	
	@Autowired
	private IOrdOdProdAtomSV ordOdProdAtomSV;
	
	@Override
	public DeliverGoodsPrintResponse deliverGoodsQuery(DeliverGoodsPrintRequest request) throws BusinessException, SystemException {
		DeliverGoodsPrintResponse response=new DeliverGoodsPrintResponse();
		/* 参数校验*/
		List<OrdOdDeliverInfo> infos = this.checkParamAndQueryInfos(request.getOrderId(), request.getTenantId());
		OrdOrder order = ordOrderAtomSV.selectByOrderId(request.getTenantId(), request.getOrderId());
		if(order==null) {
			logger.warn("未能查询到指定的订单信息[订单id:"+ request.getOrderId()+"]");
			throw new BusinessException(ExceptCodeConstants.Special.PARAM_IS_NULL, 
					"未能查询到指定的订单信息[订单id:"+ request.getOrderId()+"]");
		}
		List<OrdOdProd> ordOdProds = this.getOrdOdProds(request);
		for (OrdOdProd ordOdProd : ordOdProds) {
			if(OrdersConstants.OrdOrder.cusServiceFlag.YES.equals(ordOdProd.getCusServiceFlag())) {
				//该商品为售后标识 不可打印
				throw new BusinessException("", "订单下商品处于售后状态,不可打印");
			}
		}
		if(CollectionUtil.isEmpty(infos)) {
			logger.warn("未查询到相应的信息,请查看提货单信息是否已经打印.");
			throw new BusinessException(ExceptCodeConstants.Special.NO_RESULT, "未查询到相应的信息,请查看提货单信息是否已经打印.");
		}
		OrdOrderCriteria example=new OrdOrderCriteria();
		OrdOrderCriteria.Criteria criteria = example.createCriteria();
		criteria.andOrigOrderIdEqualTo(order.getOrderId());
		criteria.andTenantIdEqualTo(order.getTenantId());
		List<OrdOrder> ordOrderList = ordOrderAtomSV.selectByExample(example);
		Map<String,Long> prodMap=new HashMap<String,Long>();
		if(!CollectionUtil.isEmpty(ordOrderList)) {
			for (OrdOrder ordOrder : ordOrderList) {
				OrdOdProdCriteria prodExample=new OrdOdProdCriteria();
				OrdOdProdCriteria.Criteria prodCriteria = prodExample.createCriteria();
				prodCriteria.andOrderIdEqualTo(ordOrder.getOrderId());
				prodCriteria.andTenantIdEqualTo(ordOrder.getTenantId()); //售后商品明细表对应订单主表 一对一
				List<OrdOdProd> prodList = ordOdProdAtomSV.selectByExample(prodExample);
				if(!CollectionUtil.isEmpty(prodList)) {
					OrdOdProd ordOdProd = prodList.get(0);
					prodMap.put(ordOdProd.getSkuId(), ordOdProd.getBuySum());
				}
			}
		}
		List<DeliverGoodsPrintVo> list=new ArrayList<DeliverGoodsPrintVo>();
		long sum=0;
		for (OrdOdDeliverInfo ordOdDeliverInfo : infos) {
			/* 查询提发货明细信息*/
			DeliverInfoProdCriteria exampleInfo=new DeliverInfoProdCriteria();
			DeliverInfoProdCriteria.Criteria criteriaInfo = exampleInfo.createCriteria();
			criteriaInfo.andDeliverInfoIdEqualTo(ordOdDeliverInfo.getDeliverInfoId());
			List<DeliverInfoProd> deliverInfoProds = deliveryOrderPrintAtomSV.selectByExample(exampleInfo);
			if(!CollectionUtil.isEmpty(deliverInfoProds)) {
				DeliverInfoProd deliverInfoProd = deliverInfoProds.get(0);
				DeliverGoodsPrintVo invoicePrintVo=new DeliverGoodsPrintVo();
				invoicePrintVo.setSkuId(deliverInfoProd.getSkuId());
				invoicePrintVo.setProdName(deliverInfoProd.getProdName());
				invoicePrintVo.setExtendInfo(deliverInfoProd.getExtendInfo());
				Long afterSaleBuySum = prodMap.get(deliverInfoProd.getSkuId());
				invoicePrintVo.setBuySum(deliverInfoProd.getBuySum()-(afterSaleBuySum==null?0:afterSaleBuySum));
				invoicePrintVo.setSalePrice(String.valueOf(deliverInfoProd.getSalePrice()/1000));//厘转元
				invoicePrintVo.setHorOrderId(ordOdDeliverInfo.getHorOrderId());
				sum+=deliverInfoProd.getBuySum()-(afterSaleBuySum==null?0:afterSaleBuySum);
				list.add(invoicePrintVo);
			}
		}
		/* 查询订单配送信息 父订单对应配送信息*/
		List<OrdOdLogistics> logistics = getOrdOdLogistics(order.getParentOrderId(), order.getTenantId());
		if(CollectionUtil.isEmpty(logistics)) {
			logger.warn("未能查询到指定的订单配送信息[订单id:"+order.getParentOrderId()+",租户id:"+order.getTenantId()+"]");
			throw new BusinessException(ExceptCodeConstants.Special.PARAM_IS_NULL,
					"未能查询到指定的订单配送信息[订单id:"+order.getParentOrderId()+",租户id:"+order.getTenantId()+"]");
		}
		OrdOdLogistics ordOdLogistics = logistics.get(0);
		ICacheSV iCacheSv = DubboConsumerFactory.getService("iCacheSV");
		response.setRouteId(order.getRouteId());
		response.setContactName(ordOdLogistics.getContactName());
	    response.setContactTel(ordOdLogistics.getContactTel());
	    response.setProvinceCode(ordOdLogistics.getProvinceCode()==null?"":iCacheSv.getAreaName(ordOdLogistics.getProvinceCode()));
	    response.setCityCode(ordOdLogistics.getCityCode()==null?"":iCacheSv.getAreaName(ordOdLogistics.getCityCode()));
	    response.setCountyCode(ordOdLogistics.getCountyCode()==null?"":iCacheSv.getAreaName(ordOdLogistics.getCountyCode()));
	    response.setAreaCode(ordOdLogistics.getAreaCode()==null?"":iCacheSv.getAreaName(ordOdLogistics.getAreaCode()));
	    response.setAddress(ordOdLogistics.getAddress());
		response.setOrderId(request.getOrderId());
		response.setExpressOddNumber(ordOdLogistics.getExpressOddNumber());
		response.setInvoiceDate(DateUtil.getDate());
		response.setSum(sum);
		response.setInvoicePrintVos(list);
		return response;
	}
	
	
	
	@Override
	public void deliverGoodsPrint(DeliverGoodsPrintInfosRequest request)
			throws BusinessException, SystemException {
		List<DeliverGoodsPrintInfoVo> invoicePrintVos = request.getInvoicePrintVos();
		for (DeliverGoodsPrintInfoVo invoicePrintVo : invoicePrintVos) {
			Long invoiceInfoId = SequenceUtil.createdeliverInfoId();
			OrdOdDeliverInfo invoiceInfo=new OrdOdDeliverInfo();
			invoiceInfo.setDeliverInfoId(invoiceInfoId);
			invoiceInfo.setHorOrderId(invoicePrintVo.getHorOrderId());
			invoiceInfo.setOrderId(request.getOrderId());
			invoiceInfo.setPrintInfo(OrdersConstants.OrdOdDeliverInfo.printInfo.TWO);
			invoiceInfo.setUpdateTime(DateUtil.getSysDate());
			deliveryOrderPrintAtomSV.insertSelective(invoiceInfo);
			DeliverInfoProd deliverInfoProd=new DeliverInfoProd();
			BeanUtils.copyProperties(deliverInfoProd, invoicePrintVo);
			deliverInfoProd.setDeliverInfoId(invoiceInfoId);
			deliveryOrderPrintAtomSV.insertSelective(deliverInfoProd);
			/* 更新合并订单状态并写入订单状态变化轨迹*/
			this.updateOrderState(invoicePrintVo.getHorOrderId(), request.getTenantId(), DateUtil.getSysDate());
		}
		/* 更新订单状态并写入订单状态变化轨迹*/
		this.updateOrderState(request.getOrderId(),request.getTenantId(),DateUtil.getSysDate());
	}
	
	 /**
	  * 查询订单配送信息
	  */
	 private List<OrdOdLogistics> getOrdOdLogistics(Long orderId,String tenantId) {
		OrdOdLogisticsCriteria exampleLogistics=new OrdOdLogisticsCriteria();
		OrdOdLogisticsCriteria.Criteria criteriaLogistics = exampleLogistics.createCriteria();
		criteriaLogistics.andTenantIdEqualTo(tenantId);
		criteriaLogistics.andOrderIdEqualTo(orderId);
		List<OrdOdLogistics> logistics = ordOdLogisticsAtomSV.selectByExample(exampleLogistics);
		return logistics;
	 }
	 
	 private void updateOrderState(long orderId,String tenantId, Timestamp sysDate) {
			if(orderId==0) {
				return;
			}else {
				OrdOrder ordOrder = ordOrderAtomSV.selectByOrderId(tenantId, orderId);
				if(ordOrder==null) {
					logger.warn("未能查询到指定的订单信息[订单id:"+ orderId+"]");
					throw new BusinessException(ExceptCodeConstants.Special.PARAM_IS_NULL, 
							"未能查询到指定的订单信息[订单id:"+ orderId+"]");
				}
				String orgState = ordOrder.getState();
				if(OrdersConstants.OrdOrder.State.WAIT_SEND.equals(orgState)) {
					return;
				}
				String state1 = OrdersConstants.OrdOrder.State.INVOICE_FINISH_PRINT;
			    String state2 = OrdersConstants.OrdOrder.State.FINISH_DELIVERY;
			    String newState = OrdersConstants.OrdOrder.State.WAIT_SEND;
				ordOrder.setState(newState);
				ordOrder.setStateChgTime(sysDate);
				ordOrderAtomSV.updateById(ordOrder);
				// 写入订单状态变化轨迹表
		        orderFrameCoreSV.ordOdStateChg(ordOrder.getOrderId(), ordOrder.getTenantId(), orgState, state1,
		                OrdOdStateChg.ChgDesc.INVOICE_ORDER_TO_PRINT, null, null, null, sysDate);
		        orderFrameCoreSV.ordOdStateChg(ordOrder.getOrderId(), ordOrder.getTenantId(), state1, state2,
		                OrdOdStateChg.ChgDesc.ORDER_TO_FINISH_LOGISTICS_DELIVERY, null, null, null, sysDate);
		        orderFrameCoreSV.ordOdStateChg(ordOrder.getOrderId(), ordOrder.getTenantId(), state2, newState,
		                OrdOdStateChg.ChgDesc.ORDER_TO_WAIT_SEND, null, null, null, sysDate);
			}
		 }
	 
	 
		/**
		 * 参数检验
		 */
		private List<OrdOdDeliverInfo> checkParamAndQueryInfos(long orderId,String tenantId) {
			CommonCheckUtils.checkTenantId(tenantId, "");
			if(orderId==0) {
				throw new BusinessException(ExceptCodeConstants.Special.PARAM_IS_NULL, "订单id不能为空");
			}
			/* 判断是否存在提货单打印信息*/
			OrdOdDeliverInfoCriteria exampleDeliver=new OrdOdDeliverInfoCriteria();
			OrdOdDeliverInfoCriteria.Criteria criteriaDeliver = exampleDeliver.createCriteria();
			criteriaDeliver.andOrderIdEqualTo(orderId);
			criteriaDeliver.andPrintInfoEqualTo(OrdersConstants.OrdOdDeliverInfo.printInfo.ONE);
			List<OrdOdDeliverInfo> deliverInfos = deliveryOrderPrintAtomSV.selectByExample(exampleDeliver);
			return deliverInfos;
		}
		
		
	  /**
	   * 获取订单下的商品信息
	   */
		private List<OrdOdProd> getOrdOdProds(DeliverGoodsPrintRequest request) {
			List<OrdOdProd> ordOdProds = ordOdProdAtomSV.selectByOrd(request.getTenantId(), request.getOrderId());
			if(CollectionUtil.isEmpty(ordOdProds)) {
				logger.warn("未能查询到指定的订单商品明细信息[订单id:"+request.getOrderId()+"]");
				throw new BusinessException(ExceptCodeConstants.Special.PARAM_IS_NULL, 
						"未能查询到指定的订单商品明细信息[订单id:"+request.getOrderId()+"]");
			}
			return ordOdProds;
		}

}
