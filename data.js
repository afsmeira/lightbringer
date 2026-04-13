const CARDS_API    = 'https://thronesdb.com/api/public/cards';
const DECKLIST_API = id => `https://thronesdb.com/api/public/decklist/${id}`;

/** Card types eligible for setup. */
const SETUP_TYPES = new Set(['Character', 'Location', 'Attachment']);

/** Card types that belong to the plot deck rather than the main deck. */
const NON_DECK_TYPES = new Set(['Agenda', 'Plot']);

/** Maximum gold available during a normal setup. */
const SETUP_GOLD = 8;

/** Display order for card types in the deck table. */
const CARD_TYPE_ORDER = { Character: 0, Attachment: 1, Location: 2, Event: 3 };

// ── Faction metadata ───────────────────────────────────────────────────────

/** Canonical display order for factions in charts. */
const FACTION_ORDER = [
  'stark', 'lannister', 'targaryen', 'baratheon',
  'greyjoy', 'tyrell', 'thenightswatch', 'martell', 'neutral',
];

/** CSS color for each faction, used for icons and labels. */
const FACTION_COLORS = {
  stark:          '#cfcfcf',
  lannister:      '#c00106',
  targaryen:      '#9c6b9e',
  baratheon:      '#e3d852',
  greyjoy:        '#1d7a99',
  tyrell:         '#509f16',
  thenightswatch: '#7a7a7a',
  martell:        '#e89521',
  neutral:        '#a99560',
};

/** Human-readable name for each faction. */
const FACTION_NAMES = {
  stark:          "House Stark",
  lannister:      "House Lannister",
  targaryen:      "House Targaryen",
  baratheon:      "House Baratheon",
  greyjoy:        "House Greyjoy",
  tyrell:         "House Tyrell",
  thenightswatch: "The Night's Watch",
  martell:        "House Martell",
  neutral:        "Neutral",
};

/** Unicode code points in the ThronesDB icon font for each faction. */
const FACTION_ICONS = {
  stark:          '\ue608',
  lannister:      '\ue603',
  targaryen:      '\ue609',
  baratheon:      '\ue600',
  greyjoy:        '\ue601',
  tyrell:         '\ue60a',
  thenightswatch: '\ue606',
  martell:        '\ue604',
  neutral:        '\ue612',
};

// ── Mutable deck state ─────────────────────────────────────────────────────

/** All ThronesDB cards keyed by code; populated on first API fetch. */
let cachedCards    = null;

/** Slot map for the active deck: { code: qty, … }. */
let currentSlots   = null;

/** Card map for the active deck. */
let currentCardMap = null;

/** True when the active deck contains agenda 08039 (The House With the Red Door). */
let hasRedDoor     = false;

// ── Known card sets ────────────────────────────────────────────────────────

/** Cards that provide economy benefits but aren't limited or don't provide income. */
const knownEconomyCards = new Set([
  {"id":"01056", "name":"Dragonstone Faithful"},
  {"id":"01074", "name":"Iron Islands Fishmonger"},
  {"id":"01094", "name":"Lannisport Merchant"},
  {"id":"01110", "name":"Desert Scavenger"},
  {"id":"01133", "name":"Steward at the Wall"},
  {"id":"01152", "name":"Winterfell Steward"},
  {"id":"01170", "name":"Targaryen Loyalist"},
  {"id":"01188", "name":"Garden Caretaker"},
  {"id":"02007", "name":"Renly Baratheon"},
  {"id":"02105", "name":"Sworn Brother"},
  {"id":"03015", "name":"House Tully Septon"}, // Maybe? It has an extraneous cost/condition
  {"id":"03042", "name":"Tourney Grounds"},
  {"id":"04017", "name":"Tower of the Sun"}, // Maybe? It has an extraneous cost/condition
  {"id":"04021", "name":"Donella Hornwood"},
  {"id":"04028", "name":"The Stone Drum"}, // Maybe? It has an extraneous cost/condition
  {"id":"04042", "name":"Bear Island"},
  {"id":"04057", "name":"Hot Pie"},
  {"id":"05019", "name":"Lannisport Treasury"},
  {"id":"06005", "name":"Eastwatch Carpenter"}, // Maybe? It has an extraneous cost/condition
  {"id":"06043", "name":"Elinor Tyrell"},
  {"id":"07016", "name":"Builder at the Wall"},
  {"id":"10014", "name":"Maester Kedry"}, // Maybe? It has an extraneous cost/condition
  {"id":"11017", "name":"The Shadow City"},
  {"id":"11033", "name":"Hizdahr zo Loraq"},
  {"id":"13077", "name":"Advisor to the Crown"},
  {"id":"14004", "name":"Renly Baratheon"},
  {"id":"14008", "name":"Selyse Baratheon"},
  {"id":"14029", "name":"Prince's Attendant"},
  {"id":"17101", "name":"Selyse Baratheon"},
  {"id":"17130", "name":"Hizdahr zo Loraq"},
  {"id":"21004", "name":"The Iron Fleet"},
  {"id":"21023", "name":"Longtable"},
  {"id":"21027", "name":"Squire"},
  {"id":"22008", "name":"Casterly Rock"},
  {"id":"23030", "name":"Gulltown Merchant"},
  {"id":"26034", "name":"Queen of Meereen"},
  {"id":"26111", "name":"Howland Reed"},

]);

