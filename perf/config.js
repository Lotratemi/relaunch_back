// Shared config for all k6 scenarios.
// Override the target with:  k6 run -e BASE_URL=http://localhost:8080 perf/<script>.js
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Latency budgets for the DB-bound routes (NO Mistral in the path).
// Tune these as you learn the real numbers; failing a threshold fails the run (good for CI).
export const dbThresholds = {
  http_req_failed: ['rate<0.01'],            // <1% errors
  http_req_duration: ['p(95)<200', 'p(99)<500'],
};

// Helper: build a unique-ish user payload. k6 has no per-iter UUID, so we
// derive uniqueness from VU id + iteration counter.
export function newUser() {
  const tag = `${__VU}-${__ITER}-${Date.now()}`;
  return {
    name: `perf_${tag}`,
    mail: `perf_${tag}@example.com`,
    age: 30,
  };
}

export function newObjective(userId) {
  return {
    user_id: userId,
    end_at: '2026-12-31T00:00:00Z',
    frequency: 7,
    title: 'Run 5km',
    description: 'perf test objective',
  };
}
