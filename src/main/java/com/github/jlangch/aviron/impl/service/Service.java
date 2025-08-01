package com.github.jlangch.aviron.impl.service;

import static com.github.jlangch.aviron.impl.service.ServiceStatus.CLOSED;
import static com.github.jlangch.aviron.impl.service.ServiceStatus.CREATED;
import static com.github.jlangch.aviron.impl.service.ServiceStatus.INITIALISING;
import static com.github.jlangch.aviron.impl.service.ServiceStatus.RUNNING;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public abstract class Service implements IService, Closeable {

    protected abstract String name();

    protected abstract void onStart();

    protected abstract void onClose() throws IOException;


    @Override
    public final void start() {
        if (status.compareAndSet(CREATED, INITIALISING)) {
            try {
                onStart();
            }
            catch(Exception ex) {
                status.set(CLOSED);
                throw new RuntimeException(
                        "Failed to start the service '" +  name() + "'!",
                        ex);
            }
        }
        else {
            throw new RuntimeException(
                    "Rejected to start the service '" +  name() + "'. "
                        + "The service is in status " + status.get());
        }
    }

    @Override
    public final void close() {
        if (status.compareAndSet(RUNNING, CLOSED)) {
            try {
                onClose();
            }
            catch(Exception ex) {
                status.set(CLOSED);
                throw new RuntimeException(
                        "Failed to close the service '" +  name() + "'!",
                        ex);
            }
        }
        else {
            throw new RuntimeException(
                    "Rejected to close the service '" +  name() + "'. "
                        + "The service is in status " + status.get());
        }
    }

    @Override
    public final ServiceStatus getStatus() {
        return status.get();
    }

    @Override
    public void startServiceThread(final Runnable runnable) {
        final Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("aviron-service-" + threadCounter.getAndIncrement());
        thread.start();
    }

    protected void enteredRunningState() {
        status.set(RUNNING);
    }

    protected boolean isInRunningState() {
        return status.get() == RUNNING;
    }

    protected boolean isInClosedState() {
        return status.get() == CLOSED;
    }

    protected void waitForServiceStarted(final int maxSeconds) {
        final long tsLimitEnd = System.currentTimeMillis() + maxSeconds * 1_000;

        // spin wait service to be ready or closed
        while(System.currentTimeMillis() < tsLimitEnd) {
            if (isInRunningState() || isInClosedState()) break;
            sleep(100);
        }
    }

    protected void sleep(final int millis) {
        try { Thread.sleep(millis); } catch(Exception ex) {}
    }


    private static final AtomicLong threadCounter = new AtomicLong(1L);

    private final AtomicReference<ServiceStatus> status = new AtomicReference<>(CREATED);
}
