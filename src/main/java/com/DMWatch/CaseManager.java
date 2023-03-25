package com.DMWatch;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class CaseManager
{
	private static final HttpUrl DMWatch_LIST_URL = HttpUrl.parse("https://raw.githubusercontent.com/DMWatch/DMWatch/main/data/mixedlist.json");

	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private static final Type typeToken = new TypeToken<List<Case>>()
	{
	}.getType();
	final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
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
	private final OkHttpClient client;
	private final Map<String, Case> dmCases = new ConcurrentHashMap<>();
	private final Map<Long, Case> dmCasesHash = new ConcurrentHashMap<>();
	private final ClientThread clientThread;
	private final DMWatchConfig config;

	@Inject
	private CaseManager(OkHttpClient client, ClientThread clientThread, DMWatchConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
	}

	/**
	 * @param onComplete called once the list has been refreshed. Called on the client thread
	 */
	public void refresh(Runnable onComplete)
	{
		Request rwReq = new Request.Builder().url(DMWatch_LIST_URL).build();

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
						List<Case> cases = GSON.fromJson(new InputStreamReader(response.body().byteStream()), typeToken);
						dmCases.clear();
						for (Case c : cases)
						{
							String rsn = c.getRsn().toLowerCase();
							Map<String, Case> sourceCases = dmCases;

							Case old = sourceCases.get(rsn);
							// keep the newest case
							if (old == null || old.getDate().before(c.getDate()))
							{
								sourceCases.put(rsn, c);
							}
						}
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
		String cleanRsn = Text.removeTags(Text.toJagexName(rsn)).toLowerCase();

		Case c = dmCases.get(cleanRsn);
		if (c != null)
		{
			return c;
		}

		return null;
	}

	public Case getByAccountHash(String hashID)
	{
		for (String s : dmCases.keySet())
		{
			if (dmCases.get(s).getAccountHash().equals(hashID))
			{
				return dmCases.get(s);
			}
		}
		return null;
	}

	public Case getByHWID(String hwid)
	{
		for (String s : dmCases.keySet())
		{
			if (dmCases.get(s).getHardwareID().equals(hwid))
			{
				return dmCases.get(s);
			}
		}
		return null;
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
