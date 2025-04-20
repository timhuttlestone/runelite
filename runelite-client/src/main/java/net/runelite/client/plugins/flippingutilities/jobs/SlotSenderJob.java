package net.runelite.client.plugins.flippingutilities.jobs;

import net.runelite.client.plugins.flippingutilities.controller.FlippingPlugin;
import net.runelite.client.plugins.flippingutilities.model.OfferEvent;
import net.runelite.client.plugins.flippingutilities.ui.widgets.SlotActivityTimer;
import net.runelite.client.plugins.flippingutilities.utilities.AccountSlotsUpdate;
import net.runelite.client.plugins.flippingutilities.utilities.SlotState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.events.GrandExchangeOfferChanged;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class SlotSenderJob {
    FlippingPlugin plugin;
    ScheduledExecutorService executor;
    OkHttpClient httpClient;
    Future slotStateSenderTask;
    AccountSlotsUpdate previouslySentSlotUpdate;
    List<Consumer<Integer>> subscribers = new ArrayList<>();
    public static int PERIOD = 10; //seconds
    public boolean justLoggedIn = false;

    public SlotSenderJob(FlippingPlugin plugin, OkHttpClient httpClient) {
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void subscribe(Consumer<Integer> subscriber) {
        subscribers.add(subscriber);
    }

    public void start() {
        slotStateSenderTask = executor.scheduleAtFixedRate(this::sendSlots, 10, PERIOD, TimeUnit.SECONDS);
        log.info("started slot sender job");
    }

    public void stop() {
        if (!slotStateSenderTask.isCancelled()) {
            slotStateSenderTask.cancel(true);
            log.info("shut down slot sender job");
        }
    }

    private void sendSlots() {
        if (!plugin.getApiAuthHandler().canCommunicateWithApi(plugin.getCurrentlyLoggedInAccount())) {
            return;
        }

        List<SlotState> currentSlotStates = this.getCurrentSlots();
        AccountSlotsUpdate accountSlotsUpdate = new AccountSlotsUpdate(plugin.getCurrentlyLoggedInAccount(), currentSlotStates);
        if (accountSlotsUpdate.equals(this.previouslySentSlotUpdate) && !justLoggedIn) {
            log.debug("no updates to slots since the last time I sent them, not sending any requests.");
            subscribers.forEach(subscriber -> subscriber.accept(0));
            return;
        }
        justLoggedIn = false;
        plugin.getApiRequestHandler().updateGeSlots(accountSlotsUpdate)
                .whenComplete((response, exception) -> {
                    if (exception != null) {
                        log.debug("could not send slot update successfully", exception);
                        subscribers.forEach(subscriber -> subscriber.accept(2));

                    } else {
                        previouslySentSlotUpdate = accountSlotsUpdate;
                        subscribers.forEach(subscriber -> subscriber.accept(1));
                        log.debug("sent slot update successfully!");

                    }
                });
    }

    /**
     * Gets the current slots by looking at both the history tracked by the plugin (lastOffers in the DataHandler) and
     * the current slots returned by client.getGrandExchangeOffers(). client.getGrandExchangeOffers is always correct.
     * The reason we don't solely rely on it is because it simply tells us the current offers in the ge and is missing
     * additional info such as when the trade for that offer was created. And the reason we can't solely rely on the
     * offers in lastOffers in DataHandler is because it may not always reflect the current state of the slot. For
     * example, if someone collected an offer on mobile and then logged into runelite, the slot is empty, but lastOffers
     * won't reflect that.
     * <p>
     * Whenever the offer in lastOffers reflects the offer actually in the slot, we prefer to use it over the
     * offer in client.getGrandExchangeOffers(). This is because the offer in lastOffers is decorated with additional
     * info such as the time the trade for the offer was created.
     * <p>
     * Whenever the offer in lastOffers does not reflect the offer actually in the slot, we use the offer from
     * client.getGrandExchangeOffers(). However, in this case, the slotState object will have its createdAt field be null
     * as the offer from client.getGrandExchangeOffers() is missing that information.
     */
    private List<SlotState> getCurrentSlots() {
        Map<Integer, OfferEvent> lastOfferEventForEachSlot = plugin.getDataHandler().getAccountData(plugin.getCurrentlyLoggedInAccount()).getLastOffers();
        List<SlotActivityTimer> slotActivityTimers = plugin.getDataHandler().getAccountData(plugin.getCurrentlyLoggedInAccount()).getSlotTimers();
        List<SlotState> slotStates = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            GrandExchangeOffer grandExchangeOffer = plugin.getClient().getGrandExchangeOffers()[i];
            //the offer event constructed from the true offer retrieved from client.getGrandExchangeOffers()
            OfferEvent trueOfferInSlot = this.getOfferEventConstructedFromClient(i);

            if (lastOfferEventForEachSlot.containsKey(i)) {
                OfferEvent lastOfferEventForSlotTrackedByPlugin = lastOfferEventForEachSlot.get(i);
                lastOfferEventForSlotTrackedByPlugin.setListedPrice(grandExchangeOffer.getPrice());
                lastOfferEventForSlotTrackedByPlugin.setSpent(grandExchangeOffer.getSpent());
                if (i < slotActivityTimers.size()) {
                    lastOfferEventForSlotTrackedByPlugin.setBeforeLogin(slotActivityTimers.get(i).offerOccurredAtUnknownTime);
                    lastOfferEventForSlotTrackedByPlugin.setTradeStartedAt(slotActivityTimers.get(i).tradeStartTime);
                }
                //when tracked offer is the same, prefer to use it as it has more info.
                if (lastOfferEventForSlotTrackedByPlugin.isDuplicate(trueOfferInSlot)) {
                    slotStates.add(SlotState.fromOfferEvent(lastOfferEventForSlotTrackedByPlugin));
                }
                //sometimes tracked offer can be incongruent with slot (collected an offer on mobile). In that case, use
                //the true offer from the client object.
                else {
                    trueOfferInSlot.setBeforeLogin(true); //we don't know when this offer came in as it wasn't tracked
                    if (trueOfferInSlot.isCausedByEmptySlot()) {
                        slotStates.add(SlotState.createEmptySlot(i));
                    }
                    else {
                        slotStates.add(SlotState.fromOfferEvent(trueOfferInSlot));
                    }
                }
            }
            //in the case when there is no tracked offer for the slot
            else {
                //check if the slot is actually empty
                if (trueOfferInSlot.isCausedByEmptySlot()) {
                    slotStates.add(SlotState.createEmptySlot(i));
                }
                //if it is not actually empty, add the true offer we got from client.getGrandExchangeOffers()
                else {
                    slotStates.add(SlotState.fromOfferEvent(trueOfferInSlot));
                }
            }
        }
        return slotStates;
    }

    /**
     * Converts a GrandExchangeOffer from client.getGrandExchangeOffers into an OfferEvent object which is our
     * representation of offer events with extra methods that make it easier to deal with.
     */
    private OfferEvent getOfferEventConstructedFromClient(int slot) {
        GrandExchangeOffer clientOffer = this.plugin.getClient().getGrandExchangeOffers()[slot];
        GrandExchangeOfferChanged grandExchangeOfferChanged = new GrandExchangeOfferChanged();
        grandExchangeOfferChanged.setSlot(slot);
        grandExchangeOfferChanged.setOffer(clientOffer);
        return OfferEvent.fromGrandExchangeEvent(grandExchangeOfferChanged);
    }
}