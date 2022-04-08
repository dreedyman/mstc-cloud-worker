package mstc.cloud.worker.job;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author dreedy
 */
@Getter
public class K8sJob {
    private String image;
    private String namespace;
    private String jobName;
    private List<String> args = new ArrayList<>();
    private static final int BACKOFF_LIMIT = 2;
    public static final int TIMEOUT_MINS = 15;
    private static final int TTL_SECONDS_AFTER_FINISHED = 5;

    public K8sJob args(String... args) {
        Collections.addAll(this.args, args);
        return this;
    }

    public K8sJob image(String image) {
        this.image = image;
        return this;
    }

    public K8sJob name(String name) {
        this.jobName = name;
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
        String jobName = String.format("%s-%s",
                                       this.jobName, UUID.randomUUID());
        return new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .withLabels(Collections.singletonMap("label1", "maximum-length-of-63-characters"))
                .withAnnotations(Collections.singletonMap("annotation1", "some-very-long-annotation"))
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(this.jobName)
                //.withImage("perl")
                .withImage(image)
                .withArgs("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)")
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
