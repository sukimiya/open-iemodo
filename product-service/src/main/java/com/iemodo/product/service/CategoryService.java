package com.iemodo.product.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.product.domain.Category;
import com.iemodo.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Category service with tree structure support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get all active categories.
     */
    public Flux<Category> getAllCategories() {
        return categoryRepository.findAllByIsActiveTrueOrderByLevelAscSortOrderAsc();
    }

    /**
     * Get category tree structure.
     */
    public Mono<List<CategoryTreeNode>> getCategoryTree() {
        return categoryRepository.findAllByIsActiveTrueOrderByLevelAscSortOrderAsc()
                .collectList()
                .map(this::buildTree);
    }

    /**
     * Get categories by level.
     */
    public Flux<Category> getCategoriesByLevel(Integer level) {
        return categoryRepository.findByLevelAndIsActiveTrueOrderBySortOrderAsc(level);
    }

    /**
     * Get child categories.
     */
    public Flux<Category> getChildCategories(Long parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(parentId);
    }

    /**
     * Get category by ID.
     */
    public Mono<Category> getCategory(Long id) {
        return categoryRepository.findById(id)
                .filter(Category::isActive)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Category not found")));
    }

    /**
     * Get category with product count.
     */
    public Mono<CategoryWithCount> getCategoryWithCount(Long id) {
        return getCategory(id)
                .flatMap(category -> 
                        categoryRepository.countProductsByCategoryId(id)
                                .map(count -> new CategoryWithCount(category, count))
                );
    }

    /**
     * Create category.
     */
    public Mono<Category> createCategory(Category category) {
        return validateCategory(category)
                .then(generatePath(category))
                .then(categoryRepository.save(category))
                .doOnSuccess(c -> log.info("Created category id={}, name={}", c.getId(), c.getName()));
    }

    /**
     * Update category.
     */
    public Mono<Category> updateCategory(Long id, Category updates) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(existing -> {
                    if (updates.getName() != null) existing.setName(updates.getName());
                    if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
                    if (updates.getImageUrl() != null) existing.setImageUrl(updates.getImageUrl());
                    if (updates.getSortOrder() != null) existing.setSortOrder(updates.getSortOrder());
                    if (updates.getIsActive() != null) existing.setIsActive(updates.getIsActive());
                    return categoryRepository.save(existing);
                })
                .doOnSuccess(c -> log.info("Updated category id={}", id));
    }

    /**
     * Delete category (soft delete by setting inactive).
     */
    public Mono<Void> deleteCategory(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> {
                    category.setIsActive(false);
                    return categoryRepository.save(category);
                })
                .doOnSuccess(c -> log.info("Deleted category id={}", id))
                .then();
    }

    /**
     * Get breadcrumb path for a category.
     */
    public Flux<Category> getBreadcrumb(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .flatMapMany(category -> {
                    if (category.getPath() == null) {
                        return Flux.just(category);
                    }
                    Long[] pathIds = category.getPathArray();
                    return categoryRepository.findAllById(List.of(pathIds));
                });
    }

    // ─── Helper methods ────────────────────────────────────────────────────

    private Mono<Void> validateCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            return Mono.error(new BusinessException(
                    ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Category name is required"));
        }
        return Mono.empty();
    }

    private Mono<Void> generatePath(Category category) {
        if (category.getParentId() == null) {
            // Root category
            category.setLevel(1);
            return categoryRepository.count()
                    .map(count -> "/" + (count + 1))
                    .doOnNext(category::setPath)
                    .then();
        }
        
        return categoryRepository.findById(category.getParentId())
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST, "Parent category not found")))
                .flatMap(parent -> {
                    category.setLevel(parent.getLevel() + 1);
                    return categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(parent.getId())
                            .count()
                            .map(count -> parent.getPath() + "/" + category.getId())
                            .doOnNext(category::setPath)
                            .then();
                });
    }

    private List<CategoryTreeNode> buildTree(List<Category> categories) {
        List<CategoryTreeNode> roots = new ArrayList<>();
        java.util.Map<Long, CategoryTreeNode> nodeMap = new java.util.HashMap<>();
        
        // First pass: create all nodes
        for (Category cat : categories) {
            CategoryTreeNode node = new CategoryTreeNode(cat, new ArrayList<>());
            nodeMap.put(cat.getId(), node);
        }
        
        // Second pass: build hierarchy
        for (Category cat : categories) {
            CategoryTreeNode node = nodeMap.get(cat.getId());
            if (cat.getParentId() == null) {
                roots.add(node);
            } else {
                CategoryTreeNode parent = nodeMap.get(cat.getParentId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        
        return roots;
    }

    // ─── DTOs ──────────────────────────────────────────────────────────────

    public record CategoryWithCount(Category category, Long productCount) {}
    
    public record CategoryTreeNode(Category category, List<CategoryTreeNode> children) {}
}
