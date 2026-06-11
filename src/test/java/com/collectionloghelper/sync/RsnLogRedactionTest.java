/*
 * Copyright (c) 2026, cha-ndler
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
package com.collectionloghelper.sync;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.collectionloghelper.AccountType;
import com.collectionloghelper.AfkFilter;
import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.RaidTeamSize;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerMasterDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.learning.KillTimeTracker;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Privacy regression tests: no CLH log line may contain the logged-in
 * player's name (checklist T0.7 — Plugin Hub privacy gate).
 *
 * <p>Captures every event logged under the {@code com.collectionloghelper}
 * logger tree with a logback {@link ListAppender} while driving the code
 * paths that previously embedded the RSN (export path, sync fetch URL,
 * sync orchestration, data-directory init), then asserts the formatted
 * messages are clean.
 *
 * <p>The per-player data <em>directory name</em> itself is ecosystem-standard
 * and intentionally out of scope — only log output is asserted here.
 */
public class RsnLogRedactionTest
{
	/**
	 * Stand-in RSN. Deliberately unusual so an accidental substring match
	 * cannot come from anything but a leak.
	 */
	private static final String RSN = "RedactTestRsn77";

	private Logger clhLogger;
	private ListAppender<ILoggingEvent> appender;
	private ScheduledExecutorService syncerExecutor;
	private OkHttpClient httpClient;

	@BeforeEach
	public void attachAppender()
	{
		clhLogger = (Logger) LoggerFactory.getLogger("com.collectionloghelper");
		appender = new ListAppender<>();
		appender.start();
		clhLogger.addAppender(appender);
	}

	@AfterEach
	public void detachAppender()
	{
		clhLogger.detachAppender(appender);
		if (syncerExecutor != null)
		{
			syncerExecutor.shutdownNow();
		}
	}

	private void assertNoRsnLogged()
	{
		for (ILoggingEvent event : appender.list)
		{
			String msg = event.getFormattedMessage();
			assertFalse(msg.contains(RSN),
				"CLH log line leaks the player name: \"" + msg + "\"");
			// Attached throwables are written to client.log too — an IO
			// exception message embeds the full per-player path.
			for (IThrowableProxy t = event.getThrowableProxy(); t != null; t = t.getCause())
			{
				String thrown = t.getMessage();
				assertFalse(thrown != null && thrown.contains(RSN),
					"CLH logged throwable leaks the player name: \"" + thrown + "\"");
			}
		}
	}

	// ========================================================================
	// EfficiencyCalculator — export log line embeds the per-player path
	// ========================================================================

	@Test
	@DisplayName("efficiency export logs no player name even though the path embeds it")
	public void efficiencyExport_logsNoRsn(@TempDir Path tempDir) throws IOException
	{
		DropRateDatabase database = mock(DropRateDatabase.class);
		when(database.getAllSources()).thenReturn(Collections.emptyList());
		PlayerCollectionState collectionState = mock(PlayerCollectionState.class);
		RequirementsChecker requirementsChecker = mock(RequirementsChecker.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);
		when(config.hideLockedContent()).thenReturn(false);
		when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);
		when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		when(config.accountType()).thenReturn(AccountType.MAIN);
		ClueCompletionEstimator clueEstimator = mock(ClueCompletionEstimator.class);
		SlayerTaskState slayerTaskState = mock(SlayerTaskState.class);
		SlayerMasterDatabase slayerMasterDatabase = mock(SlayerMasterDatabase.class);

		EfficiencyCalculator calculator;
		try
		{
			Constructor<EfficiencyCalculator> ctor = EfficiencyCalculator.class.getDeclaredConstructor(
				DropRateDatabase.class, PlayerCollectionState.class,
				RequirementsChecker.class, CollectionLogHelperConfig.class,
				ClueCompletionEstimator.class, SlayerTaskState.class,
				SlayerMasterDatabase.class);
			ctor.setAccessible(true);
			calculator = ctor.newInstance(database, collectionState, requirementsChecker,
				config, clueEstimator, slayerTaskState, slayerMasterDatabase);
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}

		// The real export path is <data dir>/<player name>/efficiency-export.txt
		File playerDir = Files.createDirectories(tempDir.resolve(RSN)).toFile();
		File outputFile = new File(playerDir, "efficiency-export.txt");

		calculator.exportEfficiencyList(outputFile, null);

