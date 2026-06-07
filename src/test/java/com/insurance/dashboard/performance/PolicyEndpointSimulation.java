package com.insurance.dashboard.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Performance gate: p95 < 500ms under 50 concurrent users.
 *
 * Requires the application to be running before execution:
 *   mvn spring-boot:run
 *
 * Run with:
 *   mvn gatling:test
 *   mvn gatling:test -DbaseUrl=http://localhost:8081
 *
 * Reports are written to target/gatling/
 */
public class PolicyEndpointSimulation extends Simulation {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:8081");

    private static final int CONCURRENT_USERS = 50;
    private static final int DURATION_SECONDS  = 30;
    private static final int P95_THRESHOLD_MS  = 500;

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder policyList = scenario("Policy list retrieval")
            .exec(
                http("GET /api/v1/policies?page=0&size=10")
                    .get("/api/v1/policies?page=0&size=10")
                    .check(status().is(200))
                    .check(jsonPath("$.content").exists())
                    .check(jsonPath("$.totalElements").exists())
            );

    {
        setUp(
            policyList.injectClosed(
                constantConcurrentUsers(CONCURRENT_USERS).during(DURATION_SECONDS)
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().percentile(95).lt(P95_THRESHOLD_MS),
            global().successfulRequests().percent().gt(99.0)
        );
    }
}
