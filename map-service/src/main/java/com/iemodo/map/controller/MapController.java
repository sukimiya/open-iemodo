package com.iemodo.map.controller;

import com.iemodo.common.response.Response;
import com.iemodo.map.dto.*;
import com.iemodo.map.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Map controller
 */
@Slf4j
@RestController
@RequestMapping("/map/api/v1")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    /**
     * Geocode address to coordinates
     */
    @GetMapping("/geocode")
    public Mono<Response<GeocodeResult>> geocode(
            @RequestParam String address,
            @RequestParam(required = false) String countryCode) {
        
        return mapService.geocode(address, countryCode)
                .map(Response::success);
    }

    /**
     * Reverse geocode coordinates to address
     */
    @GetMapping("/reverse-geocode")
    public Mono<Response<GeocodeResult>> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lng) {
        
        return mapService.reverseGeocode(lat, lng)
                .map(Response::success);
    }

    /**
     * Validate address
     */
    @PostMapping("/validate-address")
    public Mono<Response<Boolean>> validateAddress(
            @RequestParam String address,
            @RequestParam(required = false) String countryCode) {
        
        return mapService.validateAddress(address, countryCode)
                .map(Response::success);
    }

    /**
     * Calculate route between two points
     */
    @PostMapping("/calculate-route")
    public Mono<Response<RouteResult>> calculateRoute(
            @RequestBody RouteRequest request) {
        
        return mapService.calculateRoute(request.getOrigin(), request.getDestination())
                .map(Response::success);
    }

    /**
     * Calculate distance between two points
     */
    @PostMapping("/calculate-distance")
    public Mono<Response<BigDecimal>> calculateDistance(
            @RequestBody DistanceRequest request) {
        
        return mapService.calculateDistance(request.getOrigin(), request.getDestination())
                .map(Response::success);
    }

    /**
     * Find nearest warehouses
     */
    @GetMapping("/warehouses/nearby")
    public Mono<Response<List<WarehouseDistance>>> findNearestWarehouses(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false) Double maxDistanceKm) {
        
        return mapService.findNearestWarehouses(GeoPoint.of(lat, lng), limit, maxDistanceKm)
                .map(Response::success);
    }
}
