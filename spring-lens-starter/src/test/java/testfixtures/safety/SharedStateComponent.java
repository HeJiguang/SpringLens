package testfixtures.safety;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

@Component
public class SharedStateComponent {

    private final ThreadLocal<String> requestScope = new ThreadLocal<>();
    private final Map<String, String> sharedCache = new HashMap<>();
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<String> pendingEvents = new LinkedBlockingQueue<>();
    private int sharedCounter;

    @Async
    public void refreshAsync() {
        sharedCounter++;
    }
}
