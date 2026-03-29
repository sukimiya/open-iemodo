package com.iemodo.rma.repository;

import com.iemodo.rma.domain.RmaItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RmaItemRepository extends ReactiveCrudRepository<RmaItem, Long> {

    Flux<RmaItem> findByRmaId(Long rmaId);
}
