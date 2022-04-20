package mstc.cloud.worker.job;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * POJO used to track k8s job timings.
 *
 * @author dreedy
 */
@Getter
@Setter
public class JobMetrics {
    private Instant submitted;
    private Instant returned;

    /**
     * Get the total job duration (in milliseconds). If values used to compute this are null, return 0;
     */
    public long getJobDuration() {
        long duration = 0;
        if (submitted != null && returned != null) {
            duration = Duration.between(submitted, returned).toMillis();
        }
        return duration;
    }

}