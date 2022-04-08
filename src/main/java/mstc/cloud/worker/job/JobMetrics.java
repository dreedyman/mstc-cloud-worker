package mstc.cloud.worker.job;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * POJO used to track k8s job timings.
 *
 * @author dreedy
 */
@Getter
public class JobMetrics {
    @Setter
    private Instant submitted;
    @Setter
    private Instant returned;
    private Instant jobEntry;
    private Instant jobExit;

    /**
     * Parse the returned job log, setting the job's entry and exit instances.
     *
     * @param log The job's log content.
     */
    public void setJobTiming(String log) {
        if (log == null) {
            return;
        }
        OffsetDateTime beginning = null;
        OffsetDateTime end = null;
        for (String line : log.split("\n")) {
            String[] parts = line.split(" ");
            try {
                OffsetDateTime o = OffsetDateTime.parse(parts[0]);
                if (beginning == null) {
                    beginning = o;
                }
                end = o;
            } catch (DateTimeParseException e) {
                /* ignore */
            }
        }
        if (beginning != null) {
            jobEntry = beginning.toInstant();
        }

        if (end != null) {
            jobExit = end.toInstant();
        }
    }

    /**
     * @return The duration (in milliseconds) between job submission and job entry.
     * If values used to compute this are null, return 0;
     */
    public long getJobSpinup() {
        long spinup = 0;
        if (submitted != null && jobEntry != null) {
            spinup = Duration.between(submitted,
                                      jobEntry).toMillis();
        }
        return spinup;
    }

    /**
     * Get how long the agent ran for.
     *
     * @return How long the agent ran for (in milliseconds). If values used to compute this are null, return 0;
     */
    public long getJobExecDuration() {
        long exec = 0;
        if (jobEntry != null && jobExit != null) {
            exec = Duration.between(jobEntry, jobExit).toMillis();
        }
        return exec;
    }

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