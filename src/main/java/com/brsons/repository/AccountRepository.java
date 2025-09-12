package com.brsons.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.brsons.dto.TrialBalanceRow;
import com.brsons.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
	
	@Query("""
		       SELECT new com.brsons.dto.TrialBalanceRow(
		           a.name,
		           SUM(COALESCE(ve.debit, 0)),
		           SUM(COALESCE(ve.credit, 0))
		       )
		       FROM VoucherEntry ve
		       JOIN ve.account a
		       JOIN ve.voucher v
		       WHERE v.date BETWEEN :startDate AND :endDate
		       GROUP BY a.name
		       """)
		List<TrialBalanceRow> findTrialBalance(@Param("startDate") LocalDate startDate,
		                                       @Param("endDate") LocalDate endDate);
	
	// Find accounts by name containing the given text (case insensitive)
	List<Account> findByNameContainingIgnoreCase(String name);
	
	// Find parent accounts (accounts with no parent)
	List<Account> findByParentIsNullAndIsActiveTrue();
	
	// Find sub-accounts by parent
	List<Account> findByParentIdAndIsActiveTrue(Long parentId);
	
	// Find accounts by type
	List<Account> findByTypeAndIsActiveTrue(String type);
	
	// Find all active accounts
	List<Account> findByIsActiveTrue();
	
	// Find account by code
	Account findByCode(String code);
}