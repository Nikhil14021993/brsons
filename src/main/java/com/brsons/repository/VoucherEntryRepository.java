package com.brsons.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.brsons.model.VoucherEntry;

@Repository
public interface VoucherEntryRepository extends JpaRepository<VoucherEntry, Long> {
}
