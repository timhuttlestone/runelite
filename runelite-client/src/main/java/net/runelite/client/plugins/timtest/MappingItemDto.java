package net.runelite.client.plugins.timtest;

import lombok.Data;

@Data
public class MappingItemDto {

  private int id;
  private String name;
  private String examine;
  private boolean members;
  private int value;
  private int limit;
  private int lowalch;
  private int highalch;
}
