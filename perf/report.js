// Shared k6 handleSummary -> writes one concise, self-contained HTML report per route.
// Used by each scenario:  export const handleSummary = makeHandleSummary('users');

function n(v, d = 0) {
  if (v === undefined || v === null || Number.isNaN(v)) return '—';
  return v.toFixed(d);
}

function ms(v) {
  return v === undefined ? '—' : `${v.toFixed(1)} ms`;
}

export function makeHandleSummary(route) {
  return function handleSummary(data) {
    const m = data.metrics;
    const dur = (m['http_req_duration'] && m['http_req_duration'].values) || {};
    const reqs = (m['http_reqs'] && m['http_reqs'].values) || {};
    const failed = (m['http_req_failed'] && m['http_req_failed'].values) || {};
    const iters = (m['iterations'] && m['iterations'].values) || {};
    const vus = (m['vus_max'] && m['vus_max'].values) || {};
    const checks = (m['checks'] && m['checks'].values) || {};

    // Verdict = all thresholds passed.
    let allOk = true;
    const thresholdRows = [];
    for (const name of Object.keys(m)) {
      const t = m[name].thresholds;
      if (!t) continue;
      for (const cond of Object.keys(t)) {
        const ok = t[cond].ok;
        if (!ok) allOk = false;
        thresholdRows.push({ metric: `${name} ${cond}`, ok });
      }
    }

    const errRate = (failed.rate !== undefined ? failed.rate * 100 : NaN);
    const checkRate = (checks.rate !== undefined ? checks.rate * 100 : NaN);

    // Per-check breakdown.
    const checkItems = [];
    function walk(group) {
      (group.checks || []).forEach((c) =>
        checkItems.push({ name: c.name, passes: c.passes, fails: c.fails }));
      (group.groups || []).forEach(walk);
    }
    if (data.root_group) walk(data.root_group);

    const badge = allOk
      ? '<span class="badge ok">PASS</span>'
      : '<span class="badge ko">FAIL</span>';

    const html = `<!doctype html>
<html lang="fr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Perf — ${route}</title>
<style>
  :root { --ok:#16a34a; --ko:#dc2626; --bg:#0f172a; --card:#1e293b; --txt:#e2e8f0; --muted:#94a3b8; --accent:#38bdf8; }
  * { box-sizing: border-box; }
  body { margin:0; font:15px/1.5 system-ui,-apple-system,Segoe UI,Roboto,sans-serif; background:var(--bg); color:var(--txt); padding:32px; }
  .wrap { max-width:880px; margin:0 auto; }
  h1 { font-size:24px; margin:0 0 4px; }
  .sub { color:var(--muted); margin:0 0 24px; }
  .badge { font-size:13px; font-weight:700; padding:3px 10px; border-radius:999px; vertical-align:middle; margin-left:8px; }
  .badge.ok { background:rgba(22,163,74,.2); color:var(--ok); }
  .badge.ko { background:rgba(220,38,38,.2); color:var(--ko); }
  .cards { display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:14px; margin-bottom:28px; }
  .card { background:var(--card); border-radius:12px; padding:16px; }
  .card .k { color:var(--muted); font-size:13px; }
  .card .v { font-size:26px; font-weight:700; margin-top:4px; }
  .card .v.ok { color:var(--ok); } .card .v.ko { color:var(--ko); }
  table { width:100%; border-collapse:collapse; background:var(--card); border-radius:12px; overflow:hidden; margin-bottom:24px; }
  th,td { text-align:left; padding:10px 14px; border-bottom:1px solid rgba(148,163,184,.12); font-size:14px; }
  th { color:var(--muted); font-weight:600; }
  tr:last-child td { border-bottom:none; }
  .t-ok { color:var(--ok); } .t-ko { color:var(--ko); }
  h2 { font-size:16px; margin:24px 0 10px; color:var(--accent); }
  .foot { color:var(--muted); font-size:12px; margin-top:24px; }
  a { color:var(--accent); }
</style></head>
<body><div class="wrap">
  <h1>Route <code>${route}</code> ${badge}</h1>
  <p class="sub">Test de charge k6 — ${n(vus.max)} utilisateurs virtuels max</p>

  <div class="cards">
    <div class="card"><div class="k">Requêtes</div><div class="v">${n(reqs.count)}</div></div>
    <div class="card"><div class="k">Débit</div><div class="v">${n(reqs.rate, 0)}<span style="font-size:14px;color:var(--muted)"> req/s</span></div></div>
    <div class="card"><div class="k">Taux d'erreur</div><div class="v ${errRate < 1 ? 'ok' : 'ko'}">${n(errRate, 2)}%</div></div>
    <div class="card"><div class="k">Checks OK</div><div class="v ${checkRate >= 99.9 ? 'ok' : 'ko'}">${n(checkRate, 1)}%</div></div>
  </div>

  <h2>Latence</h2>
  <table>
    <tr><th>Médiane (p50)</th><th>p90</th><th>p95</th><th>p99</th><th>moy.</th><th>max</th></tr>
    <tr><td>${ms(dur.med)}</td><td>${ms(dur['p(90)'])}</td><td>${ms(dur['p(95)'])}</td><td>${ms(dur['p(99)'])}</td><td>${ms(dur.avg)}</td><td>${ms(dur.max)}</td></tr>
  </table>

  <h2>Seuils</h2>
  <table>
    <tr><th>Critère</th><th>Résultat</th></tr>
    ${thresholdRows.map((r) => `<tr><td>${r.metric}</td><td class="${r.ok ? 't-ok' : 't-ko'}">${r.ok ? '✓ OK' : '✗ ÉCHEC'}</td></tr>`).join('')}
  </table>

  <h2>Checks</h2>
  <table>
    <tr><th>Vérification</th><th>OK</th><th>KO</th></tr>
    ${checkItems.map((c) => `<tr><td>${c.name}</td><td class="t-ok">${c.passes}</td><td class="${c.fails ? 't-ko' : ''}">${c.fails}</td></tr>`).join('')}
  </table>

  <p class="foot">Environnement isolé : Postgres local (Docker) + mock Mistral. Aucune donnée de prod, aucun coût LLM. — <a href="index.html">← tous les rapports</a></p>
</div></body></html>`;

    const out = {};
    out[`perf/reports/${route}.html`] = html;
    out.stdout = `\n  [${route}] ${allOk ? 'PASS' : 'FAIL'} — ${n(reqs.count)} req, ${n(errRate, 2)}% err, p95=${ms(dur['p(95)'])}\n`;
    return out;
  };
}
