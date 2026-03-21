package io.springlens.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                create table if not exists orders (
                    id bigint primary key,
                    customer_name varchar(128),
                    status varchar(64)
                )
                """);
        jdbcTemplate.execute("create alias if not exists SLEEP_MS for \"io.springlens.demo.DatabaseFunctions.sleepMs\"");
        jdbcTemplate.update("merge into orders (id, customer_name, status) key(id) values (?, ?, ?)", 1L, "Ada", "CREATED");
        jdbcTemplate.update("merge into orders (id, customer_name, status) key(id) values (?, ?, ?)", 2L, "Linus", "PAID");
        jdbcTemplate.update("merge into orders (id, customer_name, status) key(id) values (?, ?, ?)", 3L, "Grace", "PACKING");
        jdbcTemplate.update("merge into orders (id, customer_name, status) key(id) values (?, ?, ?)", 4L, "Margaret", "SHIPPED");
        jdbcTemplate.update("merge into orders (id, customer_name, status) key(id) values (?, ?, ?)", 5L, "Ken", "FAILED");
    }
}
