package com.FinalProject.feature_sales_report.domain;

import com.FinalProject.feature_sales_report.data.SalesReportRepository;
import com.FinalProject.feature_sales_report.data.SalesStatistics;
import com.google.android.gms.tasks.Task;

/**
 * UseCase lấy thống kê doanh thu
 */
public class GetStatisticsUseCase {
    
    private final SalesReportRepository repository;

    public GetStatisticsUseCase(SalesReportRepository repository) {
        this.repository = repository;
    }

    public Task<SalesStatistics> execute(String eventId) {
        return repository.calculateStatistics(eventId);
    }
}
