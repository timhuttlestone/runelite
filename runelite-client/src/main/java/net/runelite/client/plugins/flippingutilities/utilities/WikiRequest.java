package net.runelite.client.plugins.flippingutilities.utilities;

import lombok.Data;

import java.util.Map;

@Data
public class WikiRequest {
    Map<Integer, WikiItemMargins> data;
}
