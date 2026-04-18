/**
 * Builds the ordered card pool for simulation from the deck's slot map.
 * Removes exactly one copy of the Red Door location when the agenda is active,
 * since it is placed behind the door before setup begins.
 */
function buildDeckPool(slots, cardMap, redDoorCode, armedCodes) {
  const pool = [];
  let redDoorRemoved = false;
  const armedRemoved = new Set();
  for (const [code, qty] of Object.entries(slots)) {
    const card = cardMap[code];
    if (!card || NON_DECK_TYPES.has(card.type_name)) continue;
    for (let i = 0; i < qty; i++) {
      if (redDoorCode && code === redDoorCode && !redDoorRemoved) {
        redDoorRemoved = true;
        continue;
      }
      // Remove exactly one copy of each armed attachment — it's set aside before setup.
      if (armedCodes && armedCodes.has(code) && !armedRemoved.has(code)) {
        armedRemoved.add(code);
        continue;
      }
      pool.push(card);
    }
  }
  return pool;
}

/** Draws n cards from pool using a Fisher-Yates partial shuffle. */
function drawHand(pool, n) {
  const arr = pool.slice();
  const draw = [];
  for (let i = 0; i < n && i < arr.length; i++) {
    const j = i + Math.floor(Math.random() * (arr.length - i));
    [arr[i], arr[j]] = [arr[j], arr[i]];
    draw.push(arr[i]);
  }
  return draw;
}

/** Returns the numeric cost of a card, or null if unparseable (e.g. "-"). */
function cardCost(card) {
  const n = parseInt(card.cost);
  return isNaN(n) ? null : n;
}

/** Gold cost to place a card into shadows during setup. */
const SHADOW_COST = 2;

/** IDs of attachments that target locations rather than characters. */
const locationAttachmentIds = new Set(
  [...attachmentWithRestrictions].filter(r => r.location === true).map(r => r.id)
);

/** Map from card code to its attachment restriction object. */
const restrictionsByCode = new Map(
  [...attachmentWithRestrictions].map(r => [r.id, r])
);

/** Parses "Lord. Lady. Spy." into Set(["lord", "lady", "spy"]). */
function cardTraits(card) {
  if (!card.traits) return new Set();
  return new Set(card.traits.split('.').map(t => t.trim().toLowerCase()).filter(Boolean));
}

/**
 * Returns true if `character` can receive an attachment with the given traits.
 * Handles "No attachments." and "No attachments except [Trait]." card text.
 */
function characterAcceptsAttachment(character, attachTraits) {
  const r = character.no_attachments;
  if (!r) return true;
  if (r.none) return false;
  return r.except.some(t => attachTraits.has(t));
}

/**
 * Returns true if `attachment` has at least one valid target among `candidates`.
 * `candidates` are the non-shadow characters (or locations) in the current setup combo.
 */
function attachmentHasValidTarget(attachment, candidates) {
  const restriction = restrictionsByCode.get(attachment.code);
  if (!restriction || !restriction.predicate) return candidates.length > 0;

  const parts = restriction.predicate.split(/(&&|\|\|)/);

  // ThronesDB faction codes: thenightswatch → "the night's watch"; others pass through.
  function normFaction(code) {
    return code === 'thenightswatch' ? "the night's watch" : (code || '');
  }

  function evalField(field, target) {
    switch (field.trim()) {
      case 'traits':
        if (!restriction.traits) return true;
        { const t = cardTraits(target);
          return restriction.traits.some(rt => t.has(rt.toLowerCase())); }
      case 'excluded_traits':
        if (!restriction.excluded_traits) return true;
        { const t = cardTraits(target);
          return !restriction.excluded_traits.some(rt => t.has(rt.toLowerCase())); }
      case 'factions':
      case 'faction':   // 'faction' (singular) in some predicates — same data field
        if (!restriction.factions) return true;
        return restriction.factions.includes(normFaction(target.faction_code));
      case 'unique':
        if (restriction.unique === undefined) return true;
        return !!target.is_unique === restriction.unique;
      case 'limited':
        if (restriction.limited === undefined) return true;
        return !!target.is_limited === restriction.limited;
      case 'location':
        if (restriction.location === undefined) return true;
        return (target.type_name === 'Location') === restriction.location;
      case 'loyal':
        if (restriction.loyal === undefined) return true;
        // restriction.loyal is the required value of target.loyal
        return !!target.loyal === restriction.loyal;
      case 'shadow':
        if (restriction.shadow === undefined) return true;
        return !!target.shadow === restriction.shadow;
      case 'title':
        if (!restriction.title) return true;
        return (target.name || '').toLowerCase() === restriction.title.toLowerCase();
      case 'cost':
        if (!restriction.cost) return true;
        { const c = parseInt(target.cost);
          return !isNaN(c) && restriction.cost.includes(c); }
      case 'limit':
        return true;  // attaching once in setup always satisfies any limit ≥ 1
      case 'opponent':
        return false; // can never target opponent's cards during our own setup
      default:
        return true;
    }
  }

  return candidates.some(target => {
    let result = evalField(parts[0], target);
    for (let i = 1; i < parts.length; i += 2) {
      const val = evalField(parts[i + 1], target);
      if (parts[i] === '&&') result = result && val;
      else result = result || val;
    }
    return result;
  });
}