/** Cards that are undesirable to have in setup (e.g. Bestow characters, conditional effects). */
const knownAvoidableCards = new Set([
  {"id":"01028", "name":"Littlefinger"},
  {"id":"01029", "name":"Varys"},
  {"id":"01047", "name":"Melisandre"},
  {"id":"01047", "name":"Vanguard Lancer"},
  {"id":"01095", "name":"Queen's Assassin"},
  {"id":"01112", "name":"Greeblood Trader"},
  {"id":"01129", "name":"Yoren"},
  {"id":"01141", "name":"Arya Stark"},
  {"id":"01148", "name":"Summer"},
  {"id":"01189", "name":"Olenna's Informant"},
  {"id":"02023", "name":"Lady-in-Waiting"},
  {"id":"02043", "name":"Ser Hobber Redwyne"},
  {"id":"02051", "name":"Newly-Made Lord"},
  {"id":"02086", "name":"Northern Rookery"},
  {"id":"03009", "name":"Riverrun Minstrel"},
  {"id":"03037", "name":"House Florent Knight"},
  {"id":"04035", "name":"Starfall Cavalry"},
  {"id":"04036", "name":"Venomous Blade"},
  {"id":"04047", "name":"Asshai Priestess"},
  {"id":"04051", "name":"Lordsport Fisherman"},
  {"id":"04069", "name":"Tanda Stokeworth"},
  {"id":"04077", "name":"Jaqen H'ghar"},
  {"id":"04089", "name":"Joffrey Baratheon"},
  {"id":"04109", "name":"Podrick Payne"},
  {"id":"04113", "name":"Qaithe of the Shadow"},
  {"id":"04115", "name":"Ser Arys Oakheart"},
  {"id":"05003", "name":"Ser Kevan Lannister"},
  {"id":"05009", "name":"Shagga Son of Dolf"},
  {"id":"05011", "name":"Moon Brothers"},
  {"id":"05012", "name":"Red Keep Spy"},
  {"id":"05016", "name":"Lannisport Guard"},
  {"id":"05037", "name":"Alerie Tyrell"},
  {"id":"06115", "name":"Dornish Spy"},
  {"id":"07031", "name":"Southron Messenger"},
  {"id":"07040", "name":"Dalla"},
  {"id":"08008", "name":"Queen's Men"},
  {"id":"08016", "name":"Dorea Sand"},
  {"id":"08029", "name":"Black Ears"},
  {"id":"08041", "name":"Ramsay Snow"},
  {"id":"08063", "name":"Hightower Spy"},
  {"id":"08067", "name":"Selyse Baratheon"},
  {"id":"08085", "name":"Coldhands"},
  {"id":"09011", "name":"Emissary of the Hightower"},
  {"id":"10019", "name":"Hotah's Axe"},
  {"id":"11041", "name":"Umber Loyalist"},
  {"id":"11051", "name":"Drowned God Fanatic"},
  {"id":"11081", "name":"Bear Island Scout"},
  {"id":"11091", "name":"Iron Victory's Crew"},
  {"id":"12002", "name":"Euron Crow's Eye"},
  {"id":"12014", "name":"Eager Deckhand"},
  {"id":"12040", "name":"Rogue Wildling"},
  {"id":"13085", "name":"Yoren"},
  {"id":"13096", "name":"The Mountain's Skull"},
  {"id":"14005", "name":"Salladhor Saan"},
  {"id":"14007", "name":"Alester Florent"},
  {"id":"14010", "name":"Dale Seaworth"},
  {"id":"14012", "name":"Red Priest"},
  {"id":"14015", "name":"Bastard of Robert"},
  {"id":"14033", "name":"She-Bear"},
  {"id":"15006", "name":"Galazza Galare"},
  {"id":"15025", "name":"Delena Florent"},
  {"id":"15041", "name":"Archmaester Marwyn"},
  {"id":"16005", "name":"Spider's Whisperer"},
  {"id":"17103", "name":"Euron Crow's Eye"},
  {"id":"17106", "name":"Drowned God Fanatic"},
  {"id":"17116", "name":"Yoren"},
  {"id":"17124", "name":"Bear Island Scout"},
  {"id":"18005", "name":"Ser Addam Marbrand"},
  {"id":"18010", "name":"Leathers"},
  {"id":"18015", "name":"Orton Merryweather"},
  {"id":"19017", "name":"Gulltown City Guard"},
  {"id":"20012", "name":"Farlen"},
  {"id":"20036", "name":"Mina Redwyne"},
  {"id":"20047", "name":"Sweetsleep"},
  {"id":"21010", "name":"Lord Ander's Host"},
  {"id":"21013", "name":"Great Ranging"},
  {"id":"22022", "name":"Alla Tyrel"},
  {"id":"23009", "name":"Craster's Keep Mutineer"},
  {"id":"24023", "name":"Oldtown City Watch"},
  {"id":"25013", "name":"Free Brothers"},
  {"id":"25081", "name":"Blackhaven Rider"},
  {"id":"25105", "name":"Tywin's Whisperer"},
  {"id":"26005", "name":"Ashemark Councilor"},
  {"id":"26027", "name":"Ghost Hill Elite"},
  {"id":"26049", "name":"Northern Envoy"},
  {"id":"26067", "name":"Southron Informant"},
]);

