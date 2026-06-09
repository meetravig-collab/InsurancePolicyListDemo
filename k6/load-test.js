// k6 load test — list & detail endpoints under 50 concurrent VUs.
//
// Mirrors the Gatling gate: each iteration lists policies, captures a real UUID,
// then fetches that policy's detail. Per-endpoint p95 < 300ms thresholds make the
// run pass/fail (k6 exits non-zero on a breached threshold).
//
// Requires the k6 binary (https://k6.io) and the app running.
//   k6 run k6/load-test.js
//   k6 run -e BASE_URL=http://localhost:8081 k6/load-test.js
//
// Behind a TLS-intercepting proxy (or on Windows where `localhost` resolves to IPv6),
// target the loopback IPv4 directly and bypass the proxy, otherwise concurrent
// connections may hang:
//   set NO_PROXY=127.0.0.1,localhost
//   k6 run -e BASE_URL=http://127.0.0.1:8081 k6/load-test.js
//
// Note: the Maven/Gatling test (mvn gatling:test) is the in-build, CI-runnable p95 gate;
// this k6 script is the lightweight, JS-based equivalent of the same scenario.

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

// per-endpoint response-time metrics so each can be gated independently
const listDuration = new Trend('list_duration', true);
const detailDuration = new Trend('detail_duration', true);

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    'list_duration': ['p(95)<300'],     // GET /api/v1/policies        p95 < 300ms
    'detail_duration': ['p(95)<300'],   // GET /api/v1/policies/{id}    p95 < 300ms
    'http_req_failed': ['rate<0.01'],   // < 1% errors
    'checks': ['rate>0.99'],            // > 99% successful assertions
  },
};

export default function () {
  // --- list ---
  const listRes = http.get(`${BASE_URL}/api/v1/policies?page=0&size=10`);
  listDuration.add(listRes.timings.duration);
  check(listRes, {
    'list status is 200': (r) => r.status === 200,
    'list has content': (r) => Array.isArray(r.json('content')),
  });

  // capture a policy id from the page
  const id = listRes.status === 200 ? listRes.json('content.0.id') : null;

  // --- detail ---
  if (id) {
    const detailRes = http.get(`${BASE_URL}/api/v1/policies/${id}`);
    detailDuration.add(detailRes.timings.duration);
    check(detailRes, {
      'detail status is 200': (r) => r.status === 200,
      'detail id matches': (r) => r.json('id') === id,
    });
  }
}
