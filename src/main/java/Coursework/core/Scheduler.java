package Coursework.core;

/**
 * Интерфейс Scheduler для выполнения задач
 */
public interface Scheduler {
    void execute(Runnable task);
}
