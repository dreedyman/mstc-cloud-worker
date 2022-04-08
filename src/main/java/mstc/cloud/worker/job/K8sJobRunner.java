package mstc.cloud.worker.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author dreedy
 */
public class K8sJobRunner {
    private static final Logger logger = LoggerFactory.getLogger(K8sJobRunner.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public String submit(K8sJob k8sJob, int timeoutMinutes) {
        Job job = k8sJob.getJob();
        Config config = new ConfigBuilder().withMasterUrl(getBasePath()).build();
        String namespace = k8sJob.getNamespace();
        K8sJobWatcher watcher;
        JobMetrics jobMetrics = new JobMetrics();
        boolean watchLatchAtZero;
        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            String jobName = k8sJob.getJobName();
            logJobCreation(jobName, namespace, job);

            jobMetrics.setSubmitted(Instant.now());
            client.batch()
                  .v1()
                  .jobs()
                  .inNamespace(namespace)
                  .create(job);
            logger.info(String.format("Job %s is created in namespace %s, waiting for result...",
                                      jobName,
                                      namespace));

            CountDownLatch watchLatch = new CountDownLatch(1);
            watcher = new K8sJobWatcher(job,
                                        watchLatch,
                                        namespace,
                                        client,
                                        jobMetrics,
                                        jobName);
            watchLatchAtZero = watch(watchLatch,
                                     watcher,
                                     namespace,
                                     client,
                                     jobName,
                                     timeoutMinutes);
        } finally {
            jobMetrics.setReturned(Instant.now());
            logger.info(String.format(
                    "Job %s metrics: duration: %d ms, job exec: %d ms, job spin-up: %d ms",
                    k8sJob.getJobName(),
                    jobMetrics.getJobDuration(),
                    jobMetrics.getJobExecDuration(),
                    jobMetrics.getJobSpinup()
            ));
        }
        //return watchLatchAtZero && watcher.getSucceeded();
        return watcher.getOutput();
    }

    private String getBasePath() {
        String kubernetesServiceHost = System.getProperty("kubernetes.service.host",
                                                          System.getenv("KUBERNETES_SERVICE_HOST"));
        String kubernetesServicePort = System.getProperty("kubernetes.service.port",
                                                          System.getenv("KUBERNETES_SERVICE_PORT"));
        String basePath = String.format("http://%s:%s", kubernetesServiceHost, kubernetesServicePort);
        logger.debug(String.format("kubernetes basePath: %s", basePath));
        return basePath;
    }

    private boolean watch(CountDownLatch watchLatch,
                          K8sJobWatcher watcher,
                          String namespace,
                          KubernetesClient client,
                          String jobName,
                          long timeoutMinutes) {
        boolean watchLatchAtZero = false;
        try (Watch ignored = client.pods()
                                   .inNamespace(namespace)
                                   .withLabel("job-name", jobName)
                                   .watch(watcher)) {
            watchLatchAtZero = watchLatch.await(timeoutMinutes, TimeUnit.MINUTES);
            if (!watchLatchAtZero) {
                logger.warn("Pod watching timed out");
            }
        } catch (InterruptedException e) {
            logger.warn("Pod watching interrupted", e);
            Thread.currentThread().interrupt();
        }
        return watchLatchAtZero;
    }


    private void logJobCreation(String jobName, String namespace, Job job) {
        if (logger.isDebugEnabled()) {
            try {
                logger.debug(String.format("Creating job %s in namespace %s.%n%s",
                                           jobName,
                                           namespace,
                                           mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job)));
            } catch (JsonProcessingException e) {
                logger.warn(
                        String.format("Caught [JsonProcessingException: %s] while formatting job %s, continue job submission",
                                      e.getMessage(),
                                      jobName));
            }
        } else {
            logger.info(String.format("Creating job %s in namespace %s.", jobName, namespace));
        }
    }

}