/**
 * Returns all valid setup combinations for the given hand.
 * Events are only eligible if they have Shadow (they always go into shadows).
 * Cards with cost "-" always go into shadows at SHADOW_COST.
 */
function allValidSetups(hand, redDoorCode) {
  // Only consider cards that can be set up.
  // Events are only eligible if they have shadow (always go into shadows).
  // Cards with cost "-" always have shadow and go into shadows at SHADOW_COST.
  const eligible = hand
    .filter(c => (SETUP_TYPES.has(c.type_name) || (c.type_name === 'Event' && c.shadow)) && (cardCost(c) !== null || c.shadow))
    .map(c => ({ ...c, _cost: cardCost(c) ?? 0 }));

  // Red Door agenda: setup gold is 4 instead of 8; the selected location is already
  // "in play" so any copy in hand is always a duplicate.
  const effectiveGold = redDoorCode ? 4 : SETUP_GOLD;

  const results = [];
  const n = eligible.length;
  const seen = new Set();

  for (let mask = 1; mask < (1 << n); mask++) {
    const combo = [];
    for (let i = 0; i < n; i++) {
      if (mask & (1 << i)) combo.push(eligible[i]);
    }

    // Unique cards: first copy costs full price; extra copies are free duplicates.
    // Non-unique cards always pay full price.
    // Pre-seed with the Red Door location so any copy in hand is always a duplicate.
    const uniqueSeen = new Set(redDoorCode ? [redDoorCode] : []);
    let total = 0;
    let cardsWithDetails = combo.map((card) => {
      const inShadow = !!card.shadow;
      if (card.is_unique && uniqueSeen.has(card.code)) {
        // Shadow duplicates are excluded: bringing them out of shadows costs full price,
        // which defeats the purpose of treating them as free duplicates.
        if (inShadow) return null;
        return { ...card, isDuplicate: true, effectiveCost: 0, inShadow };
      }
      if (card.is_unique) uniqueSeen.add(card.code);
      const cost = inShadow ? SHADOW_COST : card._cost;
      total += cost;
      return { ...card, isDuplicate: false, effectiveCost: cost, inShadow };
    }).filter(c => c !== null);

    // Non-shadow attachments need a valid target in the combo.
    // Location-attachments need a non-shadow location; others need a non-shadow character.
    // An attachment without a valid target is dropped from the combo, not the whole combo.
    cardsWithDetails = cardsWithDetails.filter(c => {
      if (c.type_name !== 'Attachment' || c.inShadow) return true;
      const isLocAttach = locationAttachmentIds.has(c.code);
      const attachTraits = isLocAttach ? null : cardTraits(c);
      const candidates = cardsWithDetails.filter(t => {
        if (t.inShadow) return false;
        if (isLocAttach) return t.type_name === 'Location';
        return t.type_name === 'Character' && characterAcceptsAttachment(t, attachTraits);
      });
      return attachmentHasValidTarget(c, candidates);
    });
    if (cardsWithDetails.length === 0) continue;
    total = cardsWithDetails.reduce((sum, c) => sum + c.effectiveCost, 0);

    const limitedCount = cardsWithDetails.filter(c => c.is_limited).length;
    if (total <= effectiveGold && limitedCount <= 1) {
      const key = cardsWithDetails.map(c => `${c.code}:${c.inShadow ? 's' : 'n'}`).sort().join('|');
      if (!seen.has(key)) {
        seen.add(key);
        results.push({ cards: cardsWithDetails, total });
      }
    }
  }

  return results;
}

