/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.RequiredItemDisplay;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import com.collectionloghelper.guidance.RequiredItemResolver;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Integration test verifying that {@link RequiredItemsChipPanel} reads bank-scan
 * state correctly via {@link RequiredItemResolver} after a real {@link PlayerBankState}
 * scan is performed.
 *
 * <p>Uses concrete {@link PlayerBankState} and {@link PlayerInventoryState} instances
 * (no mocks for those) so the test validates the full resolve path from bank-container
 * scan through {@link RequiredItemResolver#resolve} to the chip panel's
 * {@link RequiredItemsChipPanel#borderColorFor} and {@link RequiredItemsChipPanel#tooltipFor}
 * helpers.
 *
 * <p>Render-path Swing testing is omitted — AWT/Swing rendering requires a display
 * and EDT synchronisation that is unreliable in headless CI environments. The color-
 * logic helpers are covered as pure functions in {@link RequiredItemsChipPanelTest}.
 * The full update() visibility path is confirmed in that class as well.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BankScanIntegrationTest
{
	private static final int TINDERBOX = 590;
	private static final int PYRE_LOGS = 3438;
	private static final int SHADE_REMAINS = 3392;

	@Mock
	private Client client;

	@Mock
	private ConfigManager configManager;

	@Mock
	private ItemContainer bankContainer;

	@Mock
	private ItemManager itemManager;

	@Mock
	private GuidanceOverlayCoordinator coordinator;

	@Mock
	private ItemComposition tinderboxComp;

	@Mock
	private ItemComposition pyreLogsComp;

	@Mock
	private ItemComposition shadeRemainsComp;

	private PlayerBankState bankState;
	private PlayerInventoryState inventoryState;
	private RequiredItemResolver resolver;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<PlayerBankState> bankCtor = PlayerBankState.class.getDeclaredConstructor(
			Client.class, ConfigManager.class);
		bankCtor.setAccessible(true);
		bankState = bankCtor.newInstance(client, configManager);

		Constructor<PlayerInventoryState> invCtor =
			PlayerInventoryState.class.getDeclaredConstructor();
		invCtor.setAccessible(true);
		inventoryState = invCtor.newInstance();

		Constructor<RequiredItemResolver> resolverCtor =
			RequiredItemResolver.class.getDeclaredConstructor(
				PlayerInventoryState.class, PlayerBankState.class, ItemManager.class);
		resolverCtor.setAccessible(true);
		resolver = resolverCtor.newInstance(inventoryState, bankState, itemManager);
		resolver.setCoordinator(coordinator);

		lenient().when(coordinator.getActiveTargetItemId()).thenReturn(null);
		lenient().when(configManager.getRSProfileConfiguration(anyString(), anyString()))
			.thenReturn(null);

		lenient().when(tinderboxComp.getName()).thenReturn("Tinderbox");
		lenient().when(pyreLogsComp.getName()).thenReturn("Pyre logs");
		lenient().when(shadeRemainsComp.getName()).thenReturn("Shade remains");
		lenient().when(itemManager.getItemComposition(TINDERBOX)).thenReturn(tinderboxComp);
		lenient().when(itemManager.getItemComposition(PYRE_LOGS)).thenReturn(pyreLogsComp);
		lenient().when(itemManager.getItemComposition(SHADE_REMAINS)).thenReturn(shadeRemainsComp);
	}

	// ── Verify panel reads bank-scan state correctly ────────────────────────

	/**
	 * Before any bank scan the chip panel must map all items to MISSING.
	 * Confirms that an empty/unscan state does not produce false IN_BANK hits.
	 */
	@Test
	public void chipPanel_noBankScan_allItemsMissing()
	{
		GuidanceStep step = stepWith(Arrays.asList(TINDERBOX, PYRE_LOGS, SHADE_REMAINS));

		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals( 3, rows.size(),"Must have 3 rows for 3 item IDs");
		for (RequiredItemDisplay row : rows)
		{
			assertEquals(
				Status.MISSING, row.getStatus(),"All items must be MISSING before any bank scan");
			assertEquals(
				RequiredItemDisplay.COLOR_MISSING,
				RequiredItemsChipPanel.borderColorFor(row.getStatus()),"MISSING chip must have red border");
		}
	}

	/**
	 * After scanning a bank that contains PYRE_LOGS and SHADE_REMAINS (but not
	 * TINDERBOX), the resolver must return:
	 * <ul>
	 *   <li>TINDERBOX → MISSING (red chip)</li>
	 *   <li>PYRE_LOGS → IN_BANK (white chip, bank tooltip)</li>
	 *   <li>SHADE_REMAINS → IN_BANK (white chip, bank tooltip)</li>
	 * </ul>
	 */
	@Test
	public void chipPanel_bankContainsTwoItems_twoInBankOneRed()
	{
		Item[] bankItems = {
			new Item(PYRE_LOGS, 10),
			new Item(SHADE_REMAINS, 5)
		};
		when(bankContainer.getItems()).thenReturn(bankItems);
		bankState.scanBank(bankContainer);

		GuidanceStep step = stepWith(Arrays.asList(TINDERBOX, PYRE_LOGS, SHADE_REMAINS));
		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals(3, rows.size());

		RequiredItemDisplay tinderbox = rows.get(0);
		RequiredItemDisplay pyreLogs = rows.get(1);
		RequiredItemDisplay shadeRemains = rows.get(2);

		assertEquals( Status.MISSING, tinderbox.getStatus(),"Tinderbox not in bank must be MISSING");
		assertEquals( Status.IN_BANK, pyreLogs.getStatus(),"Pyre logs in bank must be IN_BANK");
		assertEquals( Status.IN_BANK, shadeRemains.getStatus(),"Shade remains in bank must be IN_BANK");

		// Chip styling
		assertEquals(
			RequiredItemDisplay.COLOR_MISSING,
			RequiredItemsChipPanel.borderColorFor(tinderbox.getStatus()),"MISSING chip border must be red");
		assertEquals(
			Color.WHITE,
			RequiredItemsChipPanel.borderColorFor(pyreLogs.getStatus()),"IN_BANK chip border must be white");
		assertEquals(
			RequiredItemsChipPanel.IN_BANK_TOOLTIP,
			RequiredItemsChipPanel.tooltipFor(pyreLogs),"IN_BANK chip tooltip must be Quest Helper bank message");
		assertEquals(
			RequiredItemsChipPanel.IN_BANK_TOOLTIP,
			RequiredItemsChipPanel.tooltipFor(shadeRemains),"IN_BANK chip tooltip must be Quest Helper bank message");
	}

	/**
	 * Inventory presence takes priority over bank scan. An item held in the inventory
	 * must resolve to HELD (green), not IN_BANK (white), even when the same item is
	 * also in the bank.
	 */
	@Test
	public void chipPanel_inventoryPriorityOverBank_heldTakesPrecedence() throws Exception
	{
		// Bank has all three items
		Item[] bankItems = {
			new Item(TINDERBOX, 1),
			new Item(PYRE_LOGS, 10),
			new Item(SHADE_REMAINS, 5)
		};
		when(bankContainer.getItems()).thenReturn(bankItems);
		bankState.scanBank(bankContainer);

		// Inventory also has TINDERBOX
		Constructor<PlayerInventoryState> invCtor =
			PlayerInventoryState.class.getDeclaredConstructor();
		invCtor.setAccessible(true);
		PlayerInventoryState invState = invCtor.newInstance();

		ItemContainer invContainer = Mockito.mock(ItemContainer.class);
		when(invContainer.getItems()).thenReturn(new Item[]{new Item(TINDERBOX, 1)});
		invState.scanInventory(invContainer);

		Constructor<RequiredItemResolver> resolverCtor2 =
			RequiredItemResolver.class.getDeclaredConstructor(
				PlayerInventoryState.class, PlayerBankState.class, ItemManager.class);
		resolverCtor2.setAccessible(true);
		RequiredItemResolver resolverWithInv = resolverCtor2.newInstance(invState, bankState, itemManager);
		resolverWithInv.setCoordinator(coordinator);

		GuidanceStep step = stepWith(Arrays.asList(TINDERBOX, PYRE_LOGS, SHADE_REMAINS));
		List<RequiredItemDisplay> rows = resolverWithInv.resolve(step);

		assertEquals(3, rows.size());
		assertEquals( Status.HELD, rows.get(0).getStatus(),"Tinderbox in inventory must be HELD");
		assertEquals( Status.IN_BANK, rows.get(1).getStatus(),"Pyre logs only in bank must be IN_BANK");
		assertEquals( Status.IN_BANK, rows.get(2).getStatus(),"Shade remains only in bank must be IN_BANK");

		assertEquals(
			RequiredItemDisplay.COLOR_HELD,
			RequiredItemsChipPanel.borderColorFor(rows.get(0).getStatus()),"HELD chip border must be green");
	}

	/**
	 * After scanning an empty bank, all items remain MISSING.
	 * Confirms that an empty bank scan does not corrupt the item-ID set.
	 */
	@Test
	public void chipPanel_emptyBankScan_allItemsMissing()
	{
		when(bankContainer.getItems()).thenReturn(new Item[0]);
		bankState.scanBank(bankContainer);
		assertFalse(
			bankState.hasBankItemData(),"hasBankItemData must be false after empty scan");

		GuidanceStep step = stepWith(Arrays.asList(TINDERBOX, PYRE_LOGS));
		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals(2, rows.size());
		assertEquals(Status.MISSING, rows.get(0).getStatus());
		assertEquals(Status.MISSING, rows.get(1).getStatus());
	}

	/**
	 * Rescan replaces the previous bank state. After an initial scan that contains
	 * TINDERBOX, a second scan with only PYRE_LOGS must mark TINDERBOX as MISSING.
	 */
	@Test
	public void chipPanel_rescanReplacesPreviousState()
	{
		// First scan: bank has TINDERBOX
		when(bankContainer.getItems()).thenReturn(new Item[]{new Item(TINDERBOX, 1)});
		bankState.scanBank(bankContainer);
		assertTrue( bankState.hasItem(TINDERBOX),"TINDERBOX must be in bank after first scan");

		// Second scan: bank has only PYRE_LOGS
		when(bankContainer.getItems()).thenReturn(new Item[]{new Item(PYRE_LOGS, 1)});
		bankState.scanBank(bankContainer);

		assertFalse(
			bankState.hasItem(TINDERBOX),"After rescan, TINDERBOX must no longer be in bank");
		assertTrue( bankState.hasItem(PYRE_LOGS),"After rescan, PYRE_LOGS must be in bank");

		GuidanceStep step = stepWith(Arrays.asList(TINDERBOX, PYRE_LOGS));
		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals(Status.MISSING, rows.get(0).getStatus());
		assertEquals(Status.IN_BANK, rows.get(1).getStatus());
		assertEquals(
			Color.WHITE,
			RequiredItemsChipPanel.borderColorFor(rows.get(1).getStatus()),"IN_BANK chip border must be white");
	}

	// ── Helpers ─────────────────────────────────────────────────────────────

	private static GuidanceStep stepWith(List<Integer> requiredItemIds)
	{
		return new GuidanceStep(
			"Test step",
			null, // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null,
			requiredItemIds,
			null,  // perItemRequiredItemIds
			null,  // recommendedItemIds
			null,  // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,
			null,
			0, null, null,
			null,
			null,
			null,
			0, 0,
			false,
			0,
			null,
			null,
			0, 0,
			null, // skipIfHasAnyItemIds
			null, // dynamicItemObjectTiers
			null, // completionZone
			null, // conditionalAlternatives
			null, // section
			null, // waypoints
			null,  // dynamicTargetEvaluator
			null,  // conditionTree
						null, // perItemStepPriority
						null  // activityObtainableItemIds
		
			, null // restockIfMissingAllItemIds (#719)
		);
	}
}
