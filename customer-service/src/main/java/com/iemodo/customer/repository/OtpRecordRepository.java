package com.iemodo.customer.repository;

import com.iemodo.customer.domain.OtpRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface OtpRecordRepository extends ReactiveCrudRepository<OtpRecord, Long> {

    Flux<OtpRecord> findByTenantIdAndPhoneAndPurposeAndCreateTimeAfter(
            String tenantId, String phone, String purpose, Instant after);
}
