package net.runelite.client.plugins.flippingutilities.utilities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WikiRequestWrapper {
    WikiRequest wikiRequest;
    WikiDataSource wikiDataSource;
}
