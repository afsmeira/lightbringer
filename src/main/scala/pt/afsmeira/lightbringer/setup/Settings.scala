package pt.afsmeira.lightbringer.setup

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

case class Settings(
  meta: MetaSettings,
  setup: SetupSettings
)

/**
  * Settings for internal Lightbringer properties.
  *
  * @param economyCards The set of cards (either name or code) that provide economy.
  * @param setupRuns    The number of setup runs to be made.
  */
case class MetaSettings(
  economyCards: Set[String],
  setupRuns: Int
)

/**
  * Settings for Lightbringer's Setup Analyzer.
  *
  * @param requireTwoCharacters     Whether a setup requires two characters to not be considered poor.
  * @param requireFourCostCharacter Whether a setup requires a four cost character to not be considered poor.
  * @param requireEconomy           Whether a setup requires an economy card to not be considered poor.
  * @param requireKeyCard           Whether a setup requires a key card to not be considered poor.
  * @param minCardsRequired         The minimum number of cards required for a setup to not be considered poor.
  * @param keyCards                 The set of cards (either name or code) that are considered key.
  * @param avoidableCards           The set of cards (either name or code) that are considered avoidable.
  */
case class SetupSettings(
  requireTwoCharacters: Boolean,
  requireFourCostCharacter: Boolean,
  requireEconomy: Boolean,
  requireKeyCard: Boolean,
  minCardsRequired: Int,
  keyCards: Set[String],
  avoidableCards: Set[String]
)

object Settings {
  private val Lightbringer: Settings = fromConfig(ConfigFactory.parseResources("lightbringer.conf"))

  val Setup: SetupSettings = Lightbringer.setup
  val Meta : MetaSettings  = Lightbringer.meta

  // TODO: If config key not found, assume defaults
  private def fromConfig(config: Config): Settings = {
    val metaConfig = config.getConfig("meta")
    val metaSettings = MetaSettings(
      metaConfig.getStringList("economy-cards").asScala.toSet,
      metaConfig.getInt("setup-runs")
    )

    val setupConfig = config.getConfig("setup")
    val setupSettings = SetupSettings(
      setupConfig.getBoolean("require-two-characters"),
      setupConfig.getBoolean("require-four-cost-character"),
      setupConfig.getBoolean("require-economy"),
      setupConfig.getBoolean("require-key-card"),
      setupConfig.getInt("min-cards-required"),
      setupConfig.getStringList("key-cards").asScala.toSet,
      setupConfig.getStringList("avoidable-cards").asScala.toSet
    )

    Settings(metaSettings, setupSettings)
  }
}
