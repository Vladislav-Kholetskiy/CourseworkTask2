package Coursework.operators;

import Coursework.core.Observable;
import Coursework.core.OnSubscribe;
import Coursework.core.Observer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class MapOperator {
    public static <T, R> Observable<R> map(Observable<T> source, Function<? super T, ? extends R> mapper) {
        return Observable.create(new OnSubscribe<R>() {
            @Override
            public void call(Observer<? super R> downstream) {
                AtomicBoolean errored = new AtomicBoolean(false);
                source.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        if (errored.get()) {
                            return;
                        }
                        R mapped;
                        try {
                            mapped = mapper.apply(item);
                        } catch (Throwable e) {
                            if (errored.compareAndSet(false, true)) {
                                downstream.onError(e);
                            }
                            return;
                        }
                        downstream.onNext(mapped);
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