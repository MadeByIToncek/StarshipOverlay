package cz.iqlandia.iqplanetarium.starshipoverlay;

import java.time.ZonedDateTime;

public record CacheData(byte[] data, ZonedDateTime ttl_end) {
	public boolean ttl_over() {
		return ZonedDateTime.now().isAfter(ttl_end);
	}
}
