package jj.biztrip.batch.krx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ExceptionHandlingScheduledExecutor extends ScheduledThreadPoolExecutor {
    private final Thread.UncaughtExceptionHandler ueh;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public ExceptionHandlingScheduledExecutor(int corePoolSize, Thread.UncaughtExceptionHandler ueh) {
        super(corePoolSize);
        this.ueh = ueh;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(wrap(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrap(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(wrap(task));
    }

    private Runnable wrap(final Runnable runnable) {
        return () -> {
                try {
                    runnable.run();
                } catch (final Throwable t) {
                    DataGather dataGather = (DataGather)runnable;
                    logger.error("[##MONITOR##] UNCAUGHT_EXCEPTION Occur:" + dataGather.getCodeList() + "[" + t.getMessage() + "]");
                    ueh.uncaughtException(Thread.currentThread(), t);
                    //throw t; # 주석 해제할경우 Task가 종료되서 이후 스케쥴링이 기동을 안하니 판단후 주석해제
                }
            };
    }

    private <T> Callable<T> wrap(final Callable<T> callable) {
        return () -> {
                try {
                    return callable.call();
                } catch (Throwable t) {
                    logger.error("[##MONITOR##] UNCAUGHT_EXCEPTION Occur:["+ t.getMessage() + "]");
                    ueh.uncaughtException(Thread.currentThread(), t);
                    throw t;
                }
            };
    }
}
