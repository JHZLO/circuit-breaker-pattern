import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 10,  // 동시 사용자 수
    iterations: 1000, // 총 1000번 요청
};

export default function () {
    let testUrl = `http://localhost:8080/client/test`;

    let response = http.get(testUrl);

    check(response, {
        'status is 200': (r) => r.status === 200
    });

    sleep(0.1);
}

export function teardown() {
    let statsUrl = `http://localhost:8080/client/stats`;
    let statsResponse = http.get(statsUrl);

    console.log(`[STATS RESPONSE] ${statsResponse.body}`);
}
