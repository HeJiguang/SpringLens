package io.springlens.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private static final long SLOW_QUERY_SLEEP_MS = 120L;

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> findById(long id) {
        return jdbcTemplate.queryForMap("select id, customer_name, status from orders where id = ?", id);
    }

    public long runSlowQuery() {
        jdbcTemplate.queryForObject("CALL SLEEP_MS(?)", Long.class, SLOW_QUERY_SLEEP_MS);
        Long count = jdbcTemplate.queryForObject("select count(*) from orders", Long.class);
        return count == null ? 0L : count;
    }

    public void runFailingFlow() {
        Map<String, Object> failedOrder = findById(5L);
        jdbcTemplate.queryForObject("select count(*) from orders", Long.class);
        throw new IllegalStateException("Inventory reservation failed for order " + failedOrder.get("ID"));
    }

    public long countByStatus(String status) {
        Long count = jdbcTemplate.queryForObject("select count(*) from orders where status = ?", Long.class, status);
        return count == null ? 0L : count;
    }

    public Map<String, Long> statusBreakdown() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select status, count(*) as total
                from orders
                group by status
                order by status
                """);
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object total = row.get("TOTAL");
            breakdown.put(String.valueOf(row.get("STATUS")), total instanceof Number number ? number.longValue() : 0L);
        }
        return Map.copyOf(breakdown);
    }

    public long slowQuerySleepMs() {
        return SLOW_QUERY_SLEEP_MS;
    }
}
