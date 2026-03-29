package com.iemodo.rma.domain;

/**
 * The three business types of an RMA request.
 * Each type drives different state-machine paths.
 */
public enum RmaType {

    /** Buyer returns goods and receives a refund. */
    RETURN,

    /** Buyer returns defective goods and receives a replacement. */
    EXCHANGE,

    /**
     * Refund only — no goods to return.
     * Use cases: item never arrived, seller-initiated goodwill refund.
     */
    REFUND_ONLY
}
