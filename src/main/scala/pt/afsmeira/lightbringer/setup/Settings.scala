package pt.afsmeira.lightbringer.setup

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

case class Settings(
  meta: MetaSettings,
  setup: SetupSettings
)

case class MetaSettings(
  economyCards: Set[String],
  setupRuns: Int
)

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
