package io.springlens.demo;

import io.springlens.starter.probe.Lens;
import io.springlens.starter.probe.LensTool;
import io.springlens.starter.probe.LensToolParam;
import io.springlens.starter.probe.LensWatch;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderProbeService {

    private final OrderRepository orderRepository;

    public OrderProbeService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @LensWatch(id = "order.lookup.result", description = "Observe repository lookup result", target = "#result")
    public Map<String, Object> probeOrder(long id) {
        Map<String, Object> order = orderRepository.findById(id);
        Lens.look("order.status", order.get("STATUS"), "Current order status");
        Lens.look("order.customer_name", order.get("CUSTOMER_NAME"), "Customer name used by this order");
        Lens.look("order.summary", summarize(order), "Human-readable order summary");
        return order;
    }

    @LensTool(name = "count_orders_by_status", description = "Count orders grouped by a single status.")
    public Map<String, Object> countOrdersByStatus(@LensToolParam("status") String status) {
        return Map.of("status", status, "count", orderRepository.countByStatus(status));
    }

    private String summarize(Map<String, Object> order) {
        return "Order " + order.get("ID")
                + " for " + order.get("CUSTOMER_NAME")
                + " is currently " + order.get("STATUS");
    }
}
