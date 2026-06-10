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
package com.collectionloghelper.ui.preview;

import com.collectionloghelper.AfkFilter;
import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.EfficientSortMode;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import com.collectionloghelper.learning.DryStreakAnalyzer;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.collectionloghelper.ui.CollectionLogHelperPanel.Mode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * Builds a fully wired {@link CollectionLogHelperPanel} from realistic sample
 * data for the test-scoped preview/snapshot harness. Nothing here ships in the
 * plugin jar; it exists only so the side panel can be rendered headless.
 *
 * <p>Construction strategy:
 * <ul>
 *   <li>{@code CollectionLogHelperConfig} and the leaf RuneLite services
 *       ({@code Client}, {@code ConfigManager}, {@code ClientThread}, and a few
 *       deep internal collaborators) are Mockito mocks with sensible stubs.</li>
 *   <li>{@code ItemManager} is a mock whose {@code getImage(...)} calls return a
 *       non-null placeholder so the panel never NPEs on a missing icon.</li>
 *   <li>The project POJOs ({@code DropRateDatabase}, {@code PlayerCollectionState},
 *       calculators, checkers, state holders) are real instances, instantiated
 *       reflectively because most have package/private {@code @Inject}
 *       constructors. A shared object pool feeds the same real instances into
 *       every constructor that wants them.</li>
 * </ul>
 */
public final class PanelPreviewFixtures
{
	private PanelPreviewFixtures()
	{
	}

	/** Knobs a scenario can vary. */
	public static final class Scenario
	{
		private Mode mode = Mode.EFFICIENT;
		private boolean slayerTaskActive = false;
		private boolean longLabels = false;
		private CollectionLogCategory focusCategory = null;

		public Scenario mode(Mode m)
		{
			this.mode = m;
			return this;
		}

		public Scenario slayerTaskActive(boolean active)
		{
			this.slayerTaskActive = active;
			return this;
		}

		public Scenario longLabels(boolean on)
		{
			this.longLabels = on;
			return this;
		}

		/**
		 * Drives the panel into Category Focus mode on the given category via the
		 * real selector path (mirrors a user picking a category). Overrides
		 * {@link #mode(Mode)} for the focus-mode scenarios.
		 */
		public Scenario focusCategory(CollectionLogCategory category)
		{
			this.focusCategory = category;
			this.mode = Mode.CATEGORY_FOCUS;
			return this;
		}
	}

	public static Scenario scenario()
	{
		return new Scenario();
	}

	/**
	 * Builds the panel for the given scenario. Must be called on the EDT (the
	 * snapshot test wraps it in invokeAndWait).
	 */
	public static CollectionLogHelperPanel buildPanel(Scenario scenario) throws Exception
	{
		Map<Class<?>, Object> pool = new HashMap<>();

		// --- Leaf RuneLite mocks -------------------------------------------
		Client client = Mockito.mock(Client.class);
		ConfigManager configManager = Mockito.mock(ConfigManager.class);
		ClientThread clientThread = Mockito.mock(ClientThread.class);
		pool.put(Client.class, client);
		pool.put(ConfigManager.class, configManager);
		pool.put(ClientThread.class, clientThread);

		// --- Config mock ---------------------------------------------------
		CollectionLogHelperConfig config = Mockito.mock(CollectionLogHelperConfig.class);
		Mockito.when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		Mockito.when(config.efficientSortMode()).thenReturn(EfficientSortMode.EFFICIENCY);
		Mockito.when(config.hideObtainedItems()).thenReturn(false);
		Mockito.when(config.hideLockedContent()).thenReturn(false);
		Mockito.when(config.enableTempleOsrsSync()).thenReturn(true);
		pool.put(CollectionLogHelperConfig.class, config);

		// --- ItemManager mock: never return a null icon -------------------
		ItemManager itemManager = Mockito.mock(ItemManager.class);
		Answer<AsyncBufferedImage> iconAnswer = inv ->
			new AsyncBufferedImage(clientThread, 32, 32, BufferedImage.TYPE_INT_ARGB);
		Mockito.when(itemManager.getImage(Mockito.anyInt())).thenAnswer(iconAnswer);
		Mockito.when(itemManager.getImage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean()))
			.thenAnswer(iconAnswer);
		pool.put(ItemManager.class, itemManager);

		// --- Real DropRateDatabase (loads drop_rates.json from resources) --
		DropRateDatabase database = new DropRateDatabase();
		Gson gson = new GsonBuilder().create();
		setField(database, "gson", gson);
		database.load();
		pool.put(DropRateDatabase.class, database);

		// --- Real POJOs constructed reflectively, sharing the pool ---------
		PlayerCollectionState collectionState = construct(PlayerCollectionState.class, pool);
		pool.put(PlayerCollectionState.class, collectionState);
		seedCollectionState(collectionState, database);

		ClueCompletionEstimator clueEstimator = construct(ClueCompletionEstimator.class, pool);
		pool.put(ClueCompletionEstimator.class, clueEstimator);

		PlayerInventoryState inventoryState = construct(PlayerInventoryState.class, pool);
		pool.put(PlayerInventoryState.class, inventoryState);

		PlayerBankState bankState = construct(PlayerBankState.class, pool);
		pool.put(PlayerBankState.class, bankState);

