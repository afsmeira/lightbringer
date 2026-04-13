/** Renders a loading spinner with the given message into the #stats-display element. */
function renderLoadingState(message) {
  document.getElementById('stats-display').innerHTML = `
    <div class="sim-loading">
      <div class="sim-loading-spinner"></div>
      <div class="sim-loading-text">${message}</div>
    </div>`;
}

/**
 * Renders simulation results — stat cards, distribution charts, and simulation log —
 * into the #stats-display element.
 */
function renderStats(results) {
  const {
    poorCount, economyCount, limitedCount,
    mulliganCount, mulliganSuccessCount,
    avgGold, avgCards,
    goldDist, cardsDist, charsDist, strengthDist,
    keyCardsDist, avoidableDist, shadowDist,
    iconAvg, iconTotals, iconStrengthAvg, iconStrengthTotals,
    simLog,
  } = results;
  const pct = v => `${Math.round(v / SIM_COUNT * 100)}%`;
  const mulliganPct = mulliganCount > 0 ? `${Math.round(mulliganSuccessCount / mulliganCount * 100)}% of mulligans` : 'n/a';
  document.getElementById('stats-display').innerHTML = `
    <div class="stats-grid">
      <div class="stat-card poor">
        <div class="stat-value">${poorCount}</div>
        <div class="stat-pct">${pct(poorCount)} of ${SIM_COUNT}</div>
        <div class="stat-label">Poor Setups</div>
      </div>
      <div class="stat-card good">
        <div class="stat-value">${economyCount}</div>
        <div class="stat-pct">${pct(economyCount)} of ${SIM_COUNT}</div>
        <div class="stat-label">With Economy Card</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${limitedCount}</div>
        <div class="stat-pct">${pct(limitedCount)} of ${SIM_COUNT}</div>
        <div class="stat-label">With Limited Card</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${mulliganCount}</div>
        <div class="stat-pct">${pct(mulliganCount)} of ${SIM_COUNT}</div>
        <div class="stat-label">Mulligans Taken</div>
      </div>
      <div class="stat-card good">
        <div class="stat-value">${mulliganSuccessCount}</div>
        <div class="stat-pct">${mulliganPct}</div>
        <div class="stat-label">Successful Mulligans</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${avgGold}</div>
        <div class="stat-pct">avg per setup</div>
        <div class="stat-label">Average Gold</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${avgCards}</div>
        <div class="stat-pct">avg per setup</div>
        <div class="stat-label">Average Cards</div>
      </div>
    </div>
    ${trimAndRender(cardsDist, 'Total Cards',   i => String(i))}
    ${trimAndRender(goldDist,  'Gold Distribution', i => `${i}g`)}
    ${trimAndRender(charsDist,     'Distinct Characters',  i => String(i))}
    ${trimAndRender(strengthDist,  'Character Strength',   i => String(i))}
    ${trimAndRender(keyCardsDist,  'Key Cards',  i => String(i))}
    ${trimAndRender(avoidableDist, 'Avoidable Cards', i => String(i))}
    ${trimAndRender(shadowDist,    'Cards in Shadow',  i => String(i))}
    ${renderIconSpread(iconAvg,         iconTotals,         'Icon Spread')}
    ${renderIconSpread(iconStrengthAvg, iconStrengthTotals, 'Icon Strength')}
    ${renderSimLog(simLog)}`;
}

