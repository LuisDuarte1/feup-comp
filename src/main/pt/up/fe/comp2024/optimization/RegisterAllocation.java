package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

public class RegisterAllocation {

    private final OllirResult ollirResult;
    private final int maxRegisters;

    public RegisterAllocation(OllirResult ollirResult, int maxRegisters) {
        this.ollirResult = ollirResult;
        this.maxRegisters = maxRegisters;
    }

    public void optimizeRegisters() {
        ollirResult.getOllirClass().getMethods().forEach(method -> {
            Map<Instruction, Set<String>> liveIns = new HashMap<>();
            Map<Instruction, Set<String>> liveOuts = new HashMap<>();
            liveVariableAnalysis(method, liveIns, liveOuts);
            Map<String, Integer> registerAllocation = buildAndAllocateRegisters(method, liveIns, liveOuts);

            int requiredRegisters = new TreeSet<>(registerAllocation.values()).size();
            if (maxRegisters > 0 && requiredRegisters > maxRegisters) {
                throw new IllegalStateException("Insufficient registers: required " + requiredRegisters + ", available " + maxRegisters);
            }

            registerAllocation.forEach((varName, reg) ->
                    method.getVarTable().get(varName).setVirtualReg(reg)
            );
        });
    }

    private void liveVariableAnalysis(Method method, Map<Instruction, Set<String>> liveIns, Map<Instruction, Set<String>> liveOuts) {
        List<Instruction> instructions = method.getInstructions();
        instructions.forEach(instruction -> {
            liveIns.put(instruction, new HashSet<>());
            liveOuts.put(instruction, new HashSet<>());
        });

        Queue<Instruction> workQueue = new LinkedList<>(instructions);
        while (!workQueue.isEmpty()) {
            Instruction instruction = workQueue.poll();
            Set<String> oldLiveIn = new HashSet<>(liveIns.get(instruction));
            Set<String> oldLiveOut = new HashSet<>(liveOuts.get(instruction));

            Set<String> liveIn = new HashSet<>(getUses(instruction));
            Set<String> liveOut = new HashSet<>();

            for (Instruction successor : getSuccessors(instructions, instruction)) {
                liveOut.addAll(liveIns.get(successor));
            }

            liveIn.addAll(liveOut);
            liveIn.removeAll(getDefs(instruction));

            liveIns.put(instruction, liveIn);
            liveOuts.put(instruction, liveOut);

            if (!oldLiveIn.equals(liveIn) || !oldLiveOut.equals(liveOut)) {
                workQueue.addAll(getPredecessors(instructions, instruction));
            }
        }
    }

    private Set<String> getDefs(Instruction instruction) {
        Set<String> defs = new HashSet<>();
        if (instruction instanceof AssignInstruction assign) {
            if (assign.getDest() instanceof Operand op) {
                defs.add(op.getName());
            }
        }
        return defs;
    }

    private Set<String> getUses(Instruction instruction) {
        Set<String> uses = new HashSet<>();
        switch (instruction.getInstType()) {
            case ASSIGN -> {
                if (instruction instanceof AssignInstruction assign) {
                    uses.addAll(getUses(assign.getRhs()));
                }
            }
            case CALL -> {
                if (instruction instanceof CallInstruction call) {
                    if (call.getInvocationType() != CallType.invokestatic && call.getInvocationType() != CallType.NEW && call.getCaller() instanceof Operand op) {
                        uses.add(op.getName());
                    }
                    call.getArguments().stream()
                            .filter(op -> op instanceof Operand)
                            .forEach(op -> uses.add(((Operand) op).getName()));
                }
            }
            case RETURN -> {
                if (instruction instanceof ReturnInstruction ret) {
                    if (ret.getOperand() instanceof Operand op) {
                        uses.add(op.getName());
                    }
                }
            }
            case UNARYOPER -> {
                if (instruction instanceof UnaryOpInstruction unop) {
                    if (unop.getOperand() instanceof Operand op) {
                        uses.add(op.getName());
                    }
                }
            }
            case BINARYOPER -> {
                if (instruction instanceof BinaryOpInstruction binop) {
                    if (binop.getLeftOperand() instanceof Operand l_op) {
                        uses.add(l_op.getName());
                    }
                    if (binop.getRightOperand() instanceof Operand r_op) {
                        uses.add(r_op.getName());
                    }
                }
            }
            case NOPER -> {
                if (instruction instanceof OpCondInstruction opcond) {
                    opcond.getOperands().stream()
                            .filter(op -> op instanceof Operand)
                            .forEach(op -> uses.add(((Operand) op).getName()));
                }
            }
            case PUTFIELD -> {
                if (instruction instanceof PutFieldInstruction put) {
                    if (put.getValue() instanceof Operand op) {
                        uses.add(op.getName());
                    }
                }
            }
        }
        return uses;
    }

