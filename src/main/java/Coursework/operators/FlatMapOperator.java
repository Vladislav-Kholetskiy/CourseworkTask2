package Coursework.operators;

import Coursework.core.Observable;
import Coursework.core.OnSubscribe;
import Coursework.core.Observer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class FlatMapOperator {
    public static <T, R> Observable<R> flatMap(Observable<T> source,
                                               Function<? super T, ? extends Observable<? extends R>> mapper) {
        return Observable.create(new OnSubscribe<R>() {
            @Override
            public void call(Observer<? super R> downstream) {
                AtomicBoolean errored = new AtomicBoolean(false);
                AtomicInteger active = new AtomicInteger(1);
                source.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        if (errored.get()) {
                            return;
                        }
                        Observable<? extends R> inner;
                        try {
                            inner = mapper.apply(item);
                        } catch (Throwable e) {
                            if (errored.compareAndSet(false, true)) {
                                downstream.onError(e);
                            }
                            return;
                        }
                        active.incrementAndGet();
                        inner.subscribe(new Observer<R>() {
                            @Override
                            public void onNext(R r) {
                                if (!errored.get()) {
                                    downstream.onNext(r);
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                if (errored.compareAndSet(false, true)) {
                                    downstream.onError(t);
                                }
                            }

                            @Override
                            public void onComplete() {
                                if (active.decrementAndGet() == 0 && !errored.get()) {
                                    downstream.onComplete();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (errored.compareAndSet(false, true)) {
                            downstream.onError(t);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (active.decrementAndGet() == 0 && !errored.get()) {
                            downstream.onComplete();
                        }
                    }
                });
            }
        });
    }
}
