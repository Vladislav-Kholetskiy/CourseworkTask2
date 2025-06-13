package Coursework.core;

/**
 * Интерфейс для отмены подписки
 */
public interface Disposable {
    void dispose();
    boolean isDisposed();
}
