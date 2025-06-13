package Coursework.core;

import Coursework.operators.FilterOperator;
import Coursework.operators.FlatMapOperator;
import Coursework.operators.MapOperator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Класс Observable
 */
public class Observable<T> {
    private final OnSubscribe<T> onSubscribe;

    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    /**
     * Создаёт Observable с переданной логикой
     */
    public static <T> Observable<T> create(OnSubscribe<T> onSubscribe) {
        return new Observable<>(onSubscribe);
    }

    /**
     * Подписывает Observer на события и возвращает Disposable для отмены подписки.
     * Эмиссия событий запускается в отдельном потоке.
     */
    public Disposable subscribe(Observer<? super T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observer<T> safeObserver = new Observer<>() {
            @Override
            public void onNext(T item) {
                if (!disposed.get()) {
                    observer.onNext(item);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (!disposed.get()) {
                    observer.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!disposed.get()) {
                    observer.onComplete();
                }
            }
        };

        Disposable disposable = new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };


        try {
            onSubscribe.call(safeObserver);
        } catch (Throwable t) {
            safeObserver.onError(t);
        }

        return disposable;
    }

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return MapOperator.map(this, mapper);
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return FilterOperator.filter(this, predicate);
    }

    public <R> Observable<R> flatMap(Function<? super T, ? extends Observable<? extends R>> mapper) {
        return FlatMapOperator.flatMap(this, mapper);
    }


    /**
     * Выполняет подписку в указанном Scheduler-е
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return Observable.create(downstream ->
                scheduler.execute(() -> this.subscribe(downstream))
        );
    }

    /**
     * Переключает контекст обработки onNext/onError/onComplete на указанный Scheduler
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return Observable.create(downstream ->
                this.subscribe(new Observer<>() {
                    @Override
                    public void onNext(T item) {
                        scheduler.execute(() -> downstream.onNext(item));
                    }

                    @Override
                    public void onError(Throwable t) {
                        scheduler.execute(() -> downstream.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(downstream::onComplete);
                    }
                })
        );
    }
}
