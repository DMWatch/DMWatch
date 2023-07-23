package com.DMWatch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
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
public class BannedCaseManager
{
	// This list is our live end point to pull bans from
	private static final HttpUrl DMWatch_FAST_BAN_LIST = HttpUrl.parse("https://dmwatch.in/dev/bans.json");

	// This list is the github-pages deployed copy of the live data, has a slight delay but better than regular github lists
	private static final HttpUrl DMWatch_DEFAULT_BAN_LIST = HttpUrl.parse("https://dmwatch.github.io/dmwatchlist/bans.json");

	private static final Type typeToken = new TypeToken<List<BannedPlayer>>()
	{
	}.getType();

	private final OkHttpClient client;
	private final Queue<BannedPlayer> dmCases = new ConcurrentLinkedQueue<>();
	private final ClientThread clientThread;
	private final Gson gson;

	@Getter
	private ConcurrentHashMap<String, String> mappingsRSN = new ConcurrentHashMap<>();
	@Getter
	private HashSet<String> hwidBans = new HashSet<>();
	@Getter
	private HashSet<String> accidBans = new HashSet<>();


	@Inject
	private BannedCaseManager(OkHttpClient client, ClientThread clientThread, Gson gson)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.gson = gson.newBuilder().create();
	}

	/**
	 * @param onComplete called once the list has been refreshed. Called on the client thread
	 */
	public void refresh(Runnable onComplete, boolean useLiveList)
	{
		Request rwReq;

		if (useLiveList)
		{
			rwReq = new Request.Builder().url(DMWatch_FAST_BAN_LIST).addHeader("Cache-Control", "no-cache").build();
		}
		else
		{
			rwReq = new Request.Builder().url(DMWatch_DEFAULT_BAN_LIST).addHeader("Cache-Control", "no-cache").build();
		}

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
						List<BannedPlayer> cases = gson.fromJson(new InputStreamReader(response.body().byteStream()), typeToken);
						dmCases.clear();
						ConcurrentHashMap<String, String> localMappingRSN = new ConcurrentHashMap<>();
						for (BannedPlayer c : cases)
						{
							if (!localMappingRSN.containsKey(c.getNiceRSN()))
							{
								localMappingRSN.put(c.getNiceRSN(), c.getReason());
							}
							dmCases.add(c);
						}
						mappingsRSN = localMappingRSN;

						HashSet<String> localHashBans = new HashSet<>();
						for (BannedPlayer c : cases)
						{
							localHashBans.add(c.getAccountHash());
						}
						accidBans = localHashBans;

						HashSet<String> localHWIDBans = new HashSet<>();
						for (BannedPlayer c : cases)
						{
							localHWIDBans.add(c.getHardwareID());
						}
						hwidBans = localHWIDBans;

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
	public BannedPlayer get(String rsn)
	{
		if (rsn == null || dmCases.size() == 0)
		{
			return null;
		}
		String cleanRsn = Text.removeTags(Text.toJagexName(rsn)).toLowerCase();
		Optional<BannedPlayer> foundCase = dmCases.stream().filter(c -> c.getNiceRSN().equals(cleanRsn)).findFirst();

		if (foundCase.isPresent())
		{
			return foundCase.get();
		}
		else
		{
			return null;
		}
	}

	public BannedPlayer getByAccountHash(String hashID)
	{
		if (hashID == null || dmCases.size() == 0)
		{
			return null;
		}

		if (!accidBans.contains(hashID))
		{
			return null;
		}

		Optional<BannedPlayer> foundCase = dmCases.stream().filter(c -> c.getAccountHash().equals(hashID)).findFirst();

		if (foundCase.isPresent())
		{
			return foundCase.get();
		}
		else
		{
			return null;
		}
	}

	public BannedPlayer getByHWID(String hwid)
	{
		if (hwid == null || dmCases.size() == 0)
		{
			return null;
		}

		if (!hwidBans.contains(hwid))
		{
			return null;
		}

		Optional<BannedPlayer> foundCase1 = dmCases.stream().filter(c -> c.getAccountHash().equals(hwid)).findFirst();

		if (foundCase1.isPresent())
		{
			return foundCase1.get();
		}
		else
		{
			return null;
		}
	}

	public boolean inListByRsn(String rsn)
	{
		if (rsn == null || dmCases.size() == 0)
		{
			return false;
		}
		String cleanRsn = Text.removeTags(Text.toJagexName(rsn)).toLowerCase();
		return getMappingsRSN().containsKey(cleanRsn);
	}

	public int getListSize()
	{
		return dmCases.size();
	}
}