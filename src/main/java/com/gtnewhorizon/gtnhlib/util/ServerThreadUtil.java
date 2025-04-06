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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

@SuppressWarnings("UnstableApiUsage")
public class ServerThreadUtil {

    @NotNull
    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private static final Queue<FutureTask<?>> futureTaskQueue = Queues.newArrayDeque();

    @Nullable
    private static MinecraftServer server;

    @Nullable
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

    /**
     * @return {@code true} if current thread is server thread. {@code false} if either current thread is not server
     *         thread or the server thread has not been captured yet.
     */
    public static boolean isCallingFromMinecraftThread() {
        return Thread.currentThread() == serverThread;
    }

    /**
     * Run a {@link Callable} in server thread.
     * <p>
     * If the current thread is server thread, it will be invoked immediately. Otherwise, it will be invoked in the next
     * server loop.
     *
     * @throws IllegalStateException if the server is {@code null} or the server is stopped.
     */
    public static <V> ListenableFuture<V> callFromMainThread(Callable<V> callable) {
        Validate.notNull(callable);

        if (server == null || server.isServerStopped()) {
            throw new IllegalStateException("Server is not set or not running");
        }

        if (!isCallingFromMinecraftThread()) {
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

    /**
     * Run a {@link Runnable} in server thread.
     * <p>
     * If the current thread is server thread, it will be invoked immediately. Otherwise, it will be invoked in the next
     * server loop.
     *
     * @throws IllegalStateException if the server is {@code null} or the server is stopped.
     */
    public static ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule) {
        Validate.notNull(runnableToSchedule);
        return callFromMainThread(Executors.callable(runnableToSchedule));
    }

}
