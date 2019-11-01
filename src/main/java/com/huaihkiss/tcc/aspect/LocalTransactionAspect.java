package com.huaihkiss.tcc.aspect;

import com.alibaba.fastjson.JSON;
import com.huaihkiss.tcc.annotations.TryMethod;
import com.huaihkiss.tcc.configuration.TransactionConfiguration;
import com.huaihkiss.tcc.configuration.ZooKeeperConfiguration;
import com.huaihkiss.tcc.exception.GlobalTransationException;
import com.huaihkiss.tcc.exception.LocalTransactionException;
import com.huaihkiss.tcc.info.TransactionInfo;
import com.huaihkiss.tcc.zk.watcher.TransactionWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
public class LocalTransactionAspect {
    private ZooKeeper zooKeeper;

    public LocalTransactionAspect(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Around(value = "@annotation(tryMethod)")
    public void execute(ProceedingJoinPoint point, TryMethod tryMethod) throws Throwable {
        Signature signature = point.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method targetMethod = methodSignature.getMethod();
        String confirmMethodName = tryMethod.confirmMethodName();
        String cancelMethodName = tryMethod.cancelMethodName();
        Object target = point.getTarget();
        try{
            zooKeeper.create(ZooKeeperConfiguration.TRANSACTION_ROOT_NODE_NAME,"create".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }catch (Throwable ex){
//            ex.printStackTrace();
        }

        String xid = TransactionConfiguration.XIDS.get();
        if(StringUtils.isEmpty(xid)){
            throw new LocalTransactionException("xid is empty");
        }
        try {
            point.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        }
        String nodeName = ZooKeeperConfiguration.TRANSACTION_LOCAL_NODE_NAME + "/" + targetMethod.getName() + "/" + xid;
        try{
            zooKeeper.create(ZooKeeperConfiguration.TRANSACTION_LOCAL_NODE_NAME,"create".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }catch (Throwable ex){
//            ex.printStackTrace();
        }
        try{
            zooKeeper.create(ZooKeeperConfiguration.TRANSACTION_LOCAL_NODE_NAME + "/" + targetMethod.getName(),"create".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }catch (Throwable ex){
//            ex.printStackTrace();
        }
        //修改znode中的值
        TransactionWatcher transactionWatcher = null;
        try{
            transactionWatcher = new TransactionWatcher() {
                @Override
                public void confirm() {
                    try {
                        executeResultMethod(targetMethod,confirmMethodName,target,point.getArgs(),nodeName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void cancel() {
                    try {
                        executeResultMethod(targetMethod,cancelMethodName,target,point.getArgs(),nodeName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                }
            };
        }catch (Throwable ex){
            ex.printStackTrace();
        }
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionWatcher.setZooKeeper(zooKeeper);
        transactionWatcher.setNodeName(ZooKeeperConfiguration.TRANSACTION_ROOT_NODE_NAME + "/" + xid);
        transactionInfo.setXid(xid);
        transactionInfo.setMethodName(targetMethod.getName());
        transactionInfo.setArgs(point.getArgs());
        try{
            zooKeeper.create(nodeName,JSON.toJSONString(transactionInfo).getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }catch (Throwable ex){
//            ex.printStackTrace();
        }
        zooKeeper.getData(ZooKeeperConfiguration.TRANSACTION_ROOT_NODE_NAME + "/" + xid, transactionWatcher,new Stat());

    }

    private void executeResultMethod(Method targetMethod, String methodName, Object target, Object[] args,String nodeName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, KeeperException, InterruptedException {
        if(methodName != null && methodName.length() > 0){
            Method method = target.getClass().getDeclaredMethod(methodName, targetMethod.getParameterTypes());
            if(method == null){
                throw new GlobalTransationException("result method parameters not equals method parameters");
            }
            method.invoke(target,args);
            zooKeeper.delete(nodeName,-1);
        }
    }
}