package com.huaihkiss.tcc.interceptor;

import com.huaihkiss.tcc.configuration.TransactionConfiguration;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.concurrent.ThreadPoolExecutor;

public class FeignHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String xid = TransactionConfiguration.XIDS.get();
        if(xid == null || xid.length() < 1){
            return;
        }
        template.header(TransactionConfiguration.HEADER_XID,xid);
    }
}
