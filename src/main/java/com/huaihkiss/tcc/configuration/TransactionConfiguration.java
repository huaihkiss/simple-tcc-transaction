package com.huaihkiss.tcc.configuration;

import com.huaihkiss.tcc.aspect.GlobalAspect;
import com.huaihkiss.tcc.aspect.LocalTransactionAspect;
import com.huaihkiss.tcc.interceptor.FeignHeaderInterceptor;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "tcc.zookeeper",name = "enabled",matchIfMissing = true)
@AutoConfigureAfter({ZooKeeperConfiguration.class})
@Configuration
public class TransactionConfiguration {
    public static final ThreadLocal<String> XIDS = new ThreadLocal<String>();
    public static final String HEADER_XID = "X-TCC-XID";

    @Bean
    public GlobalAspect globalAspect(ZooKeeper zooKeeper){
        GlobalAspect globalAspect = new GlobalAspect(zooKeeper);
        return globalAspect;
    }

    @Bean
    public LocalTransactionAspect localTransactionAspect(ZooKeeper zooKeeper){
        LocalTransactionAspect localTransactionAspect = new LocalTransactionAspect(zooKeeper);
        return localTransactionAspect;
    }
    @Bean
    public FeignHeaderInterceptor feignHeaderInterceptor(){
        return new FeignHeaderInterceptor();
    }
    @Bean
    public LocalTransactionFilter localTransactionFilter(){
        return new LocalTransactionFilter();
    }
}
