package net.runelite.client.plugins.flippingcopilot.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class Transaction {
    private OfferStatus type;
    private int itemId;
    private int price;
    private int quantity;
    private int boxId;
    private int amountSpent;
    private Instant timestamp;

    public boolean equals(Transaction other) {
        return this.type == other.type &&
                this.itemId == other.itemId &&
                this.price == other.price &&
                this.quantity == other.quantity &&
                this.boxId == other.boxId &&
                this.amountSpent == other.amountSpent;
    }
}
