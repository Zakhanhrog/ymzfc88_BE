package com.xsecret.repository;

import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.type = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndType(@Param("user") User user, @Param("type") Transaction.TransactionType type);
    
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndStatus(@Param("user") User user, @Param("status") Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Transaction> findByStatusOrderByCreatedAtAsc(@Param("status") Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.createdAt ASC")
    Page<Transaction> findByStatusOrderByCreatedAtAsc(@Param("status") Transaction.TransactionStatus status, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByTypeAndStatus(@Param("type") Transaction.TransactionType type, 
                                        @Param("status") Transaction.TransactionStatus status, 
                                        Pageable pageable);
    
    // Check if transaction code exists
    boolean existsByTransactionCode(String transactionCode);
    
    // Admin queries
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR t.createdAt <= :endDate) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsWithFilters(@Param("type") Transaction.TransactionType type,
                                                 @Param("status") Transaction.TransactionStatus status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);
    
    // Statistics queries
    long countByTypeAndStatus(Transaction.TransactionType type, Transaction.TransactionStatus status);
    
    long countByTypeAndStatusAndCreatedAtBetween(Transaction.TransactionType type, 
                                               Transaction.TransactionStatus status,
                                               LocalDateTime startDate, 
                                               LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "t.type = :type AND t.status = :status AND " +
           "t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTypeAndStatusAndCreatedAtBetween(@Param("type") Transaction.TransactionType type,
                                                          @Param("status") Transaction.TransactionStatus status,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    long countByStatusInAndCreatedAtBetween(List<Transaction.TransactionStatus> statuses,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.netAmount), 0) FROM Transaction t WHERE t.status IN :statuses AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumNetAmountByStatusInAndCreatedAtBetween(@Param("statuses") List<Transaction.TransactionStatus> statuses,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    @Query("SELECT DATE(t.createdAt) AS day, COUNT(t) AS txnCount, COALESCE(SUM(t.netAmount), 0) AS totalAmount " +
           "FROM Transaction t WHERE t.status IN :statuses AND t.createdAt BETWEEN :start AND :end " +
           "GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt)")
    List<Object[]> getDailyTransactionStats(@Param("statuses") List<Transaction.TransactionStatus> statuses,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.status IN :statuses ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactionsByStatuses(@Param("statuses") List<Transaction.TransactionStatus> statuses,
                                                       Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (:type IS NULL OR t.type = :type) AND (:status IS NULL OR t.status = :status) AND (:start IS NULL OR t.createdAt >= :start) AND (:end IS NULL OR t.createdAt <= :end)")
    Page<Transaction> findAnalytics(@Param("type") Transaction.TransactionType type,
                                    @Param("status") Transaction.TransactionStatus status,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE (:type IS NULL OR t.type = :type) AND (:status IS NULL OR t.status = :status) AND (:start IS NULL OR t.createdAt >= :start) AND (:end IS NULL OR t.createdAt <= :end)")
    BigDecimal sumAmountByFilters(@Param("type") Transaction.TransactionType type,
                                  @Param("status") Transaction.TransactionStatus status,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.netAmount), 0) FROM Transaction t WHERE (:type IS NULL OR t.type = :type) AND (:status IS NULL OR t.status = :status) AND (:start IS NULL OR t.createdAt >= :start) AND (:end IS NULL OR t.createdAt <= :end)")
    BigDecimal sumNetAmountByFilters(@Param("type") Transaction.TransactionType type,
                                     @Param("status") Transaction.TransactionStatus status,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}