/** Cards that are illegal or impractical during setup (never have valid targets, etc.). */
const knownRestrictedCards = new Set([
  {"id":"01035", "name":"Milk of the Poppy"},
  {"id":"02006", "name":"Pleasure Barge"},
  {"id":"02034", "name":"Crown of Gold"},
  {"id":"02102", "name":"Ward"},
  {"id":"03021", "name":"Frozen Solid"},
  {"id":"04026", "name":"Craven"},
  {"id":"04098", "name":"The Frostfangs"},
  {"id":"06022", "name":"Marriage Pact"},
  {"id":"06116", "name":"Locked Away"},
  {"id":"07032", "name":"Lingering Venom"},
  {"id":"08078", "name":"Seized by the Guard"},
  {"id":"09043", "name":"Strangler"},
  {"id":"10021", "name":"Beguiled"},
  {"id":"10032", "name":"Turncloak"},
  {"id":"10032", "name":"Reckless"},
  {"id":"14021", "name":"Hunting Accident"},
  {"id":"16002", "name":"Melisandre's Favor"},
  {"id":"16004", "name":"Conquer"},
  {"id":"16022", "name":"Sky Cell"},
  {"id":"17126", "name":"Ward"},
  {"id":"17134", "name":"Crown of Gold"},
  {"id":"25030", "name":"Insubordination"},
  {"id":"25059", "name":"Greyscale"},
  {"id":"25062", "name":"Martial Law"},
  {"id":"26094", "name":"Poisoned Locusts"}, // Should be caught by attachment restrictions during setup simulation
  {"id":"26113", "name":"War Elephants"},
]);

