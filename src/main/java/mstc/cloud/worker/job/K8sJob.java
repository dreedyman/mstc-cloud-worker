package mstc.cloud.worker.job;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import lombok.Getter;

import java.util.*;

/**
 * @author dreedy
 */
@Getter
public class K8sJob {
    private String image;
    private String namespace;
    private String jobName;
    private final List<String> command = new ArrayList<>();
    private final List<String> args = new ArrayList<>();
    private static final int BACKOFF_LIMIT = 2;
    public static final int TIMEOUT_MINS = 15;
    private static final int TTL_SECONDS_AFTER_FINISHED = 5;

    public K8sJob command(List<String> command) {
        this.command.addAll(command);
        return this;
    }

    public K8sJob args(List<String> args) {
        this.args.addAll(args);
        return this;
    }

    public K8sJob image(String image) {
        this.image = image;
        return this;
    }

    public K8sJob name(String name) {
        this.jobName = name.replace(" ", "-");
        return this;
    }

    public K8sJob namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getImage() {
        return image;
    }

    public String getNamespace() {
        return namespace == null ? "default" : namespace;
    }

    public Job getJob() {
        /*Map<String, Quantity> resourceLimitsMap = new HashMap<>();
        resourceLimitsMap.put("cpu", new Quantity("1000m"));
        resourceLimitsMap.put("memory", new Quantity("1000M"));*/
        JobBuilder jobBuilder = new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(this.jobName)
                .withImage(this.image)
                .withCommand(this.command)
                .withArgs(this.args)
                .withImagePullPolicy("IfNotPresent")
                /*.withResources(new ResourceRequirementsBuilder()
                                       .withLimits(resourceLimitsMap)
                                       .withRequests(resourceLimitsMap)
                                       .build())*/
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec();


        return new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                 .withName(jobName)
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(this.jobName)
                .withImage(this.image)
                .withArgs(this.args)
                .withCommand(this.args)
                .withImagePullPolicy("IfNotPresent")
                /*.withResources(new ResourceRequirementsBuilder()
                                       .withLimits(resourceLimitsMap)
                                       .withRequests(resourceLimitsMap)
                                       .build())*/
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