/** Renders the simulation log (first 100 entries) as a collapsible detail block. */
function renderSimLog(simLog) {
  const sample = simLog.slice(0, 100);
  const sep = `<span class="sim-sep">·</span>`;

  const renderCards = cards => cards.map(c =>
    !c.isRestricted && (SETUP_TYPES.has(c.type) || c.isShadowEvent)
      ? c.name
      : `<span class="sim-card-dim">${c.name}</span>`
  ).join(sep);

  const rows = sample.map(sim => {
    const initialHtml  = renderCards(sim.initialHand);
    const mulliganHtml = sim.mulliganHand ? renderCards(sim.mulliganHand) : null;

    const setupHtml = sim.setup
      ? sim.setup.map(c => {
          const cls = c.isLimited   ? 'sim-card-limited'
                    : c.isKey       ? 'sim-card-key'
                    : c.isEconomy   ? 'sim-card-economy'
                    : c.isAvoidable ? 'sim-card-avoidable'
                    : c.inShadow    ? 'sim-card-shadow'
                    :                 'sim-card-normal';
          const costLabel = c.isDup ? 'dup' : c.inShadow ? SHADOW_COST + 'g' : c.cost + 'g';
          return `<span class="${cls}">${c.name}<span class="sim-card-cost">(${costLabel})</span></span>`;
        }).join(sep)
      : `<span class="sim-no-setup">No valid setup</span>`;

    const setupLabel = sim.setup
      ? `Chosen Setup <span class="sim-setup-total">(${sim.total}g)</span>`
      : `Chosen Setup`;

    const badges = sim.poor ? `
        <div class="sim-row-badges">
          <span class="sim-row-label">Labels</span>
          <span class="sim-badge poor" title="This setup is considered to be poor according to configuration">poor</span>
        </div>` : '';

    return `
      <div class="sim-row">
        <div class="sim-hand"><span class="sim-row-label">Initial Draw</span>${initialHtml}</div>
        ${mulliganHtml ? `<div class="sim-hand"><span class="sim-row-label">Mulligan Draw</span>${mulliganHtml}</div>` : ''}
        <div class="sim-setup"><span class="sim-row-label">${setupLabel}</span>${setupHtml}</div>
        ${badges}
      </div>`;
  }).join('');

  return `
    <details class="sim-log-details">
      <summary>Simulation Log (sample of ${sample.length} from ${simLog.length})</summary>
      <div class="sim-log-key">
        <span class="sim-card-limited">&#9632; Limited</span>
        <span class="sim-card-key">&#9632; Key card</span>
        <span class="sim-card-economy">&#9632; Economy</span>
        <span class="sim-card-avoidable">&#9632; Avoidable</span>
        <span class="sim-card-shadow">&#9632; Shadow</span>
        <span class="sim-card-normal">&#9632; Normal</span>
      </div>
      <div class="sim-log">${rows}</div>
    </details>`;
}

/** Trims leading/trailing zero buckets from a distribution and renders it as a histogram. */
function trimAndRender(dist, title, labelFn) {
  const first   = dist.findIndex(v => v > 0);
  const last    = dist.length - 1 - [...dist].reverse().findIndex(v => v > 0);
  const trimmed = dist.slice(first, last + 1);
  return renderHistogram(trimmed, title, i => labelFn(i + first));
}

