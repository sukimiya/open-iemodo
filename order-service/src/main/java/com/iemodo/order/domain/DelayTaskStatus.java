package com.iemodo.order.domain;

public enum DelayTaskStatus {
    PENDING,    // waiting to be processed
    PROCESSING, // claimed by a scheduler instance
    DONE,       // order was cancelled and inventory rolled back
    SKIPPED     // order was already paid or cancelled — no action needed
}
