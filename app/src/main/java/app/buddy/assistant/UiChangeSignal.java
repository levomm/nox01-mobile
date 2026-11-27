package app.buddy.assistant;

/** Coalesces UI events into a generation counter; a wakeup is not proof of a UI change. */
final class UiChangeSignal {
    private long generation;
    synchronized long version() { return generation; }
    synchronized void changed() { generation++; notifyAll(); }
    synchronized boolean awaitChange(long observed, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + Math.max(0, timeoutMs) * 1_000_000L;
        while (generation == observed) {
            long left = deadline - System.nanoTime();
            if (left <= 0) return false;
            wait(left / 1_000_000L, (int) (left % 1_000_000L));
        }
        return true;
    }
}
