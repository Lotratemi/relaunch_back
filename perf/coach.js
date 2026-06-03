// Load test for the /coach routes.
//
// REQUIRES the Mistral mock running and the app in test mode:
//   ./gradlew runMistralMock           (terminal 1)
//   ./gradlew runTestMode              (terminal 2)
//   k6 run perf/coach.js               (terminal 3)
//
// Without the mock these endpoints hit the real Mistral API (rate-limited + billed).
import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, JSON_HEADERS, newUser } from './config.js';
import { makeHandleSummary } from './report.js';

export const handleSummary = makeHandleSummary('coach');

export const options = {
  scenarios: {
    coach_chat: {
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
    // Looser than DB routes: the path includes an (mocked) HTTP hop to Mistral.
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

export default function () {
  const userId = ensureUser();
  if (!userId) return;

  let mistralConvId;

  group('create conversation', () => {
    const res = http.post(`${BASE_URL}/coach/chat/${userId}`, null, { headers: JSON_HEADERS });
    check(res, { 'POST /coach/chat/{userId} -> 200': (r) => r.status === 200 });
    mistralConvId = res.json('mistral_conv_id');
  });

  if (!mistralConvId) return;

  group('chat', () => {
    const res = http.post(
      `${BASE_URL}/coach/chat/${userId}/${mistralConvId}`,
      'Je veux reprendre le sport',
      { headers: { 'Content-Type': 'text/plain' } },
    );
    check(res, {
      'POST /coach/chat/{userId}/{convId} -> 200': (r) => r.status === 200,
      'reply has response field': (r) => r.json('response') !== undefined,
    });
  });
}