const SORT_CRITERIA = [
  {
    id: 'poor',
    label: 'Not poor',
    hint: null,
    compare(a, b) {
      return (isPoorSetup(a.cards) ? 1 : 0) - (isPoorSetup(b.cards) ? 1 : 0);
    }
  },
  {
    id: 'card-count',
    label: 'Number of cards',
    hint: 'More is better',
    compare(a, b) {
      return b.cards.length - a.cards.length;
    }
  },
  {
    id: 'key-cards',
    label: '🗝️ Number of key cards',
    hint: 'More is better',
    compare(a, b) {
      const keySet = getKeySet();
      return b.cards.filter(c => cardInSet(c, keySet)).length
           - a.cards.filter(c => cardInSet(c, keySet)).length;
    }
  },
  {
    id: 'virtual-gold',
    label: 'Total virtual gold — card cost + income provided',
    hint: 'More is better',
    compare(a, b) {
      const vg = cards => cards.reduce((sum, c) => {
        if (c.isDuplicate) return sum;
        return sum + c.effectiveCost + (c.income_modifier > 0 ? c.income_modifier : 0);
      }, 0);
      return vg(b.cards) - vg(a.cards);
    }
  },
  {
    id: 'economy',
    label: '💰 Number of economy cards',
    hint: 'More is better',
    compare(a, b) {
      return b.cards.filter(c => c.is_economy).length
           - a.cards.filter(c => c.is_economy).length;
    }
  },
  {
    id: 'avoidable',
    label: '😐 Number of avoidable cards',
    hint: 'Fewer is better',
    compare(a, b) {
      const avoidSet = getAvoidSet();
      return a.cards.filter(c => cardInSet(c, avoidSet)).length
           - b.cards.filter(c => cardInSet(c, avoidSet)).length;
    }
  },
  {
    id: 'char-count',
    label: 'Number of non-duplicate characters',
    hint: 'More is better',
    compare(a, b) {
      const ndc = cards => cards.filter(c => c.type_name === 'Character' && !c.isDuplicate);
      return ndc(b.cards).length - ndc(a.cards).length;
    }
  },
  {
    id: 'char-strength',
    label: 'Total character strength',
    hint: 'More is better',
    compare(a, b) {
      const ndc = cards => cards.filter(c => c.type_name === 'Character' && !c.isDuplicate);
      const str = cards => ndc(cards).reduce((s, c) => s + (c.strength || 0), 0);
      return str(b.cards) - str(a.cards);
    }
  },
  {
    id: 'shadow-count',
    label: 'Number of cards in shadows',
    hint: 'More is better',
    compare(a, b) {
      return b.cards.filter(c => c.inShadow).length
           - a.cards.filter(c => c.inShadow).length;
    }
  },
];

let sortCriteriaOrder = SORT_CRITERIA.map((_, i) => i);

