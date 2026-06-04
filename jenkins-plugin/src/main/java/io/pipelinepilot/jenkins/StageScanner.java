package io.pipelinepilot.jenkins;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives {@code StageInfo} records from a run's flow graph so the IDE can draw
 * the stage graph. A stage (or parallel branch) is a {@link BlockStartNode} that
 * carries a {@link LabelAction}; parallel branches additionally carry a
 * {@link ThreadNameAction}.
 */
final class StageScanner {

    private StageScanner() {}

    /** Returns one JSON object per stage/branch, ordered by appearance. */
    static List<JSONObject> scan(WorkflowRun run) {
        List<JSONObject> stages = new ArrayList<>();
        FlowExecution exec = run.getExecution();
        if (exec == null) return stages;

        // Map each block start node id -> its matching end node (if the block has ended).
        Map<String, BlockEndNode<?>> endByStart = new HashMap<>();
        List<FlowNode> all = new ArrayList<>();
        for (FlowNode n : new DepthFirstScanner().allNodes(exec)) {
            all.add(n);
            if (n instanceof BlockEndNode) {
                BlockEndNode<?> end = (BlockEndNode<?>) n;
                endByStart.put(end.getStartNode().getId(), end);
            }
        }
        all.sort(Comparator.comparingInt(node -> safeId(node.getId())));

        boolean building = run.isBuilding();
        for (FlowNode n : all) {
            if (!(n instanceof BlockStartNode)) continue;
            LabelAction label = n.getAction(LabelAction.class);
            if (label == null) continue;

            String name = label.getDisplayName();
            if (name == null || name.isEmpty()) continue;
            boolean parallel = n.getAction(ThreadNameAction.class) != null;

            BlockEndNode<?> end = endByStart.get(n.getId());
            String status;
            if (end == null) {
                status = building ? "RUNNING" : "SUCCESS";
            } else if (hasError(end) || hasError(n)) {
                status = "FAILED";
            } else {
                status = "SUCCESS";
            }

            JSONObject s = new JSONObject();
            s.put("name", name);
            s.put("status", status);
            s.put("parallel", parallel);
            String parent = enclosingStageName(n);
            if (parent != null) s.put("parent", parent);
            stages.add(s);
        }
        return stages;
    }

    private static String enclosingStageName(FlowNode n) {
        for (FlowNode b : n.getEnclosingBlocks()) {
            LabelAction la = b.getAction(LabelAction.class);
            if (la != null && la.getDisplayName() != null && !la.getDisplayName().isEmpty()) {
                return la.getDisplayName();
            }
        }
        return null;
    }

    private static boolean hasError(FlowNode n) {
        return n.getAction(ErrorAction.class) != null;
    }

    private static int safeId(String id) {
        try { return Integer.parseInt(id); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }
}
