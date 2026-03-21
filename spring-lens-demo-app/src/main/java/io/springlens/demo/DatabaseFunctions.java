package io.springlens.demo;

public final class DatabaseFunctions {

    private DatabaseFunctions() {
    }

    public static long sleepMs(long millis) throws InterruptedException {
        Thread.sleep(millis);
        return millis;
    }
}
