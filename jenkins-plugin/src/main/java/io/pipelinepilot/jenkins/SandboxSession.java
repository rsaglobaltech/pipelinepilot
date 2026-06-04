package io.pipelinepilot.jenkins;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Tracks one ephemeral IDE sandbox/replay execution: the temporary job and its run.
 * The job lives under a hidden folder and is destroyed on cleanup so the original
 * pipeline configuration is never mutated.
 */
public class SandboxSession {

    public enum State { STARTING, RUNNING, FINISHED, FAILED, STOPPED }

    private final String sessionId;
    private final String user;
    private final long startedAt = System.currentTimeMillis();

    private volatile WorkflowJob job;
    private volatile WorkflowRun run;
    private volatile State state = State.STARTING;

    public SandboxSession(String sessionId, String user) {
        this.sessionId = sessionId;
        this.user = user;
    }

    public String getSessionId() { return sessionId; }
    public String getUser() { return user; }
    public long getStartedAt() { return startedAt; }

    public WorkflowJob getJob() { return job; }
    public void setJob(WorkflowJob job) { this.job = job; }

    public WorkflowRun getRun() { return run; }
    public void setRun(WorkflowRun run) { this.run = run; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
}