/**
 * Attachments that have placement restrictions beyond the default "attach to a character".
 *
 * Each entry may include:
 *   predicate        — logical expression defining which fields to evaluate
 *   traits           — card must have at least one of these traits
 *   excluded_traits  — card must not have any of these traits
 *   factions         — card must belong to one of these factions
 *   opponent         — if true, can only target opponent cards (never valid during own setup)
 *   title            — card must have exactly this name
 *   cost             — card cost must be in this list
 *   unique           — card uniqueness must match this value
 *   limited          — card limited-status must match this value
 *   location         — if true, attaches to a location rather than a character
 *   loyal            — card loyalty must match this value
 *   shadow           — card shadow-status must match this value
 *   limit            — maximum copies attachable to the same card (≥1 always satisfied in setup)
 */
const attachmentWithRestrictions = new Set([
  {"id":"01032", "name":"Seal of the Hand", "traits": ["lord", "lady"], "predicate": "traits"},
  {"id":"01033", "name":"Bodyguard", "traits": ["lord", "lady"], "predicate": "traits"},
  {"id":"01058", "name":"Lightbringer", "factions": ["baratheon"], "predicate": "factions"},
  {"id":"01077", "name":"Throwing Axe", "traits": ["ironborn"], "predicate": "traits"},
  {"id":"01135", "name":"Longclaw", "factions": ["the night's watch"], "predicate": "factions"},
  {"id":"01153", "name":"Ice", "factions": ["stark"], "predicate": "factions"},
  {"id":"01172", "name":"Drogo's Arakh", "traits": ["dothraki"], "predicate": "traits"},
  {"id":"01191", "name":"Heartsbane", "factions": ["tyrell"], "predicate": "factions"},
  {"id":"02004", "name":"Lady", "factions": ["stark"], "predicate": "factions"},
  {"id":"02044", "name":"Mare in Heat", "traits": ["dothraki"], "predicate": "traits"},
  {"id":"02046", "name":"Practice Blade", "factions": ["the night's watch"], "predicate": "factions"},
  {"id":"02052", "name":"Fishing Net", "opponent": true, "predicate": "opponent"},
  {"id":"02054", "name":"The Silver Steed", "traits": ["dothraki"], "title": "Daenerys Targaryen", "predicate": "traits||title"}, // OR
  {"id":"02055", "name":"Attainted", "opponent": true, "predicate": "opponent"},
  {"id":"02071", "name":"Paid Off", "opponent": true, "predicate": "opponent"},
  {"id":"02077", "name":"Condemned", "opponent": true, "predicate": "opponent"},
  {"id":"02088", "name":"Stinking Drunk", "opponent": true, "predicate": "opponent"},
  {"id":"02102", "name":"Ward", "cost": [4, 3, 2, 1, 0], "predicate": "cost"},
  {"id":"02112", "name":"Drowned God's Blessing", "factions": ["greyjoy"], "predicate": "factions"},
  {"id":"02116", "name":"Imprisioned", "opponent": true, "predicate": "opponent"},
  {"id":"02117", "name":"Maester's Chain", "traits": ["maester"], "predicate": "traits"},
  {"id":"03019", "name":"Nymeria", "unique": true, "factions": ["stark"], "predicate": "unique&&factions"}, // AND
  {"id":"03020", "name":"Needle", "factions": ["stark"], "predicate": "factions"},
  {"id":"03021", "name":"Frozen Solid", "limited": false, "location": true, "cost": [3, 2, 1, 0], "predicate":"limited&&location&&cost"}, // AND cost lower
  {"id":"03025", "name":"Motley", "opponent": true, "predicate": "opponent"},
  {"id":"04009", "name":"Ruby of R'hllor", "traits": ["r'hllor"], "predicate": "traits"},
  {"id":"04030", "name":"The Boy King", "traits": ["lord"], "predicate": "traits"},
  {"id":"04034", "name":"Beggar King", "factions": ["targaryen"], "predicate": "factions"},
  {"id":"04036", "name":"Venomous Blade", "factions": ["martell"], "predicate": "factions"},
  {"id":"04044", "name":"Crown of Golden Roses", "traits": ["lord"], "predicate": "traits"},
  {"id":"04048", "name":"Visited by Shadows", "opponent": true, "predicate": "opponent"},
  {"id":"04052", "name":"King of Salt and Rock", "traits": ["ironborn"], "predicate": "traits"},
  {"id":"04062", "name":"The Wolf King", "factions": ["stark"], "predicate": "factions"},
  {"id":"04079", "name":"King Beyond the Wall", "traits": ["wildling"], "predicate": "traits"},
  {"id":"04086", "name":"Dragonglass Dagger", "factions": ["the night's watch"], "predicate": "factions"},
  {"id":"04094", "name":"Mother of Dragons", "traits": ["stormborn"], "predicate": "traits"},
  {"id":"05020", "name":"Shield of Lannisport","factions": ["lannister"], "traits": ["lord", "lady"], "predicate": "factions&&traits"}, // AND or on traits
  {"id":"05026", "name":"Disputed Claim", "traits": ["bastard", "lord", "lady"], "predicate": "traits"},
  {"id":"05036", "name":"Daenery's Favor", "factions": ["targaryen"], "predicate": "factions"},
  {"id":"05043", "name":"Appointed", "unique": true, "predicate": "unique"},
  {"id":"06028", "name":"Light of the Lord", "factions": ["baratheon"], "traits": ["r'hllor"], "predicate": "factions||traits"}, // OR
  {"id":"06044", "name":"Silver Hair Net", "traits": ["lady"], "predicate": "traits"},
  {"id":"06050", "name":"Fever Dreams", "opponent": true, "predicate": "opponent"},
  {"id":"06052", "name":"Corsair's Dirk", "traits": ["ironborn"], "predicate": "traits"},
  {"id":"06066", "name":"Improved Fortifications", "location": true, "predicate": "location"},
  {"id":"06074", "name":"Breaker of Chains", "unique": true, "factions": ["targaryen"], "predicate": "unique&&factions"}, // AND
  {"id":"06106", "name":"Ranger's Bow", "factions": ["the night's watch"], "predicate": "factions"},
  {"id":"06108", "name":"King's Blood", "traits": ["bastard", "king"], "predicate": "traits"},
  {"id":"06114", "name":"Warrior's Braid", "factions": ["targaryen"], "predicate": "factions"},
  {"id":"07021", "name":"Ghost", "factions": ["the night's watch", "stark"], "predicate": "factions"},
  {"id":"07034", "name":"Summer", "unique": true, "factions": ["stark"], "predicate": "unique&&factions"}, // AND
  {"id":"07043", "name":"Weirwood Bow", "factions": ["the night's watch"], "traits": ["wildling"], "predicate": "factions||traits"}, // OR
  {"id":"08001", "name":"Archmaester's Key", "traits": ["maester"], "predicate": "traits"},
  {"id":"08009", "name":"Traitor to the Crown", "opponent": true, "predicate": "opponent"},
  {"id":"08015", "name":"Bloody Arakh", "traits": ["dothraki"], "predicate": "traits"},
  {"id":"08030", "name":"Kingslayer", "traits": ["kingsguard"], "predicate": "traits"},
  {"id":"08056", "name":"Sand Steed", "limit": 1, "predicate": "limit"},
  {"id":"08078", "name":"Seized by the Guard", "limited": false, "location": true, "predicate": "limited&&location"}, // AND
  {"id":"08090", "name":"Ser Pounce", "unique": true, "cost": [3, 2, 1, 0], "predicate": "unique&&cost"}, // AND cost lower
  {"id":"08094", "name":"Mhysa", "traits": ["lady"], "predicate": "traits"},
  {"id":"08096", "name":"Secret Pact", "opponent": true, "predicate": "opponent"}, // Not true actually
  {"id":"08108", "name":"King at the Wall", "unique": true, "factions": ["baratheon"], "predicate": "unique&&factions"}, // AND
  {"id":"08112", "name":"Driftwood Cudgel", "traits": ["drowned god"], "predicate": "traits"},
  {"id":"09020", "name":"Queen of the Seven Kingdoms", "traits": ["lady"], "predicate": "traits"},
  {"id":"09021", "name":"Tourney Lance", "traits": ["knight"], "predicate": "traits"},
  {"id":"09030", "name":"Lion's Tooth", "unique": true, "factions": ["lannister"], "predicate": "unique&&factions"}, // AND
  {"id":"09038", "name":"Tokar", "factions": ["targaryen"], "predicate": "factions"},
  {"id":"10028", "name":"Plundered", "opponent": true, "location": true, "predicate":"opponent&&location"}, // AND
  {"id":"10032", "name":"Turncloack", "loyal": false, "predicate": "loyal"},
  {"id":"10038", "name":"Beacon of the South", "factions": ["tyrell"], "predicate": "factions"},
  {"id":"10043", "name":"Brother's Robes", "traits": ["the seven"], "predicate": "traits"},
  {"id":"11046", "name":"Miner's Pick", "traits": ["builder", "steward"], "predicate": "traits"},
  {"id":"11054", "name":"Queensguard", "excluded_traits": ["lady"], "predicate": "excluded_traits"},
  {"id":"11062", "name":"Shaggydog", "factions": ["stark"], "predicate": "factions"},
  {"id":"11066", "name":"Lord Commander", "factions": ["the night's watch"], "cost": [5, 6, 7, 8, 9, 10], "predicate": "factions&&cost"}, // AND cost higher
  {"id":"11116", "name":"Blood of the Viper", "traits": ["sand snake"], "predicate": "traits"},
  {"id":"12021", "name":"Red Rain", "unique": true, "factions": ["greyjoy"], "predicate": "unique&&factions"}, // AND
  {"id":"12030", "name":"The Bloodroyal", "factions": ["martell"], "predicate": "factions"},
  {"id":"13030", "name":"Pyromancer's Cache", "location": true, "predicate": "location"},
  {"id":"13044", "name":"Unexpected Guile", "shadow": true, "predicate": "shadow"},
  {"id":"13046", "name":"Guard Duty", "factions": ["the night's watch"], "predicate": "factions"},
  {"id":"13052", "name":"Outfitted for War", "traits": ["warship"], "location": true, "predicate": "traits&&location"}, // AND
  {"id":"13074", "name":"Dragon Skull", "factions": ["targaryen"], "predicate": "factions"},
  {"id":"13098", "name":"White Cloak", "traits": ["knight"], "predicate": "traits"},
  {"id":"13108", "name":"Long May He Reign", "traits": ["king"], "predicate": "traits"},
  {"id":"14020", "name":"Azor Ahai Reborn", "unique": true, "predicate": "unique"},
  {"id":"14036", "name":"Dothraki Steed", "limit": 1, "predicate": "limit"},
  {"id":"15019", "name":"Khaleesi", "traits": ["lady"], "predicate": "traits"},
  {"id":"15034", "name":"Cage of Ravens", "traits": ["steward"], "predicate": "traits"},
  {"id":"15038", "name":"Champion's Favor", "traits": ["lady"], "predicate": "traits"},
  {"id":"15043", "name":"Water Dancer's Sword", "cost": [3, 2, 1, 0], "predicate": "cost"}, // cost lower
  {"id":"16004", "name":"Conquer", "unique": false, "location": true, "predicate": "unique&&location"}, // AND
  {"id":"17126", "name":"Ward", "unique": true, "cost": [4, 3, 2, 1, 0], "predicate": "unique&&cost"}, // AND cost lower
  {"id":"17139", "name":"Unexpected Guile", "shadow": true, "predicate": "shadow"},
  {"id":"20013", "name":"Mountain's Man", "factions": ["lannister"], "predicate": "factions"},
  {"id":"20018", "name":"Sword of the Morning", "unique": true, "factions": ["martell"], "traits": ["house dayne"], "predicate": "unique&&faction||traits"}, // AND or traits
  {"id":"20019", "name":"Elia's Lance", "traits": ["sand snake"], "predicate": "traits"},
  {"id":"20022", "name":"First Ranger", "unique": true, "predicate": "unique"},
  {"id":"20023", "name":"Lord Steward", "unique": true, "predicate": "unique"},
  {"id":"21018", "name":"Grey Wind", "unique": true, "factions": ["stark"], "predicate": "unique&&factions"}, // AND
  {"id":"21028", "name":"Magnar of Thenn", "unique": true, "traits": ["wildling"], "predicate": "unique&&traits"}, // AND
  {"id":"22009", "name":"Lady Whiskers", "unique": true, "cost": [3, 2, 1, 0], "predicate": "unique&&cost"}, // AND cost lower
  {"id":"22015", "name":"First Builder", "unique": true, "predicate": "unique"},
  {"id":"22027", "name":"The Horn of Winter", "unique": true, "predicate": "unique"},
  {"id":"23002", "name":"Prince of the Narrow Sea", "unique": true, "factions": ["baratheon"], "predicate": "unique&&factions"}, // AND
  {"id":"23006", "name":"The Warden of the West", "factions": ["lannister"], "traits": ["lord"], "predicate": "factions&&traits"}, // AND
  {"id":"23035", "name":"Lord Protector of the Vale", "traits": ["lord"], "predicate": "traits"},
  {"id":"24027", "name":"Vanguard Leader", "unique": true, "predicate": "unique"},
  {"id":"25026", "name":"Boots", "unique": true, "cost": [3, 2, 1, 0], "predicate": "unique&&cost"}, // AND cost lower
  {"id":"25042", "name":"The Laughing Storm", "unique": true, "location": true, "factions": ["baratheon"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25050", "name":"Legacy of the Watch", "unique": true, "location": true, "factions": ["the night's watch"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25052", "name":"Ice", "unique": true, "factions": ["stark"], "predicate": "unique&&factions"}, // AND
  {"id":"25056", "name":"The Warden of the South", "factions": ["tyrell"], "traits": ["lord"], "predicate": "factions&&traits"}, // AND
  {"id":"25062", "name":"Martial Law", "limited": false, "location": true, "predicate": "limited&&location"}, // AND
  {"id":"25068", "name":"Nymeria of Ny Sar", "unique": true, "location": true, "factions": ["martell"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25074", "name":"Aegon the Conqueror", "unique": true, "location": true, "factions": ["targaryen"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25084", "name":"Harren the Black", "unique": true, "location": true, "factions": ["greyjoy"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25086", "name":"Lann the Clever", "unique": true, "location": true, "factions": ["lannister"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25112", "name":"Bran the Breaker", "unique": true, "location": true, "factions": ["stark"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"25116", "name":"Garth Greenhand", "unique": true, "location": true, "factions": ["tyrell"], "predicate": "unique&&location&&factions"}, // AND
  {"id":"26002", "name":"Roast Boar", "opponent": true, "predicate": "opponent"},
  {"id":"26012", "name":"The Warden of the North", "factions": ["tyrell"], "traits": ["lord"], "predicate": "factions&&traits"}, // AND
  {"id":"16016", "name":"Highgarden Destrier", "limit": 1, "cost": [3, 2, 1, 0], "predicate": "limit&&cost"}, // AND
  {"id":"26022", "name":"The King's Justice", "unique": true, "predicate": "unique"},
  {"id":"26034", "name":"Queen of Meereen", "traits": ["lady"], "predicate": "traits"},
  {"id":"26052", "name":"Dubious Loyalties", "loyal": false, "predicate": "loyal"},
  {"id":"26058", "name":"The Warden of the East", "traits": ["house arryn"], "predicate": "traits"},
  {"id":"26064", "name":"Reaver's Greataxe", "unique": false, "factions": ["greyjoy"], "predicate": "unique&&factions"}, // AND
  {"id":"26086", "name":"Slander and Lies", "opponent": true, "predicate": "opponent"},
  {"id":"26088", "name":"The Viper's Spear", "traits": ["sand snake"], "title": "The Red Viper", "predicate": "traits||title"}, // OR
  {"id":"26094", "name":"Poisoned Locusts", "opponent": true, "predicate": "opponent"},
])

