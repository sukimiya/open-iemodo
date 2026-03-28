package com.iemodo.product.controller;

import com.iemodo.common.response.Response;
import com.iemodo.product.domain.Product;
import com.iemodo.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for product management.
 * 
 * <p>Base path: /pc/api/v1/products
 */
@Slf4j
@RestController
@RequestMapping("/pc/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Get product list for a country.
     */
    @GetMapping
    public Flux<Response<Product>> getProducts(
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return productService.getProductsForCountry(countryCode, status, page, size)
                .map(Response::success);
    }

    /**
     * Get product details.
     */
    @GetMapping("/{id}")
    public Mono<Response<ProductService.ProductDetail>> getProduct(
            @PathVariable Long id,
            @RequestParam(value = "country", defaultValue = "US") String countryCode) {
        return productService.getProductDetail(id, countryCode)
                .map(Response::success);
    }

    /**
     * Search products.
     */
    @GetMapping("/search")
    public Flux<Response<Product>> searchProducts(
            @RequestParam("q") String keyword,
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return productService.searchProducts(keyword, countryCode, limit, offset)
                .map(Response::success);
    }

    /**
     * Get featured products.
     */
    @GetMapping("/featured")
    public Flux<Response<Product>> getFeaturedProducts(
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return productService.getFeaturedProducts(countryCode, limit)
                .map(Response::success);
    }

    /**
     * Get new arrivals.
     */
    @GetMapping("/new-arrivals")
    public Flux<Response<Product>> getNewArrivals(
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return productService.getNewArrivals(countryCode, limit)
                .map(Response::success);
    }

    /**
     * Get products by category.
     */
    @GetMapping("/category/{categoryId}")
    public Flux<Response<Product>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return productService.getProductsByCategory(categoryId, countryCode, page, size)
                .map(Response::success);
    }

    /**
     * Create product (Admin).
     */
    @PostMapping
    public Mono<Response<Product>> createProduct(@Valid @RequestBody Product product) {
        return productService.createProduct(product)
                .map(Response::success);
    }

    /**
     * Update product (Admin).
     */
    @PutMapping("/{id}")
    public Mono<Response<Product>> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        return productService.updateProduct(id, product)
                .map(Response::success);
    }

    /**
     * Delete product (Admin).
     */
    @DeleteMapping("/{id}")
    public Mono<Response<Void>> deleteProduct(@PathVariable Long id) {
        return productService.deleteProduct(id)
                .then(Mono.just(Response.success()));
    }

    /**
     * Set country visibility (Admin).
     */
    @PutMapping("/{id}/visibility/{countryCode}")
    public Mono<Response<Void>> setCountryVisibility(
            @PathVariable Long id,
            @PathVariable String countryCode,
            @RequestParam boolean visible,
            @RequestParam boolean purchasable) {
        return productService.setCountryVisibility(id, countryCode, visible, purchasable)
                .then(Mono.just(Response.success()));
    }
}
