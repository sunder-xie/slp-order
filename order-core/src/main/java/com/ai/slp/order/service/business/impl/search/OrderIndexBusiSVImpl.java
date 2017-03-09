package com.ai.slp.order.service.business.impl.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.base.exception.SystemException;
import com.ai.opt.sdk.components.ses.SESClientFactory;
import com.ai.opt.sdk.util.CollectionUtil;
import com.ai.slp.order.api.sesdata.param.SesDataRequest;
import com.ai.slp.order.constants.OrdersConstants;
import com.ai.slp.order.constants.SearchConstants;
import com.ai.slp.order.dao.mapper.bo.OrdOdFeeProd;
import com.ai.slp.order.dao.mapper.bo.OrdOdFeeTotal;
import com.ai.slp.order.dao.mapper.bo.OrdOdLogistics;
import com.ai.slp.order.dao.mapper.bo.OrdOdProd;
import com.ai.slp.order.dao.mapper.bo.OrdOrder;
import com.ai.slp.order.search.bo.OrdProdExtend;
import com.ai.slp.order.search.bo.OrderInfo;
import com.ai.slp.order.search.bo.ProdInfo;
import com.ai.slp.order.service.atom.interfaces.IOrdOdFeeProdAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOdFeeTotalAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOdLogisticsAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOdProdAtomSV;
import com.ai.slp.order.service.atom.interfaces.IOrdOrderAtomSV;
import com.ai.slp.order.service.business.interfaces.search.IOrderIndexBusiSV;


@Service
public class OrderIndexBusiSVImpl implements IOrderIndexBusiSV {

	@Autowired
	private IOrdOdProdAtomSV ordOdProdAtomSV;
	@Autowired
	private IOrdOdLogisticsAtomSV ordOdLogisticsAtomSV;
	@Autowired
	private IOrdOdFeeTotalAtomSV ordOdFeeTotalAtomSV;
	@Autowired
	private IOrdOrderAtomSV ordOrderAtomSV;
	@Autowired
	private IOrdOdFeeProdAtomSV ordOdFeeProdAtomSV;
	
	@Override
    @Transactional
	public boolean insertSesData(SesDataRequest request) throws BusinessException, SystemException {
		 try {	
			 	String tenantId = request.getTenantId();
			 	Long parentOrderId = request.getParentOrderId();
			 	//父订单信息
			 	OrdOrder ord = ordOrderAtomSV.selectByOrderId(tenantId,parentOrderId);
				List<OrderInfo> orderList = new ArrayList<OrderInfo>();
				if(ord!=null){
					OrderInfo ordInfo = new OrderInfo();
					ordInfo.setTenantid(tenantId);
					ordInfo.setChlid(ord.getChlId());
					ordInfo.setPorderid(ord.getOrderId());
					ordInfo.setUsername(ord.getUserName());
					ordInfo.setUsertel(ord.getUserTel());
					ordInfo.setDeliveryflag(ord.getDeliveryFlag());
					ordInfo.setDeliveryflagname("");
					ordInfo.setOrdertime(ord.getOrderTime());
					// 获取手机号
					OrdOdLogistics ordOdLogistics = ordOdLogisticsAtomSV.selectByOrd(tenantId, parentOrderId);
					if(ordOdLogistics!=null) {
						ordInfo.setContacttel(ordOdLogistics.getContactTel());
					}
					// 获取积分
					List<OrdOdFeeProd> orderFeeProdList = ordOdFeeProdAtomSV.selectByOrderId(parentOrderId);
					long points = 0; 
					if (!CollectionUtil.isEmpty(orderFeeProdList)) {
						for (OrdOdFeeProd ordOdFeeProd : orderFeeProdList) {
							if (OrdersConstants.OrdOdFeeProd.PayStyle.JF.equals(ordOdFeeProd.getPayStyle())) {
								points += ordOdFeeProd.getPaidFee();
								break;
							}
						}
					}
					ordInfo.setPoints(points);
					// 查询费用信息
					OrdOdFeeTotal ordOdFeeTotal = ordOdFeeTotalAtomSV.selectByOrderId(tenantId, parentOrderId);
					if (ordOdFeeTotal != null) {
						ordInfo.setAdjustfee(ordOdFeeTotal.getAdjustFee());
						ordInfo.setDiscountfee(ordOdFeeTotal.getDiscountFee());
					}
					// 查询其它信息
					ordInfo = this.queryOrdProdExtends(ordInfo, ord, 
							tenantId, parentOrderId);
					orderList.add(ordInfo);
					SESClientFactory.getSearchClient(SearchConstants.SearchNameSpace).bulkInsert(orderList);
					
					
				}
		 }catch(Exception e){
			 throw new SystemException("","订单信息加入搜索引擎失败,订单ID:"+request.getParentOrderId());
		 }
            return true;
	}