    private Map<String, Integer> buildAndAllocateRegisters(Method method, Map<Instruction, Set<String>> liveIns, Map<Instruction, Set<String>> liveOuts) {
        Map<String, Set<String>> interferenceGraph = new HashMap<>();
        List<Instruction> instructions = method.getInstructions();

        for (Instruction instruction : instructions) {
            Set<String> liveVariables = new HashSet<>(liveIns.get(instruction));
            liveVariables.addAll(liveOuts.get(instruction));
            liveVariables.addAll(getDefs(instruction));

            for (String var : liveVariables) {
                interferenceGraph.putIfAbsent(var, new HashSet<>());
            }

            List<String> vars = new ArrayList<>(liveVariables);
            for (int i = 0; i < vars.size(); i++) {
                for (int j = i + 1; j < vars.size(); j++) {
                    String var1 = vars.get(i);
                    String var2 = vars.get(j);
                    interferenceGraph.get(var1).add(var2);
                    interferenceGraph.get(var2).add(var1);
                }
            }
        }

        return allocateRegisters(interferenceGraph);
    }

    private Map<String, Integer> allocateRegisters(Map<String, Set<String>> graph) {
        Deque<String> stack = new ArrayDeque<>();
        Set<String> spilledNodes = new HashSet<>();
        Map<String, Integer> colorMap = new HashMap<>();
        int numColors = maxRegisters > 0 ? maxRegisters : graph.size();

        Map<String, Set<String>> mutableGraph = new HashMap<>(graph);

        while (!mutableGraph.isEmpty()) {
            boolean foundNode = false;

            for (Map.Entry<String, Set<String>> entry : mutableGraph.entrySet()) {
                String node = entry.getKey();
                Set<String> neighbors = entry.getValue();

                if (neighbors.size() < numColors) {
                    stack.push(node);
                    mutableGraph.remove(node);
                    for (String neighbor : neighbors) {
                        mutableGraph.get(neighbor).remove(node);
                    }
                    foundNode = true;
                    break;
                }
            }

            if (!foundNode) {
                String nodeToSpill = mutableGraph.keySet().iterator().next();
                stack.push(nodeToSpill);
                mutableGraph.remove(nodeToSpill);
                for (String neighbor : graph.get(nodeToSpill)) {
                    mutableGraph.get(neighbor).remove(nodeToSpill);
                }
                spilledNodes.add(nodeToSpill);
            }
        }

        while (!stack.isEmpty()) {
            String node = stack.pop();
            boolean[] usedColors = new boolean[numColors];

            for (String neighbor : graph.get(node)) {
                Integer neighborColor = colorMap.get(neighbor);
                if (neighborColor != null) {
                    usedColors[neighborColor] = true;
                }
            }

            for (int color = 0; color < usedColors.length; color++) {
                if (!usedColors[color]) {
                    colorMap.put(node, color);
                    break;
                }
            }
        }

        if (!spilledNodes.isEmpty()) {
            System.out.println("Register spilling occurred for variables: " + spilledNodes);
        }

        return colorMap;
    }

    private List<Instruction> getSuccessors(List<Instruction> instructions, Instruction instruction) {
        int index = instructions.indexOf(instruction);
        if (index >= 0 && index < instructions.size() - 1) {
            return Collections.singletonList(instructions.get(index + 1));
        }
        return Collections.emptyList();
    }

    private List<Instruction> getPredecessors(List<Instruction> instructions, Instruction instruction) {
        int index = instructions.indexOf(instruction);
        if (index > 0 && index < instructions.size()) {
            return Collections.singletonList(instructions.get(index - 1));
        }
        return Collections.emptyList();
    }
}
