package com.insurance.dashboard.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Performance gate for the BFF read endpoints.
 *
 * Target: list AND detail endpoints respond at p95 < 300ms under 50 concurrent sessions.
 *
 * Requires the application to be running first:
 *   mvn spring-boot:run
 * Run with:
 *   mvn gatling:test
 *   mvn gatling:test -DbaseUrl=http://localhost:8081
 * Reports: target/gatling/
 */
public class PolicyEndpointSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8081");

    private static final int CONCURRENT_USERS = 50;
    private static final int DURATION_SECONDS  = 30;
    private static final int P95_THRESHOLD_MS  = 300;

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Each session: hit the list endpoint, capture a policy ID from the page, then
    // fetch that policy's detail — exercising both list and detail under load.
    ScenarioBuilder scn = scenario("List and detail under concurrent load")
            .exec(
                http("GET list")
                    .get("/api/v1/policies?page=0&size=10")
                    .check(status().is(200))
                    .check(jsonPath("$.content[0].id").saveAs("policyId"))
            )
            .exec(
                http("GET detail")
                    .get("/api/v1/policies/#{policyId}")
                    .check(status().is(200))
            );

    {
        setUp(
            scn.injectClosed(
                constantConcurrentUsers(CONCURRENT_USERS).during(DURATION_SECONDS)
            )
        )
        .protocols(httpProtocol)
        .assertions(
            // p95 < 300ms for EACH endpoint, plus a near-zero error budget
            details("GET list").responseTime().percentile(95.0).lt(P95_THRESHOLD_MS),
            details("GET detail").responseTime().percentile(95.0).lt(P95_THRESHOLD_MS),
            global().successfulRequests().percent().gt(99.0)
        );
    }
}
