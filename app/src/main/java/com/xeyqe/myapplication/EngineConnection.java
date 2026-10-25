package com.xeyqe.myapplication;

/** Owns one asynchronous engine connection. All state changes run on the UI scheduler. */
final class EngineConnection<E> {
    interface Result { void complete(boolean success); }
    interface Factory<E> {
        E create(String name, Result result);
        void shutdown(E engine);
    }
    interface Scheduler {
        void post(Runnable action);
        void postDelayed(Runnable action, long delayMillis);
        void remove(Runnable action);
    }
    interface Listener<E> {
        void onReady(E engine);
        void onFailure();
    }

    private final Factory<E> factory;
    private final Scheduler scheduler;
    private E engine;
    private long generation;
    private boolean completed;
    private Runnable timeout;
    private Listener<E> listener;

    EngineConnection(Factory<E> factory, Scheduler scheduler) {
        this.factory = factory;
        this.scheduler = scheduler;
    }

    void connect(String name, Listener<E> listener) {
        close();
        this.listener = listener;
        completed = false;
        long attempt = generation;
        timeout = () -> finish(attempt, false);
        scheduler.postDelayed(timeout, 10_000);
        try {
            // Always defer callbacks: TextToSpeech may invoke onInit inside its constructor.
            engine = factory.create(name, success -> scheduler.post(() -> finish(attempt, success)));
        } catch (RuntimeException error) {
            finish(attempt, false);
        }
    }

    private void finish(long attempt, boolean success) {
        if (attempt != generation || completed) return;
        completed = true;
        scheduler.remove(timeout);
        if (success && engine != null) {
            listener.onReady(engine);
        } else {
            E failed = engine;
            engine = null;
            if (failed != null) factory.shutdown(failed);
            listener.onFailure();
        }
    }

    void close() {
        generation++;
        completed = true;
        if (timeout != null) scheduler.remove(timeout);
        timeout = null;
        E previous = engine;
        engine = null;
        listener = null;
        if (previous != null) factory.shutdown(previous);
    }
}
