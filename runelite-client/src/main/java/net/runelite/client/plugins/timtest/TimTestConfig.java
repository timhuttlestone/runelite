package net.runelite.client.plugins.timtest;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("timtest")
public interface TimTestConfig extends Config {

  @ConfigItem(
      keyName = "volume",
      name = "Volume Lower Bound",
      description = "Lower bound for volumes"
  )
  default int volume() {
    return 500000;
  }

}
