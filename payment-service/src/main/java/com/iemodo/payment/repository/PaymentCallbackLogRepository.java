package com.iemodo.payment.repository;

import com.iemodo.payment.domain.PaymentCallbackLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCallbackLogRepository extends ReactiveCrudRepository<PaymentCallbackLog, Long> {
}
