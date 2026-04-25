package com.iemodo.user.repository;

import com.iemodo.user.domain.ConsentRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConsentRecordRepository extends ReactiveCrudRepository<ConsentRecord, Long> {

    Mono<ConsentRecord> findByUserIdAndPurpose(Long userId, String purpose);

    Flux<ConsentRecord> findAllByUserId(Long userId);
}
