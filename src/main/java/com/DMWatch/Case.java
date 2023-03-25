package com.DMWatch;

import com.google.gson.annotations.SerializedName;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Case
{
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

	// "Stella"
	@SerializedName("display_rsn")
	private String rsn;

	// "2019-03-10 13:35:34" GMT
	@SerializedName("signup_date")
	private Date date;

	// "-7383353695795164313"
	@SerializedName("account_hash")
	private String accountHash;

	// "d5d9eac938fe755e25e408d898ed839c"
	@SerializedName("hardware_id")
	private String hardwareID;

	// "Scammed a 1b dm"
	private String reason;

	// "0 - unknown // 1 - clean // 2 - accused // 3 - scammed"
	@SerializedName("evidence_rating")
	private String status;


	public String niceDate()
	{
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT);
	}
}
