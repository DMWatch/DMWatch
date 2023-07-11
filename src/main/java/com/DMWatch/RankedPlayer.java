package com.DMWatch;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.client.util.Text;

@Data
@AllArgsConstructor
public class RankedPlayer
{
	@SerializedName("rsn")
	private String rsn;

	@SerializedName("rank")
	private String rank;

	public String getNiceRSN()
	{
		return Text.toJagexName(rsn.toLowerCase());
	}
}