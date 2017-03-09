package com.ai.slp.order.api.orderlist.param;

import java.util.List;

import com.ai.opt.base.vo.BaseInfo;

/**
 * 订单相关信息 Date: 2016年8月25日 <br>
 * Copyright (c) 2016 asiainfo.com <br>
 * 
 * @author caofz
 */
public class BehindParentOrdOrderVo extends BaseInfo {

    private static final long serialVersionUID = 1L;
    
    /*租户id 参数和搜索引擎一致*/
    private String tenantid;
    
    /**
     * 订单来源 (受理渠道)
     */
    private String chlid;
  //  private String chlId;
    
    /**
     * 订单来源展示名称
     */
 //   private String chlIdName;
    private String chlidname;
    
    /**
     * 父订单id
     */
  //  private Long pOrderId;
    private Long porderid;
    
    /**
     * 买家帐号 (userid)
     */
    private String userid;
    
    /**
     * 用户姓名
     */
    private String username;
    
    /**
     * 绑定手机号 (用户相关)
     */
    private String usertel;
    
    /**
     * 是否需要物流
     */
    private String deliveryflag;
    
    /**
     * 是否需要物流展示名称
     */
    private String deliveryflagname;
    
    
    /**
     * 总优惠金额
     */
    private Long discountfee;


    /**
     * 总实收费用
     */
    private Long adjustfee;

    
    /**
     * 收件人电话
     */
    private String contacttel;
    
    /**
     * 积分
     */
    private Long points;
    
    /**
     * 优惠券
     */
    private Long totalcouponfee;
    
    /**
     * 父订单对应的子订单下的所有商品数量
     */
    private long totalprodsize;
    
    /**
     * 订单及商品信息
     */
  //  private List<BehindOrdOrderVo> orderList;
    private List<BehindOrdOrderVo> ordextendes;

	public String getChlid() {
		return chlid;
	}

	public void setChlid(String chlid) {
		this.chlid = chlid;
	}

	public String getChlidname() {
		return chlidname;
	}

	public void setChlidname(String chlidname) {
		this.chlidname = chlidname;
	}

	public Long getPorderid() {
		return porderid;
	}

	public void setPorderid(Long porderid) {
		this.porderid = porderid;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsertel() {
		return usertel;
	}

	public void setUsertel(String usertel) {
		this.usertel = usertel;
	}

	public String getDeliveryflag() {
		return deliveryflag;
	}

	public void setDeliveryflag(String deliveryflag) {
		this.deliveryflag = deliveryflag;
	}

	public String getDeliveryflagname() {
		return deliveryflagname;
	}

	public void setDeliveryflagname(String deliveryflagname) {
		this.deliveryflagname = deliveryflagname;
	}

	public Long getDiscountfee() {
		return discountfee;
	}

	public void setDiscountfee(Long discountfee) {
		this.discountfee = discountfee;
	}

	public Long getAdjustfee() {
		return adjustfee;
	}

	public void setAdjustfee(Long adjustfee) {
		this.adjustfee = adjustfee;
	}

	public String getContacttel() {
		return contacttel;
	}

	public void setContacttel(String contacttel) {
		this.contacttel = contacttel;
	}

	public Long getPoints() {
		return points;
	}

	public void setPoints(Long points) {
		this.points = points;
	}

	public Long getTotalcouponfee() {
		return totalcouponfee;
	}

	public void setTotalcouponfee(Long totalcouponfee) {
		this.totalcouponfee = totalcouponfee;
	}

	public long getTotalprodsize() {
		return totalprodsize;
	}

	public void setTotalprodsize(long totalprodsize) {
		this.totalprodsize = totalprodsize;
	}

	public List<BehindOrdOrderVo> getOrdextendes() {
		return ordextendes;
	}

	public void setOrdextendes(List<BehindOrdOrderVo> ordextendes) {
		this.ordextendes = ordextendes;
	}

	public String getTenantid() {
		return tenantid;
	}

	public void setTenantid(String tenantid) {
		this.tenantid = tenantid;
	}
}