		assertTrue(outputFile.exists(), "export should still be written");
		assertNoRsnLogged();
	}

	@Test
	@DisplayName("efficiency export IO failure logs no player name via the throwable")
	public void efficiencyExportFailure_logsNoRsn(@TempDir Path tempDir) throws IOException
	{
		DropRateDatabase database = mock(DropRateDatabase.class);
		when(database.getAllSources()).thenReturn(Collections.emptyList());
		PlayerCollectionState collectionState = mock(PlayerCollectionState.class);
		RequirementsChecker requirementsChecker = mock(RequirementsChecker.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);
		when(config.hideLockedContent()).thenReturn(false);
		when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);
		when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		when(config.accountType()).thenReturn(AccountType.MAIN);
		ClueCompletionEstimator clueEstimator = mock(ClueCompletionEstimator.class);
		SlayerTaskState slayerTaskState = mock(SlayerTaskState.class);
		SlayerMasterDatabase slayerMasterDatabase = mock(SlayerMasterDatabase.class);

		EfficiencyCalculator calculator;
		try
		{
			Constructor<EfficiencyCalculator> ctor = EfficiencyCalculator.class.getDeclaredConstructor(
				DropRateDatabase.class, PlayerCollectionState.class,
				RequirementsChecker.class, CollectionLogHelperConfig.class,
				ClueCompletionEstimator.class, SlayerTaskState.class,
				SlayerMasterDatabase.class);
			ctor.setAccessible(true);
			calculator = ctor.newInstance(database, collectionState, requirementsChecker,
				config, clueEstimator, slayerTaskState, slayerMasterDatabase);
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}

		// Writing to a directory makes FileWriter throw an IOException whose
		// message embeds the RSN-bearing path.
		File playerDir = Files.createDirectories(tempDir.resolve(RSN)).toFile();

		calculator.exportEfficiencyList(playerDir, null);

		assertNoRsnLogged();
	}

	// ========================================================================
	// TempleOsrsKcSyncer — fetch URL / username in every doSync branch
	// ========================================================================

	private TempleOsrsKcSyncer newSyncer(boolean syncEnabled)
	{
		httpClient = mock(OkHttpClient.class);
		syncerExecutor = Executors.newSingleThreadScheduledExecutor();
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);
		when(config.enableTempleOsrsSync()).thenReturn(syncEnabled);
		return new TempleOsrsKcSyncer(httpClient, new Gson(), syncerExecutor, config);
	}

	private void mockHttpResponse(int code, String body, long contentLength) throws IOException
	{
		Call call = mock(Call.class);
		when(httpClient.newCall(any(Request.class))).thenReturn(call);
		Request dummyRequest = new Request.Builder()
			.url(TempleOsrsKcSyncer.BASE_URL + "?player=test")
			.build();
		ResponseBody responseBody = mock(ResponseBody.class);
		when(responseBody.contentLength()).thenReturn(contentLength);
		when(responseBody.string()).thenReturn(body);
		Response response = new Response.Builder()
			.request(dummyRequest)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "Error")
			.body(responseBody)
			.build();
		when(call.execute()).thenReturn(response);
	}

	@Test
	@DisplayName("doSync disabled-config branch logs no player name")
	public void doSync_disabled_logsNoRsn()
	{
		TempleOsrsKcSyncer syncer = newSyncer(false);

		SyncResult result = syncer.doSync(RSN);

		assertFalse(result.isSuccess());
		assertNoRsnLogged();
	}

	@Test
	@DisplayName("doSync fetch logs neither player name nor the URL embedding it")
	public void doSync_fetch_logsNoRsn() throws IOException
	{
		TempleOsrsKcSyncer syncer = newSyncer(true);
		mockHttpResponse(200, "{\"data\": {\"Zulrah\": 1}}", 30);

		SyncResult result = syncer.doSync(RSN);

		assertTrue(result.isSuccess());
		assertNoRsnLogged();
	}

	@Test
	@DisplayName("doSync HTTP-error branch leaks no player name in logs or failure message")
	public void doSync_httpError_logsNoRsn_andFailureMessageClean() throws IOException
	{
		TempleOsrsKcSyncer syncer = newSyncer(true);
		mockHttpResponse(404, "", 0);

		SyncResult result = syncer.doSync(RSN);

		assertFalse(result.isSuccess());
		// The failure message is re-logged by the orchestrator and posted to
		// the chat box — it must not carry the RSN either.
		assertFalse(result.getErrorMessage().contains(RSN),
			"failure message leaks the player name: " + result.getErrorMessage());
		assertNoRsnLogged();
	}

	@Test
	@DisplayName("doSync oversized-response branch logs no player name")
	public void doSync_oversizedResponse_logsNoRsn() throws IOException
	{
		TempleOsrsKcSyncer syncer = newSyncer(true);
		mockHttpResponse(200, "{}", Long.MAX_VALUE);

		SyncResult result = syncer.doSync(RSN);

		assertFalse(result.isSuccess());
		assertNoRsnLogged();
	}

	@Test
	@DisplayName("doSync network-error branch logs no player name")
	public void doSync_networkError_logsNoRsn() throws IOException
	{
		TempleOsrsKcSyncer syncer = newSyncer(true);
		Call call = mock(Call.class);
		when(httpClient.newCall(any(Request.class))).thenReturn(call);
		when(call.execute()).thenThrow(new IOException("Connection refused"));

		SyncResult result = syncer.doSync(RSN);

		assertFalse(result.isSuccess());
		assertNoRsnLogged();
	}

	// ========================================================================
	// TempleSyncOrchestrator — request + failed-future log lines
	// ========================================================================

	@Test
	@DisplayName("orchestrator request and failed-future paths log no player name")
	public void orchestrator_requestAndFailure_logsNoRsn() throws Exception
	{
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);
		when(config.enableTempleOsrsSync()).thenReturn(true);
		DataModule data = mock(DataModule.class);
		PluginDataManager pluginDataManager = mock(PluginDataManager.class);
		when(data.getPluginDataManager()).thenReturn(pluginDataManager);
		when(pluginDataManager.getCurrentPlayerName()).thenReturn(RSN);
		TempleOsrsKcSyncer syncer = mock(TempleOsrsKcSyncer.class);
		@SuppressWarnings("unchecked")
		Future<SyncResult> failingFuture = mock(Future.class);
		when(failingFuture.get(anyLong(), any())).thenThrow(new RuntimeException("boom"));
		when(syncer.syncKc(RSN)).thenReturn(failingFuture);
		SourceKcStore kcStore = mock(SourceKcStore.class);
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		// Run callbacks inline so the failed-future log line is emitted before
		// the assertion runs.
		doAnswer(inv ->
		{
			((Runnable) inv.getArgument(0)).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		ExecutorService resultExecutor = mock(ExecutorService.class);
		doAnswer(inv ->
		{
			((Runnable) inv.getArgument(0)).run();
			return null;
		}).when(resultExecutor).submit(any(Runnable.class));

		TempleSyncOrchestrator orchestrator = new TempleSyncOrchestrator(
			config, data, syncer, kcStore, client, clientThread);

		orchestrator.requestSync(null, resultExecutor);

		assertNoRsnLogged();
	}

	// ========================================================================
	// KillTimeTracker — load log line embeds the per-player data-file path
	// ========================================================================

	@Test
	@DisplayName("kill-time tracker load logs no player name even though the path embeds it")
	public void killTimeTracker_load_logsNoRsn(@TempDir Path tempDir) throws Exception
	{
		File playerDir = Files.createDirectories(tempDir.resolve(RSN)).toFile();
		File dataFile = new File(playerDir, "kill_times.json");
		Files.write(dataFile.toPath(),
			"{\"Zulrah\": [30, 31, 32]}".getBytes(StandardCharsets.UTF_8));

		Constructor<KillTimeTracker> ctor = KillTimeTracker.class.getDeclaredConstructor(
			CollectionLogHelperConfig.class, DropRateDatabase.class, Gson.class);
		ctor.setAccessible(true);
		KillTimeTracker tracker = ctor.newInstance(
			mock(CollectionLogHelperConfig.class), mock(DropRateDatabase.class), new Gson());

		try
		{
			tracker.init(playerDir);
			assertNoRsnLogged();
		}
		finally
		{
			tracker.reset();
		}
	}

	@Test
	@DisplayName("kill-time tracker load IO failure logs no player name via the throwable")
	public void killTimeTrackerLoadFailure_logsNoRsn(@TempDir Path tempDir) throws Exception
	{
		// A directory named like the data file makes FileReader throw an
		// IOException whose message embeds the RSN-bearing path.
		File playerDir = Files.createDirectories(tempDir.resolve(RSN)).toFile();
		Files.createDirectories(playerDir.toPath().resolve("kill_times.json"));

		Constructor<KillTimeTracker> ctor = KillTimeTracker.class.getDeclaredConstructor(
			CollectionLogHelperConfig.class, DropRateDatabase.class, Gson.class);
		ctor.setAccessible(true);
		KillTimeTracker tracker = ctor.newInstance(
			mock(CollectionLogHelperConfig.class), mock(DropRateDatabase.class), new Gson());

		try
		{
			tracker.init(playerDir);
			assertNoRsnLogged();
		}
		finally
		{
			tracker.reset();
		}
	}

	// ========================================================================
	// PluginDataManager — data-directory-ready log line embeds the path
	// ========================================================================

	@Test
	@DisplayName("data-directory init logs no player name even though the dir embeds it")
	public void pluginDataManager_init_logsNoRsn() throws Exception
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getName()).thenReturn(RSN);

		Constructor<PluginDataManager> ctor =
			PluginDataManager.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		PluginDataManager manager = ctor.newInstance(client);

		File created = null;
		try
		{
			assertTrue(manager.init(), "init should succeed");
			created = manager.getCharacterDir();
			assertNoRsnLogged();
		}
		finally
		{
			// init() resolves against the real RuneLite data dir — remove the
			// throwaway per-test directory it created.
			if (created != null && created.getName().contains(RSN))
			{
				created.delete();
			}
		}
	}
}
