package com.iemodo.product.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.product.domain.Product;
import com.iemodo.product.domain.Sku;
import com.iemodo.product.repository.ProductCountryVisibilityRepository;
import com.iemodo.product.repository.ProductRepository;
import com.iemodo.product.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("ProductService")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private SkuRepository skuRepository;
    @Mock private ProductCountryVisibilityRepository visibilityRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(productRepository, skuRepository, visibilityRepository);
    }

    @Test
    @DisplayName("getProduct: should return product when visible in country")
    void getProduct_shouldReturn_whenVisible() {
        Product product = Product.builder()
                .id(1L)
                .productCode("PROD-001")
                .title("Test Product")
                .status("ACTIVE")
                .basePrice(new BigDecimal("99.99"))
                .build();

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Mono.just(product));
        when(visibilityRepository.isVisibleInCountry(1L, "US")).thenReturn(Mono.just(true));

        StepVerifier.create(productService.getProduct(1L, "US"))
                .assertNext(p -> {
                    assertThat(p.getId()).isEqualTo(1L);
                    assertThat(p.getProductCode()).isEqualTo("PROD-001");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getProduct: should fail when not visible in country")
    void getProduct_shouldFail_whenNotVisible() {
        Product product = Product.builder()
                .id(1L)
                .productCode("PROD-001")
                .title("Test Product")
                .status("ACTIVE")
                .build();

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Mono.just(product));
        when(visibilityRepository.isVisibleInCountry(1L, "US")).thenReturn(Mono.just(false));

        StepVerifier.create(productService.getProduct(1L, "US"))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be &&
                        be.getErrorCode() == ErrorCode.NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("createProduct: should create product with valid code")
    void createProduct_shouldSucceed_withValidCode() {
        Product product = Product.builder()
                .productCode("PROD-NEW")
                .title("New Product")
                .basePrice(new BigDecimal("49.99"))
                .categoryId(1L)
                .build();

        Product saved = Product.builder()
                .id(1L)
                .productCode("PROD-NEW")
                .title("New Product")
                .basePrice(new BigDecimal("49.99"))
                .categoryId(1L)
                .createdAt(Instant.now())
                .build();

        when(productRepository.existsByProductCode("PROD-NEW")).thenReturn(Mono.just(false));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(productService.createProduct(product))
                .assertNext(p -> {
                    assertThat(p.getId()).isEqualTo(1L);
                    assertThat(p.getProductCode()).isEqualTo("PROD-NEW");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createProduct: should fail when product code exists")
    void createProduct_shouldFail_whenCodeExists() {
        Product product = Product.builder()
                .productCode("PROD-EXISTING")
                .title("Existing Product")
                .basePrice(new BigDecimal("49.99"))
                .build();

        when(productRepository.existsByProductCode("PROD-EXISTING")).thenReturn(Mono.just(true));

        StepVerifier.create(productService.createProduct(product))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be &&
                        be.getErrorCode() == ErrorCode.BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("deleteProduct: should soft delete product")
    void deleteProduct_shouldSoftDelete() {
        Product product = Product.builder()
                .id(1L)
                .productCode("PROD-001")
                .title("Test Product")
                .status("ACTIVE")
                .build();

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(productService.deleteProduct(1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("reserveStock: should succeed when enough stock")
    void reserveStock_shouldSucceed_whenEnoughStock() {
        Sku sku = Sku.builder()
                .id(1L)
                .skuCode("SKU-001")
                .stockQuantity(100)
                .reservedQuantity(0)
                .status("ACTIVE")
                .build();

        when(skuRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Mono.just(sku));
        when(skuRepository.reserveStock(1L, 5)).thenReturn(Mono.just(1));

        StepVerifier.create(productService.reserveStock(1L, 5))
                .assertNext(result -> assertThat(result).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("reserveStock: should fail when not enough stock")
    void reserveStock_shouldFail_whenNotEnoughStock() {
        Sku sku = Sku.builder()
                .id(1L)
                .skuCode("SKU-001")
                .stockQuantity(3)
                .reservedQuantity(0)
                .status("ACTIVE")
                .build();

        when(skuRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Mono.just(sku));

        StepVerifier.create(productService.reserveStock(1L, 5))
                .assertNext(result -> assertThat(result).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("getFeaturedProducts: should return featured products")
    void getFeaturedProducts_shouldReturnFeatured() {
        Product p1 = Product.builder().id(1L).productCode("P1").isFeatured(true).status("ACTIVE").build();
        Product p2 = Product.builder().id(2L).productCode("P2").isFeatured(true).status("ACTIVE").build();

        when(productRepository.findByIsFeaturedTrueAndStatusAndDeletedAtIsNull("ACTIVE"))
                .thenReturn(Flux.just(p1, p2));
        when(visibilityRepository.isVisibleInCountry(anyLong(), eq("US"))).thenReturn(Mono.just(true));

        StepVerifier.create(productService.getFeaturedProducts("US", 10))
                .assertNext(p -> assertThat(p.getProductCode()).isEqualTo("P1"))
                .assertNext(p -> assertThat(p.getProductCode()).isEqualTo("P2"))
                .verifyComplete();
    }
}
