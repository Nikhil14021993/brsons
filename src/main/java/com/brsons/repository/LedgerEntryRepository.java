package com.brsons.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.brsons.model.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByBillType(String billType);
}
