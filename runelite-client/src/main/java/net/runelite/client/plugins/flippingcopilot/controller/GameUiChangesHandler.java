package net.runelite.client.plugins.flippingcopilot.controller;

import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;

import net.runelite.client.plugins.flippingcopilot.model.Suggestion;
import net.runelite.client.plugins.flippingcopilot.ui.OfferEditor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;

@Slf4j
public class GameUiChangesHandler {
    private static final int GE_HISTORY_TAB_WIDGET_ID = 149;
    private final FlippingCopilotPlugin plugin;
    boolean quantityOrPriceChatboxOpen;
    boolean itemSearchChatboxOpen = false;
    @Getter
    boolean offerJustPlaced = false;
    GameUiChangesHandler(FlippingCopilotPlugin plugin) {
        this.plugin = plugin;
    }

    public void onVarClientIntChanged(VarClientIntChanged event) {
        Client client = plugin.getClient();

        if (event.getIndex() == VarClientInt.INPUT_TYPE
                && client.getVarcIntValue(VarClientInt.INPUT_TYPE) == 14
                && client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS) != null) {
            itemSearchChatboxOpen = true;
            plugin.getClientThread().invokeLater(plugin.gePreviousSearch::showSuggestedItemInSearch);
        }

        if (quantityOrPriceChatboxOpen
                && event.getIndex() == VarClientInt.INPUT_TYPE
                && client.getVarcIntValue(VarClientInt.INPUT_TYPE) == 0
        ) {
            quantityOrPriceChatboxOpen = false;
            return;
        }

        if (itemSearchChatboxOpen
                && event.getIndex() == VarClientInt.INPUT_TYPE
                && client.getVarcIntValue(VarClientInt.INPUT_TYPE) == 0
        ) {
            plugin.getClientThread().invokeLater(plugin.highlightController::redraw);
            itemSearchChatboxOpen = false;
            return;
        }

        //Check that it was the chat input that got enabled.
        if (event.getIndex() != VarClientInt.INPUT_TYPE
                || client.getWidget(ComponentID.CHATBOX_TITLE) == null
                || client.getVarcIntValue(VarClientInt.INPUT_TYPE) != 7
                || client.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER) == null) {
            return;
        }
        quantityOrPriceChatboxOpen = true;

        plugin.getClientThread().invokeLater(() ->
        {
            OfferEditor flippingWidget = new OfferEditor(client.getWidget(ComponentID.CHATBOX_CONTAINER), plugin);
            Suggestion suggestion = plugin.suggestionHandler.getCurrentSuggestion();
            flippingWidget.showSuggestion(suggestion);
        });
    }

    public void onVarClientStrChanged(VarClientStrChanged event) {
        if (event.getIndex() == VarClientStr.INPUT_TEXT && itemSearchChatboxOpen) {
            plugin.getClientThread().invokeLater(plugin.highlightController::redraw);
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == 383
                || event.getGroupId() == InterfaceID.GRAND_EXCHANGE
                || event.getGroupId() == 213
                || event.getGroupId() == GE_HISTORY_TAB_WIDGET_ID) {
            plugin.getClientThread().invokeLater(plugin.highlightController::redraw);
        }
    }

    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.GRAND_EXCHANGE) {
            plugin.getClientThread().invokeLater(plugin.highlightController::removeAll);
        }
    }

    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() == 375
                || event.getVarpId() == CURRENT_GE_ITEM
                || event.getVarbitId() == 4396
                || event.getVarbitId() == 4398
                || event.getVarbitId() == 4439) {
            plugin.getClientThread().invokeLater(plugin.highlightController::redraw);
        }

        if (event.getVarpId() == CURRENT_GE_ITEM) {
            plugin.getClientThread().invokeLater(() -> plugin.offerHandler.fetchSlotItemPrice(event.getValue() > -1));
        }
    }

    public void handleMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("Confirm") && plugin.grandExchange.isSlotOpen()) {
            offerJustPlaced = true;
        }
    }
}
