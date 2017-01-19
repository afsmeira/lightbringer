package pt.afsmeira.lightbringer.setup

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

case class Settings(
  meta: MetaSettings,
  setup: SetupSettings,
  mulligan: MulliganSettings
)

case class MetaSettings(
  economyCards: Set[String]
)

case class SetupSettings(
  requireTwoCharacters: Boolean,
  requireFourCostCharacter: Boolean,
  requireEconomy: Boolean,
  minCardsRequired: Int,
  keyCards: Set[String]
)

case class MulliganSettings(
  onPoorSetup: Boolean,
  onNoKeyCard: Boolean,
  onNoEconomy: Boolean
)

object Settings {
  def fromFile(file: String): Settings = fromConfig(ConfigFactory.parseResources(file))

  // TODO: If config key not found, assume defaults
  def fromConfig(config: Config): Settings = {
    val metaConfig = config.getConfig("meta")
    val metaSettings = MetaSettings(
      metaConfig.getStringList("economy-cards").asScala.toSet
    )

    val setupConfig = config.getConfig("setup")
    val setupSettings = SetupSettings(
      setupConfig.getBoolean("require-two-characters"),
      setupConfig.getBoolean("require-four-cost-character"),
      setupConfig.getBoolean("require-economy"),
      setupConfig.getInt("min-cards-required"),
      setupConfig.getStringList("key-cards").asScala.toSet
    )

    val mulliganConfig = config.getConfig("mulligan")
    val mulliganSettings = MulliganSettings(
      mulliganConfig.getBoolean("on-poor-setup"),
      mulliganConfig.getBoolean("on-no-key-card"),
      mulliganConfig.getBoolean("on-no-economy")
    )

    Settings(metaSettings, setupSettings, mulliganSettings)
  }
}
