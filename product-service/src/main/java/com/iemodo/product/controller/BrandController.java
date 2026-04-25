package com.iemodo.product.controller;

import com.iemodo.common.response.Response;
import com.iemodo.product.domain.Brand;
import com.iemodo.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pc/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepository;

    @GetMapping
    public Mono<Response<List<Brand>>> getAllBrands() {
        return brandRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .collectList()
                .map(Response::success);
    }

    @GetMapping("/{id}")
    public Mono<Response<Brand>> getBrand(@PathVariable("id") Long id) {
        return brandRepository.findById(id)
                .map(Response::success)
                .defaultIfEmpty(Response.error(404, "Brand not found"));
    }

    @PostMapping
    public Mono<Response<Brand>> createBrand(@RequestBody Brand brand) {
        return brandRepository.save(brand)
                .map(Response::success);
    }

    @PutMapping("/{id}")
    public Mono<Response<Brand>> updateBrand(
            @PathVariable("id") Long id,
            @RequestBody Brand brand) {
        return brandRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(brand.getName());
                    existing.setNameLocalized(brand.getNameLocalized());
                    existing.setLogoUrl(brand.getLogoUrl());
                    existing.setWebsite(brand.getWebsite());
                    existing.setDescription(brand.getDescription());
                    existing.setCountryCode(brand.getCountryCode());
                    existing.setSortOrder(brand.getSortOrder());
                    existing.setIsActive(brand.getIsActive());
                    return brandRepository.save(existing);
                })
                .map(Response::success)
                .switchIfEmpty(Mono.just(Response.error(404, "Brand not found")));
    }

    @DeleteMapping("/{id}")
    public Mono<Response<Void>> deleteBrand(@PathVariable("id") Long id) {
        return brandRepository.findById(id)
                .flatMap(brand -> {
                    brand.setIsActive(false);
                    return brandRepository.save(brand);
                })
                .then(Mono.just(Response.<Void>success()));
    }
}
