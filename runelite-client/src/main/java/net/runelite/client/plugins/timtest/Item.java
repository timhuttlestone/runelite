package net.runelite.client.plugins.timtest;

import static java.lang.Math.min;
import static org.apache.commons.lang3.math.NumberUtils.max;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Item {
  private int id;
  private String name;
  private boolean members;
  private int limit;
  private int value;
  private int highAlch;
  private int high;
  private int highTime;
  private int low;
  private int lowTime;
  private int volume;
  private int alreadyBought;

  private int calculateTax() {
    return Math.min((int) (0.01 * high), 5000000);
  }

  private int remainingLimit() {
    return limit - alreadyBought;
  }

  public int calculatePotentialProfit(int coins) {
    int canBuy = Math.min(coins / low, remainingLimit());

    return (high - calculateTax() - low) * canBuy;
  }
}
