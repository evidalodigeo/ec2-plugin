package hudson.plugins.ec2;

/**
 * Class that represent the running status details of EC2.
 *
 * @author Carlos Pereda
 */
public class InstanceStateDetails {


    private com.amazonaws.services.ec2.model.InstanceStatus instanceStatus;
    private com.amazonaws.services.ec2.model.InstanceState instanceState;

    public InstanceStateDetails(com.amazonaws.services.ec2.model.InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public InstanceStateDetails(com.amazonaws.services.ec2.model.InstanceState instanceState) {
        this.instanceState = instanceState;
    }

    public InstanceState getState() {

        InstanceState instanceState;
        if (this.instanceStatus == null) {
            instanceState = InstanceState.find(this.instanceState.getName());
        } else {
            instanceState = InstanceState.find(this.instanceStatus.getInstanceState().getName());
            if (InstanceState.RUNNING.equals(instanceState)) {
                if (areAllChecksPassed(this.instanceStatus)) {
                    instanceState = InstanceState.RUNNING_CHECKS_OK;
                } else if (!areAllChecksInitializingOrOK()) {
                    instanceState = InstanceState.RUNNING_CHECKS_KO;
                } else {
                    instanceState = InstanceState.RUNNING_CHECKS_PENDING;
                }
            }
        }
        return instanceState;
    }

    private boolean areAllChecksInitializingOrOK() {
        return "ok".equals(instanceStatus.getInstanceStatus().getStatus())
                || "initializing".equals(instanceStatus.getInstanceStatus().getStatus())
                || "ok".equals(instanceStatus.getSystemStatus().getStatus())
                || "initializing".equals(instanceStatus.getSystemStatus().getStatus());
    }

    private boolean areAllChecksPassed(com.amazonaws.services.ec2.model.InstanceStatus instanceStatus) {
        return "ok".equals(instanceStatus.getInstanceStatus().getStatus())
                && "ok".equals(instanceStatus.getSystemStatus().getStatus());
    }

    @Override
    public String toString() {
        return "InstanceStateDetails{" +
                "instanceStatus=" + instanceStatus +
                ", instanceState=" + instanceState +
                '}';
    }
}
