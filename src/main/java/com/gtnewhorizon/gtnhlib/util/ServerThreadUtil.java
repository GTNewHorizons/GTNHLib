package com.gtnewhorizon.gtnhlib.util;

import java.util.Objects;
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
import org.jetbrains.annotations.Range;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

@SuppressWarnings("UnstableApiUsage")
public class ServerThreadUtil {

    @NotNull
    private static final Logger logger = LogManager.getLogger();

    /**
     * If the remaining time to run the next server loop is less than this, we stop stealing and return to the main
     * loop.
     */
    private static final int STOP_STEALING_TIME_MS = 3;

    /**
     * If the work stealing task is skipped more than this, it is moved to the {@link #futureTaskQueue} for the
     * guaranteed execution.
     */
    private static final int COUNT_BEFORE_MOVE_TO_GUARANTEED_QUEUE = 10;

    private static class WorkStealingTask {

        @NotNull
        final FutureTask<?> futureTask;

        int skippedCount = 0;

        public WorkStealingTask(@NotNull FutureTask<?> futureTask) {
            this.futureTask = futureTask;
        }

        /**
         * @return times skipped, including this time.
         */
        int bumpSkippedCount() {
            return ++skippedCount;
        }
    }

    /**
     * The queue of tasks that will all be executed in the next server loop.
     */
    @NotNull
    private static final Queue<FutureTask<?>> futureTaskQueue = Queues.newArrayDeque();

    /**
     * The queue of tasks that will be executed in the next server loop if there has remaining time interval for the
     * next loop.
     * <p>
     * Tasks that skipped because of no time interval remaining over 3 times will be moved to {@link #futureTaskQueue}.
     */
    @NotNull
    private static final Queue<WorkStealingTask> workStealingTaskQueue = Queues.newArrayDeque();

    @Nullable
    private static MinecraftServer server;

    @Nullable
    private static Thread serverThread;

    private static void runTaskBlocking(@NotNull FutureTask<?> futureTask) {
        try {
            futureTask.run();
            futureTask.get();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error executing task", e);
        }
    }

    @ApiStatus.Internal
    public static void runJobs() {
        synchronized (futureTaskQueue) {
            while (!futureTaskQueue.isEmpty()) {
                FutureTask<?> task = futureTaskQueue.poll();
                runTaskBlocking(task);
            }
        }
    }

    /**
     * Iterate all the remaining tasks in the work stealing queue; bump their skip count, and if gt 3, move it to the
     * {@link #futureTaskQueue} for guaranteed execution.
     */
    @ApiStatus.Internal
    public static void skipRemainingWorkStealingQueue() {
        for (var iter = workStealingTaskQueue.iterator(); iter.hasNext();) {
            WorkStealingTask task = iter.next();
            if (task.bumpSkippedCount() > COUNT_BEFORE_MOVE_TO_GUARANTEED_QUEUE) {
                iter.remove();
                futureTaskQueue.add(task.futureTask);
                logger.debug("Task {} was moved to the futureTaskQueue.", task);
            }
        }
    }

    /**
     * @param msRemainingTotal the current remaining time in ms for the next server loop
     * @return the updated remaining time in ms for the next server loop
     */
    @ApiStatus.Internal
    @Range(from = 1, to = Integer.MAX_VALUE)
    public static long runWorkStealingJobs(final long msRemainingTotal) {
        long msStart = System.currentTimeMillis();
        long msElapsed = 0;

        while (!workStealingTaskQueue.isEmpty()) {
            msElapsed = System.currentTimeMillis() - msStart;
            if (msElapsed + STOP_STEALING_TIME_MS >= msRemainingTotal) { // time's up, stop stealing
                skipRemainingWorkStealingQueue();
                break;
            }

            WorkStealingTask task = Objects.requireNonNull(workStealingTaskQueue.poll());
            runTaskBlocking(task.futureTask);
        }

        // yeah, I don't know why MC make it at least 1, but I keep the behavior.
        return Math.max(1, msRemainingTotal - msElapsed);
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

    /**
     * Schedule a work stealing {@link Callable} in server thread.
     * <p>
     * If it is skipped more than {@link #COUNT_BEFORE_MOVE_TO_GUARANTEED_QUEUE} times because of too less time to
     * execute, it will be moved to {@link #futureTaskQueue}.
     *
     * @throws IllegalStateException if the server is {@code null} or the server is stopped.
     */
    public static <V> ListenableFuture<V> addWorkStealingTask(Callable<V> callable) {
        Validate.notNull(callable);

        if (server == null || server.isServerStopped()) {
            throw new IllegalStateException("Server is not set or not running");
        }

        ListenableFutureTask<V> listenableFutureTask = ListenableFutureTask.create(callable);

        synchronized (workStealingTaskQueue) {
            workStealingTaskQueue.add(new WorkStealingTask(listenableFutureTask));
            return listenableFutureTask;
        }
    }

    /**
     * Schedule a work stealing {@link Runnable} in server thread.
     * <p>
     * If it is skipped more than {@link #COUNT_BEFORE_MOVE_TO_GUARANTEED_QUEUE} times because of too less time to
     * execute, it will be moved to {@link #futureTaskQueue}.
     *
     * @throws IllegalStateException if the server is {@code null} or the server is stopped.
     */
    public static ListenableFuture<Object> addWorkStealingTask(Runnable runnable) {
        Validate.notNull(runnable);
        return addWorkStealingTask(Executors.callable(runnable));
    }

}
