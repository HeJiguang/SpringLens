package io.springlens.demo;

import java.util.List;
import io.springlens.starter.probe.LensSkillSource;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@LensSkillSource
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderProbeService orderProbeService;

    public OrderController(OrderRepository orderRepository, OrderProbeService orderProbeService) {
        this.orderRepository = orderRepository;
        this.orderProbeService = orderProbeService;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable long id) {
        return orderRepository.findById(id);
    }

    @GetMapping("/slow")
    public Map<String, Object> getSlowQuery() {
        return Map.of(
                "scenario", "slow-sql-showcase",
                "status", "ok",
                "sleepMs", orderRepository.slowQuerySleepMs(),
                "rows", orderRepository.runSlowQuery(),
                "highlightedOrder", orderRepository.findById(2L),
                "statusBreakdown", orderRepository.statusBreakdown()
        );
    }

    @GetMapping("/fail")
    public void fail() {
        orderRepository.runFailingFlow();
    }

    @GetMapping("/probe/{id}")
    public Map<String, Object> probeOrder(@PathVariable long id) {
        Map<String, Object> order = orderProbeService.probeOrder(id);
        return Map.of(
                "probeDemo", "order",
                "order", order,
                "highlights", Map.of(
                        "probeIds", List.of("order.lookup.result", "order.status", "order.customer_name", "order.summary"),
                        "generatedTools", List.of("trace_order_flow", "query_order_status", "query_order_customer_name", "query_order_summary")
                ),
                "timeline", List.of(
                        "Load order sample data from H2",
                        "Capture annotated watch for the repository result",
                        "Capture manual probes for status, customer, and summary"
                )
        );
    }
}