/** Renders a 3-bar SVG chart for military/intrigue/power icon averages per setup. */
function renderIconSpread(iconAvg, iconTotals, title) {
  const labels      = [getCSSIconChar('icon-military'), getCSSIconChar('icon-intrigue'), getCSSIconChar('icon-power')];
  const iconColors  = ['#dc322f', '#859900', '#268bd2'];
  const maxVal  = Math.max(...iconAvg, 0.1);
  const yMax    = Math.ceil(maxVal / 0.5) * 0.5;
  const tickCount = Math.round(yMax / 0.5);
  const ticks   = Array.from({ length: tickCount + 1 }, (_, i) => +(i * 0.5).toFixed(1));

  const mL = 48, mR = 16, mT = 32, mB = 36;
  const W  = 580, H  = 202;
  const iW = W - mL - mR;
  const iH = H - mT - mB;

  const barW  = iW / 3;
  const gap   = barW * 0.18;
  const xPos  = i => mL + i * barW;
  const yPos  = v => mT + iH - (v / yMax) * iH;
  const barHt = v => (v / yMax) * iH;

  let s = `<svg viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" class="chart-svg chart-svg-mt">`;

  // Chart title
  s += `<text x="${(W / 2).toFixed(1)}" y="${mT - 16}"
    text-anchor="middle" font-family="sans-serif" font-size="12" fill="#839496"
    letter-spacing="0.05em">${title}</text>`;

  // Horizontal grid lines
  for (const t of ticks) {
    const cy = yPos(t);
    s += `<line x1="${mL}" y1="${cy.toFixed(1)}" x2="${W - mR}" y2="${cy.toFixed(1)}"
      stroke="#d6cfbe" stroke-width="1"/>`;
  }

  // Bars
  for (let i = 0; i < 3; i++) {
    const v  = iconAvg[i];
    const bx = xPos(i) + gap;
    const bw = barW - gap * 2;
    const by = yPos(v);
    const bh = barHt(v);
    s += `<rect x="${bx.toFixed(1)}" y="${by.toFixed(1)}"
      width="${bw.toFixed(1)}" height="${bh.toFixed(1)}"
      fill="${iconColors[i]}" rx="2" class="chart-bar">
      <title>Total: ${iconTotals[i]}</title>
    </rect>`;
    s += `<text x="${(bx + bw / 2).toFixed(1)}" y="${(by - 5).toFixed(1)}"
      text-anchor="middle" font-family="sans-serif" font-size="10" fill="#839496"
      pointer-events="none">${v.toFixed(2)}</text>`;
  }

  // Axes
  s += `<line x1="${mL}" y1="${mT}" x2="${mL}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;
  s += `<line x1="${mL}" y1="${mT + iH}" x2="${W - mR}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;

  // X-axis labels
  for (let i = 0; i < 3; i++) {
    const cx = (xPos(i) + barW / 2).toFixed(1);
    s += `<text x="${cx}" y="${mT + iH + 22}"
      text-anchor="middle" font-family="thronesdb" font-size="20" fill="${iconColors[i]}"
      >${labels[i]}</text>`;
  }

  // Left Y-axis (averages)
  for (const t of ticks) {
    const cy = yPos(t).toFixed(1);
    s += `<text x="${mL - 7}" y="${+cy + 4}"
      text-anchor="end" font-family="sans-serif" font-size="10" fill="#93a1a1"
      >${t.toFixed(1)}</text>`;
  }

  // Axis title
  s += `<text x="${mL - 36}" y="${(mT + iH / 2).toFixed(1)}"
    text-anchor="middle" font-family="sans-serif" font-size="10" fill="#93a1a1"
    transform="rotate(-90,${mL - 36},${(mT + iH / 2).toFixed(1)})"
    >avg / setup</text>`;

  s += `</svg>`;
  return s;
}

