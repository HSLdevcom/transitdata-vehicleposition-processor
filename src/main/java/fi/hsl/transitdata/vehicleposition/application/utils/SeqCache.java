package fi.hsl.transitdata.vehicleposition.application.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;

import java.util.concurrent.TimeUnit;

/**
 * Cache for saving smallest sequence number seen from a certain vehicle.
 * This is needed for producing vehicle positions only from the first vehicle.
 */
public class SeqCache {
    private final Cache<String, Integer> smallestSeqCache = Caffeine
            .newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES).scheduler(Scheduler.systemScheduler())
            .build();

    public boolean isSmallestSeq(String uniqueVehicleId, int seq) {
        final int smallestSeq = smallestSeqCache.asMap().compute(uniqueVehicleId, (key, prev) -> (prev == null || seq <= prev) ? seq : prev);

        return seq == smallestSeq;
    }
}
