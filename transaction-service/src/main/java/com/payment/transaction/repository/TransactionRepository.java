package com.payment.transaction.repository;

import com.payment.transaction.model.Transaction;
import com.payment.common.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByMerchantId(String merchantId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