/** Renders a histogram SVG with count (left) and percentage (right) Y-axes. */
function renderHistogram(dist, title, labelFn) {
  const maxVal = Math.max(...dist, 1);
  // Round yMax up to a clean multiple for neat tick spacing
  const rawStep  = maxVal / 4;
  const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
  const niceStep  = Math.ceil(rawStep / magnitude) * magnitude;
  const yMax      = niceStep * 4;
  const ticks     = [0, 1, 2, 3, 4].map(i => i * niceStep);

  const mL = 48, mR = 48, mT = 32, mB = 36;
  const W  = 580, H  = 202;
  const iW = W - mL - mR;
  const iH = H - mT - mB;

  const barW  = iW / dist.length;
  const gap   = barW * 0.18;
  const xPos  = i => mL + i * barW;
  const yPos  = v => mT + iH - (v / yMax) * iH;
  const barHt = v => (v / yMax) * iH;

  let s = `<svg viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" class="chart-svg chart-svg-mt">`;

  // Chart title
  s += `<text x="${(W / 2).toFixed(1)}" y="${mT - 16}"
    text-anchor="middle" font-family="sans-serif" font-size="12" fill="#839496"
    letter-spacing="0.05em">${title}</text>`;

  // Horizontal grid lines
  for (const t of ticks) {
    const cy = yPos(t);
    s += `<line x1="${mL}" y1="${cy}" x2="${W - mR}" y2="${cy}"
      stroke="#d6cfbe" stroke-width="1"/>`;
  }

  // Bars
  for (let i = 0; i < dist.length; i++) {
    const v   = dist[i] || 0;
    const bx  = xPos(i) + gap;
    const bw  = barW - gap * 2;
    const by  = yPos(v);
    const bh  = barHt(v);
    const pct = Math.round(v / SIM_COUNT * 100);
    s += `<rect x="${bx.toFixed(1)}" y="${by.toFixed(1)}"
      width="${bw.toFixed(1)}" height="${bh.toFixed(1)}"
      fill="${v > 0 ? '#b58900' : '#d6cfbe'}" rx="2" class="chart-bar">
      ${v > 0 ? `<title>${pct}%</title>` : ''}
    </rect>`;
    if (v > 0) {
      s += `<text x="${(bx + bw / 2).toFixed(1)}" y="${(by - 5).toFixed(1)}"
        text-anchor="middle" font-family="sans-serif" font-size="10" fill="#839496"
        pointer-events="none">${v}</text>`;
    }
  }

  // Axes
  s += `<line x1="${mL}" y1="${mT}" x2="${mL}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;
  s += `<line x1="${W - mR}" y1="${mT}" x2="${W - mR}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;
  s += `<line x1="${mL}" y1="${mT + iH}" x2="${W - mR}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;

  // X-axis labels (only for indices with at least one setup)
  for (let i = 0; i < dist.length; i++) {
    if (!dist[i]) continue;
    const cx = (xPos(i) + barW / 2).toFixed(1);
    s += `<text x="${cx}" y="${mT + iH + 18}"
      text-anchor="middle" font-family="sans-serif" font-size="11" fill="#93a1a1"
      >${labelFn(i)}</text>`;
  }

  // Left Y-axis (counts)
  for (const t of ticks) {
    const cy = yPos(t).toFixed(1);
    s += `<text x="${mL - 7}" y="${+cy + 4}"
      text-anchor="end" font-family="sans-serif" font-size="10" fill="#93a1a1"
      >${t}</text>`;
  }

  // Right Y-axis (percentages)
  for (const t of ticks) {
    const cy = yPos(t).toFixed(1);
    const p  = Math.round(t / SIM_COUNT * 100);
    s += `<text x="${W - mR + 7}" y="${+cy + 4}"
      text-anchor="start" font-family="sans-serif" font-size="10" fill="#93a1a1"
      >${p}%</text>`;
  }

  // Axis titles
  s += `<text x="${mL - 36}" y="${(mT + iH / 2).toFixed(1)}"
    text-anchor="middle" font-family="sans-serif" font-size="10" fill="#93a1a1"
    transform="rotate(-90,${mL - 36},${(mT + iH / 2).toFixed(1)})"
    ># Setups</text>`;
  s += `<text x="${W - mR + 36}" y="${(mT + iH / 2).toFixed(1)}"
    text-anchor="middle" font-family="sans-serif" font-size="10" fill="#93a1a1"
    transform="rotate(90,${W - mR + 36},${(mT + iH / 2).toFixed(1)})"
    >% Setups</text>`;

  s += `</svg>`;
  return s;
}

// ── Deck statistics charts ─────────────────────────────────────────────────

/**
 * Reads the actual unicode character from a CSS ::before pseudo-element.
 * This keeps SVG text labels in sync with icon font definitions in the CSS.
 */
