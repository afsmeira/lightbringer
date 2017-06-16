# Lightbringer

[Lightbringer](https://thronesdb.com/card/01058) is a deck analysis tool for A Game of Thrones LCG 2nd Edition.

It helps you making the right decisions for your deck by providing insights on several statistics such as cost, strength
and icon distribution. Furthermore you can assess the viability of your deck using Ligthbringer's setup analysis.


## Deck Statistics

Lightbringer provides the deck's distribution of:

* Factions
* Icons
* Character strength
* Character, attachment, location, event and total costs
* Plot income, initiative, claim and reserve


## Setup statistics

Lightbringer provides the following setup statistics:

* Distribution of gold, key cards, avoidable card, total cards, distinct characters used and total character strength
* Number of poor setups and setups with economy or limited cards
* Average number of characters per icon and average icon strength
* Cards used and percentage of times used
* All setups and original hand


## Configuration

Lightbringer comes with a default configuration file, but you might want to change it to better analyze your deck, so
here is the configuration key:

* `meta.economy-cards`: a list of card names, or codes, for cards that provide economy. Lightbringer can automatically
pick up cards that provide income, but some cards, such as reducers, need to be included in this list. This list will be
updated with each chapter pack release, if necessary.
* `meta.setup-runs`: the number of setups that will be analyzed and generated.
* `setup.require-two-characters`: whether a setup requires two characters to not be considered poor.
* `setup.require-four-cost-character`: whether a setup requires a four cost character to not be considered poor.
* `setup.require-economy`: whether a setup requires an economy card to not be considered poor.
* `setup.require-key-card`: whether a setup requires a key card to not be considered poor.
* `setup.min-cards-required`: the minimum number of cards required for a setup to not be considered poor.
* `setup.key-cards`: the list of card names, or codes, for cards that are key to the deck being analyzed.
* `setup.avoidable-cards`: the list of card names, or codes, for cards with enter play reactions that can't trigger in
setup. All cards with the Bestow keyword are automatically considered avoidable cards.


## Run it

Currently, Lightbringer only supports analyzing decks from [thronesdb](http://thronesdb.com).

### Arguments

Lightbringer supports two option input arguments, and one mandatory:

    -o, --out <file>   Output Lightbringer analysis to a file
    -s, --setup-hands  Include each setup hand in output
    deckId             Identifier of the deck to analyse


### Run it with `sbt`
To analyze a given deck, just use its id as an argument for Lightbringer:

    sbt "run -o results.txt -s 8787"

### Run it with the jar
To analyze a given deck, just use its id as an argument for Lightbringer:

    java -jar lightbringer.jar -o results.txt -s 8787


## Analysis example

Please check the [example results file](example-results.txt) for an example of an analysis.
