package com.huaihkiss.tcc.configuration;

import com.huaihkiss.tcc.exception.LocalTransactionException;
import com.huaihkiss.tcc.properties.ZooKeeperProperties;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@EnableConfigurationProperties(ZooKeeperProperties.class)
@Configuration
public class ZooKeeperConfiguration {
    public static final String TRANSACTION_ROOT_NODE_NAME = "/tcc-transaction-starter";
    public static final String TRANSACTION_LOCAL_NODE_NAME = TRANSACTION_ROOT_NODE_NAME + "/local";
    @Bean
    public ZooKeeper zooKeeper(ZooKeeperProperties zooKeeperProperties){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try{
            ZooKeeper zooKeeper = new ZooKeeper(zooKeeperProperties.getNodes(), zooKeeperProperties.getTimeOut(), new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                        countDownLatch.countDown();
                    }
                }
            });
            countDownLatch.await();
            return zooKeeper;
        } catch (IOException e) {
            e.printStackTrace();
            throw new LocalTransactionException("zookeeper connect time out");

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

    }
}