		DataSyncState dataSyncState = construct(DataSyncState.class, pool);
		pool.put(DataSyncState.class, dataSyncState);

		SlayerTaskState slayerTaskState = construct(SlayerTaskState.class, pool);
		pool.put(SlayerTaskState.class, slayerTaskState);
		if (scenario.slayerTaskActive)
		{
			seedSlayerTask(slayerTaskState, scenario.longLabels);
		}

		RequirementsChecker requirementsChecker = construct(RequirementsChecker.class, pool);
		pool.put(RequirementsChecker.class, requirementsChecker);

		SlayerStrategyCalculator slayerStrategyCalculator =
			construct(SlayerStrategyCalculator.class, pool);
		pool.put(SlayerStrategyCalculator.class, slayerStrategyCalculator);

		EfficiencyCalculator calculator = construct(EfficiencyCalculator.class, pool);
		pool.put(EfficiencyCalculator.class, calculator);

		DryStreakAnalyzer dryStreakAnalyzer = construct(DryStreakAnalyzer.class, pool);
		pool.put(DryStreakAnalyzer.class, dryStreakAnalyzer);

		// --- No-op callbacks ----------------------------------------------
		BiConsumer<CollectionLogSource, Integer> guidanceActivator = (s, i) -> { };
		Runnable guidanceDeactivator = () -> { };
		Consumer<AfkFilter> afkFilterUpdater = f -> { };
		Consumer<EfficientSortMode> sortModeUpdater = m -> { };

		CollectionLogHelperPanel panel = new CollectionLogHelperPanel(
			config, database, collectionState, calculator, clueEstimator, itemManager,
			requirementsChecker, dataSyncState, slayerTaskState, slayerStrategyCalculator,
			inventoryState, bankState, dryStreakAnalyzer,
			guidanceActivator, guidanceDeactivator, afkFilterUpdater, sortModeUpdater);

		if (scenario.focusCategory != null)
		{
			// Real selector path: sets the category and flips to Category Focus,
			// which is the only context where the slayer advisor should surface.
			panel.switchToCategoryFocus(scenario.focusCategory);
		}
		else
		{
			panel.setMode(scenario.mode);
		}
		return panel;
	}

	// ------------------------------------------------------------------------
	// Sample-data seeding
	// ------------------------------------------------------------------------

	/**
	 * Marks a handful of items obtained and seeds completion-header varp fields
	 * so the rendered panel shows realistic counts and a partial progress bar.
	 */
	private static void seedCollectionState(PlayerCollectionState state, DropRateDatabase database)
		throws Exception
	{
		int marked = 0;
		for (CollectionLogSource source : database.getAllSources())
		{
			if (marked >= 25)
			{
				break;
			}
			// Mark the first item of each of the first sources as obtained.
			if (!source.getItems().isEmpty())
			{
				state.markItemObtained(source.getItems().get(0).getItemId());
				marked++;
			}
		}
		// Cached varp totals drive the completion header / progress bar.
		setField(state, "totalObtained", 743);
		setField(state, "totalPossible", 1699);
		setField(state, "bossesCount", 210);
		setField(state, "bossesMax", 480);
		setField(state, "cluesCount", 180);
		setField(state, "cluesMax", 360);
	}

	/**
	 * Seeds an active Slayer task directly into the (otherwise client-driven)
	 * volatile fields so the SLAYER scenario renders the strategy view.
	 */
	private static void seedSlayerTask(SlayerTaskState state, boolean longLabels) throws Exception
	{
		String creature = longLabels
			? "Greater demons in the Catacombs of Kourend extension wing"
			: "Greater demons";
		setField(state, "creatureName", creature);
		setField(state, "remaining", 134);
		setField(state, "taskActive", true);
	}

	// ------------------------------------------------------------------------
	// Reflective instantiation
	// ------------------------------------------------------------------------

	/**
	 * Instantiates {@code type} via its (possibly private) declared constructor,
	 * pulling each parameter from {@code pool}. Any parameter not present in the
	 * pool is supplied as a fresh Mockito mock — covering deep internal
	 * collaborators (e.g. SlayerMasterDatabase, PohTeleportInventory) that the
	 * panel never exercises during a headless paint.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T construct(Class<T> type, Map<Class<?>, Object> pool) throws Exception
	{
		Constructor<?>[] ctors = type.getDeclaredConstructors();
		Constructor<?> chosen = ctors[0];
		for (Constructor<?> c : ctors)
		{
			if (c.getParameterCount() >= chosen.getParameterCount())
			{
				chosen = c;
			}
		}
		chosen.setAccessible(true);

		Class<?>[] paramTypes = chosen.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++)
		{
			Object provided = pool.get(paramTypes[i]);
			if (provided != null)
			{
				args[i] = provided;
			}
			else
			{
				Object mock = Mockito.mock(paramTypes[i]);
				pool.put(paramTypes[i], mock);
				args[i] = mock;
			}
		}
		return (T) chosen.newInstance(args);
	}

	private static void setField(Object target, String name, Object value) throws Exception
	{
		Field f = findField(target.getClass(), name);
		f.setAccessible(true);
		f.set(target, value);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException
	{
		Class<?> c = type;
		while (c != null)
		{
			try
			{
				return c.getDeclaredField(name);
			}
			catch (NoSuchFieldException ignored)
			{
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name + " on " + type.getName());
	}
}
