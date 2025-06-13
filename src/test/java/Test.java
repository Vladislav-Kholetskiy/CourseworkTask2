import Coursework.core.Disposable;
import Coursework.core.Observable;
import Coursework.core.Observer;
import Coursework.schedulers.ComputationScheduler;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class Test {
    private List<Integer> received;
    private boolean completed;
    private Throwable error;

    @BeforeEach
    void setUp() {
        received = new ArrayList<>();
        completed = false;
        error = null;
    }

    /**
     * Создаёт Observer, который при первом onNext выполняет dispose(), если передан Disposable
     */
    private Observer<Integer> createIntegerObserver(AtomicReference<Disposable> dispRef) {
        return new Observer<>() {
            @Override
            public void onNext(Integer item) {
                received.add(item);
                if (dispRef != null && dispRef.get() != null) {
                    dispRef.get().dispose();
                }
            }

            @Override
            public void onError(Throwable t) {
                error = t;
            }

            @Override
            public void onComplete() {
                completed = true;
            }
        };
    }

    @org.junit.jupiter.api.Test
    void testMapOperator() {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onNext(3);
            obs.onComplete();
        });

        Observable<Integer> mapped = source.map(x -> x * 2);
        mapped.subscribe(createIntegerObserver(null));

        assertNull(error, "Не ожидается ошибка");
        assertTrue(completed, "Поток должен завершиться");
        assertEquals(List.of(2, 4, 6), received, "Результат должен быть [2, 4, 6]");
    }

    @org.junit.jupiter.api.Test
    void testFilterOperator() {
        Observable<Integer> source = Observable.create(obs -> {
            for (int i = 1; i <= 5; i++) {
                obs.onNext(i);
            }
            obs.onComplete();
        });

        Observable<Integer> filtered = source.filter(x -> x % 2 == 0);
        filtered.subscribe(createIntegerObserver(null));

        assertNull(error, "Не ожидается ошибка");
        assertTrue(completed, "Поток должен завершиться");
        assertEquals(List.of(2, 4), received, "Результат должен быть [2, 4]");
    }

    @org.junit.jupiter.api.Test
    void testFlatMapOperator() {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });

        Observable<Integer> flatMapped = source.flatMap(x ->
                Observable.create(inner -> {
                    inner.onNext(x);
                    inner.onNext(x + 10);
                    inner.onComplete();
                })
        );

        flatMapped.subscribe(createIntegerObserver(null));

        assertNull(error, "Не ожидается ошибка");
        assertTrue(completed, "Поток должен завершиться");
        assertEquals(List.of(1, 11, 2, 12), received, "Результат должен быть [1, 11, 2, 12]");
    }

    @org.junit.jupiter.api.Test
    void testMapOperatorError() {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(1);
            obs.onComplete();
        });

        RuntimeException ex = new RuntimeException("map error");
        Observable<Integer> mapped = source.map(x -> {
            throw ex;
        });
        mapped.subscribe(createIntegerObserver(null));

        assertSame(ex, error, "Должно быть передано исключение RuntimeException");
        assertFalse(completed, "Поток не должен завершиться при ошибке");
    }

    @org.junit.jupiter.api.Test
    void testDisposableStops() {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onNext(3);
            obs.onComplete();
        });

        // Синхронная подписка без автоматической отписки
        List<Integer> localReceived = new ArrayList<>();
        Observer<Integer> observer = new Observer<>() {
            @Override
            public void onNext(Integer item) {
                localReceived.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        };
        Disposable disposable = source.subscribe(observer);

        assertFalse(disposable.isDisposed(), "Disposable должен быть активен после подписки");
        disposable.dispose();
        assertTrue(disposable.isDisposed(), "Disposable должен быть отключён после dispose");
    }

    @org.junit.jupiter.api.Test
    void testSubscribeOnScheduler() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(42);
            obs.onComplete();
        });
        String mainThread = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        Coursework.schedulers.IOThreadScheduler ioScheduler = new Coursework.schedulers.IOThreadScheduler();
        source.subscribeOn(ioScheduler).subscribe(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                threadName.set(Thread.currentThread().getName());
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        boolean finished = latch.await(1, TimeUnit.SECONDS);
        assertTrue(finished, "Ожидается вызов onNext");
        assertNotEquals(mainThread, threadName.get(), "onNext должен выполняться не в основном потоке");
    }

    @org.junit.jupiter.api.Test
    void testObserveOnScheduler() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(7);
            obs.onComplete();
        });
        String mainThread = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        Coursework.schedulers.SingleThreadScheduler singleScheduler = new Coursework.schedulers.SingleThreadScheduler();
        source.observeOn(singleScheduler).subscribe(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                threadName.set(Thread.currentThread().getName());
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        boolean finished = latch.await(1, TimeUnit.SECONDS);
        assertTrue(finished, "Ожидается вызов onNext");
        assertNotEquals(mainThread, threadName.get(), "onNext должен выполняться не в основном потоке");
    }

    @org.junit.jupiter.api.Test
    void testSubscribeOnComputationScheduler() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(99);
            obs.onComplete();
        });
        String mainThread = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        ComputationScheduler compScheduler = new ComputationScheduler();
        source
                .subscribeOn(compScheduler)
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        threadName.set(Thread.currentThread().getName());
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Ожидается вызов onNext");
        assertNotEquals(mainThread, threadName.get(),
                "onNext должен выполняться не в основном потоке");
    }

    @org.junit.jupiter.api.Test
    void testObserveOnComputationScheduler() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(123);
            obs.onComplete();
        });
        String mainThread = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        ComputationScheduler compScheduler = new ComputationScheduler();
        source
                .observeOn(compScheduler)
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        threadName.set(Thread.currentThread().getName());
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Ожидается вызов onNext");
        assertNotEquals(mainThread, threadName.get(),
                "onNext должен выполняться не в основном потоке");
    }
}

