package com.hbm.saveddata.satellites;

public class StarcoreRelayUtil {

	public static boolean isStarcoreRelay(Satellite sat) {
		return sat instanceof SatelliteStarcoreRelay;
	}
}
