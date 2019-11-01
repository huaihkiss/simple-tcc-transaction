package com.huaihkiss.tcc.zk.watcher;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TransactionWatcher implements Watcher {
    private String nodeName;
    private ZooKeeper zooKeeper;
    private AtomicBoolean isFinally = new AtomicBoolean(false);
    @Override
    public void process(WatchedEvent watchedEvent) {
        if(!watchedEvent.getPath().equalsIgnoreCase(nodeName)){
            return;
        }
        if(watchedEvent.getType() != null && watchedEvent.getType() == Event.EventType.NodeDataChanged){
            boolean result = isFinally.compareAndSet(false, true);
            if(result){
                try {
                    byte[] data = zooKeeper.getData(watchedEvent.getPath(), false, new Stat());

                    if(data != null && data.length > 0){
                        String transationStatus = new String(data);
                        if("confirm".equalsIgnoreCase(transationStatus)){
                            confirm();
                        }else if("cancel".equalsIgnoreCase(transationStatus)){
                            cancel();
                        }
                    }
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }else{
            if(!isFinally.get()&&watchedEvent.getType() != null && watchedEvent.getType() == Event.EventType.NodeDeleted){
                cancel();
            }
        }
    }
    public abstract void confirm();
    public abstract void cancel();
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
