package com.github.jlangch.aviron.impl.service;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;


public abstract class Service implements IService, Closeable {

    protected abstract String name();

    protected abstract void onStart();

    protected abstract void onClose() throws Exception;


    @Override
    public void start() {
        if (status.compareAndSet(
                ServiceStatus.CREATED,
                ServiceStatus.INITIALISING)
        ) {
            try {
                onStart();
            }
            catch(Exception ex) {
                status.set(ServiceStatus.CLOSED);
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
    public void close() {
        if (status.compareAndSet(
                ServiceStatus.RUNNING,
                ServiceStatus.CLOSED)
        ) {
            try {
                onClose();
            }
            catch(Exception ex) {
                status.set(ServiceStatus.CLOSED);
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
    public ServiceStatus getStatus() {
        return status.get();
    }

    protected void enteredRunningState() {
        status.set(ServiceStatus.RUNNING);
    }

    protected boolean isInRunningState() {
        return status.get() == ServiceStatus.RUNNING;
    }

    protected boolean isInClosedState() {
        return status.get() == ServiceStatus.CLOSED;
    }


    private final AtomicReference<ServiceStatus> status = 
            new AtomicReference<>(ServiceStatus.CREATED);
}
