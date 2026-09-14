package com.job_Portal_Backend.job_portal_backend.billing.controller;

import com.job_Portal_Backend.job_portal_backend.billing.dto.*;
import com.job_Portal_Backend.job_portal_backend.billing.service.BillingService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/billing/plans")
    public ResponseEntity<ApiResponse<List<BillingPlanDto>>> listPlans() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Plans retrieved", billingService.listActivePlans()));
    }

    @GetMapping("/companies/{companyId}/billing")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<BillingSummaryDto>> getSummary(
            @PathVariable Long companyId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Billing summary retrieved",
                billingService.getSummary(companyId, user)));
    }

    @GetMapping("/companies/{companyId}/billing/transactions")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Page<PaymentTransactionDto>>> listTransactions(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transactions retrieved",
                billingService.listTransactions(companyId, user, pageable)));
    }

    @GetMapping("/companies/{companyId}/billing/invoices")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> listInvoices(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invoices retrieved",
                billingService.listInvoices(companyId, user, pageable)));
    }

    @GetMapping("/companies/{companyId}/billing/credits")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Page<CreditLedgerEntryDto>>> listCreditActivity(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Credit activity retrieved",
                billingService.listCreditActivity(companyId, user, pageable)));
    }

    @PostMapping("/companies/{companyId}/billing/checkout")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CheckoutResponseDto>> checkout(
            @PathVariable Long companyId,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Checkout created",
                billingService.checkout(companyId, request, user)));
    }

    @PostMapping("/companies/{companyId}/billing/subscription/cancel")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(
            @PathVariable Long companyId, @AuthenticationPrincipal User user) {
        billingService.cancelSubscription(companyId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subscription set to cancel at period end", null));
    }

    @PostMapping("/companies/{companyId}/billing/subscription/resume")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> resumeSubscription(
            @PathVariable Long companyId, @AuthenticationPrincipal User user) {
        billingService.resumeSubscription(companyId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subscription cancellation resumed", null));
    }
}
