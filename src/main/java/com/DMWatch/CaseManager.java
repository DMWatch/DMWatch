package com.DMWatch;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class CaseManager
{
	private static final HttpUrl DMWatch_LIST_URL_DEFAULT = HttpUrl.parse("https://dm.watch/api/mixedlist.json");

	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private static final Type typeToken = new TypeToken<List<Case>>()
	{
	}.getType();

	private final OkHttpClient client;
	private final DMWatchConfig config;
	private final Queue<Case> dmCases = new ConcurrentLinkedQueue<>();
	private ConcurrentHashMap<String, HashSet<String>> mappings = new ConcurrentHashMap<>();
	private final ClientThread clientThread;
	private final Gson gson;

	@Inject
	private CaseManager(OkHttpClient client, ClientThread clientThread, Gson gson, DMWatchConfig config)
	{
		this.client = client;
		this.config = config;
		this.clientThread = clientThread;
		this.gson = gson.newBuilder().registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
			try
			{
				// Allow handling of the occasional empty string as a date.
				return df.parse(json.getAsString());
			}
			catch (ParseException e)
			{
				return Date.from(Instant.ofEpochSecond(0));
			}
		}).create();
	}

	/**
	 * @param onComplete called once the list has been refreshed. Called on the client thread
	 */
	public void refresh(Runnable onComplete)
	{
		Request rwReq = new Request.Builder().url(DMWatch_LIST_URL_DEFAULT).build();

		// call on background thread
		client.newCall(rwReq).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("failed to get DMWatch list: {}", e.toString());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					if (response.code() < 200 || response.code() >= 400)
					{
						log.error("failed to get watchlist. status: {}", response.code());
					}
					else
					{
						List<Case> cases = gson.fromJson(new InputStreamReader(response.body().byteStream()), typeToken);
						dmCases.clear();
						ConcurrentHashMap<String, HashSet<String>> localMappings = new ConcurrentHashMap<>();
						for (Case c : cases)
						{
							if (localMappings.containsKey(c.getStatus()))
							{
								HashSet<String> currentSet = localMappings.get(c.getStatus());
								currentSet.add(c.getNiceRSN());
								localMappings.put(c.getStatus(), currentSet);
							}
							else {
								HashSet<String> currentSet = new HashSet();
								currentSet.add(c.getNiceRSN());
								localMappings.put(c.getStatus(), currentSet);
							}
							dmCases.add(c);
						}
						mappings = localMappings;
						log.debug("saved {}/{} dm cases", dmCases.size(), cases.size());
					}
				}
				finally
				{
					response.close();
					if (onComplete != null)
					{
						clientThread.invokeLater(onComplete);
					}
				}
			}
		});
	}

	/**
	 * Get a DMWatch case from the cached list
	 *
	 * @param rsn
	 * @return
	 */
	public Case get(String rsn)
	{
		if (dmCases.size() == 0) return null;
		String cleanRsn = Text.removeTags(Text.toJagexName(rsn)).toLowerCase();
		Optional<Case> foundCase = dmCases.stream().filter(c -> c.getNiceRSN().equals(cleanRsn)).findFirst();

		if (foundCase.isPresent()) {
			return foundCase.get();
		} else {
			return null;
		}
	}

	public Case getByAccountHash(String hashID)
	{
		if (dmCases.size() == 0) return null;
		Optional<Case> foundCase = dmCases.stream().filter(c -> c.getAccountHash().equals(hashID)).findFirst();

		if (foundCase.isPresent()) {
			return foundCase.get();
		} else {
			return null;
		}
	}

	public Case getByHWID(String hwid)
	{
		if (dmCases.size() == 0) return null;
		Optional<Case> foundCase = dmCases.stream().filter(c -> c.getHardwareID().equals(hwid)).findFirst();

		if (foundCase.isPresent()) {
			return foundCase.get();
		} else {
			return null;
		}
	}

	public int getListSize()
	{
		return dmCases.size();
	}

	public Queue<Case> getList()
	{
		return dmCases;
	}

	public ConcurrentHashMap<String, HashSet<String>> getMappings()
	{
		return mappings;
	}

	/**
	 * Lookup a non-cached DMWatch case.
	 *
	 * @param rsn
	 * @param onComplete function returning the Case (mullable) if found. Called on the client thread
	 * @return
	 */
	public void get(String rsn, Consumer<Case> onComplete)
	{
		refresh(() -> onComplete.accept(get(rsn)));
	}
}
