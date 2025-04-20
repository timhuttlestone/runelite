/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package net.runelite.client.plugins.flippingutilities.model;

import net.runelite.client.plugins.flippingutilities.utilities.ListUtils;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages the history for an item. This class is responsible for figuring out how much profit a user made for
 * an item along with tracking how many items they bought since the last ge limit refresh and when the
 * next ge limit refresh for this an item will be.
 */
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class HistoryManager
{
	@SerializedName("sO")
	@Getter
	@Setter
	private List<OfferEvent> compressedOfferEvents = new ArrayList<>();

	@SerializedName("nGLR")
	@Getter
	private Instant nextGeLimitRefresh;

	@SerializedName("iBTLW")
	@Getter
	private int itemsBoughtThisLimitWindow;

	@SerializedName("pIB")
	private int itemsBoughtThroughCompleteOffers;

	public HistoryManager clone()
	{
		List<OfferEvent> clonedCompressedOfferEvents = compressedOfferEvents.stream().map(OfferEvent::clone).collect(Collectors.toList());
		Instant clonedGeLimitRefresh = nextGeLimitRefresh == null ? null : Instant.ofEpochMilli(nextGeLimitRefresh.toEpochMilli());
		return new HistoryManager(
				clonedCompressedOfferEvents,
				clonedGeLimitRefresh,
				itemsBoughtThisLimitWindow,
				itemsBoughtThroughCompleteOffers
		);
	}

	public void updateHistory(OfferEvent newOffer)
	{
		//if slot is -1 than the offer was added manually from GE history.
		//Since we don't know when it came or its slot/it doesn't have a time or slot, there is no point in updating ge
		//properties or trying to delete previous offers for the trade.
		if (newOffer.getSlot() != -1)
		{
			updateGeLimitProperties(newOffer);
			deletePreviousOffersForTrade(newOffer);
		}

		compressedOfferEvents.add(newOffer);
	}

	/**
	 * Updates when the ge limit will refresh and how many items have been bought since the last
	 * ge limit refresh.
	 *
	 * @param newOfferEvent offer event just received
	 */
	private void updateGeLimitProperties(OfferEvent newOfferEvent)
	{
		//there is a small bug in this method. Basically, when we get non-complete offers from different slots it will
		//only take one of the slots buys into account. When the offer completes or is cancelled things are fine.

		if (!newOfferEvent.isBuy())
		{
			return;
		}
		// when the time of the last offer (most recent offer) is greater than nextGeLimitRefresh,
		// you know the ge limits have refreshed. Since this is the first offer after the ge limits
		// have refreshed, the next refresh will be four hours after this offer's buy time.


		//if we got the event before login, there could be a problem. If the login was outside the current window
		//you don't know whether the event occurred within the window or outside.

		//if the login was within the window that was established by the purchase of an item while logged in
		// there isn't a problem, bc u know to just add to the items bought this window.
		if (nextGeLimitRefresh == null || newOfferEvent.getTime().compareTo(nextGeLimitRefresh) > 0)
		{
			nextGeLimitRefresh = newOfferEvent.getTime().plus(4, ChronoUnit.HOURS);
			if (newOfferEvent.isComplete())
			{
				itemsBoughtThroughCompleteOffers = newOfferEvent.getCurrentQuantityInTrade();
				itemsBoughtThisLimitWindow = itemsBoughtThroughCompleteOffers;
			}
			else
			{
				itemsBoughtThroughCompleteOffers = 0;
				itemsBoughtThisLimitWindow = newOfferEvent.getCurrentQuantityInTrade();

			}
		}
		//if the last offer (most recent offer) is before the next ge limit refresh, add its currentQuantityInTrade to the
		//amount bought this limit window.
		else
		{
			if (newOfferEvent.isComplete())
			{
				itemsBoughtThroughCompleteOffers += newOfferEvent.getCurrentQuantityInTrade();
				itemsBoughtThisLimitWindow = itemsBoughtThroughCompleteOffers;
			}
			else
			{
				itemsBoughtThisLimitWindow = itemsBoughtThroughCompleteOffers + newOfferEvent.getCurrentQuantityInTrade();
			}
		}
	}

	/**
	 * Deletes previous offer events for the same trade as the given offer event so that each trade has only one
	 * offer event representing it.
	 *
	 * @param newOfferEvent offer event just received
	 */
	//TODO pretty sure this has an edge cases where we think an offer is part of the same trade but it isn't...so we delete too much.
	//Ex:
	//IN RL: set offer to buy 100 lobsters in slot X. Offer event comes in and Five lobsters buy. log off.
	//log onto mobile, cancel the previous lobster offer and set another for 100 lobsters again in slot X.
	//Log back into RL with 10 lobsters being bought. This method will think those offers are for the same trade and delete
	//the first offer event for 5 lobsters...There are ways to make this more unlikely, such as checking if all the relevant
	//properties of the offers match (except currentQuantityInTrade). But, there is no way to be 100% sure because all
	//those properties could match but it could still be from a different trade if they cancel and make a trade outside of
	//RL
	public void deletePreviousOffersForTrade(OfferEvent newOfferEvent)
	{
		for (int i = compressedOfferEvents.size() - 1; i > -1; i--)
		{
			OfferEvent aPreviousOffer = compressedOfferEvents.get(i);
			if (aPreviousOffer.getSlot() == newOfferEvent.getSlot() && aPreviousOffer.isBuy() == newOfferEvent.isBuy())
			{
				//if it belongs to the same slot and its complete, it must belong to a previous trade given that
				//the most recent offer was for the same slot
				if (aPreviousOffer.isComplete())
				{
					return;
				}
				else
				{
					compressedOfferEvents.remove(i);
				}
			}
		}
	}

	/**
	 * Returns the history of the item that were traded between earliestTime and now.
	 *
	 * @param earliestTime the earliest time that trades from the trade history are added to the resulting list.
	 * @return A list of offers that were within the interval of earliestTime and now.
	 */
	public ArrayList<OfferEvent> getIntervalsHistory(Instant earliestTime)
	{
		ArrayList<OfferEvent> result = new ArrayList<>();

		for (OfferEvent offer : compressedOfferEvents)
		{
			if (offer.getTime().isAfter(earliestTime))
			{
				result.add(offer);
			}
		}

		return result;
	}

	/**
	 * This is to prevent old values from remaining for items that a user has bought and whose
	 * refresh times have already passed. If the user buys the item again, the values will be up to date,
	 * so this method wouldn't be needed, but there is no guarantee the user buys the item again after the
	 * limit refreshes. This method should be called periodically to ensure no old values will remain.
	 */
	public void validateGeProperties()
	{
		if (nextGeLimitRefresh == null)
		{
			return;
		}

		if (Instant.now().compareTo(nextGeLimitRefresh) >= 0)
		{
			nextGeLimitRefresh = null;
			itemsBoughtThisLimitWindow = 0;
		}
	}

	public boolean hasValidOffers()
	{
		return compressedOfferEvents.size() > 0;
	}

	public boolean hasOfferInInterval(Instant earliestTime) {
		if (!hasValidOffers()) {
			return false;
		}
		OfferEvent lastOffer = compressedOfferEvents.get(compressedOfferEvents.size()-1);
		return lastOffer.getTime().isAfter(earliestTime);
	}

	/**
	 * Used to mark offers as invalid. These offers are then pruned later. This is the mechanism
	 * by which offers are deleted.
	 * @param offerList the offers being invalidated
	 */
	public void deleteOffers(List<OfferEvent> offerList)
	{
		if (offerList.isEmpty()) {
			return;
		}

		Set<String> idsOfOffersToBeDeleted = offerList.stream().map(OfferEvent::getUuid).collect(Collectors.toSet());
		compressedOfferEvents.removeIf(o -> idsOfOffersToBeDeleted.contains(o.getUuid()));
	}

	/**
	 * Gets offers that have the same quantity, price ea, and buy/sell state as the given offer. This is currently used
	 * to see if there are any potential duplicates of an offer a user is trying to add manually from their GE history.
	 * Since the offers scraped from the GE history tab don't have slot information, the slots are not compared here
	 * to see if an offer is a match/duplicate.
	 *
	 * @param offer offer that duplicates are being found for.
	 * @return offers that could potentially be duplicates of the given offer event.
	 * @param limit max amount of potentially duplicate offers to find.
	 */
	public List<OfferEvent> getOfferMatches(OfferEvent offer, int limit)
	{
		List<OfferEvent> matches = new ArrayList<>();
		int count = 0;
		//look from the back to get the N most recent matches where N = limit.
		for (int i = compressedOfferEvents.size() - 1; i > -1; i--)
		{
			OfferEvent pastOffer = compressedOfferEvents.get(i);
			//don't just compare state because the scraped offer will only be of state BOUGHT/SOLD, whereas the offer in history
			//might be CANCELLED_BUY/CANCELLED_SELL
			if (offer.getPrice() == pastOffer.getPrice() && offer.getCurrentQuantityInTrade() == pastOffer.getCurrentQuantityInTrade()
				&& offer.isBuy() == pastOffer.isBuy() && pastOffer.isComplete())
			{
				matches.add(pastOffer);
				count++;
				if (count == limit)
				{
					break;
				}
			}
		}
		return matches;
	}

	/**
	 * Gets the YOUNGEST offer that matches some arbitrary conditions
	 * @param predicate conditions
	 */
	public Optional<OfferEvent> getLatestOfferThatMatchesPredicate(Predicate<OfferEvent> predicate)
	{
		for (int i = compressedOfferEvents.size() - 1; i > -1; i--)
		{
			if (predicate.test(compressedOfferEvents.get(i)))
			{
				return Optional.of(compressedOfferEvents.get(i));
			}
		}
		return Optional.empty();
	}

	/**
	 * Because we don't persist the itemName field in an OfferEvent, we need to hydrate every
	 * OfferEvent with it
	 */
	public void setOfferNames(String itemName) {
		compressedOfferEvents.forEach(o -> o.setItemName(itemName));
	}

	/**
	 * We don't persist the madeBy field, so it has to be hydrated.
	 */
	public void setOfferMadeBy(String name) {
		compressedOfferEvents.forEach(o -> o.setMadeBy(name));
	}

	/**
	 * Every new offer event created nowadays has a uuid associated with it. However, the old
	 * offer events that have already been persisted need their uuids' set.
	 */
	public void setOfferIds() {
		compressedOfferEvents.forEach(o -> {
			if (o.getUuid() == null) {
				o.setUuid(UUID.randomUUID().toString());
			}
		});
	}

	//STATIC METHODS BEGIN (wondering if i should move them elsewhere, but, where?)

	/**
	 * Calculates profit for a list of trades made with this item by counting the expenses and revenues
	 * accrued over these trades and figuring out the difference in value.
	 *
	 * @param tradeList The list of trades whose total profits will be calculated.
	 * @return profit
	 */
	public static long getProfit(List<OfferEvent> tradeList)
	{
		return getValueOfMatchedOffers(tradeList, false) -
			getValueOfMatchedOffers(tradeList, true);
	}

	/**
	 * This method finds the value of all the offers with the buyState that are
	 * contributing to flips. As such, this is NOT the value of the entire trade list, or the value of
	 * ALL the buys/sells. It is only the value of buys OR sells that are contributing to flips, meaning
	 * they would be matched to another buy/sell when constructing flips.
	 *
	 * @param tradeList  A list of offers
	 * @param isBuy Refers to whether buys or sells should be looked at
	 * @return Returns the value of flips
	 */
	public static long getValueOfMatchedOffers(List<OfferEvent> tradeList, boolean isBuy)
	{
		return getValueOfOffersUpToLimit(
			tradeList.stream().filter(o -> o.isBuy() == isBuy).collect(Collectors.toList()),
			countFlipQuantity(tradeList));
	}

	/**
	 * Calculates the total value of the sell or buy offers in a trade list.
	 */
	public static long getTotalRevenueOrExpense(List<OfferEvent> tradeList, boolean isBuy)
	{
		return tradeList.stream().
			filter(o -> o.isBuy() == isBuy).
			mapToLong(o -> o.getCurrentQuantityInTrade() * o.getPrice()).sum();
	}

	/**
	 * Gets the amount of items in a given tradelist that have been "flipped". We take
	 * min(itemsBought, itemsSold) bc we only want the amount of items that will actually be matched against each
	 * other to create flips and if either buys or sells has a surplus relative to the other, that surplus won't be
	 * matched.
	 *
	 * @param tradeList The list of offers that the flip count is based on
	 * @return An integer representing the total currentQuantityInTrade of items flipped in the list of offers
	 */
	public static int countFlipQuantity(List<OfferEvent> tradeList)
	{
		int numBoughtItems = 0;
		int numSoldItems = 0;

		for (OfferEvent offer : tradeList)
		{
			if (offer.isBuy())
			{
				numBoughtItems += offer.getCurrentQuantityInTrade();
			}
			else
			{
				numSoldItems += offer.getCurrentQuantityInTrade();
			}
		}

		return Math.min(numBoughtItems, numSoldItems);
	}

	/**
	 * Calculates the amount of money spent on either a buy or sell list, up to the amount of items
	 * specified by the limit.
	 *
	 * @param tradeList a buy or a sell list
	 * @param itemLimit the amount of items to calculate the value up until. This is for the case
	 *                  when a user has an unequal amount of buys/sells in which case you want to return the
	 *                  profit the items only up until the buys and sells are equal. If this values is -1, it
	 *                  ignores the limit.
	 * @return the amount of money spent on the offer list, up to the amount of items specified by the
	 * limit
	 */
	private static long getValueOfOffersUpToLimit(List<OfferEvent> tradeList, long itemLimit)
	{
		int itemsSeen = 0;
		long moneySpent = 0;

		itemLimit = itemLimit == -1 ? Long.MAX_VALUE : itemLimit;

		for (OfferEvent offer : tradeList)
		{
			if (itemsSeen + offer.getCurrentQuantityInTrade() >= itemLimit)
			{
				moneySpent += (itemLimit - itemsSeen) * offer.getPrice();
				break;
			}
			else
			{
				moneySpent += offer.getCurrentQuantityInTrade() * offer.getPrice();
				itemsSeen += offer.getCurrentQuantityInTrade();
			}

		}

		return moneySpent;
	}

	/**
	 * Returns a view of the offer events that accounts for the amount consumed by partial offers
	 * in the recipe flips
	 */
	static List<OfferEvent> getPartialOfferAdjustedView(List<OfferEvent> offers, Map<String, PartialOffer> partialOffers) {
		return offers.stream().map(o -> {
			if (partialOffers.containsKey(o.getUuid())) {
				return partialOffers.get(o.getUuid()).toRemainingOfferEvent();
			}
			return o;
		}).collect(Collectors.toList());
	}

	/**
	 * Creates Flips from offers. Flips represent a buy trade followed by a sell trade. A trade is a collection
	 * of offers from the empty offer to the completed offer. A completed offer marks the end of a trade.
	 *
	 * @param tradeList offers
	 * @return flips
	 */
	public static List<Flip> getFlips(List<OfferEvent> tradeList)
	{
		//group offers based on which account those offers belong to (this is really only relevant when getting the flips
		//of the account wide tradelist as you don't want to match offers from diff accounts.

		Map<String, List<OfferEvent>> groupedOffers = tradeList.stream().collect(Collectors.groupingBy(OfferEvent::getMadeBy));

		//take each offer list and create flips out of them, then put those flips into one list.
		List<Flip> flips = new ArrayList<>();
		groupedOffers.values().forEach(offers -> flips.addAll(createFlips(offers)));

		flips.sort(Comparator.comparing(Flip::getTime));

		return flips;
	}

	/**
	 * Creates flips out of a list of offers. It does this by first pairing margin check offers together and then
	 * pairing regular offers together.
	 *
	 * @param offers the offer list
	 * @return flips
	 */
	public static List<Flip> createFlips(List<OfferEvent> offers)
	{
		List<OfferEvent>[] subLists = ListUtils.partition(
			offers.stream().map(OfferEvent::clone).collect(Collectors.toList()),
			o -> o.isMarginCheck() && o.isBuy(),
			o -> o.isMarginCheck() && !o.isBuy(),
			o -> !o.isMarginCheck() && o.isBuy(),
			o -> !o.isMarginCheck() && !o.isBuy());

		List<OfferEvent> buyMarginChecks = subLists[0];
		List<OfferEvent> sellMarginChecks = subLists[1];
		List<OfferEvent> nonMarginCheckBuys = subLists[2];
		List<OfferEvent> nonMarginCheckSells = subLists[3];

		ArrayList<Flip> flips = new ArrayList<>();

		List<OfferEvent> unPairedMarginChecks = new ArrayList<>();
		List<Flip> flipsFromMarginChecks = pairMarginChecks(buyMarginChecks, sellMarginChecks, unPairedMarginChecks);

		unPairedMarginChecks.forEach(offer ->
		{
			if (offer.isBuy())
			{
				nonMarginCheckBuys.add(offer);
			}
			else
			{
				nonMarginCheckSells.add(offer);
			}
		});

		//we sort the offers because we added the unpaired margin checks back to the offer list and it should be
		//placed in the appropriate place in the list so it doesn't get matched with an offer from many days ago or something.
		nonMarginCheckBuys.sort(Comparator.comparing(OfferEvent::getTime));
		nonMarginCheckSells.sort(Comparator.comparing(OfferEvent::getTime));

		flips.addAll(flipsFromMarginChecks);
		flips.addAll(combineToFlips(nonMarginCheckBuys, nonMarginCheckSells));

		return flips;
	}

	/**
	 * We need to pair margin check offers together because we don't want them to be paired with a regular offer in the case
	 * of an uneven quantity of items bought/sold. Pairing margin checks is tricky...A "whole" margin check is defined as a
	 * buy margin check offer followed by a sell margin check offer. However, when flipping, one often insta
	 * buys an item just to see its optimal sell price and likewise they might randomly insta sell an item to see
	 * its optimal buy price. These "half" margin checks may not be followed by a corresponding buy/sell margin check offer
	 * to make it a "whole" margin check. As such, if we are grouping margin check offers together to create flips,
	 * if a user has done some of these "half" margin checks, we have to be careful not to accidently group them with a
	 * buy/sell margin check offer that actually has its corresponding buy/sell margin check offer that makes it a whole
	 * margin check. This can result in REALLY inaccurate Flips as half margin check (lets say its a margin check buy offer)
	 * from a day before can be matched with a sell margin check offer from another day (when the margin's are totally
	 * different). And since that buy margin check offer was erroneously matched to that sell margin check offer the
	 * buy offer that was actually supposed to be matched to it, might match with some sell margin check offer that
	 * IT doesn't correspond to, etc.
	 *
	 * @param buys      a list of buy margin check offers
	 * @param sells     a list of sell margin check offers
	 * @param remainder an empty list to be populated with margin check offers that don't have companion buy/sell offers.
	 * @return a list of flips created from "whole" margin checks.
	 */
	public static List<Flip> pairMarginChecks(List<OfferEvent> buys, List<OfferEvent> sells, List<OfferEvent> remainder)
	{
		List<Flip> flips = new ArrayList<>();
		int buyIdx;
		int sellIdx = 0;
		for (buyIdx = 0; buyIdx < buys.size(); buyIdx++)
		{

			if (sellIdx == sells.size())
			{
				break;
			}

			OfferEvent buy = buys.get(buyIdx);
			OfferEvent sell = sells.get(sellIdx);

			//just a subjective heuristic i am using to determine whether a buy margin check has a companion sell margin
			//check. Chances are, if there's a 1 minute difference
			long millisBetweenBuyAndSell = Duration.between(buy.getTime(), sell.getTime()).toMillis();
			if (millisBetweenBuyAndSell >= 0 && millisBetweenBuyAndSell < 60000) //60k milliseconds is a minute
			{
				flips.add(new Flip(buy.getPrice(), sell.getPrice(), sell.getCurrentQuantityInTrade(), sell.getTime(), sell.isMarginCheck(), false));
				sellIdx++;
			}

			//if the buy is more than 1 minute before the sell, its probably not for that sell.
			else if (millisBetweenBuyAndSell >= 0 && !(millisBetweenBuyAndSell < 60000))
			{
				remainder.add(buy);
			}

			//if the sell comes before the buy its a stand alone insta sell (a "half" margin check")
			else if (millisBetweenBuyAndSell < 0)
			{
				remainder.add(sell);
				sellIdx++;
				buyIdx--; //stay on this buy offer
			}

		}

		//if the sells were exhausted, it won't add anything as "i" will be equal to sells.size. The same applies with
		//the buys
		remainder.addAll(sells.subList(sellIdx, sells.size()));
		remainder.addAll(buys.subList(buyIdx, buys.size()));
		return flips;
	}

	/**
	 * Creates flips based on the buy and sell list. It does this by going through the sell list and the buy list
	 * and only moving onto the next sell offer when the current sell offer is exhausted (seen more items bought than it
	 * has items sold). This ensures that a flip is only created on a completed sell offer
	 *
	 * @param buys  the buy offers
	 * @param sells the sell offers
	 * @return a list of Flips based on the buy and sell list.
	 */
	private static ArrayList<Flip> combineToFlips(List<OfferEvent> buys, List<OfferEvent> sells)
	{
		ArrayList<Flip> flips = new ArrayList<>();

		int buyIdx = 0;
		for (OfferEvent sell : sells)
		{
			if (sell.getCurrentQuantityInTrade() == 0) {
				continue;
			}
			int numBuysSeen = 0;
			int totalRevenue = 0;
			while (buyIdx < buys.size())
			{
				OfferEvent buy = buys.get(buyIdx);
				numBuysSeen += buy.getCurrentQuantityInTrade();

				if (numBuysSeen >= sell.getCurrentQuantityInTrade())
				{
					int leftOver = numBuysSeen - sell.getCurrentQuantityInTrade();
					int amountTaken = buy.getCurrentQuantityInTrade() - leftOver;
					totalRevenue += amountTaken * buy.getPrice();
					buy.setCurrentQuantityInTrade(leftOver);
					flips.add(new Flip(totalRevenue / sell.getCurrentQuantityInTrade(), sell.getPrice(), sell.getCurrentQuantityInTrade(), sell.getTime(), false, !sell.isComplete()));
					break;
				}
				else
				{
					totalRevenue += buy.getCurrentQuantityInTrade() * buy.getPrice();
					buyIdx++;
				}
			}

			//buys only partially exhausted a sell
			if (buyIdx == buys.size() && numBuysSeen != 0)
			{
				flips.add(new Flip(totalRevenue / numBuysSeen, sell.getPrice(), numBuysSeen, sell.getTime(), false, true));
				break;
			}
		}

		return flips;
	}
}