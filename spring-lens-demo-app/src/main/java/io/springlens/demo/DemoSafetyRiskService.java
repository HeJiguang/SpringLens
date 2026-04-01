package io.springlens.demo;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

@Component
class DemoSafetyRiskService {

    private final ThreadLocal<String> currentOrderScope = new ThreadLocal<>();
    private final Map<String, String> sharedOrderCache = new HashMap<>();
    private final SimpleDateFormat legacyFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<String> pendingEvents = new LinkedBlockingQueue<>();
    private int sharedCounter;

    @Async
    public void refreshAsync() {
        sharedCounter++;
    }
}
