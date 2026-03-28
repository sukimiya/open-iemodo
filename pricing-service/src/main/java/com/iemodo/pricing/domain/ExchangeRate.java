package com.iemodo.pricing.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exchange rate entity - historical exchange rates
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("exchange_rates")
public class ExchangeRate extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("from_currency")
    private String fromCurrency;

    @Column("to_currency")
    private String toCurrency;

    @Column("rate")
    private BigDecimal rate;

    @Column("inverse_rate")
    private BigDecimal inverseRate;

    @Column("source")
    private String source;  // API, MANUAL, CALCULATED

    @Column("api_provider")
    private String apiProvider;  // fixer, exchangerate-api

    @Column("recorded_at")
    private Instant recordedAt;

    /**
     * Create a new exchange rate
     */
    public static ExchangeRate create(String from, String to, BigDecimal rate, String source) {
        return ExchangeRate.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .rate(rate)
                .inverseRate(BigDecimal.ONE.divide(rate, 10, BigDecimal.ROUND_HALF_UP))
                .source(source)
                .recordedAt(Instant.now())
                .build();
    }

    /**
     * Check if rate is recent (within 1 hour)
     */
    public boolean isRecent() {
        return recordedAt != null && 
               recordedAt.isAfter(Instant.now().minusSeconds(3600));
    }
}
