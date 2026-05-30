package com.timemate.gonow.global.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class GeoUtils {

    private static final int EARTH_RADIUS_METERS = 6371000;

    // 목적지까지 남은 시간 기반 GPS 폴링 주기 계산 (단위: 초) — NEARDEST 대기 구간에서 사용
    public static int computeIntervalByTime(LocalDateTime targetTime) {
        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), targetTime);
        if (minutesLeft > 30) return 120;
        else if (minutesLeft > 10) return 60;
        else return 30;
    }

    // 두 좌표 사이 거리 계산 (단위: 미터, Haversine 공식)
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = phi2 - phi1;
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double sinPhi = Math.sin(deltaPhi / 2);
        double sinLambda = Math.sin(deltaLambda / 2);

        double a = sinPhi * sinPhi +
                   Math.cos(phi1) * Math.cos(phi2) *
                   sinLambda * sinLambda;

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
