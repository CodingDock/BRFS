package com.bonree.brfs.rebalance.task;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bonree.brfs.common.rebalance.Constants;
import com.bonree.brfs.common.service.ServiceManager;
import com.bonree.brfs.common.utils.JsonUtils;
import com.bonree.brfs.common.zookeeper.curator.CuratorClient;
import com.bonree.brfs.common.zookeeper.curator.cache.CuratorCacheFactory;
import com.bonree.brfs.common.zookeeper.curator.cache.CuratorTreeCache;
import com.bonree.brfs.duplication.storageregion.StorageRegion;
import com.bonree.brfs.duplication.storageregion.StorageRegionManager;
import com.bonree.brfs.rebalance.DataRecover;
import com.bonree.brfs.rebalance.DataRecover.RecoverType;
import com.bonree.brfs.rebalance.recover.MultiRecover;
import com.bonree.brfs.rebalance.recover.VirtualRecover;
import com.bonree.brfs.rebalance.task.listener.TaskExecutorListener;
import com.bonree.brfs.server.identification.ServerIDManager;

/*******************************************************************************
 * 版权信息：博睿宏远科技发展有限公司
 * Copyright: Copyright (c) 2007博睿宏远科技发展有限公司,Inc.All Rights Reserved.
 * 
 * @date 2018年3月30日 下午3:11:15
 * @Author: <a href=mailto:weizheng@bonree.com>魏征</a>
 * @Description: 任务执行节点
 ******************************************************************************/
public class TaskOperation implements Closeable {

    private final static Logger LOG = LoggerFactory.getLogger(TaskOperation.class);

    private CuratorClient client;
    private ServerIDManager idManager;
    private CuratorTreeCache treeCache;
    private String tasksPath;
    private String dataDir;
    private StorageRegionManager snManager;
    private ServiceManager serviceManager;
    private String baseRoutesPath;
    private ExecutorService es = Executors.newFixedThreadPool(10, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "task_executor");
        }
    });

    public TaskOperation(final CuratorClient client, final String baseBalancePath, String baseRoutesPath, ServerIDManager idManager, String dataDir, StorageRegionManager snManager, ServiceManager serviceManager) {
        this.client = client;
        this.idManager = idManager;
        this.tasksPath = ZKPaths.makePath(baseBalancePath, Constants.TASKS_NODE);
        this.baseRoutesPath = baseRoutesPath;
        this.dataDir = dataDir;
        treeCache = CuratorCacheFactory.getTreeCache();
        this.snManager = snManager;
        this.serviceManager = serviceManager;
    }

    public void start() {
        LOG.info("add tree cache:" + tasksPath);
        treeCache.addListener(tasksPath, new TaskExecutorListener(this));
    }

    public void launchDelayTaskExecutor(BalanceTaskSummary taskSummary, String taskPath) {
        DataRecover recover = null;
        List<String> multiIds = taskSummary.getOutputServers();
        if (multiIds.contains(idManager.getSecondServerID(taskSummary.getStorageIndex()))) {
            // 注册自身的selfMultiId,并设置为created阶段
            if (taskSummary.getTaskType() == RecoverType.NORMAL) { // 正常迁移任务
                StorageRegion node = snManager.findStorageRegionById(taskSummary.getStorageIndex());
                if (node == null) {
                    LOG.error("无法开启对" + taskSummary.getStorageIndex() + "的任务");
                    return;
                }
                String storageName = snManager.findStorageRegionById(taskSummary.getStorageIndex()).getName();
                recover = new MultiRecover(taskSummary, idManager, serviceManager, taskPath, client, dataDir, storageName, baseRoutesPath);
            } else if (taskSummary.getTaskType() == RecoverType.VIRTUAL) { // 虚拟迁移任务
                StorageRegion node = snManager.findStorageRegionById(taskSummary.getStorageIndex());
                if (node == null) {
                    LOG.error("无法开启对" + taskSummary.getStorageIndex() + "的任务");
                    return;
                }
                String storageName = snManager.findStorageRegionById(taskSummary.getStorageIndex()).getName();
                recover = new VirtualRecover(client, taskSummary, taskPath, dataDir, storageName, idManager, serviceManager);
            }

            updateTaskStatus(taskSummary, TaskStatus.RUNNING);
            launchTask(recover);
        }
    }

    public void updateTaskStatus(BalanceTaskSummary task, TaskStatus status) {
        task.setTaskStatus(status);
        String taskNode = ZKPaths.makePath(tasksPath, String.valueOf(task.getStorageIndex()), Constants.TASK_NODE);
        client.setData(taskNode, JsonUtils.toJsonBytesQuietly(task));
    }

    /** 概述：生成一个具有延时的任务
     * @param delay
     * @param recover
     * @user <a href=mailto:weizheng@bonree.com>魏征</a>
     */
    private void launchTask(final DataRecover recover) {
        es.execute(new Runnable() {
            @Override
            public void run() {
                recover.recover();
            }
        });
    }

    @Override
    public void close() throws IOException {
        es.shutdown();
        treeCache.cancelListener(tasksPath);
    }
}
