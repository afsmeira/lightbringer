# Lightbringer

A setup analyzer for *A Game of Thrones: The Card Game — 2nd Edition*.

Paste a [ThronesDB](https://thronesdb.com) deck URL and Lightbringer simulates **5000 opening hands**, evaluating every valid setup for each hand and picking the best one according to configurable ranking criteria.

## Features

- **Setup statistics** — how often a deck produces poor setups, how often it mulligans, average gold and card count across all simulated hands.
- **Distribution charts** — histograms for total cards, gold spent, distinct characters, character strength, key cards, avoidable cards, cards in shadow, and icon spread.
- **Configurable poor-setup criteria** — mark a setup as poor if it lacks characters, economy cards, key cards, or falls below a minimum card count.
- **Card marking** — tag individual cards as economy, key, avoidable, or restricted directly in the deck table. The simulation uses these tags when evaluating setups.
- **Configurable ranking criteria** — drag to reorder the criteria used to pick the best setup from a given hand (card count, key cards, virtual gold, economy, avoidable cards, character strength, shadow cards, and more).
- **Simulation log** — an annotated sample of up to 100 simulated hands showing the initial draw, mulligan draw (if taken), and the chosen setup.
- **Special rule support** — handles limited cards, shadow cards, attachment targeting restrictions, and *The House With the Red Door* agenda.
- **Dark mode** — automatically follows the OS preference using the Solarized color scheme.

## Usage

Open [the app](https://afsmeira.github.io/lightbringer), paste a ThronesDB decklist URL, and click **Load**. Once the deck is loaded, configure the criteria to your liking and click **Analyze**.

## Running locally

No build step required. Clone the repository and open `index.html` in a browser:

```bash
git clone https://github.com/afsmeira/lightbringer.git
cd lightbringer
open index.html
```

The app fetches card data from the [ThronesDB public API](https://thronesdb.com/api/public/cards), so an internet connection is required on first load.

## Contributing

Found a bug or have a suggestion? [Open an issue](https://github.com/afsmeira/lightbringer/issues/new).

## Disclaimer

This project is not affiliated with Fantasy Flight Publishing, Inc.