function getCSSIconChar(className) {
  const el = document.createElement('span');
  el.className = className;
  el.style.cssText = 'position:absolute;visibility:hidden';
  document.body.appendChild(el);
  const raw = window.getComputedStyle(el, '::before').content;
  document.body.removeChild(el);
  // Browsers return the character wrapped in quotes, e.g. '"⚔"' or '"\e60b"'
  return raw.replace(/^['"]|['"]$/g, '');
}

/** Counter used to generate unique IDs for deck line chart SVG elements. */
let deckLineIdCounter = 0;

/** Toggles focus on series `idx` in a deck line chart; clicking the same series again clears focus. */
function deckLineToggle(chartId, idx) {
  const svg = document.getElementById(chartId);
  if (!svg) return;
  const next = svg._deckFocus === idx ? null : idx;
  svg._deckFocus = next;
  svg.querySelectorAll('[data-si]').forEach(g => {
    const active = next === null || +g.dataset.si === next;
    g.style.opacity = active ? '1' : '0.1';
    g.style.pointerEvents = active ? '' : 'none';
  });
  svg.querySelectorAll('[data-li]').forEach(g => {
    g.style.opacity = next === null || +g.dataset.li === next ? '1' : '0.3';
  });
}

/** Renders a bar chart SVG for deck statistics. */
function renderDeckBar(values, labels, title, opts = {}) {
  const {
    barColors   = null,
    barColor    = '#b58900',
    xFontFamily = 'sans-serif',
    xFontSize   = 11,
    xFills      = null,
    tooltips    = null,
    yLabel      = '# Cards',
  } = opts;

  if (!values.length) return `<p class="chart-no-data">No data</p>`;

  const maxVal = Math.max(...values, 1);
  const rawStep = maxVal / 4;
  const exp = Math.floor(Math.log10(rawStep || 1));
  const mag = Math.pow(10, exp);
  const niceStep = Math.ceil((rawStep || 1) / mag) * mag;
  const yMax = niceStep * 4;
  const ticks = [0, 1, 2, 3, 4].map(i => i * niceStep);

  const mL = 42, mR = 12, mT = 32, mB = 36;
  const W = 560, H = 192;
  const iW = W - mL - mR;
  const iH = H - mT - mB;
  const n = values.length;
  const barW = iW / n;
  const gap = Math.max(barW * 0.15, 2);
  const xPos = i => mL + i * barW;
  const yPos = v => mT + iH - (v / yMax) * iH;
  const barHt = v => Math.max(0, (v / yMax) * iH);

  let s = `<svg viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" class="chart-svg">`;

  // Title
  s += `<text x="${(W / 2).toFixed(1)}" y="${mT - 14}"
    text-anchor="middle" font-family="sans-serif" font-size="12" fill="#839496"
    letter-spacing="0.04em">${title}</text>`;

  // Grid lines
  for (const t of ticks) {
    const cy = yPos(t).toFixed(1);
    s += `<line x1="${mL}" y1="${cy}" x2="${W - mR}" y2="${cy}" stroke="#d6cfbe" stroke-width="1"/>`;
  }

  // Bars
  for (let i = 0; i < n; i++) {
    const v = values[i] || 0;
    const color = barColors ? (barColors[i] || barColor) : barColor;
    const bx = xPos(i) + gap;
    const bw = Math.max(barW - gap * 2, 1);
    const by = yPos(v);
    const bh = barHt(v);
    const tip = tooltips ? tooltips[i] : '';
    s += `<rect x="${bx.toFixed(1)}" y="${by.toFixed(1)}" width="${bw.toFixed(1)}" height="${bh.toFixed(1)}"
      fill="${v > 0 ? color : '#d6cfbe'}" rx="2">${tip ? `<title>${tip}</title>` : ''}</rect>`;
    if (v > 0) {
      s += `<text x="${(bx + bw / 2).toFixed(1)}" y="${(by - 4).toFixed(1)}"
        text-anchor="middle" font-family="sans-serif" font-size="10" fill="#839496"
        pointer-events="none">${v}</text>`;
    }
  }

  // Axes
  s += `<line x1="${mL}" y1="${mT}" x2="${mL}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;
  s += `<line x1="${mL}" y1="${mT + iH}" x2="${W - mR}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;

  // X-axis labels (skip zero-value bars to reduce clutter when many buckets)
  const showAllLabels = n <= 12;
  for (let i = 0; i < n; i++) {
    if (!showAllLabels && !values[i]) continue;
    const cx = (xPos(i) + barW / 2).toFixed(1);
    const fill = xFills ? (xFills[i] || '#93a1a1') : '#93a1a1';
    s += `<text x="${cx}" y="${(mT + iH + 18).toFixed(1)}"
      text-anchor="middle" font-family="${xFontFamily}" font-size="${xFontSize}" fill="${fill}">${labels[i]}</text>`;
  }

  // Y-axis ticks and labels
  for (const t of ticks) {
    const cy = (yPos(t) + 4).toFixed(1);
    s += `<text x="${mL - 6}" y="${cy}" text-anchor="end" font-family="sans-serif" font-size="10" fill="#93a1a1">${t}</text>`;
  }

  // Y-axis title
  if (yLabel) {
    s += `<text x="${mL - 30}" y="${(mT + iH / 2).toFixed(1)}"
      text-anchor="middle" font-family="sans-serif" font-size="10" fill="#93a1a1"
      transform="rotate(-90,${mL - 30},${(mT + iH / 2).toFixed(1)})">${yLabel}</text>`;
  }

  s += `</svg>`;
  return s;
}

/**
 * Renders a multi-line chart SVG for deck statistics.
 * Each series has { name, color, values, xOffset } where values[i] is the count at x = xOffset + i.
 */
function renderDeckLines(series, title, opts = {}) {
  const { yLabel = '# Cards' } = opts;
  const chartId = `dl${++deckLineIdCounter}`;

  // Build the common x-axis from the union of all x-values in any series
  const allX = new Set();
  for (const s of series) {
    for (let i = 0; i < s.values.length; i++) allX.add(s.xOffset + i);
  }
  const xVals = [...allX].sort((a, b) => a - b);
  if (!xVals.length) return `<p class="chart-no-data">No data</p>`;

  // Map each series onto the common x-axis (0 where series has no value)
  const resolved = series
    .filter(s => s.values.length)
    .map(s => ({
      name: s.name,
      color: s.color,
      points: xVals.map(x => {
        const i = x - s.xOffset;
        return (i >= 0 && i < s.values.length) ? (s.values[i] || 0) : 0;
      }),
    }));
  if (!resolved.length) return `<p class="chart-no-data">No data</p>`;

  // Y-axis scale
  const yRaw = Math.max(...resolved.flatMap(s => s.points), 1);
  const rawStep = yRaw / 4;
  const exp = Math.floor(Math.log10(rawStep || 1));
  const mag = Math.pow(10, exp);
  const niceStep = Math.ceil((rawStep || 1) / mag) * mag;
  const yMax = niceStep * 4;
  const ticks = [0, 1, 2, 3, 4].map(i => i * niceStep);

  const mL = 46, mR = 12, mT = 34, mB = 54;
  const W = 700, H = 240;
  const iW = W - mL - mR;
  const iH = H - mT - mB;
  const n = xVals.length;
  const xPos = i => n <= 1 ? mL + iW / 2 : mL + (i / (n - 1)) * iW;
  const yPos = v => mT + iH - (v / yMax) * iH;

  let s = `<svg id="${chartId}" viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" class="chart-svg">`;

  // Title
  s += `<text x="${(W / 2).toFixed(1)}" y="${mT - 16}"
    text-anchor="middle" font-family="sans-serif" font-size="12" fill="#839496"
    letter-spacing="0.04em">${title}</text>`;

  // Grid lines
  for (const t of ticks) {
    const cy = yPos(t).toFixed(1);
    s += `<line x1="${mL}" y1="${cy}" x2="${W - mR}" y2="${cy}" stroke="#d6cfbe" stroke-width="1"/>`;
  }

  // Series groups — each wrapped in a <g> for opacity toggling
  for (let si = 0; si < resolved.length; si++) {
    const sr = resolved[si];
    s += `<g data-si="${si}">`;
    const pts = sr.points.map((v, i) => `${xPos(i).toFixed(1)},${yPos(v).toFixed(1)}`).join(' ');
    s += `<polyline points="${pts}" fill="none" stroke="${sr.color}" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>`;
    for (let i = 0; i < sr.points.length; i++) {
      const v = sr.points[i];
      if (v <= 0) continue;
      s += `<circle cx="${xPos(i).toFixed(1)}" cy="${yPos(v).toFixed(1)}" r="3" fill="${sr.color}" pointer-events="none"/>`;
      s += `<circle cx="${xPos(i).toFixed(1)}" cy="${yPos(v).toFixed(1)}" r="10" fill="transparent"><title>${sr.name}: ${v}</title></circle>`;
    }
    s += `</g>`;
  }

  // Axes
  s += `<line x1="${mL}" y1="${mT}" x2="${mL}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;
  s += `<line x1="${mL}" y1="${mT + iH}" x2="${W - mR}" y2="${mT + iH}" stroke="#93a1a1" stroke-width="1"/>`;

  // X-axis labels (thin when many ticks)
  const labelEvery = n > 20 ? 5 : n > 12 ? 2 : 1;
  for (let i = 0; i < n; i++) {
    if (i % labelEvery !== 0 && i !== n - 1) continue;
    s += `<text x="${xPos(i).toFixed(1)}" y="${(mT + iH + 16).toFixed(1)}"
      text-anchor="middle" font-family="sans-serif" font-size="11" fill="#93a1a1">${xVals[i]}</text>`;
  }

  // Y-axis ticks and labels
  for (const t of ticks) {
    const cy = (yPos(t) + 4).toFixed(1);
    s += `<text x="${mL - 6}" y="${cy}" text-anchor="end" font-family="sans-serif" font-size="10" fill="#93a1a1">${t}</text>`;
  }

  // Y-axis title
  if (yLabel) {
    s += `<text x="${mL - 34}" y="${(mT + iH / 2).toFixed(1)}"
      text-anchor="middle" font-family="sans-serif" font-size="10" fill="#93a1a1"
      transform="rotate(-90,${mL - 34},${(mT + iH / 2).toFixed(1)})">${yLabel}</text>`;
  }

  // Interactive legend — clicking a key focuses that series
  const legendY = mT + iH + 36;
  const itemW = Math.min(110, Math.floor(iW / resolved.length));
  const totalW = resolved.length * itemW;
  const legendX0 = mL + (iW - totalW) / 2;
  for (let i = 0; i < resolved.length; i++) {
    const lx = legendX0 + i * itemW;
    s += `<g data-li="${i}" onclick="deckLineToggle('${chartId}',${i})">`;
    s += `<rect x="${lx.toFixed(1)}" y="${(legendY - 8).toFixed(1)}" width="${(itemW - 2).toFixed(1)}" height="16" fill="transparent"/>`;
    s += `<line x1="${lx.toFixed(1)}" y1="${legendY}" x2="${(lx + 20).toFixed(1)}" y2="${legendY}" stroke="${resolved[i].color}" stroke-width="2"/>`;
    s += `<circle cx="${(lx + 10).toFixed(1)}" cy="${legendY}" r="3" fill="${resolved[i].color}"/>`;
    s += `<text x="${(lx + 26).toFixed(1)}" y="${(legendY + 4).toFixed(1)}"
      font-family="sans-serif" font-size="11" fill="#839496">${resolved[i].name}</text>`;
    s += `</g>`;
  }

  s += `</svg>`;
  return s;
}

/** Renders all deck statistics charts and returns the combined HTML string. */
function renderDeckCharts(slots, cardMap) {
  const allEntries = Object.entries(slots)
    .map(([code, qty]) => ({ card: cardMap[code], qty }))
    .filter(({ card }) => card);

  const deckCards = allEntries.filter(({ card }) => !NON_DECK_TYPES.has(card.type_name));
  const plots     = allEntries.filter(({ card }) => card.type_name === 'Plot');
  const chars     = deckCards.filter(({ card }) => card.type_name === 'Character');
  const locs      = deckCards.filter(({ card }) => card.type_name === 'Location');
  const atts      = deckCards.filter(({ card }) => card.type_name === 'Attachment');
  const evts      = deckCards.filter(({ card }) => card.type_name === 'Event');

  // ── Faction distribution ─────────────────────────────────────────────
  const factionMap = {};
  for (const { card, qty } of deckCards) {
    const f = card.faction_code || 'neutral';
    factionMap[f] = (factionMap[f] || 0) + qty;
  }
  const factionPresent = FACTION_ORDER.filter(f => factionMap[f]);
  const factionChart = renderDeckBar(
    factionPresent.map(f => factionMap[f]),
    factionPresent.map(f => FACTION_ICONS[f] || f),
    'Cards by Faction',
    {
      barColors:   factionPresent.map(f => FACTION_COLORS[f] || '#b58900'),
      xFontFamily: 'thronesdb',
      xFontSize:   20,
      xFills:      factionPresent.map(f => FACTION_COLORS[f] || '#93a1a1'),
      tooltips:    factionPresent.map(f => `${FACTION_NAMES[f] || f}: ${factionMap[f]}`),
    }
  );

  // ── Character icon distribution ──────────────────────────────────────
  const iconCounts = [0, 0, 0];
  for (const { card, qty } of chars) {
    if (card.is_military) iconCounts[0] += qty;
    if (card.is_intrigue) iconCounts[1] += qty;
    if (card.is_power)    iconCounts[2] += qty;
  }
  const iconChart = renderDeckBar(
    iconCounts,
    [getCSSIconChar('icon-military'), getCSSIconChar('icon-intrigue'), getCSSIconChar('icon-power')],
    'Character Icon Distribution',
    {
      yLabel:      '# Characters',
      barColors:   ['#dc322f', '#859900', '#268bd2'],
      xFontFamily: 'thronesdb',
      xFontSize:   20,
      xFills:      ['#dc322f', '#859900', '#268bd2'],
      tooltips:    [`Military: ${iconCounts[0]}`, `Intrigue: ${iconCounts[1]}`, `Power: ${iconCounts[2]}`],
    }
  );

  // ── Cost/stat distribution helpers ───────────────────────────────────
  function costDist(entries) {
    const costs = entries.flatMap(({ card }) => {
      const c = parseInt(card.cost);
      return isNaN(c) || c < 0 ? [] : [c];
    });
    if (!costs.length) return { values: [], labels: [], xOffset: 0 };
    const max = Math.max(...costs);
    const dist = new Array(max + 1).fill(0);
    for (const { card, qty } of entries) {
      const c = parseInt(card.cost);
      if (!isNaN(c) && c >= 0) dist[c] += qty;
    }
    return { values: dist, labels: dist.map((_, i) => String(i)), xOffset: 0 };
  }

  function statDist(entries, getter) {
    const vals = entries.flatMap(({ card }) => {
      const v = getter(card);
      return isNaN(v) || v < 0 ? [] : [v];
    });
    if (!vals.length) return { values: [], labels: [], xOffset: 0 };
    const min = Math.min(...vals);
    const max = Math.max(...vals);
    const dist = new Array(max - min + 1).fill(0);
    for (const { card, qty } of entries) {
      const v = getter(card);
      if (!isNaN(v) && v >= 0) dist[v - min] += qty;
    }
    return { values: dist, labels: dist.map((_, i) => String(i + min)), xOffset: min };
  }

  const charCostData  = costDist(chars);
  const charStrData   = statDist(chars, c => parseInt(c.strength));
  const locCostData   = costDist(locs);
  const attCostData   = costDist(atts);
  const evtCostData   = costDist(evts);
  const allCostData   = costDist(deckCards);
  const plotIncData   = statDist(plots, c => parseInt(c.income));
  const plotInitData  = statDist(plots, c => parseInt(c.initiative));
  const plotClaimData = statDist(plots, c => parseInt(c.claim));
  const plotResvData  = statDist(plots, c => parseInt(c.reserve));

  // ── Combined cost curve (multi-line) ─────────────────────────────────
  const costCurveChart = renderDeckLines([
    { name: 'Character',       color: '#b58900', values: charCostData.values, xOffset: charCostData.xOffset },
    { name: 'Location',        color: '#859900', values: locCostData.values,  xOffset: locCostData.xOffset  },
    { name: 'Attachment',      color: '#6c71c4', values: attCostData.values,  xOffset: attCostData.xOffset  },
    { name: 'Event',           color: '#268bd2', values: evtCostData.values,  xOffset: evtCostData.xOffset  },
    { name: 'All Cards',       color: '#657b83', values: allCostData.values,  xOffset: allCostData.xOffset  },
    { name: 'Char Strength',   color: '#cb4b16', values: charStrData.values,  xOffset: charStrData.xOffset  },
  ], 'Cost & Strength Curve');

  // ── Combined plot statistics (multi-line) ────────────────────────────
  const plotStatsChart = renderDeckLines([
    { name: 'Income',     color: '#b58900', values: plotIncData.values,   xOffset: plotIncData.xOffset   },
    { name: 'Initiative', color: '#859900', values: plotInitData.values,  xOffset: plotInitData.xOffset  },
    { name: 'Claim',      color: '#dc322f', values: plotClaimData.values, xOffset: plotClaimData.xOffset },
    { name: 'Reserve',    color: '#268bd2', values: plotResvData.values,  xOffset: plotResvData.xOffset  },
  ], 'Plot Statistics', { yLabel: '# Plots' });

  const row = (...cells) => `<div class="deck-chart-grid">${cells.map(c => `<div class="deck-chart-cell">${c}</div>`).join('')}</div>`;

  return [
    row(factionChart),
    row(iconChart),
    row(costCurveChart),
    row(plotStatsChart),
  ].join('');
}
