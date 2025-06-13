package Coursework;

import Coursework.core.Observable;
import Coursework.core.Observer;
import Coursework.schedulers.ComputationScheduler;
import Coursework.schedulers.IOThreadScheduler;
import Coursework.schedulers.SingleThreadScheduler;

import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch doneLatch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
                    for (int i = 1; i <= 5; i++) {
                        emitter.onNext(i);
                    }
                    emitter.onComplete();
                })
                // 1) Эмиссия на пуле для I/O
                .subscribeOn(new IOThreadScheduler())
                // 2) Фильтрация чётных чисел
                .filter(i -> i % 2 == 0)
                // 3) Преобразование в строку
                .map(i -> "Value: " + i)
                // 4) Для каждого создаём под-Observable с задержкой
                .flatMap(str ->
                        Observable.<String>create(em -> {
                                    try {
                                        Thread.sleep(100); // имитируем работу
                                        em.onNext(str + " (processed)");
                                        em.onComplete();
                                    } catch (Throwable t) {
                                        em.onError(t);
                                    }
                                })
                                .subscribeOn(new ComputationScheduler())
                )
                // 5) Обработка результатов в одном потоке
                .observeOn(new SingleThreadScheduler())
                // 6) Подписка и вывод
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        System.out.println(Thread.currentThread().getName() + " -> " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        doneLatch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("All done!");
                        doneLatch.countDown();
                    }
                });

        // Ждём завершения, иначе JVM может выйти раньше
        doneLatch.await();
    }
}