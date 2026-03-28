package com.iemodo.map.service;

import com.iemodo.map.dto.*;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

/**
 * Map service unit tests
 */
class MapServiceTest {

    private final MapServiceImpl mapService = new MapServiceImpl();

    @Test
    void geocode_Success() {
        StepVerifier.create(mapService.geocode("Beijing", "CN"))
                .expectNextMatches(result -> 
                        result.getLatitude() != null && 
                        result.getLongitude() != null)
                .verifyComplete();
    }

    @Test
    void reverseGeocode_Success() {
        StepVerifier.create(mapService.reverseGeocode(39.9042, 116.4074))
                .expectNextMatches(result -> 
                        result.getFormattedAddress() != null)
                .verifyComplete();
    }

    @Test
    void calculateDistance_Success() {
        GeoPoint origin = GeoPoint.of(39.9042, 116.4074); // Beijing
        GeoPoint destination = GeoPoint.of(31.2304, 121.4737); // Shanghai
        
        StepVerifier.create(mapService.calculateDistance(origin, destination))
                .expectNextMatches(distance -> 
                        distance.compareTo(BigDecimal.ZERO) > 0)
                .verifyComplete();
    }

    @Test
    void calculateRoute_Success() {
        GeoPoint origin = GeoPoint.of(39.9042, 116.4074);
        GeoPoint destination = GeoPoint.of(31.2304, 121.4737);
        
        StepVerifier.create(mapService.calculateRoute(origin, destination))
                .expectNextMatches(route -> 
                        route.getDistance() != null && 
                        route.getDurationMinutes() != null)
                .verifyComplete();
    }

    @Test
    void findNearestWarehouses_Success() {
        GeoPoint location = GeoPoint.of(39.9042, 116.4074);
        
        StepVerifier.create(mapService.findNearestWarehouses(location, 5, 1000.0))
                .expectNextMatches(warehouses -> !warehouses.isEmpty())
                .verifyComplete();
    }

    @Test
    void validateAddress_Success() {
        StepVerifier.create(mapService.validateAddress("Test Address", "CN"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void validateAddress_EmptyAddress() {
        StepVerifier.create(mapService.validateAddress("", "CN"))
                .expectNext(false)
                .verifyComplete();
    }
}
