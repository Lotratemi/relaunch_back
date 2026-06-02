// Quick sanity check: 1 VU, a handful of iterations. Run this first to verify
// the target is up and the scripts work before launching a real load test.
//
//   k6 run perf/smoke.js
//
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, JSON_HEADERS, newUser } from './config.js';

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: { http_req_failed: ['rate==0'] },
};

export default function () {
  const create = http.post(`${BASE_URL}/users`, JSON.stringify(newUser()), { headers: JSON_HEADERS });
  check(create, { 'create 201': (r) => r.status === 201 });
  const id = create.json('id');

  const get = http.get(`${BASE_URL}/users/${id}`);
  check(get, { 'get 200': (r) => r.status === 200 });

  http.del(`${BASE_URL}/users/${id}`);
}