/** Renders the draggable sort-criteria list and wires up drag-and-drop reordering. */
function renderSortCriteria() {
  const list = document.getElementById('sort-criteria-list');
  if (!list) return;
  list.innerHTML = sortCriteriaOrder.map((criterionIdx, rank) => {
    const c = SORT_CRITERIA[criterionIdx];
    return `<li class="sort-criterion" draggable="true" data-rank="${rank}" data-idx="${criterionIdx}">
      <span class="sort-criterion-rank">${rank + 1}.</span>
      <span class="sort-criterion-handle">⠿</span>
      <span class="sort-criterion-label">${c.label}</span>
      ${c.hint ? `<span class="sort-criterion-hint">${c.hint}</span>` : ''}
    </li>`;
  }).join('');

  let dragSrcRank = null;

  list.querySelectorAll('.sort-criterion').forEach(item => {
    item.addEventListener('dragstart', e => {
      dragSrcRank = parseInt(item.dataset.rank);
      e.dataTransfer.effectAllowed = 'move';
    });
    item.addEventListener('dragover', e => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      list.querySelectorAll('.sort-criterion').forEach(i => i.classList.remove('drag-over'));
      item.classList.add('drag-over');
    });
    item.addEventListener('dragleave', () => {
      item.classList.remove('drag-over');
    });
    item.addEventListener('drop', e => {
      e.preventDefault();
      item.classList.remove('drag-over');
      const destRank = parseInt(item.dataset.rank);
      if (dragSrcRank === null || dragSrcRank === destRank) return;
      const moved = sortCriteriaOrder.splice(dragSrcRank, 1)[0];
      sortCriteriaOrder.splice(destRank, 0, moved);
      dragSrcRank = null;
      renderSortCriteria();
    });
    item.addEventListener('dragend', () => {
      list.querySelectorAll('.sort-criterion').forEach(i => i.classList.remove('drag-over'));
      dragSrcRank = null;
    });
  });
}

/** Sorts setups by the current criteria order; first differentiating criterion wins. */
function sortSetups(setups) {
  return setups.slice().sort((a, b) => {
    for (const idx of sortCriteriaOrder) {
      const diff = SORT_CRITERIA[idx].compare(a, b);
      if (diff !== 0) return diff;
    }
    return 0;
  });
}

const SIM_COUNT = 5000;

