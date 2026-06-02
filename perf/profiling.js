// Load test for the /profiling routes.
//
// REQUIRES the Mistral mock + app in test mode (see coach.js header):
//   ./gradlew runMistralMock
//   ./gradlew runTestMode
//   k6 run perf/profiling.js
//
import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, JSON_HEADERS, newUser } from './config.js';
import { makeHandleSummary } from './report.js';

export const handleSummary = makeHandleSummary('profiling');

export const options = {
  scenarios: {
    profiling: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 10 },
        { duration: '40s', target: 10 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<400', 'p(99)<800'],
  },
};

const userCache = {};
function ensureUser() {
  if (userCache[__VU]) return userCache[__VU];
  const res = http.post(`${BASE_URL}/users`, JSON.stringify(newUser()), { headers: JSON_HEADERS });
  const id = res.json('id');
  userCache[__VU] = id;
  return id;
}

// 15-question profiling answers (matches PROFILING_INSTRUCTIONS).
function answers() {
  return {
    1: 'A', 2: 'A', 3: '4', 4: 'A', 5: 'A', 6: '4', 7: 'A', 8: 'B',
    9: 'A', 10: '3', 11: 'A', 12: 'A', 13: 'D', 14: 'A', 15: 'A',
  };
}

export default function () {
  const userId = ensureUser();
  if (!userId) return;

  group('analyze', () => {
    const body = { user_id: userId, answers: answers(), version: 1 };
    const res = http.post(`${BASE_URL}/profiling`, JSON.stringify(body), { headers: JSON_HEADERS });
    check(res, {
      'POST /profiling -> 201': (r) => r.status === 201,
      'has profile': (r) => r.json('profile') !== undefined,
    });
  });

  group('latest', () => {
    const res = http.get(`${BASE_URL}/profiling/${userId}`);
    check(res, { 'GET /profiling/{userId} -> 200': (r) => r.status === 200 });
  });

  group('history', () => {
    const res = http.get(`${BASE_URL}/profiling/${userId}/history`);
    check(res, { 'GET /profiling/{userId}/history -> 200': (r) => r.status === 200 });
  });
}
