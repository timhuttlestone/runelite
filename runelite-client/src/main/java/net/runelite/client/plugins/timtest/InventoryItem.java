package net.runelite.client.plugins.timtest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryItem {
  private int id;
  private String name;
  private int quantity;

  public void addQuantity(int quantity) {
    this.quantity += quantity;
  }
}