/** Runs SIM_COUNT simulations against the current deck and renders the results. */
function runSimulations() {
  if (!currentSlots || !currentCardMap) return;

  const btn = document.getElementById('analyze-btn');
  btn.disabled = true;
  renderLoadingState('Running simulations…');

  // Defer to next frame so the browser can repaint before the heavy loop
  setTimeout(() => {
    const redDoorCode = hasRedDoor ? getRedDoorLocationCode() : null;
    const armedCodes  = hasArmedToTheTeeth ? getArmedToTeethSet() : null;
    const pool = buildDeckPool(currentSlots, currentCardMap, redDoorCode, armedCodes);
    let poorCount = 0, economyCount = 0, limitedCount = 0, mulliganCount = 0, mulliganSuccessCount = 0;
    let totalGold = 0, totalCards = 0;
    const goldDist      = new Array(SETUP_GOLD + 1).fill(0);
    const cardsDist     = new Array(8).fill(0);  // indices 0-7 cards
    const charsDist     = new Array(8).fill(0);  // indices 0-7 distinct characters
    const strengthDist  = new Array(31).fill(0); // indices 0-30 strength
    const keyCardsDist  = new Array(8).fill(0);  // indices 0-7 key cards
    const avoidableDist = new Array(8).fill(0);  // indices 0-7 avoidable cards
    const shadowDist    = new Array(8).fill(0);  // indices 0-7 shadow cards
    const keySet            = getKeySet();
    const avoidSet          = getAvoidSet();
    const restrictedSet     = getRestrictedSet();
    const filterRestricted  = hand => hand.filter(c => !cardInSet(c, restrictedSet));
    const iconTotals         = [0, 0, 0]; // [military, intrigue, power] character counts
    const iconStrengthTotals = [0, 0, 0]; // [military, intrigue, power] strength sums
    let   iconSetupCount = 0;
    const simLog = [];

    for (let i = 0; i < SIM_COUNT; i++) {
      let initialHand  = drawHand(pool, 7);
      let mulliganHand = null;
      let sorted       = sortSetups(allValidSetups(filterRestricted(initialHand), redDoorCode));
      let best         = sorted.length > 0 ? sorted[0] : null;

      // Mulligan if best setup is poor (or no setup at all)
      if (!best || isPoorSetup(best.cards)) {
        mulliganCount++;
        mulliganHand = drawHand(pool, 7);
        sorted = sortSetups(allValidSetups(filterRestricted(mulliganHand), redDoorCode));
        best   = sorted.length > 0 ? sorted[0] : null;
        if (best && !isPoorSetup(best.cards)) mulliganSuccessCount++;
      }

      if (!best) {
        poorCount++;
        simLog.push({
          initialHand:  initialHand.map(c => ({ name: c.name, type: c.type_name, isShadowEvent: c.type_name === 'Event' && !!c.shadow, isRestricted: cardInSet(c, restrictedSet) })),
          mulliganHand: mulliganHand ? mulliganHand.map(c => ({ name: c.name, type: c.type_name, isShadowEvent: c.type_name === 'Event' && !!c.shadow, isRestricted: cardInSet(c, restrictedSet) })) : null,
          setup: null, total: 0,
        });
        continue;
      }

      if (isPoorSetup(best.cards)) poorCount++;
      goldDist[best.total]++;
      cardsDist[best.cards.length]++;
      totalGold  += best.total;
      totalCards += best.cards.length;
      const distinctChars = best.cards.filter(c => c.type_name === 'Character' && !c.isDuplicate && !c.inShadow);
      charsDist[distinctChars.length]++;
      const totalStr = distinctChars.reduce((sum, c) => sum + (c.strength || 0), 0);
      if (totalStr < strengthDist.length) strengthDist[totalStr]++;
      keyCardsDist[best.cards.filter(c => cardInSet(c, keySet)).length]++;
      avoidableDist[best.cards.filter(c => cardInSet(c, avoidSet)).length]++;
      shadowDist[best.cards.filter(c => c.inShadow).length]++;
      iconSetupCount++;
      for (const c of distinctChars) {
        const str = c.strength || 0;
        if (c.is_military) { iconTotals[0]++; iconStrengthTotals[0] += str; }
        if (c.is_intrigue) { iconTotals[1]++; iconStrengthTotals[1] += str; }
        if (c.is_power)    { iconTotals[2]++; iconStrengthTotals[2] += str; }
      }
      if (best.cards.some(c => c.is_economy)) economyCount++;
      if (best.cards.some(c => c.is_limited)) limitedCount++;
      simLog.push({
        initialHand:  initialHand.map(c => ({ name: c.name, type: c.type_name, isShadowEvent: c.type_name === 'Event' && !!c.shadow, isRestricted: cardInSet(c, restrictedSet) })),
        mulliganHand: mulliganHand ? mulliganHand.map(c => ({ name: c.name, type: c.type_name, isShadowEvent: c.type_name === 'Event' && !!c.shadow, isRestricted: cardInSet(c, restrictedSet) })) : null,
        setup:    best ? best.cards.map(c => ({
          name:        c.name,
          cost:        c.effectiveCost,
          isDup:       c.isDuplicate,
          isLimited:   c.is_limited,
          isEconomy:   c.is_economy && !c.is_limited,
          isKey:       cardInSet(c, keySet),
          isAvoidable: cardInSet(c, avoidSet),
          inShadow:    !!c.inShadow,
        })) : null,
        total:    best ? best.total : 0,
        poor:     !best || isPoorSetup(best.cards),
      });
    }

    const iconAvg         = iconTotals.map(t => iconSetupCount > 0 ? t / iconSetupCount : 0);
    const iconStrengthAvg = iconStrengthTotals.map(t => iconSetupCount > 0 ? t / iconSetupCount : 0);
    const avgGold  = (totalGold  / SIM_COUNT).toFixed(2);
    const avgCards = (totalCards / SIM_COUNT).toFixed(2);
    renderStats({
      poorCount, economyCount, limitedCount,
      mulliganCount, mulliganSuccessCount,
      avgGold, avgCards,
      goldDist, cardsDist, charsDist, strengthDist,
      keyCardsDist, avoidableDist, shadowDist,
      iconAvg, iconTotals, iconStrengthAvg, iconStrengthTotals,
      simLog,
    });

    document.getElementById('setup-section').scrollIntoView({ behavior: 'smooth' });
    btn.disabled = false;
    setDeckLoadLocked(false);
  }, 0);
}
