package com.iemodo.product.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.product.domain.Product;
import com.iemodo.product.domain.ProductCountryVisibility;
import com.iemodo.product.domain.Sku;
import com.iemodo.product.repository.ProductCountryVisibilityRepository;
import com.iemodo.product.repository.ProductRepository;
import com.iemodo.product.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Product service with internationalization support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final ProductCountryVisibilityRepository visibilityRepository;

    /**
     * Get product by ID with country-specific visibility check.
     */
    public Mono<Product> getProduct(Long id, String countryCode) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .filterWhen(product -> isVisibleInCountry(product.getId(), countryCode))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found or not available")))
                .doOnSuccess(p -> log.debug("Retrieved product id={}", id));
    }

    /**
     * Get product details with SKUs, filtered by country availability.
     */
    public Mono<ProductDetail> getProductDetail(Long productId, String countryCode) {
        return getProduct(productId, countryCode)
                .flatMap(product -> 
                        skuRepository.findAllByProductIdAndDeletedAtIsNull(productId)
                                .filter(sku -> sku.isAvailableInCountry(countryCode))
                                .filter(Sku::isActive)
                                .collectList()
                                .map(skus -> new ProductDetail(product, skus))
                );
    }

    /**
     * List products for a specific country with pagination.
     */
    public Flux<Product> getProductsForCountry(String countryCode, String status, int page, int size) {
        return visibilityRepository.findByCountryCodeAndIsVisibleTrue(countryCode)
                .map(ProductCountryVisibility::getProductId)
                .collectList()
                .flatMapMany(productIds -> 
                        productRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status)
                                .filter(product -> productIds.contains(product.getId()))
                                .skip((long) page * size)
                                .take(size)
                );
    }

    /**
     * Search products by keyword for a country.
     */
    public Flux<Product> searchProducts(String keyword, String countryCode, int limit, int offset) {
        String searchPattern = "%" + keyword + "%";
        return productRepository.searchByKeyword(searchPattern, limit, offset)
                .filterWhen(product -> isVisibleInCountry(product.getId(), countryCode));
    }

    /**
     * Get featured products for a country.
     */
    public Flux<Product> getFeaturedProducts(String countryCode, int limit) {
        return productRepository.findByIsFeaturedTrueAndStatusAndDeletedAtIsNull("ACTIVE")
                .filterWhen(product -> isVisibleInCountry(product.getId(), countryCode))
                .take(limit);
    }

    /**
     * Get new arrival products for a country.
     */
    public Flux<Product> getNewArrivals(String countryCode, int limit) {
        return productRepository.findByIsNewArrivalTrueAndStatusAndDeletedAtIsNull("ACTIVE")
                .filterWhen(product -> isVisibleInCountry(product.getId(), countryCode))
                .take(limit);
    }

    /**
     * Get products by category for a country.
     */
    public Flux<Product> getProductsByCategory(Long categoryId, String countryCode, int page, int size) {
        return productRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(categoryId, "ACTIVE")
                .filterWhen(product -> isVisibleInCountry(product.getId(), countryCode))
                .skip((long) page * size)
                .take(size);
    }

    /**
     * Create a new product.
     */
    @Transactional
    public Mono<Product> createProduct(Product product) {
        return validateProductCode(product.getProductCode())
                .flatMap(valid -> productRepository.save(product))
                .doOnSuccess(p -> log.info("Created product id={}, code={}", p.getId(), p.getProductCode()));
    }

    /**
     * Update product.
     */
    @Transactional
    public Mono<Product> updateProduct(Long id, Product updates) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found")))
                .flatMap(existing -> {
                    // Apply updates
                    if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
                    if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
                    if (updates.getBasePrice() != null) existing.setBasePrice(updates.getBasePrice());
                    if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
                    if (updates.getIsFeatured() != null) existing.setIsFeatured(updates.getIsFeatured());
                    if (updates.getIsNewArrival() != null) existing.setIsNewArrival(updates.getIsNewArrival());
                    if (updates.getMainImage() != null) existing.setMainImage(updates.getMainImage());
                    existing.setUpdatedAt(Instant.now());
                    return productRepository.save(existing);
                })
                .doOnSuccess(p -> log.info("Updated product id={}", id));
    }

    /**
     * Soft delete product.
     */
    @Transactional
    public Mono<Void> deleteProduct(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found")))
                .flatMap(product -> {
                    product.softDelete();
                    return productRepository.save(product);
                })
                .doOnSuccess(p -> log.info("Deleted product id={}", id))
                .then();
    }

    /**
     * Set country visibility for a product.
     */
    @Transactional
    public Mono<ProductCountryVisibility> setCountryVisibility(Long productId, String countryCode, 
                                                                boolean visible, boolean purchasable) {
        return visibilityRepository.findByProductIdAndCountryCode(productId, countryCode)
                .flatMap(existing -> {
                    existing.setIsVisible(visible);
                    existing.setIsPurchasable(purchasable);
                    existing.setUpdatedAt(Instant.now());
                    return visibilityRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ProductCountryVisibility visibility = ProductCountryVisibility.builder()
                            .productId(productId)
                            .countryCode(countryCode)
                            .isVisible(visible)
                            .isPurchasable(purchasable)
                            .build();
                    return visibilityRepository.save(visibility);
                }))
                .doOnSuccess(v -> log.info("Set visibility for product {} in country {}: visible={}, purchasable={}",
                        productId, countryCode, visible, purchasable));
    }

    /**
     * Check if product is visible in a country.
     */
    public Mono<Boolean> isVisibleInCountry(Long productId, String countryCode) {
        if (countryCode == null) return Mono.just(true);
        return visibilityRepository.isVisibleInCountry(productId, countryCode)
                .defaultIfEmpty(true); // Default visible if no record
    }

    /**
     * Check if product is purchasable in a country.
     */
    public Mono<Boolean> isPurchasableInCountry(Long productId, String countryCode) {
        if (countryCode == null) return Mono.just(true);
        return visibilityRepository.isPurchasableInCountry(productId, countryCode)
                .defaultIfEmpty(true);
    }

    /**
     * Reserve stock for an SKU.
     */
    @Transactional
    public Mono<Boolean> reserveStock(Long skuId, int quantity) {
        return skuRepository.findByIdAndDeletedAtIsNull(skuId)
                .filter(sku -> sku.hasEnoughStock(quantity))
                .flatMap(sku -> skuRepository.reserveStock(skuId, quantity))
                .map(updated -> updated > 0)
                .defaultIfEmpty(false);
    }

    /**
     * Release reserved stock.
     */
    @Transactional
    public Mono<Boolean> releaseStock(Long skuId, int quantity) {
        return skuRepository.releaseStock(skuId, quantity)
                .map(updated -> updated > 0);
    }

    /**
     * Deduct stock (confirm purchase).
     */
    @Transactional
    public Mono<Boolean> deductStock(Long skuId, int quantity) {
        return skuRepository.deductStock(skuId, quantity)
                .map(updated -> updated > 0);
    }

    // ─── Helper methods ────────────────────────────────────────────────────

    private Mono<Boolean> validateProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return Mono.error(new BusinessException(
                    ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Product code is required"));
        }
        return productRepository.existsByProductCode(productCode)
                .flatMap(exists -> exists 
                        ? Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, "Product code already exists"))
                        : Mono.just(true));
    }

    /**
     * Product detail DTO.
     */
    public record ProductDetail(Product product, java.util.List<Sku> skus) {}
}
