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

package net.runelite.client.plugins.flippingutilities.ui.statistics.items;

import net.runelite.client.plugins.flippingutilities.controller.FlippingPlugin;
import net.runelite.client.plugins.flippingutilities.model.*;
import net.runelite.client.plugins.flippingutilities.ui.statistics.StatsPanel;
import net.runelite.client.plugins.flippingutilities.ui.uiutilities.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class FlippingItemPanel extends JPanel
{
	private FlippingPlugin plugin;
	@Getter
	private FlippingItem item;

	private StatsPanel statsPanel;

	private JLabel itemProfitAndQuantityLabel = new JLabel();
	private JPanel itemIconTitlePanel = new JPanel(new BorderLayout());

	private JLabel totalProfitValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel profitEachValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel quantityFlipped = new JLabel("", SwingConstants.RIGHT);
	private JLabel roiValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel avgBuyPriceValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel avgSellPriceValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel quantityBoughtLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel quantitySoldLabel = new JLabel("", SwingConstants.RIGHT);

	private List<FlipPanel> flipPanels = new ArrayList<>();
	private List<OfferPanel> offerPanels = new ArrayList<>();

	private Paginator flipPaginator;
	private Paginator offerPaginator;

	private JPanel offersBackgroundPanel = new JPanel();
	private JPanel flipsBackgroundPanel = new JPanel();

	/**
	 * This panel represents the middle layer of information. It contains general information about the item
	 * along with being the container for the trade history of that item.
	 *
	 * @param plugin       Used to access the plugin user config.
	 * @param item The item that the panel represents.
	 */

	FlippingItemPanel(FlippingPlugin plugin, FlippingItem item)
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createMatteBorder(1,1,1,1, ColorScheme.DARKER_GRAY_COLOR.darker()));

		this.plugin = plugin;
		this.item = item;
		this.statsPanel = plugin.getStatPanel();

		List<OfferEvent> offers = item.getIntervalHistory(statsPanel.getStartOfInterval());
		Map<String, PartialOffer> offerIdToPartialOffer = plugin.getOfferIdToPartialOffer(item.getItemId());
		List<OfferEvent> adjustedOffers = FlippingItem.getPartialOfferAdjustedView(offers, offerIdToPartialOffer);
		List<Flip> flips = FlippingItem.getFlips(adjustedOffers);

		this.flipPaginator = createPaginator(() -> buildAllFlipsPanel(flips));
		this.offerPaginator = createPaginator(() -> buildAllOffersPanels(offers));

		offerPaginator.updateTotalPages(offers.size());
		flipPaginator.updateTotalPages(flips.size());

		buildAllFlipsPanel(flips);
		buildAllOffersPanels(offers);

		JLabel[] descriptionLabels = {new JLabel("Total Profit: "), new JLabel("Avg. Profit ea: "), new JLabel("Avg. ROI: "), new JLabel("Quantity Flipped: "),
			new JLabel(" "), new JLabel("Quantity Bought: "), new JLabel("Quantity Sold: "), new JLabel("Avg. Buy Price: "), new JLabel("Avg. Sell Price: ")};

		JLabel[] valueLabels = {totalProfitValLabel, profitEachValLabel, roiValLabel, quantityFlipped,
			new JLabel(" "), quantityBoughtLabel, quantitySoldLabel, avgBuyPriceValLabel,
			avgSellPriceValLabel};

		JPanel subInfoPanel = createSubInfoPanel(descriptionLabels, valueLabels);
		JPanel tradeHistoryPanel = createTradeHistoryPanel(offersBackgroundPanel, flipsBackgroundPanel);
		JPanel subInfoAndHistoryContainer = createSubInfoAndHistoryContainer(subInfoPanel, tradeHistoryPanel);
        JPanel titlePanel = createTitlePanel(createIconPanel(plugin.getItemManager()), createNameAndProfitPanel(), createCollapseIcon(), subInfoAndHistoryContainer);

        updateLabels(offers, adjustedOffers);

        add(titlePanel, BorderLayout.NORTH);
        add(subInfoAndHistoryContainer, BorderLayout.CENTER);
	}

	private JPanel createSubInfoAndHistoryContainer(JPanel subInfoPanel, JPanel tradeHistoryPanel) {
        JPanel subInfoAndHistoryContainer = new JPanel(new BorderLayout());
        //Set background and border of container with sub infos and trade history
        subInfoAndHistoryContainer.setBackground(CustomColors.DARK_GRAY_LIGHTER);
        subInfoAndHistoryContainer.add(subInfoPanel, BorderLayout.CENTER);
        subInfoAndHistoryContainer.add(tradeHistoryPanel, BorderLayout.SOUTH);
        subInfoAndHistoryContainer.setVisible(statsPanel.getExpandedItems().contains(item.getItemName()));
        return subInfoAndHistoryContainer;
    }

	private Paginator createPaginator(Runnable runnable) {
        Paginator paginator = new Paginator(runnable);
        paginator.setPageSize(10);
        paginator.setBackground(CustomColors.DARK_GRAY);
        paginator.getStatusText().setFont(FontManager.getRunescapeSmallFont());
        paginator.setBorder(BorderFactory.createMatteBorder(1,1,1,1, ColorScheme.DARKER_GRAY_COLOR.darker()));
        return paginator;
    }

    private void putPanelsOnBackgroundPanel(List<JPanel> panels, JPanel backgroundPanel, Paginator paginator) {
		List<JPanel> panelsAndPaginator = new ArrayList<>();
		JPanel paginatorWrapper = new JPanel();
		paginatorWrapper.add(paginator);
		panelsAndPaginator.add(paginatorWrapper);
		panelsAndPaginator.addAll(panels);
		backgroundPanel.removeAll();
		UIUtilities.stackPanelsVertically(panelsAndPaginator, backgroundPanel, 2);
		if (panels.isEmpty()) {
			//if i don't wrap the label, the box layout places it weird....
			JPanel labelWrapper = new JPanel();
			JLabel noDataLabel = new JLabel("Nothing here...", SwingConstants.CENTER);
			noDataLabel.setForeground(CustomColors.TOMATO);
			noDataLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
			labelWrapper.add(noDataLabel);
			backgroundPanel.add(labelWrapper);
		}
		repaint();
		revalidate();
	}

	private void buildAllOffersPanels(List<OfferEvent> offers) {
        List<OfferEvent> reversedHistory = new ArrayList<>(offers);
        Collections.reverse(reversedHistory);
        List<OfferEvent> offersOnCurrentPage = offerPaginator.getCurrentPageItems(reversedHistory);
        offerPanels = offersOnCurrentPage.stream().map(
                offerEvent -> new OfferPanel(
                        plugin, item, offerEvent, false))
                .collect(Collectors.toList());
		putPanelsOnBackgroundPanel(new ArrayList<>(offerPanels), offersBackgroundPanel, offerPaginator);
	}

	private void buildAllFlipsPanel(List<Flip> flips) {
		List<Flip> flipsCopy = new ArrayList<>(flips);
		Collections.reverse(flipsCopy);
        List<Flip> flipsOnCurrentPage = flipPaginator.getCurrentPageItems(flipsCopy);
        flipPanels = flipsOnCurrentPage.stream().map(FlipPanel::new).collect(Collectors.toList());
		putPanelsOnBackgroundPanel(new ArrayList<>(flipPanels), flipsBackgroundPanel, flipPaginator);
	}

	private JPanel createTitlePanel(JPanel itemIconPanel, JPanel nameAndProfitPanel, JLabel collapseIcon, JPanel subInfoAndHistoryContainer)
	{
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(CustomColors.DARK_GRAY);
		titlePanel.setBorder(new EmptyBorder(5, 4, 5, 4));

		titlePanel.add(itemIconPanel, BorderLayout.WEST);
		titlePanel.add(nameAndProfitPanel, BorderLayout.CENTER);
		titlePanel.add(collapseIcon, BorderLayout.EAST);

		titlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (subInfoAndHistoryContainer.isVisible())
					{
						collapseIcon.setIcon(Icons.CLOSE_ICON);
						subInfoAndHistoryContainer.setVisible(false);
						statsPanel.getExpandedItems().remove(item.getItemName());
					}
					else
					{
						collapseIcon.setIcon(Icons.OPEN_ICON);
						subInfoAndHistoryContainer.setVisible(true);
						statsPanel.getExpandedItems().add(item.getItemName());
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				titlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
				nameAndProfitPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
				for (Component component : nameAndProfitPanel.getComponents()) {
					component.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
				}
				itemIconTitlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				titlePanel.setBackground(CustomColors.DARK_GRAY);
				nameAndProfitPanel.setBackground(CustomColors.DARK_GRAY);
				for (Component component : nameAndProfitPanel.getComponents()) {
					component.setBackground(CustomColors.DARK_GRAY);
				}
				itemIconTitlePanel.setBackground(CustomColors.DARK_GRAY);
			}
		});

		return titlePanel;
	}

	private JPanel createSubInfoPanel(JLabel[] descriptionLabels, JLabel[] valueLabels)
	{
		JPanel subInfoContainer = new JPanel();
		subInfoContainer.setBackground(CustomColors.DARK_GRAY_LIGHTER);
		subInfoContainer.setLayout(new DynamicGridLayout(valueLabels.length, descriptionLabels.length));
		subInfoContainer.setBorder(new EmptyBorder(10,6,6,6));

		for (int i = 0; i < descriptionLabels.length; i++)
		{
			JLabel textLabel = descriptionLabels[i];
			JLabel valLabel = valueLabels[i];
			JPanel panel = new JPanel(new BorderLayout());

			panel.add(textLabel, BorderLayout.WEST);
			panel.add(valLabel, BorderLayout.EAST);

			panel.setBorder(new EmptyBorder(4, 2, 4, 2));
			panel.setBackground(CustomColors.DARK_GRAY_LIGHTER);

			textLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);

			textLabel.setFont(FontManager.getRunescapeSmallFont());
			valLabel.setFont(FontManager.getRunescapeSmallFont());

			subInfoContainer.add(panel);
		}

		return subInfoContainer;
	}

	private JPanel createTradeHistoryPanel(JPanel offersPanel, JPanel flipsPanel)
	{
		boolean shouldExpandTradeHistory = statsPanel.getExpandedTradeHistories().contains(item.getItemName());
		boolean shouldSelectOffersTab = statsPanel.getItemsWithOffersTabSelected().contains(item.getItemId());
		JPanel tradeHistoryTitlePanel = new JPanel(new BorderLayout());
		tradeHistoryTitlePanel.setBackground(CustomColors.DARK_GRAY);
		tradeHistoryTitlePanel.setBorder(new EmptyBorder(4,0,4,0));

		JPanel mainDisplay = new JPanel();
		MaterialTabGroup tabGroup = new MaterialTabGroup(mainDisplay);
		MaterialTab offersTab = new MaterialTab("Offers", tabGroup, offersPanel);
		offersTab.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				statsPanel.getItemsWithOffersTabSelected().add(item.getItemId());
			}
		});
		MaterialTab flipsTab = new MaterialTab("Flips", tabGroup, flipsPanel);
		flipsTab.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				statsPanel.getItemsWithOffersTabSelected().remove(item.getItemId());
			}
		});

		tabGroup.setBorder(new EmptyBorder(5, 0, 7, 0));
		tabGroup.addTab(offersTab);
		tabGroup.addTab(flipsTab);

		tabGroup.select(shouldSelectOffersTab? offersTab: flipsTab);
		mainDisplay.setVisible(shouldExpandTradeHistory);
		tabGroup.setVisible(shouldExpandTradeHistory);

		JLabel collapseTradeHistoryIconLabel = new JLabel(shouldExpandTradeHistory ? Icons.OPEN_ICON : Icons.CLOSE_ICON);
		JLabel tradeHistoryTitleLabel = new JLabel("View Trade History", SwingConstants.CENTER);
		tradeHistoryTitleLabel.setFont(new Font("Whitney", Font.ITALIC, 10));
		tradeHistoryTitlePanel.add(tradeHistoryTitleLabel, BorderLayout.CENTER);
		tradeHistoryTitlePanel.add(collapseTradeHistoryIconLabel, BorderLayout.EAST);
		tradeHistoryTitlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (tabGroup.isVisible())
					{
						tabGroup.setVisible(false);
						mainDisplay.setVisible(false);
						collapseTradeHistoryIconLabel.setIcon(Icons.CLOSE_ICON);
						statsPanel.getExpandedTradeHistories().remove(item.getItemName());
					}
					else
					{
						tabGroup.setVisible(true);
						mainDisplay.setVisible(true);
						collapseTradeHistoryIconLabel.setIcon(Icons.OPEN_ICON);
						statsPanel.getExpandedTradeHistories().add(item.getItemName());
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				tradeHistoryTitlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				tradeHistoryTitlePanel.setBackground(CustomColors.DARK_GRAY);
			}
		});


		JPanel tradeHistoryBody = new JPanel(new BorderLayout());
		tradeHistoryBody.add(tabGroup, BorderLayout.NORTH);
		tradeHistoryBody.add(mainDisplay, BorderLayout.CENTER);
		tradeHistoryBody.setBackground(CustomColors.DARK_GRAY);

		JPanel tradeHistoryPanel = new JPanel(new BorderLayout());
		tradeHistoryPanel.add(tradeHistoryTitlePanel, BorderLayout.NORTH);
		tradeHistoryPanel.add(tradeHistoryBody, BorderLayout.CENTER);

		return tradeHistoryPanel;
	}

    /**
     * Creates icon panel that contains the item image and the delete icon which shows when
     * you hover over the item image.
     */
	private JPanel createIconPanel(ItemManager itemManager)
	{
		JLabel deleteLabel = new JLabel(Icons.DELETE_ICON);
		deleteLabel.setPreferredSize(new Dimension(24, 24));
		deleteLabel.setVisible(false);

		AsyncBufferedImage itemImage = itemManager.getImage(item.getItemId());
		JLabel itemLabel = new JLabel();
		Runnable resize = () ->
		{
			BufferedImage subIcon = itemImage.getSubimage(0, 0, 32, 32);
			ImageIcon itemIcon = new ImageIcon(subIcon.getScaledInstance(24, 24, Image.SCALE_SMOOTH));
			itemLabel.setIcon(itemIcon);
		};
		itemImage.onLoaded(resize);
		resize.run();

		itemIconTitlePanel.add(itemLabel, BorderLayout.WEST);
		itemIconTitlePanel.add(deleteLabel, BorderLayout.EAST);
		itemIconTitlePanel.setBackground(CustomColors.DARK_GRAY);
		itemIconTitlePanel.setBorder(new EmptyBorder(5, 2, 0, 5));
		itemIconTitlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (plugin.getAccountCurrentlyViewed().equals(FlippingPlugin.ACCOUNT_WIDE)) {
					JOptionPane.showMessageDialog(null, "You cannot delete offers in the Accountwide view");
					return;
				}
				int result = JOptionPane.showOptionDialog(itemIconTitlePanel, "Are you sure you want to delete this item's offers from this time interval?",
					"Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
					null, new String[] {"Yes", "No"}, "No");

				if (result == JOptionPane.YES_OPTION)
				{
					deletePanel();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				itemLabel.setVisible(false);
				deleteLabel.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				itemLabel.setVisible(true);
				deleteLabel.setVisible(false);
			}
		});

		return itemIconTitlePanel;
	}

	private JPanel createNameAndProfitPanel()
	{
		JPanel nameAndProfitPanel = new JPanel(new BorderLayout());
		nameAndProfitPanel.setBackground(CustomColors.DARK_GRAY);
		JLabel itemNameLabel = new JLabel(item.getItemName());
		nameAndProfitPanel.add(itemNameLabel, BorderLayout.NORTH);
		nameAndProfitPanel.add(itemProfitAndQuantityLabel, BorderLayout.SOUTH);
		nameAndProfitPanel.setPreferredSize(new Dimension(0, 0));
		return nameAndProfitPanel;
	}

	private JLabel createCollapseIcon()
	{
		JLabel collapseIconLabel = new JLabel();
		collapseIconLabel.setIcon(statsPanel.getExpandedItems().contains(item.getItemName()) ? Icons.OPEN_ICON : Icons.CLOSE_ICON);
		collapseIconLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
		return collapseIconLabel;
	}

	public void updateLabels(List<OfferEvent> offers, List<OfferEvent> adjustedOffers)
	{
        quantityFlipped.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        avgBuyPriceValLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        avgSellPriceValLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        quantityBoughtLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        quantitySoldLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		long totalItemsBought = 0;
		long totalItemsSold = 0;
		for (OfferEvent offer : offers)
		{
			if (offer.isBuy())
			{
				totalItemsBought += offer.getCurrentQuantityInTrade();
			}
			else
			{
				totalItemsSold += offer.getCurrentQuantityInTrade();
			}
		}

		int itemCountFlipped = FlippingItem.countFlipQuantity(adjustedOffers);
		long revenueFromFlippedItems = FlippingItem.getValueOfMatchedOffers(adjustedOffers, false);
		long expenseFromFlippedItems = FlippingItem.getValueOfMatchedOffers(adjustedOffers, true);
		long totalRevenue = FlippingItem.getTotalRevenueOrExpense(offers, false);
		long totalExpense = FlippingItem.getTotalRevenueOrExpense(offers, true);
		long profit = revenueFromFlippedItems - expenseFromFlippedItems;

		updateTitleLabels(profit, itemCountFlipped);
		updateFlippingLabels(expenseFromFlippedItems, revenueFromFlippedItems, itemCountFlipped);
		updateGeneralLabels(totalRevenue, totalExpense, totalItemsBought, totalItemsSold);
		updateTimeLabels();
	}

	/**
	 * Updates the labels on the title panel. This includes the profit label which shows how much profit you made
	 * from flipping that item and the number of times you flipped that item.
	 */
	private void updateTitleLabels(long profitFromFlips, long numItemsFlipped)
	{
		String totalProfitString = (profitFromFlips >= 0? "+": "") + UIUtilities.quantityToRSDecimalStack(profitFromFlips, true) + " gp";
		totalProfitString += " (x " + QuantityFormatter.formatNumber(numItemsFlipped) + ")";

		itemProfitAndQuantityLabel.setText(totalProfitString);
		itemProfitAndQuantityLabel.setForeground((profitFromFlips >= 0) ? ColorScheme.GRAND_EXCHANGE_PRICE : CustomColors.OUTDATED_COLOR);
		itemProfitAndQuantityLabel.setBorder(new EmptyBorder(0, 0, 2, 0));
		itemProfitAndQuantityLabel.setFont(FontManager.getRunescapeSmallFont());
	}

	private void updateFlippingLabels(long flippingExpense, long flippingRevenue, int itemsFlipped) {
		long profitFromFlips = flippingRevenue - flippingExpense;
		totalProfitValLabel.setText(UIUtilities.quantityToRSDecimalStack(profitFromFlips, true) + " gp");
		totalProfitValLabel.setForeground((profitFromFlips >= 0) ? ColorScheme.GRAND_EXCHANGE_PRICE : CustomColors.OUTDATED_COLOR);
		totalProfitValLabel.setToolTipText(QuantityFormatter.formatNumber(profitFromFlips) + " gp");

		String profitEach = UIUtilities.quantityToRSDecimalStack(itemsFlipped > 0 ? (profitFromFlips / itemsFlipped) : 0, true) + " gp/ea";
		profitEachValLabel.setText(profitEach);
		profitEachValLabel.setForeground((profitFromFlips >= 0) ? ColorScheme.GRAND_EXCHANGE_PRICE : CustomColors.OUTDATED_COLOR);
		profitEachValLabel.setToolTipText(QuantityFormatter.formatNumber(itemsFlipped > 0 ? profitFromFlips / itemsFlipped : 0) + " gp/ea");

		quantityFlipped.setText(QuantityFormatter.formatNumber(itemsFlipped) + " Items");

		float roi = (float) flippingExpense >0? (float) profitFromFlips / flippingExpense * 100: 0;

		roiValLabel.setText(String.format("%.2f", roi) + "%");
		roiValLabel.setForeground(UIUtilities.gradiatePercentage(roi, plugin.getConfig().roiGradientMax()));
		roiValLabel.setToolTipText("<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");
	}

	private void updateGeneralLabels(long totalRevenue, long totalExpense, long numBuys, long numSells) {
		avgBuyPriceValLabel.setText(QuantityFormatter.formatNumber((int) numBuys > 0? (totalExpense / numBuys) : 0) + " gp");
		avgSellPriceValLabel.setText(QuantityFormatter.formatNumber((int) numSells > 0? (totalRevenue / numSells) : 0) + " gp");

		quantityBoughtLabel.setText("" + numBuys);
		quantitySoldLabel.setText("" + numSells);
	}

	public void updateTimeLabels()
	{
		flipPanels.forEach(FlipPanel::updateTitleAndTimeDisplay);
		offerPanels.forEach(OfferPanel::updateTimeDisplay);
	}

	private void deletePanel()
	{
		statsPanel.deleteItemPanel(this);
	}
}