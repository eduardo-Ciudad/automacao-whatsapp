package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.TrackedSite;

import java.util.List;
import java.util.Optional;

public interface TrackedSiteRepository {
    List<TrackedSite> findAllWithAnalytics();

    Optional<TrackedSite> findById(Long siteId);

    void markSyncSuccess(Long siteId);

    void markSyncFailure(Long siteId, String accessStatus, String error);
}