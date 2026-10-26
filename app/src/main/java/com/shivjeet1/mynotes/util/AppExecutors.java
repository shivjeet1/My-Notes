package com.shivjeet1.mynotes.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static volatile AppExecutors instance;
    private final ExecutorService ioExecutor;

    private AppExecutors() {
        ioExecutor = Executors.newSingleThreadExecutor();
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public ExecutorService io() {
        return ioExecutor;
    }
}
