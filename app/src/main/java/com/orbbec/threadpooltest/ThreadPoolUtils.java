package com.orbbec.threadpooltest;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author tanzhuohui
 * @date 2018/1/26
 */

public class ThreadPoolUtils {
    private volatile static ThreadPoolUtils utils = null;
    private ThreadPool poolExecutor;
    private static ArrayDeque<Runnable> runnableArrayDeque = new ArrayDeque<>();
    private Runnable nowRunnable;
    private static Builder builder;
    /**
     * 用于保存任务的集合
     */
    private Map<String, Future<Integer>> mThreadFutureTaskMap;

    ThreadPoolUtils(Builder builder) {
        if (poolExecutor == null) {
            Log.d("tzh" , "poolExecutor is null");
            poolExecutor = new ThreadPool(
                    builder.corePoolSize,
                    builder.maximumPoolSize,
                    builder.keepAliveTime,
                    builder.unit,
                    builder.workQueue,
                    builder.threadFactory,
                    builder.handler) {
            };
            mThreadFutureTaskMap = new HashMap<>();
        }
    }

    public static ThreadPoolUtils getInstance() {
        if (utils == null) {
            synchronized (ThreadPoolUtils.class) {
                if (utils == null) {
                    utils = new ThreadPoolUtils(builder);
                }
            }
        }
        return utils;
    }

    private class ThreadPool extends ThreadPoolExecutor {
        ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            pauseLock.lock();
            try {
                while (isPaused) {
                    unpaused.await();
                }
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        public void pauseThreadPool() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        public void resumeThreadPool() {
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }

    }

    public static class Builder {
        private int corePoolSize;
        private int maximumPoolSize;
        private long keepAliveTime;
        private TimeUnit unit;
        private BlockingQueue<Runnable> workQueue;
        private ThreadFactory threadFactory;
        private RejectedExecutionHandler handler;

        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder keepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public Builder unit(TimeUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder workQueue(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            return this;
        }

        public Builder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder handler(RejectedExecutionHandler handler) {
            this.handler = handler;
            return this;
        }

        public Builder build() {
            if (handler == null) {
                handler = new ThreadPoolExecutor.AbortPolicy();
            }

            if (threadFactory == null) {
                threadFactory = Executors.defaultThreadFactory();
            }
            builder = this;
            return this;
        }
    }

    /**
     * 执行线程
     *
     * @param runnable 线程Runnable
     */
    public synchronized void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (poolExecutor != null) {
            poolExecutor.execute(runnable);
        }
    }

    /**
     * 将runable的线程中断标志位设定为中断
     * @param runnable
     */
    public void remove(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        poolExecutor.remove(runnable);
    }

    /**
     * 添加单个任务
     *
     * @param keyTag 任务的标签
     * @param task   任务
     * @return 任务对象
     */
    public Future addTask(Runnable task, String keyTag) {
        //添加并提交任务
        RunnableFuture futureTask = (RunnableFuture) poolExecutor.submit(task);
        //将任务添加到集合中
        Future put = mThreadFutureTaskMap.put(keyTag, futureTask);
        return futureTask;
    }

    /**
     * 移除单个任务
     *
     * @param key 任务标签
     * @return true 为成功
     */
    public boolean removeTask(String key) {
        //任务移除标识
        boolean cancel = false;
        //根据KEY来获取对应的Future对象
        Set<String> keySet = mThreadFutureTaskMap.keySet();
        if (keySet.contains(key)) {
            Future future = mThreadFutureTaskMap.get(key);

            //当前的任务已经被执行完毕，不进行操作
            if (!future.isCancelled()) {
                cancel = future.cancel(true);
                if (cancel) {
                    //当取消成功后，从本地Key集合中移除标识
                    mThreadFutureTaskMap.remove(key);
                }
            } else {
                mThreadFutureTaskMap.remove(key);
            }
        }
        return cancel;
    }

    /**
     * 暂停线程池中的任务
     */
    public void pauseThreadPool() {
        poolExecutor.pauseThreadPool();
    }

    /**
     * 恢复线程池中的任务
     */
    public void resumeThreadPool() {
        poolExecutor.resumeThreadPool();
    }


    /**
     * 线程串行执行
     * @param r 加入到串行线程队列的任务
     */
    public synchronized void serial(final Runnable r) {
        runnableArrayDeque.offer(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        // 第一次入队列时mActivie为空，因此需要手动调用scheduleNext方法
        if (nowRunnable == null) {
            scheduleNext();
        }
    }

    private void scheduleNext() {
        if ((nowRunnable = runnableArrayDeque.poll()) != null) {
            poolExecutor.execute(nowRunnable);
        }
    }

    /**
     * 阻塞线程
     * @param runnable 要阻塞的任务
     */
    public void join(Runnable runnable){
        LockSupport.park(runnable);
    }

    /**
     * 唤醒被阻塞的线程 , 与join方法配合使用
     * @param thread 要叫醒的线程
     */
    public void wakeup(Thread thread){
        LockSupport.unpark(thread);
    }
}
