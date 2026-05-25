package com.example.utils;

/**
 * 地理位置工具类 - Haversine公式计算球面距离
 */
public class GeoUtils {

    private static final double EARTH_RADIUS = 6371.0; // 地球半径，单位：公里

    /**
     * 计算两个经纬度坐标之间的距离（Haversine公式）
     * @param lat1 点1纬度
     * @param lng1 点1经度
     * @param lat2 点2纬度
     * @param lng2 点2经度
     * @return 距离，单位：公里；坐标不完整返回-1
     */
    public static double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return -1;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * 格式化距离显示
     * @param distanceKm 距离（公里）
     * @return 格式化字符串，如 "1.2km" 或 "500m"
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 0) {
            return "未知";
        }
        if (distanceKm < 1) {
            return Math.round(distanceKm * 1000) + "m";
        }
        return String.format("%.1fkm", distanceKm);
    }
}
