package Coursework.operators;

import Coursework.core.Observable;
import Coursework.core.OnSubscribe;
import Coursework.core.Observer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class FilterOperator {
    public static <T> Observable<T> filter(Observable<T> source, Predicate<? super T> predicate) {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Observer<? super T> downstream) {
                AtomicBoolean errored = new AtomicBoolean(false);
                source.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        if (errored.get()) {
                            return;
                        }
                        boolean pass;
                        try {
                            pass = predicate.test(item);
                        } catch (Throwable e) {
                            if (errored.compareAndSet(false, true)) {
                                downstream.onError(e);
                            }
                            return;
                        }
                        if (pass) {
                            downstream.onNext(item);
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
                        if (!errored.get()) {
                            downstream.onComplete();
                        }
                    }
                });
            }
        });
    }
}