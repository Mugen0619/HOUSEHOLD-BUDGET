package com.example.householdbudget.summary;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public SummaryResponse getSummary(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return summaryService.getSummary(month);
    }
}
