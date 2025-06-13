package Coursework.core;

/**
 * Интерфейс Observer для подписки на события Observable
 */
public interface Observer<T> {
    void onNext(T item);
    void onError(Throwable t);
    void onComplete();
}

