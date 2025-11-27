package com.brsons.controller;

import com.brsons.dto.DaybookEntryDto;
import com.brsons.dto.DaybookSummaryDto;
import com.brsons.service.DaybookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class DaybookController {

    @Autowired
    private DaybookService daybookService;

    /**
     * Display daybook page
     */
    @GetMapping("/admin/daybook")
    public String daybook(
            @RequestParam(value = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // Default to current month if no dates provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get daybook entries
        List<DaybookEntryDto> entries = daybookService.getDaybookEntries(startDate, endDate);
        
        // Get daybook summary
        DaybookSummaryDto summary = daybookService.getDaybookSummary(startDate, endDate);

        // Add data to model
        model.addAttribute("entries", entries);
        model.addAttribute("summary", summary);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("startDateStr", startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("endDateStr", endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        return "admin-daybook";
    }

    /**
     * Export daybook to CSV
     */
    @GetMapping("/admin/daybook/export")
    public String exportDaybook(
            @RequestParam(value = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // Default to current month if no dates provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get daybook entries
        List<DaybookEntryDto> entries = daybookService.getDaybookEntries(startDate, endDate);
        
        // Get daybook summary
        DaybookSummaryDto summary = daybookService.getDaybookSummary(startDate, endDate);

        // Add data to model for CSV export
        model.addAttribute("entries", entries);
        model.addAttribute("summary", summary);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin-daybook-csv";
    }

    /**
     * Print daybook
     */
    @GetMapping("/admin/daybook/print")
    public String printDaybook(
            @RequestParam(value = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // Default to current month if no dates provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get daybook entries
        List<DaybookEntryDto> entries = daybookService.getDaybookEntries(startDate, endDate);
        
        // Get daybook summary
        DaybookSummaryDto summary = daybookService.getDaybookSummary(startDate, endDate);

        // Add data to model for printing
        model.addAttribute("entries", entries);
        model.addAttribute("summary", summary);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin-daybook-print";
    }
}
