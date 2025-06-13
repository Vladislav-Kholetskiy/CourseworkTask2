package Coursework.core;

/**
 * Функциональный интерфейс для логики подписки
 */
@FunctionalInterface
public interface OnSubscribe<T> {
    void call(Observer<? super T> observer);
}

