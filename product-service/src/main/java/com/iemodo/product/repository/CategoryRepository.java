package com.iemodo.product.repository;

import com.iemodo.product.domain.Category;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Category} entity.
 */
@Repository
public interface CategoryRepository extends ReactiveCrudRepository<Category, Long> {

    Flux<Category> findAllByIsActiveTrueOrderByLevelAscSortOrderAsc();

    Flux<Category> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);

    Flux<Category> findByLevelAndIsActiveTrueOrderBySortOrderAsc(Integer level);

    Mono<Boolean> existsByName(String name);

    @Query("SELECT * FROM categories WHERE path LIKE :pathPrefix% AND is_active = true ORDER BY path")
    Flux<Category> findByPathStartingWith(String pathPrefix);

    @Query("SELECT * FROM categories WHERE level = :level AND parent_id = :parentId AND is_active = true")
    Flux<Category> findChildren(Long parentId, Integer level);

    @Query("SELECT COUNT(*) FROM products WHERE category_id = :categoryId AND status = 'ACTIVE' AND deleted_at IS NULL")
    Mono<Long> countProductsByCategoryId(Long categoryId);
}
