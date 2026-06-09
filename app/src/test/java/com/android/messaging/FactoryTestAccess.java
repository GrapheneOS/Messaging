package com.android.messaging;

public final class FactoryTestAccess {
    private FactoryTestAccess() {
    }

    public static void install(final Factory factory) {
        Factory.sRegistered = false;
        Factory.sInitialized = false;
        Factory.setInstance(factory);
    }

    public static void reset() {
        Factory.sRegistered = false;
        Factory.sInitialized = false;
        Factory.setInstance(null);
    }
}
