package net.runelite.client.plugins.timtest;

import static net.runelite.api.ItemID.COINS_995;

import com.google.gson.Gson;
import com.google.inject.Provides;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.WorldType;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
        name = "TimTest"
)
public class TimTestPlugin extends Plugin {

    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private Client client;
    @Inject
    private TimTestConfig config;
    @Inject
    public OkHttpClient okHttpClient;
    @Inject
    public Gson gson;
    @Inject
    private ItemManager itemManager;

    private NavigationButton navigationButton;
    private TimTestPanel timTestPanel;

    private List<Item> latestItemPrices;
    private List<InventoryItem> inventoryItems;
    private boolean isMembers;
    private int grandExchangeSlots;
    private GameState gameState;

    @Override
    protected void startUp() throws Exception {
        log.info("Tim Test started!");

        timTestPanel = new TimTestPanel();
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(),
                "/net/runelite/client/plugins/timtest/logo.png");
        navigationButton = NavigationButton.builder()
                .tooltip("TimTest")
                .icon(icon)
                .panel(timTestPanel)
                .build();

        clientToolbar.addNavigation(navigationButton);
    }

    private void updateLatestItemPrices() throws IOException {
        Request latestRequest = new Builder()
                .url("https://prices.runescape.wiki/api/v1/osrs/latest")
                .addHeader("User-Agent", "flipper-plugin - timhuttlestone@gmail.com")
                .get()
                .build();
        Response latestResponse = okHttpClient.newCall(latestRequest).execute();
        LatestDto latestDto = gson.fromJson(latestResponse.body().string(), LatestDto.class);

        Request volumesRequest = new Builder()
                .url("https://prices.runescape.wiki/api/v1/osrs/volumes")
                .addHeader("User-Agent", "flipper-plugin - timhuttlestone@gmail.com")
                .get()
                .build();
        Response volumesResponse = okHttpClient.newCall(volumesRequest).execute();
        VolumesDto volumesDto = gson.fromJson(volumesResponse.body().string(), VolumesDto.class);

        latestItemPrices = new ArrayList<>();
        latestDto.getData().forEach((key, latestDtoItem) -> {
            int id = Integer.parseInt(key);
            ItemComposition itemComposition = client.getItemDefinition(id);
            int high = latestDtoItem.getHigh();
            int low = latestDtoItem.getLow();
            int highTime = latestDtoItem.getHighTime();
            int lowTime = latestDtoItem.getLowTime();
            if (high < low) {
                int tmp = high;
                high = low;
                low = tmp;
                tmp = highTime;
                highTime = lowTime;
                lowTime = tmp;
            }
            ItemStats itemStats = itemManager.getItemStats(id);
            Integer volume = volumesDto.getData().get(key);
            latestItemPrices.add(Item.builder()
                    .id(itemComposition.getId())
                    .name(itemComposition.getName())
                    .members(itemComposition.isMembers())
                    .value(itemComposition.getPrice())
                    .limit(itemStats != null ? itemStats.getGeLimit() : 0)
                    .highAlch(itemComposition.getHaPrice())
                    .low(low)
                    .lowTime(lowTime)
                    .high(high)
                    .highTime(highTime)
                    .volume(volume != null ? volume : 0)
                    .build());
        });
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Tim Test stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws IOException {
        log.info("New gamestate: {}", gameStateChanged.getGameState());
        gameState = gameStateChanged.getGameState();
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            isMembers = client.getWorldType().contains(WorldType.MEMBERS);
            grandExchangeSlots = isMembers ? 8 : 3;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) throws IOException {
        if (event.getContainerId() != InventoryID.INVENTORY.getId() || !gameState.equals(GameState.LOGGED_IN)) {
            return;
        }
        updateInventory();
        updateLatestItemPrices();
        sortLatestItemPrices();
    }

    private void sortLatestItemPrices() {
        int coins;
        Optional<InventoryItem> coinOptional = inventoryItems.stream().filter(i -> i.getId() == COINS_995).findFirst();
        coins = coinOptional.map(InventoryItem::getQuantity).orElse(0);
        if (coins == 0) {
            return;
        }

        latestItemPrices = latestItemPrices.stream().filter(i ->
                        (!i.isMembers() || isMembers)
                                && i.getVolume() > config.volume())
                .collect(Collectors.toList());

        latestItemPrices.sort((a, b) -> {
            int difference = b.calculatePotentialProfit(coins) - a.calculatePotentialProfit(coins);
            return Integer.compare(difference, 0);
        });

    }

    private void updateInventory() {
        ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
        inventoryItems = new ArrayList<>();
        if (itemContainer == null) {
            return;
        }
        for (net.runelite.api.Item runeListApiItem : itemContainer.getItems()) {
            if (runeListApiItem.getId() == -1) {
                continue;
            }
            ItemComposition original = client.getItemDefinition(runeListApiItem.getId());
            ItemComposition nonNotedItemComposition = (original.getNote() == -1) ? original :
                    client.getItemDefinition(original.getLinkedNoteId());
            inventoryItems.stream().filter(i -> i.getId() == nonNotedItemComposition.getId()).findFirst()
                    .ifPresentOrElse(
                            inventoryItem -> inventoryItem.addQuantity(runeListApiItem.getQuantity()),
                            () -> inventoryItems.add(InventoryItem.builder()
                                    .id(nonNotedItemComposition.getId())
                                    .name(nonNotedItemComposition.getName())
                                    .quantity(runeListApiItem.getQuantity())
                                    .build()));
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {

    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged grandExchangeOfferChanged) {
        if (!gameState.equals(GameState.LOGGED_IN)) {
            return;
        }
        log.info("onGrandExchangeOfferChanged: {}", grandExchangeOfferChanged.getSlot());
        log.info("Item ID: {}", grandExchangeOfferChanged.getOffer().getItemId());
        log.info("Price: {}", grandExchangeOfferChanged.getOffer().getPrice());
        log.info("Spent: {}", grandExchangeOfferChanged.getOffer().getSpent());
        log.info("Quantity Sold: {}", grandExchangeOfferChanged.getOffer().getQuantitySold());
        log.info("Total Quantity: {}", grandExchangeOfferChanged.getOffer().getTotalQuantity());
        log.info("State: {}", grandExchangeOfferChanged.getOffer().getState());
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
        String name = "Unkown";
        switch (widgetLoaded.getGroupId()) {
            case InterfaceID.INVENTORY:
                name = "Inventory";
                break;
            case InterfaceID.BANK:
                name = "Bank";
                break;
            case InterfaceID.BANK_INVENTORY:
                name = "Bank Inventory";
                break;
            case InterfaceID.GRAND_EXCHANGE:
                name = "Grand Exchange";
                break;
            case InterfaceID.GRAND_EXCHANGE_INVENTORY:
                name = "Grand Exchange Inventory";
                break;
        }
        log.info("onWidgetLoaded");
        log.info("Group ID: {}", widgetLoaded.getGroupId());
        log.info("Name: {}", name);
        log.info("To string: {}", widgetLoaded);

    }

    @Provides
    TimTestConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TimTestConfig.class);
    }
}
