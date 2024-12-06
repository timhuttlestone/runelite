package net.runelite.client.plugins.timtest;

import java.util.Map;
import lombok.Data;

@Data
public class VolumesDto {

  private int timestamp;
  private Map<String, Integer> data;

}
