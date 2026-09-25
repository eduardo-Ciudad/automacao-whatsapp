package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.*;

import java.util.List;

public interface AnalyticsMetricsRepository {
    void upsertDailyMetrics(Long siteId, List<DailyMetrics> metrics);

    void upsertDailyBreakdowns(Long siteId, List<DailyBreakdown> breakdowns);

    void upsertPeriodUsers(Long siteId, PeriodUsers users);

    List<DailyMetrics> findDailyMetrics(Long siteId, DateRange range);


    List<DailyBreakdown> findTopBreakdowns(Long siteId, BreakdownDimension dimension, DateRange range, int limit);
}
