// ── Utilities ─────────────────────────────────────────────────────────────

/** Sets the text and CSS class of the status element. */
function setStatus(msg, type = '') {
  const el = document.getElementById('status');
  el.textContent = msg;
  el.className = type;
}

/** Parses a ThronesDB deck ID from a full URL or a bare numeric string. Returns null if unrecognised. */
function parseDeckId(raw) {
  raw = raw.trim();
  const match = raw.match(/thronesdb\.com\/decklist\/view\/(\d+)/);
  if (match) return match[1];
  if (/^\d+$/.test(raw)) return raw;
  return null;
}

/** Fetches a URL and returns the parsed JSON; throws on non-2xx status. */
async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status} from ${url}`);
  return res.json();
}

/**
 * Fetches all cards from the ThronesDB API and caches the result.
 * Augments each card with derived fields: is_limited, income_modifier, bestow, shadow, no_attachments.
 */
async function getAllCards() {
  if (cachedCards) return cachedCards;
  const cards = await fetchJson(CARDS_API);
  cachedCards = {};
  for (const card of cards) {
    card.is_limited = typeof card.text === 'string' && card.text.includes('Limited.');
    const incomeMatch = typeof card.text === 'string' && card.text.match(/([+-]\d+)\s+Income/);
    card.income_modifier = incomeMatch ? parseInt(incomeMatch[1], 10) : null;
    card.bestow  = typeof card.text === 'string' && /Bestow\s*\(\s*[1-9]\d*\s*\)/.test(card.text);
    card.shadow  = typeof card.text === 'string' && /Shadow\s*\(\s*\d+\s*\)/.test(card.text);
    const noAttach = typeof card.text === 'string'
      ? card.text.match(/No attachments(?: except ([^.]+))?\./)
      : null;
    if (noAttach) {
      if (noAttach[1]) {
        const exceptTraits = [...noAttach[1].matchAll(/\[([^\]]+)\]/g)]
          .map(m => m[1].replace(/<[^>]+>/g, '').trim().toLowerCase())
          .filter(Boolean);
        card.no_attachments = exceptTraits.length > 0 ? { except: exceptTraits } : { none: true };
      } else {
        card.no_attachments = { none: true };
      }
    }
    cachedCards[card.code] = card;
  }
  updateEconomyFlags();
  return cachedCards;
}

/** Returns true if `card` appears in `set` by name or code (case-insensitive). */
function cardInSet(card, set) {
  return set.has(card.name.toLowerCase()) || set.has(card.code.toLowerCase());
}

/** Reads the current setup-quality configuration from the UI checkboxes. */
function getConfig() {
  return {
    requireTwoChars:    document.getElementById('cfg-two-chars').checked,
    requireFourCost:    document.getElementById('cfg-four-cost').checked,
    requireEconomy:     document.getElementById('cfg-economy').checked,
    requireKeyCard:     document.getElementById('cfg-key-cards').checked,
    requireNoAvoidable: document.getElementById('cfg-no-avoidable').checked,
    minCards:           parseInt(document.getElementById('cfg-min-cards').value, 10),
  };
}

/** Returns true if the setup fails any configured requirement. Poor setups trigger a mulligan. */
function isPoorSetup(cards) {
  const cfg = getConfig();
  if (cards.length < cfg.minCards) return true;
  const nonShadow = cards.filter(c => !c.inShadow);
  if (cfg.requireTwoChars) {
    const chars = nonShadow.filter(c => c.type_name === 'Character' && !c.isDuplicate).length;
    if (chars < 2) return true;
  }
  if (cfg.requireFourCost) {
    const hasFour = nonShadow.some(c => c.type_name === 'Character' && !c.isDuplicate && c._cost >= 4);
    if (!hasFour) return true;
  }
  if (cfg.requireEconomy && !nonShadow.some(c => c.is_economy)) return true;
  if (cfg.requireKeyCard) {
    const keySet = getKeySet();
    if (keySet.size > 0 && !cards.some(c => cardInSet(c, keySet))) return true;
  }
  if (cfg.requireNoAvoidable) {
    const avoidSet = getAvoidSet();
    if (avoidSet.size > 0 && cards.some(c => cardInSet(c, avoidSet))) return true;
  }
  return false;
}

/** Returns the set of card codes currently checked as key cards in the deck table. */
function getKeySet() {
  return new Set(
    [...document.querySelectorAll('#deck-table-section .key-cb:checked')]
      .map(cb => cb.closest('tr').dataset.code.toLowerCase())
  );
}

/** Returns the set of card codes currently checked as avoidable in the deck table. */
function getAvoidSet() {
  return new Set(
    [...document.querySelectorAll('#deck-table-section .avoidable-cb:checked')]
      .map(cb => cb.closest('tr').dataset.code.toLowerCase())
  );
}

/** Returns the set of card codes currently checked as restricted in the deck table. */
function getRestrictedSet() {
  return new Set(
    [...document.querySelectorAll('#deck-table-section .restricted-cb:checked')]
      .map(cb => cb.closest('tr').dataset.code.toLowerCase())
  );
}

/** Updates the is_economy flag on all cached cards based on deck table Economy checkboxes. */
function updateEconomyFlags() {
  if (!cachedCards) return;
  const economyCodes = new Set(
    [...document.querySelectorAll('#deck-table-section .economy-cb:checked')]
      .map(cb => cb.closest('tr').dataset.code)
  );
  for (const card of Object.values(cachedCards)) {
    card.is_economy = economyCodes.has(card.code);
  }
}

// ── Analyze ───────────────────────────────────────────────────────────────

/** Clears the deck table content and hides all deck-related panels. */
function clearDeckUI() {
  document.getElementById('deck-table-section').innerHTML = '';
  document.getElementById('deck-table-details').style.display = '';
  document.getElementById('config-details').style.display = '';
  document.getElementById('sort-details').style.display = '';
  document.getElementById('deck-stats-details').style.display = '';
  document.getElementById('analyze-btn-wrapper').style.display = '';
  document.getElementById('deck-title').style.display = '';
}

/** Fetches the deck and card data then runs the setup simulation. */
async function analyze() {
  const btn = document.getElementById('analyze-btn');
  const urlInput = document.getElementById('deck-url').value;

  const deckId = parseDeckId(urlInput);
  if (!deckId) {
    setStatus('Please enter a valid ThronesDB deck URL or deck ID.', 'error');
    return;
  }

  renderLoadingState('Fetching decklist…');
  document.getElementById('setup-section').style.display = 'block';
  btn.disabled = true;
  setDeckLoadLocked(true);
  try {
    const [decklist, cardMap] = await Promise.all([
      fetchJson(DECKLIST_API(deckId)),
      getAllCards(),
    ]);

    currentSlots   = decklist.slots || {};
    currentCardMap = cardMap;

    updateEconomyFlags();
    runSimulations();
  } catch (err) {
    setStatus(`Error: ${err.message}`, 'error');
    console.error(err);
    document.getElementById('setup-section').style.display = 'none';
    btn.disabled = false;
    setDeckLoadLocked(false);
  }
}

let deckLoadLocked = false;
document.getElementById('deck-url').addEventListener('keydown', e => {
  if (e.key === 'Enter' && !deckLoadLocked) fetchAndRenderDeckTable();
});

/** Locks or unlocks the deck load button and tracks the locked state. */
function setDeckLoadLocked(locked) {
  deckLoadLocked = locked;
  document.getElementById('load-deck-btn').disabled = locked;
}

/** Returns the card code of the currently selected Red Door location, or null. */
function getRedDoorLocationCode() {
  const cb = document.querySelector('#deck-table-section .red-door-cb:checked');
  return cb ? cb.closest('tr').dataset.code : null;
}

/** Updates the Analyze button's disabled state based on Red Door selection requirements. */
function updateAnalyzeButton() {
  const btn = document.getElementById('analyze-btn');
  const msg = document.getElementById('red-door-msg');
  const needsSelection = hasRedDoor && !getRedDoorLocationCode();
  btn.disabled = needsSelection;
  msg.style.display = needsSelection ? 'block' : 'none';
}

/** Fetches a deck by URL/ID, renders the deck table, configuration panels, and deck charts. */
async function fetchAndRenderDeckTable() {
  document.getElementById('setup-section').style.display = 'none';
  const urlInput = document.getElementById('deck-url').value;
  const deckId   = parseDeckId(urlInput);
  const section  = document.getElementById('deck-table-section');
  const details  = document.getElementById('deck-table-details');
  const btnWrapper = document.getElementById('analyze-btn-wrapper');
  if (!deckId) { clearDeckUI(); setStatus('Please insert a valid ThronesDB deck URL or deck ID', 'error'); return; }

  setStatus('Loading deck data…', 'loading');
  try {
    const [decklist, cardMap] = await Promise.all([
      fetchJson(DECKLIST_API(deckId)),
      getAllCards(),
    ]);

    const slots = decklist.slots || {};
    hasRedDoor = Object.keys(slots).includes('08039');

    const rows = Object.entries(slots)
      .map(([code, qty]) => ({ card: cardMap[code], qty }))
      .filter(({ card }) => card && !NON_DECK_TYPES.has(card.type_name))
      .sort((a, b) => {
        const ta = CARD_TYPE_ORDER[a.card.type_name] ?? 4;
        const tb = CARD_TYPE_ORDER[b.card.type_name] ?? 4;
        return ta !== tb ? ta - tb : a.card.name.localeCompare(b.card.name);
      });

    if (rows.length === 0) { clearDeckUI(); return; }

    const knownEconomyIds    = new Set([...knownEconomyCards].map(e => e.id));
    const knownAvoidableIds  = new Set([...knownAvoidableCards].map(e => e.id));
    const knownRestrictedIds = new Set([...knownRestrictedCards].map(e => e.id));
    const opponentRestrictedIds = new Set(
      [...attachmentWithRestrictions].filter(e => e.opponent === true).map(e => e.id)
    );
    const colCount = hasRedDoor ? 6 : 5;
    let lastType = null;
    const trs = rows.map(({ card, qty }) => {
      const name = qty > 1 ? `${card.label || card.name} ×${qty}` : (card.label || card.name);
      const isEvent = card.type_name === 'Event';
      const isOpponentRestricted  = opponentRestrictedIds.has(card.code);
      const isDisabled            = (isEvent && !card.shadow) || isOpponentRestricted;
      const isEconomy     = card.is_limited || card.income_modifier > 0 || knownEconomyIds.has(card.code);
      const isAvoidable   = knownAvoidableIds.has(card.code) || (card.bestow && card.type_name === 'Character');
      const isRestricted  = knownRestrictedIds.has(card.code) || (card.bestow && card.type_name !== 'Character') || isOpponentRestricted;
      const cb = (checked, disabled) =>
        `<input type="checkbox"${checked ? ' checked' : ''}${disabled ? ' disabled' : ''}>`;
      const isRedDoorEligible = hasRedDoor
        && card.type_name === 'Location'
        && card.is_unique
        && !card.is_limited
        && parseInt(card.cost) <= 3
        && !isDisabled;
      const redDoorCell = hasRedDoor
        ? `<td>${isRedDoorEligible ? cb(false, false).replace('<input', '<input class="red-door-cb"') : ''}</td>`
        : '';
      const separator = card.type_name !== lastType
        ? `<tr class="deck-table-type-sep"><td colspan="${colCount}">${card.type_name}</td></tr>`
        : '';
      lastType = card.type_name;
      return `${separator}<tr data-code="${card.code}"${isDisabled ? ' class="deck-table-disabled"' : ''}>
        <td class="deck-table-card-name" data-img="${card.image_url || ''}"><span class="card-type-icon icon-${card.type_name.toLowerCase()}" style="color:${FACTION_COLORS[card.faction_code] || '#93a1a1'}"></span>${name}</td>
        <td>${cb(isEconomy, isDisabled).replace('<input', '<input class="economy-cb"')}</td>
        <td>${cb(false, isDisabled).replace('<input', '<input class="key-cb"')}</td>
        <td>${cb(isAvoidable, isDisabled).replace('<input', '<input class="avoidable-cb"')}</td>
        <td>${cb(isRestricted, isDisabled).replace('<input', '<input class="restricted-cb"')}</td>
        ${redDoorCell}
      </tr>`;
    }).join('');

    const factionColor = FACTION_COLORS[decklist.faction_code] || '#93a1a1';
    const factionName  = FACTION_NAMES[decklist.faction_code] || decklist.faction_code;
    const agendaNames  = Object.entries(slots)
      .filter(([code]) => cardMap[code]?.type_name === 'Agenda')
      .map(([code]) => cardMap[code].label || cardMap[code].name)
      .join(' · ');
    const subtitle = agendaNames ? `${factionName} · ${agendaNames}` : factionName;
    const deckTitleEl = document.getElementById('deck-title');
    deckTitleEl.innerHTML = `
      <div class="deck-title-inner">
        <div class="deck-title-row">
          <span class="icon-${decklist.faction_code} deck-title-faction-icon" style="color:${factionColor};"></span>
          <span class="deck-title-name">${decklist.name}</span>
        </div>
        <div class="deck-title-subtitle">${subtitle}</div>
      </div>`;
    deckTitleEl.style.display = 'flex';

    const redDoorLegend = hasRedDoor
      ? `<div>🚪 — <b>Red Door</b>: The unique location set up behind The House With the Red Door (cost ≤ 3, non-limited). Exactly one must be selected. It is removed from the deck before setup and setup gold is reduced to 4.</div>`
      : '';
    const redDoorHeader = hasRedDoor ? '<th>🚪</th>' : '';
    section.innerHTML = `
      <div class="deck-legend">
        <div>💰 — <b>Economy</b>: Cards that contribute to the deck economy (e.g. limited, provides income, cost reduction)</div>
        <div>🗝️ — <b>Key</b>: Cards that are crucial to the deck and are a priority for setup</div>
        <div>😐 — <b>Avoidable</b>: Cards that should be avoided in setup because they have reactions to entering play (e.g. cards with Bestow)</div>
        <div>🚫 — <b>Restricted</b>: Cards that are illegal to be played during setup (e.g. never have valid targets) or that make no sense (e.g. Milk of the Poppy). These cards are never selected for setups.</div>
        ${redDoorLegend}
      </div>
      <table class="deck-table">
        <thead><tr>
          <th>Name</th>
          <th>💰</th>
          <th>🗝️</th>
          <th>😐</th>
          <th>🚫</th>
          ${redDoorHeader}
        </tr></thead>
        <tbody>${trs}</tbody>
      </table>`;
    setStatus('');
    details.style.display = 'block';
    details.setAttribute('open', '');
    document.getElementById('config-details').style.display = 'block';
    document.getElementById('sort-details').style.display = 'block';
    renderSortCriteria();
    document.getElementById('deck-stats-details').style.display = 'block';
    document.getElementById('deck-charts').innerHTML = renderDeckCharts(slots, cardMap);
    btnWrapper.style.display = 'block';

    // Red Door: mutual exclusion — only one 🚪 checkbox may be checked at a time.
    // Also disable the row's other checkboxes while a location is selected.
    if (hasRedDoor) {
      section.addEventListener('change', e => {
        if (!e.target.classList.contains('red-door-cb')) return;
        const row = e.target.closest('tr');
        const rowCbs = row.querySelectorAll('.economy-cb, .key-cb, .avoidable-cb, .restricted-cb');
        if (e.target.checked) {
          section.querySelectorAll('.red-door-cb').forEach(cb => {
            if (cb !== e.target) { cb.checked = false; cb.disabled = true; }
          });
          rowCbs.forEach(cb => { cb.disabled = true; });
        } else {
          section.querySelectorAll('.red-door-cb').forEach(cb => { cb.disabled = false; });
          rowCbs.forEach(cb => { cb.disabled = false; });
        }
        updateAnalyzeButton();
      });
    }
    updateAnalyzeButton();
  } catch (err) {
    setStatus(`Error: ${err.message}`, 'error');
    clearDeckUI();
  }
}


// ── Card image tooltip ─────────────────────────────────────────────────────
// Follows the cursor and shows the card art when hovering a card name cell.
{
  const tooltip    = document.getElementById('card-image-tooltip');
  const skeleton   = tooltip.querySelector('.card-img-skeleton');
  const tooltipImg = tooltip.querySelector('img');
  const TOOLTIP_MARGIN = 12;

  function showSkeleton() {
    skeleton.style.display = 'block';
    tooltipImg.style.display = 'none';
  }

  function showImage() {
    skeleton.style.display = 'none';
    tooltipImg.style.display = 'block';
  }

  tooltipImg.addEventListener('load',  showImage);
  tooltipImg.addEventListener('error', showImage);

  document.addEventListener('mouseover', e => {
    const td = e.target.closest('.deck-table-card-name');
    if (!td || !td.dataset.img) return;
    showSkeleton();
    tooltipImg.src = td.dataset.img;
    tooltip.style.display = 'block';
  });

  document.addEventListener('mousemove', e => {
    if (tooltip.style.display !== 'block') return;
    const tw = tooltip.offsetWidth;
    const th = tooltip.offsetHeight;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    let x = e.clientX + TOOLTIP_MARGIN;
    let y = e.clientY + TOOLTIP_MARGIN;
    if (x + tw > vw) x = e.clientX - tw - TOOLTIP_MARGIN;
    if (y + th > vh) y = e.clientY - th - TOOLTIP_MARGIN;
    tooltip.style.left = x + 'px';
    tooltip.style.top  = y + 'px';
  });

  document.addEventListener('mouseout', e => {
    if (!e.target.closest('.deck-table-card-name')) return;
    tooltip.style.display = 'none';
    tooltipImg.src = '';
  });
}
