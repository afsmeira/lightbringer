package pt.afsmeira.lightbringer.setup

import com.typesafe.config.{Config, ConfigFactory}

// TODO add other settings such as income cards and key cards
case class Settings(
  setup: SetupSettings,
  mulligan: MulliganSettings
)

case class SetupSettings(
  requireTwoCharacters: Boolean,
  requireFourCostCharacter: Boolean,
  requireEconomy: Boolean,
  minCardsRequired: Int
)

case class MulliganSettings(
  onPoorSetup: Boolean,
  onNoKeyCard: Boolean,
  onNoEconomy: Boolean
)

object Settings {
  def fromFile(file: String): Settings = fromConfig(ConfigFactory.parseResources(file))

  def fromConfig(config: Config): Settings = {
    val setupConfig = config.getConfig("setup")
    val setupSettings = SetupSettings(
      setupConfig.getBoolean("require-two-characters"),
      setupConfig.getBoolean("require-four-cost-character"),
      setupConfig.getBoolean("require-economy"),
      setupConfig.getInt("min-cards-required")
    )

    val mulliganConfig = config.getConfig("mulligan")
    val mulliganSettings = MulliganSettings(
      mulliganConfig.getBoolean("on-poor-setup"),
      mulliganConfig.getBoolean("on-no-key-card"),
      mulliganConfig.getBoolean("on-no-economy")
    )

    Settings(setupSettings, mulliganSettings)
  }
}
