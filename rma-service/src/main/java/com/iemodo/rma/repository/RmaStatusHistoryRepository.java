package com.iemodo.rma.repository;

import com.iemodo.rma.domain.RmaStatusHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RmaStatusHistoryRepository extends ReactiveCrudRepository<RmaStatusHistory, Long> {

    Flux<RmaStatusHistory> findByRmaIdOrderByCreateTimeAsc(Long rmaId);
}
