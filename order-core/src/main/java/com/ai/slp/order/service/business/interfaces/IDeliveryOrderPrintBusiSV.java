package com.ai.slp.order.service.business.interfaces;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.base.exception.SystemException;
import com.ai.slp.order.api.deliveryorderprint.param.DeliveryOrderPrintInfosRequest;
import com.ai.slp.order.api.deliveryorderprint.param.DeliveryOrderPrintRequest;
import com.ai.slp.order.api.deliveryorderprint.param.DeliveryOrderPrintResponse;
import com.ai.slp.order.api.deliveryorderprint.param.DeliveryOrderQueryResponse;

public interface IDeliveryOrderPrintBusiSV {
	
	//提货单查看
	public DeliveryOrderQueryResponse query(DeliveryOrderPrintRequest request);
	//提货单展示
	public DeliveryOrderPrintResponse display(DeliveryOrderPrintRequest request);
	//提货单打印
	public void print(DeliveryOrderPrintInfosRequest request) throws BusinessException, SystemException;

}
