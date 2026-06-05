import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const responseTime = new Trend('response_time');
const errorRate = new Rate('errors');

export const options = {
    vus: 50,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        errors: ['rate<0.01'],
    },
};

export default function () {
    const res = http.get('http://localhost:8081/api/policies?page=0&size=10&sort=startDate,desc');

    responseTime.add(res.timings.duration);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'has content array': (r) => JSON.parse(r.body).content !== undefined,
    });

    errorRate.add(!success);
    sleep(0.1);
}
