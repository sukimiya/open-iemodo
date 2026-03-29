package com.iemodo.rma.controller;

import com.iemodo.common.response.Response;
import com.iemodo.rma.dto.CreateRmaRequest;
import com.iemodo.rma.dto.RmaDTO;
import com.iemodo.rma.service.RmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rma/api/v1")
@RequiredArgsConstructor
public class RmaController {

    private final RmaService rmaService;

    // ─── Customer endpoints ───────────────────────────────────────────────

    /** Submit a new RMA request. */
    @PostMapping("/requests")
    public Mono<Response<RmaDTO>> create(
            @Valid @RequestBody CreateRmaRequest req,
            @RequestHeader("X-TenantID") String tenantId,
            @RequestHeader("X-User-Id") Long customerId) {
        return rmaService.createRma(req, customerId, tenantId)
                .map(Response::success);
    }

    /** Get a single RMA by its RMA number. */
    @GetMapping("/requests/{rmaNo}")
    public Mono<Response<RmaDTO>> getByRmaNo(@PathVariable String rmaNo) {
        return rmaService.getByRmaNo(rmaNo)
                .map(Response::success);
    }

    /** List my RMA requests (paginated). */
    @GetMapping("/requests")
    public Flux<RmaDTO> list(
            @RequestHeader("X-User-Id") Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return rmaService.listByCustomer(customerId, page, size);
    }

    /** Customer submits the return shipment tracking number. */
    @PutMapping("/requests/{rmaId}/tracking")
    public Mono<Response<RmaDTO>> submitTracking(
            @PathVariable Long rmaId,
            @RequestParam String trackingNo,
            @RequestParam String carrier,
            @RequestHeader("X-User-Id") Long customerId) {
        return rmaService.submitTracking(rmaId, trackingNo, carrier, customerId)
                .map(Response::success);
    }

    /** Customer cancels their own RMA (only before approval). */
    @PutMapping("/requests/{rmaId}/cancel")
    public Mono<Response<RmaDTO>> cancel(
            @PathVariable Long rmaId,
            @RequestHeader("X-User-Id") Long customerId) {
        return rmaService.cancel(rmaId, customerId)
                .map(Response::success);
    }

    // ─── Merchant / operator endpoints ───────────────────────────────────

    /** Merchant approves the RMA. */
    @PutMapping("/requests/{rmaId}/approve")
    public Mono<Response<RmaDTO>> approve(
            @PathVariable Long rmaId,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "false") boolean orderShipped,
            @RequestHeader("X-User-Id") Long operatorId) {
        return rmaService.approve(rmaId, operatorId, notes, orderShipped)
                .map(Response::success);
    }

    /** Merchant rejects the RMA. */
    @PutMapping("/requests/{rmaId}/reject")
    public Mono<Response<RmaDTO>> reject(
            @PathVariable Long rmaId,
            @RequestParam String reason,
            @RequestHeader("X-User-Id") Long operatorId) {
        return rmaService.reject(rmaId, operatorId, reason)
                .map(Response::success);
    }

    /** Warehouse confirms physical receipt of returned goods. */
    @PutMapping("/requests/{rmaId}/receive")
    public Mono<Response<RmaDTO>> receive(
            @PathVariable Long rmaId,
            @RequestHeader("X-User-Id") Long operatorId) {
        return rmaService.markReceived(rmaId, operatorId)
                .map(Response::success);
    }

    /** Quality inspection result (EXCHANGE type only). */
    @PutMapping("/requests/{rmaId}/inspect")
    public Mono<Response<RmaDTO>> inspect(
            @PathVariable Long rmaId,
            @RequestParam boolean passed,
            @RequestParam(required = false) String notes,
            @RequestHeader("X-User-Id") Long operatorId) {
        return rmaService.inspect(rmaId, passed, operatorId, notes)
                .map(Response::success);
    }
}
