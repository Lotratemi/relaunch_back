// Load test for the /users CRUD routes (DB-bound, no Mistral).
//
//   k6 run perf/users.js
//   k6 run -e BASE_URL=http://localhost:8080 perf/users.js
//
import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, JSON_HEADERS, dbThresholds, newUser } from './config.js';
import { makeHandleSummary } from './report.js';

export const handleSummary = makeHandleSummary('users');

export const options = {
  scenarios: {
    users_crud: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },   // ramp up
        { duration: '1m', target: 20 },     // steady state
        { duration: '15s', target: 0 },     // ramp down
      ],
    },
  },
  thresholds: dbThresholds,
};

export default function () {
  let userId;

  group('create', () => {
    const res = http.post(`${BASE_URL}/users`, JSON.stringify(newUser()), { headers: JSON_HEADERS });
    check(res, { 'POST /users -> 201': (r) => r.status === 201 });
    userId = res.json('id');
  });

  if (!userId) return;

  group('read', () => {
    const res = http.get(`${BASE_URL}/users/${userId}`);
    check(res, { 'GET /users/{id} -> 200': (r) => r.status === 200 });
  });

  group('update', () => {
    const body = { ...newUser(), age: 31 };
    const res = http.put(`${BASE_URL}/users/${userId}`, JSON.stringify(body), { headers: JSON_HEADERS });
    check(res, { 'PUT /users/{id} -> 200': (r) => r.status === 200 });
  });

  group('delete', () => {
    const res = http.del(`${BASE_URL}/users/${userId}`);
    check(res, { 'DELETE /users/{id} -> 204': (r) => r.status === 204 });
  });
}
