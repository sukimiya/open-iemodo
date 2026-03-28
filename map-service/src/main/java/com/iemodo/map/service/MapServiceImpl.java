package com.iemodo.map.service;

import com.iemodo.map.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Map service implementation
 * Supports Google Maps (global) and Baidu Maps (China)
 */
@Slf4j
@Service
public class MapServiceImpl implements MapService {

    // China region codes
    private static final List<String> CHINA_REGIONS = List.of("CN", "HK", "MO");

    @Override
    public Mono<GeocodeResult> geocode(String address, String countryCode) {
        log.info("Geocoding address: {}, country: {}", address, countryCode);
        
        // For demo, return mock result
        return Mono.just(GeocodeResult.builder()
                .formattedAddress(address)
                .latitude(39.9042)
                .longitude(116.4074)
                .countryCode(countryCode != null ? countryCode : "CN")
                .city("Beijing")
                .build());
    }

    @Override
    public Mono<GeocodeResult> reverseGeocode(Double latitude, Double longitude) {
        log.info("Reverse geocoding: {}, {}", latitude, longitude);
        
        return Mono.just(GeocodeResult.builder()
                .formattedAddress("Mock Address")
                .latitude(latitude)
                .longitude(longitude)
                .countryCode("CN")
                .city("Beijing")
                .build());
    }

    @Override
    public Mono<Boolean> validateAddress(String address, String countryCode) {
        return Mono.just(address != null && !address.isBlank());
    }

    @Override
    public Mono<RouteResult> calculateRoute(GeoPoint origin, GeoPoint destination) {
        log.info("Calculating route from {} to {}", origin, destination);
        
        // Calculate distance using Haversine formula
        double distance = calculateHaversineDistance(origin, destination);
        
        // Estimate duration (avg 40 km/h)
        int durationMinutes = (int) (distance / 40.0 * 60);
        
        return Mono.just(RouteResult.builder()
                .distance(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP))
                .durationMinutes(durationMinutes)
                .polyline("mock_polyline")
                .build());
    }

    @Override
    public Mono<BigDecimal> calculateDistance(GeoPoint origin, GeoPoint destination) {
        double distance = calculateHaversineDistance(origin, destination);
        return Mono.just(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public Mono<List<WarehouseDistance>> findNearestWarehouses(GeoPoint location, Integer limit, Double maxDistanceKm) {
        // Mock warehouse data
        List<WarehouseDistance> warehouses = new ArrayList<>();
        
        // Mock warehouse 1
        warehouses.add(createWarehouseDistance("WH-001", "Warehouse Beijing", 
                39.9042, 116.4074, location));
        
        // Mock warehouse 2
        warehouses.add(createWarehouseDistance("WH-002", "Warehouse Shanghai", 
                31.2304, 121.4737, location));
        
        // Sort by distance
        warehouses.sort(Comparator.comparing(WarehouseDistance::getDistanceKm));
        
        // Apply limit
        int resultLimit = limit != null ? limit : 5;
        double maxDist = maxDistanceKm != null ? maxDistanceKm : Double.MAX_VALUE;
        
        List<WarehouseDistance> result = warehouses.stream()
                .filter(w -> w.getDistanceKm().doubleValue() <= maxDist)
                .limit(resultLimit)
                .toList();
        
        return Mono.just(result);
    }

    private WarehouseDistance createWarehouseDistance(String id, String name, 
                                                       double lat, double lng, GeoPoint from) {
        double distance = calculateHaversineDistance(from, GeoPoint.of(lat, lng));
        
        return WarehouseDistance.builder()
                .warehouseId(id)
                .warehouseName(name)
                .latitude(lat)
                .longitude(lng)
                .distanceKm(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * Calculate distance using Haversine formula
     */
    private double calculateHaversineDistance(GeoPoint p1, GeoPoint p2) {
        if (p1 == null || p2 == null || p1.getLatitude() == null || p1.getLongitude() == null
                || p2.getLatitude() == null || p2.getLongitude() == null) {
            return 0.0;
        }
        
        final int R = 6371; // Earth's radius in kilometers
        
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double deltaLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double deltaLng = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
