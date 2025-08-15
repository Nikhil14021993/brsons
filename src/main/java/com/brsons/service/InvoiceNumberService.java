package com.brsons.service;

import java.time.Year;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class InvoiceNumberService {
    // Simple in-memory counter per run (persist if you want strict sequences)
    private final AtomicLong counter = new AtomicLong(1);

    public String next(String prefix) {
        String year = String.valueOf(Year.now().getValue());
        long seq = counter.getAndIncrement();
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);
        return String.format("%s-%s-%06d", prefix, year, code, seq);
    }
}
