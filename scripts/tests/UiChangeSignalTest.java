package app.buddy.assistant;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class UiChangeSignalTest {
    public static void main(String[] args) throws Exception {
        UiChangeSignal signal = new UiChangeSignal();
        long v = signal.version();
        signal.changed();
        if (!signal.awaitChange(v, 100)) throw new AssertionError("event before wait lost");
        long current = signal.version();
        if (signal.awaitChange(current, 5)) throw new AssertionError("timeout must not imply change");
        CountDownLatch entered = new CountDownLatch(1);
        Thread sender = new Thread(() -> { try { entered.await(); signal.changed(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        sender.start(); entered.countDown();
        if (!signal.awaitChange(current, 1000)) throw new AssertionError("event during wait lost");
        sender.join();
        Thread.currentThread().interrupt();
        try { signal.awaitChange(signal.version(), 1000); throw new AssertionError("interrupt lost"); }
        catch (InterruptedException expected) { }
        System.out.println("PASS: event, timeout, concurrent wake, interruption");
    }
}
