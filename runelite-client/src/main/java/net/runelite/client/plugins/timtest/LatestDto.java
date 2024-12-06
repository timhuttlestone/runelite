package net.runelite.client.plugins.timtest;

import java.util.HashMap;
import lombok.Data;

@Data
public class LatestDto {

  private HashMap<String, LatestDtoItem> data;

  @Data
  public static class LatestDtoItem {
    private int low;
    private int lowTime;
    private int high;
    private int highTime;
  }
}
