package com.ai.slp.order.elasticjob;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;

@Service
public class OrderOfcJob implements SimpleJob {

    private static final Logger log = LoggerFactory.getLogger(OrderOfcJob.class);

    @Autowired
    OrdProdTaskJob ordProdTaskJob;
    
    @Autowired
    OrderTaskJob orderTaskJob;
    
    @Override
    public void execute(ShardingContext context)  {
        log.error("执行订单ofc任务");
        orderTaskJob.run();
        ordProdTaskJob.run();
        log.error("结束执行订单ofc任务..");

    }

}