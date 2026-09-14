package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry.Type;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CreditLedgerEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves CreditLedgerService's PESSIMISTIC_WRITE company-row lock actually serializes concurrent
 * credit mutations against the real database — a pure-mock unit test can't exercise real DB
 * locking, so this runs real threads against real transactions (see CreditLedgerServiceTest for
 * the fast, logic-level coverage of grant/consume/insufficient-balance).
 */
@SpringBootTest
class CreditLedgerConcurrencyIntegrationTest {

    @Autowired
    private CreditLedgerService creditLedgerService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CreditLedgerEntryRepository ledgerRepository;

    private Long companyId;

    @AfterEach
    void cleanUp() {
        if (companyId != null) {
            ledgerRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, org.springframework.data.domain.Pageable.unpaged())
                    .forEach(entry -> ledgerRepository.deleteById(entry.getId()));
            companyRepository.deleteById(companyId);
        }
    }

    @Test
    void concurrentConsumptionNeverOverspendsOrLosesUpdates() throws InterruptedException {
        Company company = new Company();
        company.setName("Concurrency Test Co " + UUID.randomUUID());
        company = companyRepository.save(company);
        companyId = company.getId();

        creditLedgerService.grant(companyId, 100, Type.ADMIN_ADJUSTMENT, null, null, "seed", null);

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = IntStream.range(0, threadCount)
                .<Callable<Boolean>>mapToObj(i -> () -> {
                    ready.countDown();
                    go.await();
                    try {
                        creditLedgerService.consume(companyId, 10, Type.JOB_POST, null, null, "concurrent consume", null);
                        return true;
                    } catch (ResponseStatusException insufficientCredits) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        List<Future<Boolean>> futures = tasks.stream().map(pool::submit).collect(Collectors.toList());
        ready.await();
        go.countDown();
        pool.shutdown();
        assertEquals(true, pool.awaitTermination(30, TimeUnit.SECONDS));

        long successCount = futures.stream().mapToLong(f -> {
            try {
                return f.get() ? 1 : 0;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).sum();

        // Exactly 100 credits / 10 per consumption = exactly 10 successes possible; the lock must
        // prevent any thread from over-consuming or losing another thread's update.
        assertEquals(10, successCount);
        assertEquals(0, creditLedgerService.currentBalance(companyId));
    }
}
