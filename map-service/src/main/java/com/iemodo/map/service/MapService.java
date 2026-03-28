package com.iemodo.map.service;

import com.iemodo.map.dto.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified map service interface
 */
public interface MapService {

    /**
     * Geocode address to coordinates
     */
    Mono<GeocodeResult> geocode(String address, String countryCode);

    /**
     * Reverse geocode coordinates to address
     */
    Mono<GeocodeResult> reverseGeocode(Double latitude, Double longitude);

    /**
     * Validate address
     */
    Mono<Boolean> validateAddress(String address, String countryCode);

    /**
     * Calculate route between two points
     */
    Mono<RouteResult> calculateRoute(GeoPoint origin, GeoPoint destination);

    /**
     * Calculate distance between two points
     */
    Mono<BigDecimal> calculateDistance(GeoPoint origin, GeoPoint destination);

    /**
     * Find nearest warehouses
     */
    Mono<List<WarehouseDistance>> findNearestWarehouses(GeoPoint location, Integer limit, Double maxDistanceKm);
}
