package vasco;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;

public class ParameterAnalysis {
    static CallGraph cg;
    static Set<String> visited;
    // Set of all classes
    static Set<String> classes;
    // Map of methods with list of used/unused parameters
    static Map<String, ArrayList<Boolean>> methodParamsUsed;
    // Local to used map.
    public static Map<SootMethod, Map<Local, Boolean>> localMap;
    // Number of unused parameters
    static int falseVal = 0;
    // Number of total parameters
    static int totVal = 0;

    protected static void doPreAnalysis() {
        cg = Scene.v().getCallGraph();
        visited = new HashSet<>();
        classes = new HashSet<>();
        methodParamsUsed = new HashMap<>();
        localMap = new HashMap<>();

        List<SootMethod> methods = Scene.v().getEntryPoints();
        for (SootMethod method: methods) {
            if (!visited.contains(method.getSignature())) {
                TopSort(method);
            }
        }
    }

    // Used for creating a topological sort.
    // In case of cycles (recursion), just dfs order.
    protected static void TopSort(SootMethod method) {
        if (method.isJavaLibraryMethod()) {
            return;
        }

        Iterator<Edge> itrTargets = cg.edgesOutOf(method);
        while (itrTargets.hasNext()) {
            SootMethod target = (SootMethod) itrTargets.next().getTgt();
            if (!visited.contains(target.getSignature())) {
                visited.add(target.getSignature());
                TopSort(target);
            }
        }

        processMethod(method);
    }

    // Used for checking if the given method is a phantom method.
    protected static boolean isPhantom(SootMethod method) {
        String[] parts = method.getSignature().split("\\.");
        if (method.isPhantom() || !method.hasActiveBody()) {
            return true;
        }
        return false;
    }

    protected static void processMethod(SootMethod method) {
        if (method.isJavaLibraryMethod() || isPhantom(method)) {
            return;
        }

        Body body = method.getActiveBody();
        UnitGraph cfg = new BriefUnitGraph(body);
        PatchingChain<Unit> units = body.getUnits();

        Integer numParams = method.getParameterCount();
        Map<Local, Integer> paramVars = new HashMap<>();
        ArrayList<Boolean> paramsImp = new ArrayList<>();

        for (Unit u: units) {
            Set<Value> used = new HashSet<>();

            for (ValueBox use: u.getUseBoxes()) {
                used.add(use.getValue());
            }

            if (u instanceof JIdentityStmt) {
                JIdentityStmt curr = (JIdentityStmt) u;
                Integer paramInd;

                if (curr.getRightOp() instanceof ParameterRef) {
                    paramInd = ((ParameterRef) curr.getRightOp()).getIndex();
                    paramVars.put((Local) curr.getLeftOp(), paramInd);
                    paramsImp.add(false);
                }
            }

            if (u instanceof JAssignStmt || u instanceof JInvokeStmt) {
                Stmt curr = (Stmt) u;

                // For application method calls, check if the callee also used the parameters.
                if (curr.containsInvokeExpr() && methodParamsUsed.containsKey(curr.getInvokeExpr().getMethod().getSignature())) {
                    Integer ind = 0;
                    String target = curr.getInvokeExpr().getMethod().getSignature();
                    Map<Local, ArrayList<Integer>> argMap = new HashMap<>();

                    // We use a map because a parameter can be given more than once as an argument for the call.
                    // So we must take the OR for each use.
                    for (Value arg: curr.getInvokeExpr().getArgs()) {
                        if (arg instanceof Local && paramVars.containsKey((Local) arg)) {
                            if (!argMap.containsKey((Local) arg)) {
                                argMap.put((Local) arg, new ArrayList<Integer>());
                            }
                            argMap.get((Local) arg).add(ind);
                        }
                        ind++;
                    }

                    for (Local arg: argMap.keySet()) {
                        Boolean argUsed = false;
                        for (Integer argInd: argMap.get(arg)) {
                            argUsed = argUsed || methodParamsUsed.get(target).get(argInd);
                        }
                        if (!argUsed) {
                            used.remove((Local) arg);
                        }
                    }
                }
            }

            // Parameters still in the use set should be set as important.
            for (Value use: used) {
                if (use instanceof Local && paramVars.containsKey((Local) use)) {
                    paramsImp.set(paramVars.get((Local) use), true);
                }
            }
        }

        methodParamsUsed.put(method.getSignature(), paramsImp);
        classes.add(method.getDeclaringClass().toString());

        // Create a map of Local parameters as well to be used for comparison.
        localMap.put(method, new HashMap<Local, Boolean>());
        for (int i = 0; i < numParams; ++i) {
            localMap.get(method).put(method.getActiveBody().getParameterLocal(i), paramsImp.get(i));
        }

        // System.out.println(method.getSignature() + ": " + paramsImp);
        for (boolean imp: paramsImp) {
            if (!imp) {
                falseVal++;
            }
            totVal++;
        }
    }
}
