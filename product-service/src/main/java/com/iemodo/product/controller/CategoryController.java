package com.iemodo.product.controller;

import com.iemodo.common.response.Response;
import com.iemodo.product.domain.Category;
import com.iemodo.product.service.CategoryService;
import com.iemodo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller for category management.
 * 
 * <p>Base path: /pc/api/v1/categories
 */
@Slf4j
@RestController
@RequestMapping("/pc/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * Get all categories (flat list).
     */
    @GetMapping
    public Flux<Response<Category>> getAllCategories() {
        return categoryService.getAllCategories()
                .map(Response::success);
    }

    /**
     * Get category tree structure.
     */
    @GetMapping("/tree")
    public Mono<Response<List<CategoryService.CategoryTreeNode>>> getCategoryTree() {
        return categoryService.getCategoryTree()
                .map(Response::success);
    }

    /**
     * Get categories by level.
     */
    @GetMapping("/level/{level}")
    public Flux<Response<Category>> getCategoriesByLevel(@PathVariable Integer level) {
        return categoryService.getCategoriesByLevel(level)
                .map(Response::success);
    }

    /**
     * Get category by ID.
     */
    @GetMapping("/{id}")
    public Mono<Response<Category>> getCategory(@PathVariable Long id) {
        return categoryService.getCategory(id)
                .map(Response::success);
    }

    /**
     * Get category with product count.
     */
    @GetMapping("/{id}/with-count")
    public Mono<Response<CategoryService.CategoryWithCount>> getCategoryWithCount(@PathVariable Long id) {
        return categoryService.getCategoryWithCount(id)
                .map(Response::success);
    }

    /**
     * Get child categories.
     */
    @GetMapping("/{id}/children")
    public Flux<Response<Category>> getChildCategories(@PathVariable Long id) {
        return categoryService.getChildCategories(id)
                .map(Response::success);
    }

    /**
     * Get breadcrumb path for a category.
     */
    @GetMapping("/{id}/breadcrumb")
    public Flux<Response<Category>> getBreadcrumb(@PathVariable Long id) {
        return categoryService.getBreadcrumb(id)
                .map(Response::success);
    }

    /**
     * Get products in category.
     */
    @GetMapping("/{id}/products")
    public Flux<Response<com.iemodo.product.domain.Product>> getProductsByCategory(
            @PathVariable Long id,
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // Delegate to ProductService
        return productService.getProductsByCategory(id, countryCode, page, size)
                .map(Response::success);
    }

    /**
     * Create category (Admin).
     */
    @PostMapping
    public Mono<Response<Category>> createCategory(@RequestBody Category category) {
        return categoryService.createCategory(category)
                .map(Response::success);
    }

    /**
     * Update category (Admin).
     */
    @PutMapping("/{id}")
    public Mono<Response<Category>> updateCategory(
            @PathVariable Long id,
            @RequestBody Category category) {
        return categoryService.updateCategory(id, category)
                .map(Response::success);
    }

    /**
     * Delete category (Admin).
     */
    @DeleteMapping("/{id}")
    public Mono<Response<Void>> deleteCategory(@PathVariable Long id) {
        return categoryService.deleteCategory(id)
                .then(Mono.just(Response.success()));
    }
}
