package com.xeyqe.myapplication;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EngineConnectionTest {
    @Test public void handlesAConstructorThatCallsBackImmediately() {
        Fixture fixture = new Fixture();
        fixture.factory.immediate = true;
        fixture.connection.connect("first", fixture.listener);
        assertNull(fixture.listener.ready);
        fixture.scheduler.deliver();
        assertEquals("first", fixture.listener.ready);
        assertEquals(0, fixture.listener.failures);
        assertTrue(fixture.scheduler.delayed.isEmpty());
    }

    @Test public void ignoresLateCallbacksAfterSwitchingEngines() {
        Fixture fixture = new Fixture();
        fixture.connection.connect("old", fixture.listener);
        fixture.connection.connect("new", fixture.listener);
        fixture.factory.results.get(0).complete(true);
        fixture.scheduler.deliver();
        assertNull(fixture.listener.ready);
        fixture.factory.results.get(1).complete(true);
        fixture.scheduler.deliver();
        assertEquals("new", fixture.listener.ready);
        assertEquals(List.of("old"), fixture.factory.closed);
    }

    @Test public void timesOutAndCanRetryWithoutAcceptingTheOldResult() {
        Fixture fixture = new Fixture();
        fixture.connection.connect("hung", fixture.listener);
        fixture.scheduler.expire();
        assertEquals(1, fixture.listener.failures);
        assertEquals(List.of("hung"), fixture.factory.closed);
        fixture.connection.connect("retry", fixture.listener);
        fixture.factory.results.get(0).complete(true);
        fixture.factory.results.get(1).complete(true);
        fixture.scheduler.deliver();
        assertEquals("retry", fixture.listener.ready);
        assertEquals(1, fixture.listener.failures);
    }

    @Test public void destructionShutsDownAndDiscardsPendingCallbacks() {
        Fixture fixture = new Fixture();
        fixture.connection.connect("engine", fixture.listener);
        fixture.connection.close();
        fixture.factory.results.get(0).complete(true);
        fixture.scheduler.deliver();
        assertNull(fixture.listener.ready);
        assertEquals(0, fixture.listener.failures);
        assertEquals(List.of("engine"), fixture.factory.closed);
        assertTrue(fixture.scheduler.delayed.isEmpty());
    }

    @Test public void failureIsReportedOnceAndShutsDownTheEngine() {
        Fixture fixture = new Fixture();
        fixture.connection.connect("failed", fixture.listener);
        fixture.factory.results.get(0).complete(false);
        fixture.factory.results.get(0).complete(true);
        fixture.scheduler.deliver();
        assertNull(fixture.listener.ready);
        assertEquals(1, fixture.listener.failures);
        assertEquals(List.of("failed"), fixture.factory.closed);
    }

    @Test public void constructorExceptionReportsFailureAndRemovesTimeout() {
        Fixture fixture = new Fixture();
        fixture.factory.throwsOnCreate = true;
        fixture.connection.connect("missing", fixture.listener);
        assertEquals(1, fixture.listener.failures);
        assertTrue(fixture.scheduler.delayed.isEmpty());
    }

    private static final class Fixture {
        final Factory factory = new Factory();
        final Scheduler scheduler = new Scheduler();
        final Listener listener = new Listener();
        final EngineConnection<String> connection = new EngineConnection<>(factory, scheduler);
    }
    private static final class Factory implements EngineConnection.Factory<String> {
        final List<EngineConnection.Result> results = new ArrayList<>();
        final List<String> closed = new ArrayList<>();
        boolean immediate, throwsOnCreate;
        @Override public String create(String name, EngineConnection.Result result) {
            if (throwsOnCreate) throw new IllegalStateException();
            results.add(result);
            if (immediate) result.complete(true);
            return name;
        }
        @Override public void shutdown(String name) { closed.add(name); }
    }
    private static final class Scheduler implements EngineConnection.Scheduler {
        final List<Runnable> posted = new ArrayList<>();
        final List<Runnable> delayed = new ArrayList<>();
        @Override public void post(Runnable action) { posted.add(action); }
        @Override public void postDelayed(Runnable action, long delay) {
            assertEquals(10_000, delay);
            delayed.add(action);
        }
        @Override public void remove(Runnable action) { delayed.remove(action); }
        void deliver() {
            while (!posted.isEmpty()) posted.remove(0).run();
        }
        void expire() {
            for (Runnable action : new ArrayList<>(delayed)) action.run();
        }
    }
    private static final class Listener implements EngineConnection.Listener<String> {
        String ready;
        int failures;
        @Override public void onReady(String engine) { ready = engine; }
        @Override public void onFailure() { failures++; }
    }
}
