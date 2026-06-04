package io.pipelinepilot.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.GET;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Root action exposing the IDE-facing API at {@code /ide/...}. Backs the
 * IntelliJ plugin's validate / runSandbox / replay / logs / session endpoints.
 *
 * Security:
 *   - mutating endpoints are {@link RequirePOST} (CSRF crumb enforced),
 *   - every endpoint checks the caller's Jenkins permissions,
 *   - sandbox scripts run under the Groovy sandbox in throwaway jobs.
 */
@Extension
public class IdeApiRootAction implements RootAction {

    @Override public String getIconFileName() { return null; } // hidden from UI
    @Override public String getDisplayName() { return "PipelinePilot IDE API"; }
    @Override public String getUrlName() { return "ide"; }

    // --- POST /ide/validate (form: jenkinsfile) -----------------------------

    @RequirePOST
    public void doValidate(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        String jenkinsfile = req.getParameter("jenkinsfile");

        JSONObject result = new JSONObject();
        JSONArray diagnostics = new JSONArray();
        // Integration point: delegate to the declarative linter
        // (org.jenkinsci.plugins.pipeline.modeldefinition) for full validation.
        // Skeleton check: balanced braces so the IDE round-trip works end-to-end.
        int balance = 0;
        for (char c : (jenkinsfile == null ? "" : jenkinsfile).toCharArray()) {
            if (c == '{') balance++;
            else if (c == '}') balance--;
        }
        boolean valid = jenkinsfile != null && balance == 0;
        if (!valid) {
            diagnostics.add(diagnostic("ERROR", "Unbalanced braces in pipeline", 1, 1));
        }
        result.put("valid", valid);
        result.put("diagnostics", diagnostics);
        writeJson(rsp, result);
    }

    // --- POST /ide/runSandbox (JSON: RunSandboxRequest) ---------------------

    @RequirePOST
    public void doRunSandbox(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        Jenkins.get().checkPermission(Item.CREATE);
        JSONObject body = readJson(req);
        String jenkinsfile = body.optString("jenkinsfile", "");
        String user = currentUser();

        JSONObject result = new JSONObject();
        try {
            SandboxSession session = SandboxSessionStore.get().createAndRun(jenkinsfile, user);
            result.put("sessionId", session.getSessionId());
            result.put("accepted", true);
        } catch (Exception e) {
            result.put("sessionId", "");
            result.put("accepted", false);
            result.put("message", e.getMessage());
        }
        writeJson(rsp, result);
    }

    // --- POST /ide/replay (JSON: ReplayRequest) -----------------------------

    @RequirePOST
    public void doReplay(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        Jenkins.get().checkPermission(Item.CREATE);
        JSONObject body = readJson(req);
        String jenkinsfile = body.optString("jenkinsfile", "");
        String stageName = body.optString("stageName", null);
        String sourceSessionId = body.optString("sourceSessionId", null);
        String user = currentUser();

        JSONObject result = new JSONObject();
        try {
            SandboxSession session;
            if (stageName != null && !stageName.isEmpty() && sourceSessionId != null && !sourceSessionId.isEmpty()) {
                // True per-stage replay: restart the source declarative run from this stage.
                session = SandboxSessionStore.get().restartStage(sourceSessionId, stageName);
            } else {
                // Whole-pipeline replay: a fresh sandbox from the (possibly edited) Jenkinsfile.
                session = SandboxSessionStore.get().createAndRun(jenkinsfile, user);
            }
            result.put("sessionId", session.getSessionId());
            result.put("accepted", true);
        } catch (Exception e) {
            result.put("sessionId", "");
            result.put("accepted", false);
            result.put("message", e.getMessage());
        }
        writeJson(rsp, result);
    }

    // --- GET /ide/logs/{sessionId}?start=N ----------------------------------

    @GET
    public void doLogs(StaplerRequest2 req, StaplerResponse2 rsp,
                       @org.kohsuke.stapler.QueryParameter String sessionId,
                       @org.kohsuke.stapler.QueryParameter(fixEmpty = true) Long start) throws IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        long from = start == null ? 0L : start;

        SandboxSession session = SandboxSessionStore.get().find(sessionId);
        JSONObject result = new JSONObject();
        JSONArray events = new JSONArray();

        if (session == null) {
            result.put("events", events);
            result.put("nextStart", from);
            result.put("more", false);
            writeJson(rsp, result);
            return;
        }

        WorkflowRun run = session.getRun();
        long nextStart = from;
        boolean more = true;
        if (run != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            nextStart = run.getLogText().writeLogTo(from, out);
            String chunk = out.toString(StandardCharsets.UTF_8);
            for (String line : chunk.split("\n", -1)) {
                if (!line.isEmpty()) events.add(logEvent(sessionId, line));
            }
            // Emit current stage states so the IDE can build/refresh the stage graph.
            for (JSONObject stage : StageScanner.scan(run)) {
                JSONObject ev = new JSONObject();
                ev.put("sessionId", sessionId);
                ev.put("timestampMillis", System.currentTimeMillis());
                ev.put("stage", stage);
                ev.put("finished", false);
                events.add(ev);
            }
            SandboxSessionStore.get().refreshState(session);
            boolean building = run.isBuilding();
            more = building;
            if (!building) {
                JSONObject end = new JSONObject();
                end.put("sessionId", sessionId);
                end.put("timestampMillis", System.currentTimeMillis());
                end.put("buildStatus", run.getResult() == null ? "UNKNOWN" : run.getResult().toString());
                end.put("finished", true);
                events.add(end);
            }
        }
        result.put("events", events);
        result.put("nextStart", nextStart);
        result.put("more", more);
        writeJson(rsp, result);
    }

    // --- DELETE/POST /ide/session/{sessionId} -------------------------------

    @RequirePOST
    public void doSession(StaplerRequest2 req, StaplerResponse2 rsp,
                          @org.kohsuke.stapler.QueryParameter String sessionId) throws IOException {
        Jenkins.get().checkPermission(Item.DELETE);
        SandboxSessionStore.get().destroy(sessionId);
        rsp.setStatus(200);
    }

    // --- helpers ------------------------------------------------------------

    private static String currentUser() {
        String name = Jenkins.getAuthentication2().getName();
        return name == null ? "anonymous" : name;
    }

    private static JSONObject diagnostic(String severity, String message, int line, int column) {
        JSONObject d = new JSONObject();
        d.put("severity", severity);
        d.put("message", message);
        d.put("line", line);
        d.put("column", column);
        return d;
    }

    private static JSONObject logEvent(String sessionId, String line) {
        JSONObject e = new JSONObject();
        e.put("sessionId", sessionId);
        e.put("timestampMillis", System.currentTimeMillis());
        e.put("line", line);
        e.put("finished", false);
        return e;
    }

    private static JSONObject readJson(StaplerRequest2 req) throws IOException {
        String raw = IOUtils.toString(req.getReader());
        return raw.isEmpty() ? new JSONObject() : JSONObject.fromObject(raw);
    }

    private static void writeJson(@NonNull StaplerResponse2 rsp, @NonNull JSONObject json) throws IOException {
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.getWriter().print(json.toString());
    }
}