	private OrderInfo queryOrdProdExtends(OrderInfo ordInfo,OrdOrder ord,String tenantId,
			Long parentOrderId) {
		List<OrdProdExtend> prodExtends=new ArrayList<OrdProdExtend>();
		//子订单
		List<OrdOrder> subOrders = ordOrderAtomSV.selectChildOrder(tenantId, parentOrderId);
		int totalprodsize = 0;
		if(!CollectionUtil.isEmpty(subOrders)) {
			//存在子订单
			for (OrdOrder ordOrder : subOrders) {
				OrdProdExtend prodExtend=new OrdProdExtend();
				List<ProdInfo> prodInfos=new ArrayList<ProdInfo>();
				prodExtend.setState(ordOrder.getState());
				prodExtend.setStatename("");
				prodExtend.setBusicode(ordOrder.getBusiCode());//父订单
				prodExtend.setParentorderid(parentOrderId);
				prodExtend.setOrderid(ordOrder.getOrderId());
				// 查询商品信息
				prodInfos = this.queryOrdProd(prodInfos,tenantId,
						ordOrder.getOrderId());
				prodExtend.setProdsize(prodInfos.size());
				totalprodsize=prodInfos.size()+totalprodsize;
				prodExtend.setProdinfos(prodInfos);
				prodExtends.add(prodExtend);
			}
		}else {
			//不存在子订单
			List<ProdInfo> prodInfos=new ArrayList<ProdInfo>();
			OrdProdExtend prodExtend=new OrdProdExtend();
			prodExtend.setState(ord.getState());
			prodExtend.setStatename("");
			prodExtend.setBusicode(ord.getBusiCode());//父订单
			prodExtend.setParentorderid(parentOrderId);
			prodExtend.setOrderid(0);
			// 查询商品信息
			prodInfos = this.queryOrdProd(prodInfos,tenantId,
					parentOrderId);
			prodExtend.setProdsize(prodInfos.size());
			totalprodsize=prodInfos.size()+totalprodsize;
			prodExtend.setProdinfos(prodInfos);
			prodExtends.add(prodExtend);
		}
		ordInfo.setTotalprodsize(totalprodsize);
		ordInfo.setOrdextendes(prodExtends);
		return ordInfo;
	}

	private List<ProdInfo>  queryOrdProd(List<ProdInfo> prodInfos,String tenantId, 
			Long orderId) {
		// TODO Auto-generated method stub
		List<OrdOdProd> ordOdProds = ordOdProdAtomSV.selectByOrd(tenantId, orderId);
		if (!CollectionUtil.isEmpty(ordOdProds)) {
			for (OrdOdProd ordOdProd : ordOdProds) {
				ProdInfo prodInfo=new ProdInfo();
				prodInfo.setBuysum(ordOdProd.getBuySum());
				prodInfo.setProdname(ordOdProd.getProdName());
				prodInfos.add(prodInfo);
			}
		}
		return prodInfos;
	}

	
	@Override
	public List<OrderInfo> orderFillQuery(List<OrdOrder> ordList) throws BusinessException, SystemException {
		// TODO Auto-generated method stub
		return null;
	}
}
