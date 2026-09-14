package com.job_Portal_Backend.job_portal_backend.billing.controller;

import com.job_Portal_Backend.job_portal_backend.billing.dto.AdminConfirmPaymentRequest;
import com.job_Portal_Backend.job_portal_backend.billing.dto.AdminCreditAdjustmentRequest;
import com.job_Portal_Backend.job_portal_backend.billing.dto.AdminTransactionDto;
import com.job_Portal_Backend.job_portal_backend.billing.service.BillingAdminService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Read-only platform visibility plus tightly-scoped, always-audited financial overrides. */
@RestController
@RequestMapping("/api/v1/admin/billing")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillingController {

    private final BillingAdminService billingAdminService;

    public AdminBillingController(BillingAdminService billingAdminService) {
        this.billingAdminService = billingAdminService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<AdminTransactionDto>>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transactions retrieved",
                billingAdminService.listAllTransactions(pageable)));
    }

    @PostMapping("/transactions/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(
            @PathVariable Long transactionId,
            @Valid @RequestBody AdminConfirmPaymentRequest request,
            @AuthenticationPrincipal User admin) {
        billingAdminService.confirmPayment(transactionId, request.getReason(), admin);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment confirmed", null));
    }

    @PostMapping("/companies/{companyId}/credits/adjust")
    public ResponseEntity<ApiResponse<Integer>> adjustCredits(
            @PathVariable Long companyId,
            @Valid @RequestBody AdminCreditAdjustmentRequest request,
            @AuthenticationPrincipal User admin) {
        int newBalance = billingAdminService.adjustCredits(companyId, request.getAmount(), request.getReason(), admin);
        return ResponseEntity.ok(new ApiResponse<>(true, "Credits adjusted", newBalance));
    }
}
