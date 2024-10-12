package cz.iqlandia.iqplanetarium.starshipoverlay;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Config implements Serializable {
	public ZonedDateTime t0;
	public long streamOffsetMillis;

	public Config() {
		t0 = ZonedDateTime.of(2024,10,13,12,0,0,0, ZoneId.of("UTC"));
		streamOffsetMillis = 3000;
	}
}
