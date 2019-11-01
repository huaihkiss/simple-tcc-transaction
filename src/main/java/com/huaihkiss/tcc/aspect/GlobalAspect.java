package com.huaihkiss.tcc.aspect;

import com.alibaba.fastjson.JSON;
import com.huaihkiss.tcc.annotations.GlobalTransaction;
import com.huaihkiss.tcc.configuration.TransactionConfiguration;
import com.huaihkiss.tcc.configuration.ZooKeeperConfiguration;
import com.huaihkiss.tcc.exception.GlobalTransationException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
@Aspect
public class GlobalAspect {
    @Autowired
    private ZooKeeper zooKeeper;

    public GlobalAspect(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Around(value = "@annotation(globalTransaction)")
    public void execute(ProceedingJoinPoint point, GlobalTransaction globalTransaction) throws Throwable {
        Signature signature = point.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method targetMethod = methodSignature.getMethod();
        String confirmMethodName = globalTransaction.confirmMethodName();
        String cancelMethodName = globalTransaction.cancelMethodName();
        Object target = point.getTarget();
        Object[] args = point.getArgs();
        String xid = "TCC-" + System.currentTimeMillis() + "-" + new Random(1000000).nextInt();
        String nodeName = ZooKeeperConfiguration.TRANSACTION_ROOT_NODE_NAME + "/" + xid;
        try{
            zooKeeper.create(ZooKeeperConfiguration.TRANSACTION_ROOT_NODE_NAME,"create".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }catch (Throwable ex){
//            ex.printStackTrace();
        }
        try{
            zooKeeper.create(nodeName,"create".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL);
        }catch (Throwable ex){
//            ex.printStackTrace();
        }
        //znode改变数据状态 持久化
        try {

            TransactionConfiguration.XIDS.set(xid);


            point.proceed();



            if(!StringUtils.isEmpty(confirmMethodName)){
                executeResultMethod(targetMethod, confirmMethodName, target, args);
            }
            zooKeeper.setData(nodeName,"confirm".getBytes(),-1);
        } catch (Throwable throwable) {
            if(!(throwable instanceof GlobalTransationException)){

            }
            if(!StringUtils.isEmpty(cancelMethodName)){
                executeResultMethod(targetMethod, cancelMethodName, target, args);
            }

            zooKeeper.setData(nodeName,"cancel".getBytes(),-1);
            throw throwable;
        }finally {
            TransactionConfiguration.XIDS.set(null);
        }

    }

    private void executeResultMethod(Method targetMethod, String methodName, Object target, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if(methodName != null && methodName.length() > 0){
            Method method = target.getClass().getDeclaredMethod(methodName, targetMethod.getParameterTypes());
            method.invoke(target,args);
            if(method == null){
                throw new GlobalTransationException("result method parameters not equals method parameters");
            }

        }
    }
}
