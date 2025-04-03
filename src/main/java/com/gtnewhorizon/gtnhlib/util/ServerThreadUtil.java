package com.gtnewhorizon.gtnhlib.util;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import net.minecraft.server.MinecraftServer;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

@SuppressWarnings("UnstableApiUsage")
public class ServerThreadUtil {

    private static final Logger logger = LogManager.getLogger();
    private static final Queue<FutureTask<?>> futureTaskQueue = Queues.newArrayDeque();
    private static MinecraftServer server;
    private static Thread serverThread;

    @ApiStatus.Internal
    public static void runJobs() {
        synchronized (futureTaskQueue) {
            while (!futureTaskQueue.isEmpty()) {
                FutureTask<?> task = futureTaskQueue.poll();
                try {
                    task.run();
                    task.get(); // result is ignored
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Error executing task", e);
                }
            }
        }
    }

    @ApiStatus.Internal
    public static void setup(MinecraftServer server, Thread serverThread) {
        ServerThreadUtil.server = server;
        ServerThreadUtil.serverThread = serverThread;
    }

    @ApiStatus.Internal
    public static void clear() {
        futureTaskQueue.clear();
        ServerThreadUtil.server = null;
        ServerThreadUtil.serverThread = null;
    }

    public static boolean isCallingFromMinecraftThread() {
        return Thread.currentThread() == serverThread;
    }

    public static <V> ListenableFuture<V> callFromMainThread(Callable<V> callable) {
        Validate.notNull(callable);

        if (!isCallingFromMinecraftThread() && !server.isServerStopped()) {
            ListenableFutureTask<V> listenableFutureTask = ListenableFutureTask.create(callable);

            synchronized (futureTaskQueue) {
                futureTaskQueue.add(listenableFutureTask);
                return listenableFutureTask;
            }
        } else {
            try {
                return Futures.immediateFuture(callable.call());
            } catch (Exception e) {
                return Futures.immediateFailedCheckedFuture(e);
            }
        }
    }

    public static ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule) {
        Validate.notNull(runnableToSchedule);
        return callFromMainThread(Executors.callable(runnableToSchedule));
    }

}
