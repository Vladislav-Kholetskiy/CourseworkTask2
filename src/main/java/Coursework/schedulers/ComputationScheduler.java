package Coursework.schedulers;

import Coursework.core.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Аналог Schedulers.computation(): пул фиксированного размера на количество CPU
 */
public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}