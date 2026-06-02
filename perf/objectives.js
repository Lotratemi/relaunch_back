// Load test for the /objectives CRUD routes (DB-bound, no Mistral).
// Each VU creates its own user first so objectives have a valid FK.
//
//   k6 run perf/objectives.js
//
import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, JSON_HEADERS, dbThresholds, newUser, newObjective } from './config.js';
import { makeHandleSummary } from './report.js';

export const handleSummary = makeHandleSummary('objectives');

export const options = {
  scenarios: {
    objectives_crud: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '15s', target: 0 },
      ],
    },
  },
  thresholds: dbThresholds,
};

// One user per VU, created once and reused across iterations.
export function setup() {
  // nothing global; users are per-VU below
}

const userCache = {};

function ensureUser() {
  if (userCache[__VU]) return userCache[__VU];
  const res = http.post(`${BASE_URL}/users`, JSON.stringify(newUser()), { headers: JSON_HEADERS });
  const id = res.json('id');
  userCache[__VU] = id;
  return id;
}

export default function () {
  const userId = ensureUser();
  if (!userId) return;

  let objectiveId;

  group('create', () => {
    const res = http.post(`${BASE_URL}/objectives`, JSON.stringify(newObjective(userId)), { headers: JSON_HEADERS });
    check(res, { 'POST /objectives -> 201': (r) => r.status === 201 });
    objectiveId = res.json('id');
  });

  if (!objectiveId) return;

  group('list', () => {
    const res = http.get(`${BASE_URL}/objectives/${userId}`);
    check(res, { 'GET /objectives/{userId} -> 200': (r) => r.status === 200 });
  });

  group('get', () => {
    const res = http.get(`${BASE_URL}/objectives/${userId}/${objectiveId}`);
    check(res, { 'GET /objectives/{userId}/{id} -> 200': (r) => r.status === 200 });
  });

  group('delete', () => {
    const res = http.del(`${BASE_URL}/objectives/${userId}/${objectiveId}`);
    check(res, { 'DELETE -> 204': (r) => r.status === 204 });
  });
}
