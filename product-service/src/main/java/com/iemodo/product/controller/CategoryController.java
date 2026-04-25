package com.iemodo.product.controller;

import com.iemodo.common.response.Response;
import com.iemodo.product.domain.Category;
import com.iemodo.product.service.CategoryService;
import com.iemodo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pc/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public Mono<Response<List<Category>>> getAllCategories() {
        return categoryService.getAllCategories()
                .collectList()
                .map(Response::success);
    }

    @GetMapping("/tree")
    public Mono<Response<List<CategoryService.CategoryTreeNode>>> getCategoryTree() {
        return categoryService.getCategoryTree()
                .map(Response::success);
    }

    @GetMapping("/level/{level}")
    public Mono<Response<List<Category>>> getCategoriesByLevel(@PathVariable("level") Integer level) {
        return categoryService.getCategoriesByLevel(level)
                .collectList()
                .map(Response::success);
    }

    @GetMapping("/{id}")
    public Mono<Response<Category>> getCategory(@PathVariable("id") Long id) {
        return categoryService.getCategory(id)
                .map(Response::success);
    }

    @GetMapping("/{id}/with-count")
    public Mono<Response<CategoryService.CategoryWithCount>> getCategoryWithCount(@PathVariable("id") Long id) {
        return categoryService.getCategoryWithCount(id)
                .map(Response::success);
    }

    @GetMapping("/{id}/children")
    public Mono<Response<List<Category>>> getChildCategories(@PathVariable("id") Long id) {
        return categoryService.getChildCategories(id)
                .collectList()
                .map(Response::success);
    }

    @GetMapping("/{id}/breadcrumb")
    public Mono<Response<List<Category>>> getBreadcrumb(@PathVariable("id") Long id) {
        return categoryService.getBreadcrumb(id)
                .collectList()
                .map(Response::success);
    }

    @GetMapping("/{id}/products")
    public Mono<Response<List<com.iemodo.product.domain.Product>>> getProductsByCategory(
            @PathVariable("id") Long id,
            @RequestParam(value = "country", defaultValue = "US") String countryCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return productService.getProductsByCategory(id, countryCode, page, size)
                .collectList()
                .map(Response::success);
    }

    @PostMapping
    public Mono<Response<Category>> createCategory(@RequestBody Category category) {
        return categoryService.createCategory(category)
                .map(Response::success);
    }

    @PutMapping("/{id}")
    public Mono<Response<Category>> updateCategory(
            @PathVariable("id") Long id,
            @RequestBody Category category) {
        return categoryService.updateCategory(id, category)
                .map(Response::success);
    }

    @DeleteMapping("/{id}")
    public Mono<Response<Void>> deleteCategory(@PathVariable("id") Long id) {
        return categoryService.deleteCategory(id)
                .then(Mono.just(Response.success()));
    }
}
