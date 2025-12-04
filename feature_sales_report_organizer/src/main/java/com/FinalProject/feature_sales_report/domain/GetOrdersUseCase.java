package com.FinalProject.feature_sales_report.domain;

import com.FinalProject.core.model.Orders;
import com.FinalProject.feature_sales_report.data.SalesReportRepository;
import com.google.android.gms.tasks.Task;

import java.util.List;

/**
 * UseCase lấy danh sách đơn hàng
 */
public class GetOrdersUseCase {
    
    private final SalesReportRepository repository;

    public GetOrdersUseCase(SalesReportRepository repository) {
        this.repository = repository;
    }

    public Task<List<Orders>> execute(String eventId) {
        return repository.getOrdersForEvent(eventId);
    }
}
