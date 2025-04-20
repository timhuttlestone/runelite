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

package net.runelite.client.plugins.flippingutilities.db;

import net.runelite.client.plugins.flippingutilities.model.*;
import net.runelite.client.plugins.flippingutilities.ui.uiutilities.TimeFormatters;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for handling all the IO related tasks for persisting trades. This class should contain
 * any logic that pertains to reading/writing to disk. This includes logic related to whether it should reload things
 * again, etc.
 */
@Slf4j
public class TradePersister
{
	Gson gson;

	public TradePersister(Gson gson) {
		this.gson = gson;
	}

	//this is in {user's home directory}/.runelite/flipping
	public static final File PARENT_DIRECTORY = new File(RuneLite.RUNELITE_DIR, "flipping");
	public static final File OLD_FILE = new File(PARENT_DIRECTORY, "trades.json");

	/**
	 * Creates flipping directory if it doesn't exist and partitions trades.json into individual files
	 * for each account, if it exists.
	 *
	 * @throws IOException handled in FlippingPlugin
	 */
	public static void setupFlippingFolder() throws IOException
	{
		if (!PARENT_DIRECTORY.exists())
		{
			log.info("flipping directory doesn't exist yet so it's being created");
			if (!PARENT_DIRECTORY.mkdir())
			{
				throw new IOException("unable to create parent directory!");
			}
		}
		else
		{
			log.info("flipping directory already exists so it's not being created");
			if (OLD_FILE.exists())
			{
				OLD_FILE.delete();

			}
		}
	}

	/**
	 * loads each account's data from the parent directory located at {user's home directory}/.runelite/flipping/
	 * Each account's data is stored in separate file in that directory and is named {displayName}.json
	 *
	 * Why not use loadAccount(displayName) in this method? Here we have access to the files first so we can call
	 * loadFromFile directly. rather than getting the display name from the file and then calling
	 * loadAccount
	 *
	 * @return a map of display name to that account's data
	 * @throws IOException handled in FlippingPlugin
	 */
	public Map<String, AccountData> loadAllAccounts()
	{
		Map<String, AccountData> accountsData = new HashMap<>();
		for (File f : PARENT_DIRECTORY.listFiles())
		{
			if (f.getName().equals("accountwide.json") || !f.getName().contains(".json") || f.getName().contains(".backup.json") || f.getName().contains(".special.json")) {
				continue;
			}
			String displayName = f.getName().split("\\.")[0];
			AccountData accountData  = loadAccount(displayName);
			accountsData.put(displayName, accountData);
		}

		return accountsData;
	}

	//anything that wants to load an account's data MUST go through this method as it handles various cases such as
	//loading from backups
	public AccountData loadAccount(String displayName)
	{
		log.info("loading data for {}", displayName);
		try {
			File accountFile = new File(PARENT_DIRECTORY, displayName + ".json");
			AccountData accountData = loadFromFile(accountFile);
			if (accountData == null)
			{
				log.warn("data for {} is null for some reason. Will try loading from backup", displayName);
				accountData = loadAccountFromBackup(displayName);
			}
			return accountData;
		}
		catch (Exception e) {
			log.warn("Got exception {} while loading data for {}. Will try loading from backup", e, displayName);
			return loadAccountFromBackup(displayName);
		}
	}

	private AccountData loadAccountFromBackup(String displayName) {
		log.info("loading data for {} from backup", displayName);
		try {
			File accountFile = new File(PARENT_DIRECTORY, displayName + ".backup.json");
			if (!accountFile.exists()) {
				log.info("backup for {} does not exist, returning empty AccountData", displayName);
				return new AccountData();
			}
			AccountData accountData = loadFromFile(accountFile);
			if (accountData == null) {
				log.info("data loaded from backup for {} is null for some reason, returning an empty AccountData object", displayName);
				accountData = new AccountData();
			}
			return accountData;
		}
		catch (Exception e) {
			log.info("Couldn't load data for {} from backup due to {}", displayName, e);
			return new AccountData();
		}
	}

	private AccountData loadFromFile(File f) throws IOException
	{
		String accountDataJson = new String(Files.readAllBytes(f.toPath()));
		return gson.fromJson(accountDataJson, AccountData.class);
	}

	public AccountWideData loadAccountWideData() throws IOException {
		File accountFile = new File(PARENT_DIRECTORY, "accountwide.json");
		if (accountFile.exists()){
			String accountWideDataJson = new String(Files.readAllBytes(accountFile.toPath()));
			Type type = new TypeToken<AccountWideData>(){}.getType();
			return gson.fromJson(accountWideDataJson, type);
		}
		else {
			return new AccountWideData();
		}
	}

	public BackupCheckpoints fetchBackupCheckpoints() {
		try {
			log.info("Fetching backup checkpoints");
			File backupCheckpointsFile = new File(PARENT_DIRECTORY, "backupcheckpoints.special.json");
			if (backupCheckpointsFile.exists()){
				String backupCheckpointsJson = new String(Files.readAllBytes(backupCheckpointsFile.toPath()));
				Type type = new TypeToken<BackupCheckpoints>(){}.getType();
				return gson.fromJson(backupCheckpointsJson, type);
			}
			else {
				return new BackupCheckpoints();
			}
		}
		catch (Exception e) {
			return new BackupCheckpoints();
		}
	}

	/**
	 * stores trades for an account in {user's home directory}/.runelite/flipping/{account's display name}.json
	 *
	 * @param displayName display name of the account the data is associated with
	 * @param data        the trades and last offers of that account
	 * @throws IOException
	 */
	public void writeToFile(String displayName, Object data) throws IOException
	{
		log.info("Writing to file for {}", displayName);
		File accountFile = new File(PARENT_DIRECTORY, displayName + ".json");
		final String json = gson.toJson(data);
		Files.write(accountFile.toPath(), json.getBytes());
	}

	public static long lastModified(String fileName)
	{
		return new File(PARENT_DIRECTORY, fileName).lastModified();
	}

	public static void deleteFile(String fileName)
	{
		File accountFile = new File(PARENT_DIRECTORY, fileName);
		if (accountFile.exists())
		{
			if (accountFile.delete())
			{
				log.info("{} deleted", fileName);
			}
			else
			{
				log.info("unable to delete {}", fileName);
			}
		}
	}

	public static void exportToCsv(File file, List<FlippingItem> trades, String startOfIntervalName) throws IOException {
		FileWriter out = new FileWriter(file);
		CSVPrinter csvWriter = new CSVPrinter(out,
				CSVFormat.DEFAULT.
						withHeader("name", "date", "quantity", "price", "state").
						withCommentMarker('#').
						withHeaderComments("Displaying trades for selected time interval: " + startOfIntervalName));

		for (FlippingItem item : trades) {
			for (OfferEvent offer : item.getHistory().getCompressedOfferEvents()) {
				csvWriter.printRecord(
						item.getItemName(),
						TimeFormatters.formatInstantToDate(offer.getTime()),
						offer.getCurrentQuantityInTrade(),
						offer.getPrice(),
						offer.getState()
				);
			}
			csvWriter.printComment(String.format("Total profit: %d", FlippingItem.getProfit(item.getHistory().getCompressedOfferEvents())));
			csvWriter.println();
		}
		csvWriter.close();
	}
}
