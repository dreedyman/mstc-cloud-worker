package mstc.cloud.worker.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author dreedy
 */
@Service
public class JobRunner {
    private KubernetesClient client;
    private Instant submitted;
    private Instant returned;
    @Autowired
    private ObjectMapper mapper;
    private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

    public void setClient(KubernetesClient client) {
        this.client = client;
    }

    public String submit(K8sJob k8sJob) {
        String namespace = k8sJob.getNamespace();
        String output;
        if (client == null) {
            Config config = new ConfigBuilder().build();
            client = new DefaultKubernetesClient(config);
        }
        try {
            Job job = k8sJob.getJob();
            String jobName = k8sJob.getJobNameUnique();
            logJobCreation(jobName, namespace, job);
            submitted = Instant.now();
            Job createdJob = client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);
            logger.info(String.format("Job \"%s\" is created in namespace %s, timeout of %s minutes, waiting for result...",
                                      jobName,
                                      namespace,
                                      k8sJob.getTimeOut()));
            try {
                output = watch(client, createdJob, k8sJob);
            } catch (JobException e) {
                output = e.getClass().getName() + ": " + e.getMessage();
                logger.warn("Problem running Job", e);
            } catch (Exception e) {
                logger.error("Could not complete the watch for job: " + jobName, e);
                output = e.getClass().getName() + ": " + e.getMessage();
            }

        } finally {
            client.close();
            returned = Instant.now();
            logger.info(String.format("Job %s duration: %d ms", k8sJob.getJobNameUnique(), getJobDuration()));
        }
        return output;
    }

    private String watch(KubernetesClient client, Job job, K8sJob k8sJob) throws JobException {
        String namespace = k8sJob.getNamespace();
        logger.info("Watching job: " + k8sJob.getJobNameUnique());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        JobWatcher watcher = new JobWatcher(countDownLatch);
        client.pods()
              .inNamespace(namespace)
              .withLabel("job-name", k8sJob.getJobNameUnique())
              .watch(watcher);
        boolean returned;
        try {
            returned = countDownLatch.await(k8sJob.getTimeOut(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new JobException("Job interrupted", e);
        }
        if (!returned) {
            throw new JobException("Job timed out");
        }
        String output;
        if (watcher.podError()) {
            output = watcher.getMessage() + ", Reason: " + watcher.getReason();
            if (client.batch().v1().jobs().inNamespace(namespace).withName(job.getMetadata().getName()).delete()) {
                logger.info("Deleted job " + k8sJob.getJobNameUnique());
            } else {
                logger.warn("Failed to delete job " + k8sJob.getJobNameUnique());
            }
        } else {
            try {
                output = client.batch().v1()
                               .jobs()
                               .inNamespace(namespace)
                               .withName(job.getMetadata().getName())
                               .getLog();
            } catch (KubernetesClientException e) {
                output = "Failed getting log, KubernetesClientException: " + e.getMessage();
                logger.warn(output);
            }
        }
        return output;
    }

    private void logJobCreation(String jobName, String namespace, Job job) {
        String message = String.format("Creating job \"%s\" in namespace %s.", jobName, namespace);
        if (logger.isDebugEnabled()) {
            try {
                String jsonJob = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job);
                logger.debug(String.format("%s%n%s", message, jsonJob));
            } catch (JsonProcessingException e) {
                logger.info(message);
            }
        } else {
            logger.info(message);
        }
    }

    private long getJobDuration() {
        long duration = 0;
        if (submitted != null && returned != null) {
            duration = Duration.between(submitted, returned).toMillis();
        }
        return duration;
    }

}
