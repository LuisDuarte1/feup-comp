package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.ClassType;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static pt.up.fe.comp2024.ast.Kind.*;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.getVarExprType;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String END_TAG = ":\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBool);
        addVisit(UNARY_EXPR, this::visitUnaryExpr);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(NEW_OBJECT, this::visitNewObject);
        addVisit(THIS_LITERAL, this::visitThis);
        addVisit(PRIORITY_EXPR, this::visitPriorityExpr);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(LIST_ACCESS, this::visitListAccess);
        addVisit(LENGTH_CALL, this::visitLengthCall);
        addVisit(ARRAY, this::visitArray);
        setDefaultVisit(this::defaultVisit);
    }

    public OllirExprResult visitArray(JmmNode node, Void unused) {
        List<JmmNode> children = node.getChildren();

        Type type = getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);
        String ollirElemType = ollirType.replaceFirst("\\.array", "");

        return arrayHelper(ollirType, ollirElemType, children);
    }

    private OllirExprResult arrayHelper(String arrayType, String elemType, List<JmmNode> children) {
        StringBuilder computation = new StringBuilder();
        String arrayTmp = OptUtils.getTemp();

        computation.append(arrayTmp).append(arrayType).append(SPACE).append(ASSIGN).append(arrayType).append(SPACE);
        computation.append("new(array,").append(SPACE).append(children.toArray().length).append(".i32").append(")").append(arrayType).append(END_STMT);

        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            OllirExprResult result = visit(child);
            computation.append(result.getComputation());
            computation.append(arrayTmp).append("[").append(i).append(elemType).append("]").append(elemType).append(SPACE).append(ASSIGN).append(elemType).append(SPACE);
            computation.append(result.getCode()).append(END_STMT);
        }

        return new OllirExprResult(arrayTmp + arrayType, computation.toString());
    }

    public OllirExprResult visitLengthCall(JmmNode node, Void unused) {
        JmmNode array = node.getJmmChild(0);
        OllirExprResult arrayResult = visit(array);

        StringBuilder computation = new StringBuilder();
        String tmp = OptUtils.getTemp();
        computation.append(arrayResult.getComputation());
        computation.append(tmp).append(".i32").append(SPACE).append(ASSIGN).append(".i32").append(SPACE);
        computation.append("arraylength(").append(arrayResult.getCode()).append(").i32").append(END_STMT);
        return new OllirExprResult(tmp + ".i32", computation.toString());
    }

    public OllirExprResult visitListAccess(JmmNode node, Void unused) {
        JmmNode arrayNode = node.getJmmChild(0);
        JmmNode indexNode = node.getJmmChild(1);

        OllirExprResult array = visit(arrayNode);
        OllirExprResult index;
        Type indexType = getExprType(indexNode, table);

        var code = new StringBuilder();
        var computation = new StringBuilder();

        if (!(indexNode.isInstance(INTEGER_LITERAL) || indexNode.isInstance(BOOLEAN_LITERAL))) {
            index = visitForceTemp(indexNode, OptUtils.toOllirType(indexType));
        } else index = visit(indexNode);

        String varName;

        if (arrayNode.isInstance(ARRAY)) {
            varName = array.getCode();
            varName = varName.substring(0, varName.indexOf('.'));
        } else {
            varName = arrayNode.get("name");
        }
        code.append(varName).append("[").append(index.getCode()).append("]").append(".i32");

        return new OllirExprResult(code.toString(), array.getComputation() + index.getComputation());
    }

    public OllirExprResult visitNewArray(JmmNode node, Void unused) {
        var code = new StringBuilder();
        var lengthExpr = visit(node.getChild(0));
        code.append("new(array,").append(SPACE).append(lengthExpr.getCode()).append(")").append(OptUtils.toOllirType(node));

        return new OllirExprResult(code.toString(), lengthExpr.getComputation());
    }


    public OllirExprResult visitPriorityExpr(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    public OllirExprResult visitThis(JmmNode node, Void unused) {
        return new OllirExprResult("this." + table.getClassName());
    }

    public OllirExprResult visitBool(JmmNode node, Void unused) {
        return new OllirExprResult((Objects.equals(node.get("value"), "true") ? "1" : "0") + ".bool");
    }

    public OllirExprResult visitForceTemp(JmmNode node, String type) {
        var computation = new StringBuilder();
        String register = OptUtils.getTemp();
        var nodeComp = visit(node);

        computation.append(nodeComp.getComputation());

        final Pattern pattern = Pattern.compile("(.*)(\\.[a-z0-9A-Z]*)");
        var matcher = pattern.matcher(nodeComp.getCode());
        if (!matcher.find()) {
            return nodeComp;
        }
        var returnedRegister = matcher.group(1);

        computation.append(String.format("%s%s :=%s %s%s;", register, type, type, returnedRegister, type)).append("\n");

        return new OllirExprResult(register + type, computation);

    }

    protected OllirExprResult visitNewObject(JmmNode node, Void unused) {
        var computation = new StringBuilder();
        String register = OptUtils.getTemp();
        String name = node.get("name");
        computation.append(String.format("%s.%s :=.%s new(%s).%s;", register, name, name, name, name)).append("\n");
        var arguments = node.getChildren().stream().map(this::visit).toList();
        arguments.stream().map(OllirExprResult::getComputation).toList().forEach(computation::append);
        computation.append(String.format("invokespecial(%s.%s,\"<init>\"%s).V;", register, name, arguments.stream().map(OllirExprResult::getCode).reduce("", (a, b) -> a + "," + b))).append("\n");
        return new OllirExprResult(register + "." + name, computation);
    }


    protected OllirExprResult visitUnaryExpr(JmmNode node, Void unused) {
        String register = OptUtils.getTemp();

        var computation = new StringBuilder();
        var insideContent = visit(node.getJmmChild(0));
        Type resType = getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);

        computation.append(insideContent.getComputation());
        computation.append(String.format("%s%s :=%s %s%s %s;", register, resOllirType, resOllirType, node.get("op"), resOllirType, insideContent.getCode())).append("\n");

        return new OllirExprResult(register + resOllirType, computation);

    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        boolean isShortCircuit = Objects.equals(node.get("op"), "&&");

        // code to compute self
        Type resType = getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();
        String code = OptUtils.getTemp() + resOllirType;

        // code to compute the children
        computation.append(lhs.getComputation());
        if (!isShortCircuit) {
            computation.append(rhs.getComputation());
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(lhs.getCode()).append(SPACE);

            Type type = getExprType(node, table);
            computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE).append(rhs.getCode());
            computation.append(END_STMT);
        } else {
            String andTag = OptUtils.getAndTag();
            String endAndTag = "end_" + andTag;
            computation.append("if").append(SPACE).append("(").append(lhs.getCode()).append(")").append(SPACE);
            //If the lhs of the and is false, assign the variable to false
            computation.append("goto").append(SPACE).append(andTag).append(END_STMT);
            computation.append(code).append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append("0.bool").append(END_STMT);
            computation.append("goto").append(SPACE).append(endAndTag).append(END_STMT);
            //Else compute the rhs and assign the variable to it
            computation.append(andTag).append(END_TAG);
            computation.append(rhs.getComputation());
            computation.append(code).append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append(rhs.getCode()).append(END_STMT);
            computation.append(endAndTag).append(END_TAG);
        }

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var computation = new StringBuilder();
        var id = node.get("name");
        Type type = getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        if (Objects.equals(TypeUtils.getVarExprOrigin(node, table), TypeUtils.FIELD)) {
            var fieldTmp = OptUtils.getTemp();

            computation.append(String.format("%s%s :=%s getfield(this, %s%s)%s;", fieldTmp, ollirType, ollirType, id, ollirType, ollirType)).append("\n");

            return new OllirExprResult(fieldTmp + ollirType, computation);
        }

        //TODO (luisd): maybe check imported?

        return new OllirExprResult(code, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

    public String getTypeFromParent(JmmNode node) {
        JmmNode currentNode = node;
        while (!(currentNode.hasAttribute("type") || METHOD.check(currentNode) || MAIN_METHOD.check(currentNode))) {
            currentNode = currentNode.getParent(); // it always exists here
        }
        if (currentNode.hasAttribute("type")) {
            return OptUtils.toOllirType(currentNode.getObject("type", Type.class));
        }
        return null;
    }


    public OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        var computation = new StringBuilder();
        String code = OptUtils.getTemp();
        String type = getTypeFromParent(node);
        if ((THIS_LITERAL.check(node.getJmmChild(0)) && table.getMethods().contains(node.get("name"))) && type == null) {
            var foundReturnType = OptUtils.toOllirType(table.getReturnType(node.get("name")));
            return methodCallHelper(node, computation, code + foundReturnType, "this." + table.getClassName(), foundReturnType, true);
        }
        if (THIS_LITERAL.check(node.getJmmChild(0))) {
            return methodCallHelper(node, computation, code + type, "this." + table.getClassName(), type, false);
        }

        JmmNode refNode = node.getChild(0);
        while (PRIORITY_EXPR.check(refNode)) {
            refNode = refNode.getChild(0);
        }
        String ref = refNode.get("name");

        if (!VAR_REF_EXPR.check(refNode)) {
            var fieldComp = visit(refNode);
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code + type, fieldComp.getCode(), type, false);
        }

        String origin = TypeUtils.getVarExprOrigin(refNode, table);

        // it means it's a class field
        if (Objects.equals(origin, TypeUtils.FIELD)) {
            var fieldComp = visit(refNode);
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code + type, fieldComp.getCode(), type, false);
        }

        //we then check if it belongs to a local variable
        var methodName = "";
        JmmNode currNode = node;
        while (!(METHOD.check(currNode) || MAIN_METHOD.check(currNode))) {
            currNode = currNode.getParent();
        }
        methodName = currNode.get("name");

        if (Objects.equals(origin, TypeUtils.LOCAL) && Objects.equals(getExprType(refNode, table).getName(), table.getClassName()) && type == null) {
            var foundReturnType = OptUtils.toOllirType(table.getReturnType(node.get("name")));
            var fieldComp = visit(refNode);
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code + foundReturnType, fieldComp.getCode(), foundReturnType, true);
        }

        if (Objects.equals(origin, TypeUtils.PARAM) && Objects.equals(table.getParameters(methodName).stream().filter((val) -> Objects.equals(val.getName(), ref)).findFirst().orElseThrow().getType().getName(), table.getClassName()) && type == null) {
            var foundReturnType = OptUtils.toOllirType(table.getReturnType(node.get("name")));
            return methodCallHelper(node, computation, code + foundReturnType, code + "." + table.getClassName(), foundReturnType, true);
        }

        if (Objects.equals(origin, TypeUtils.LOCAL) || Objects.equals(origin, TypeUtils.PARAM)) {
            var fieldComp = visit(refNode);
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code + type, fieldComp.getCode(), type, false);
        }

        if (Objects.equals(origin, TypeUtils.IMPORTS)) {
            var children = node.getChildren();
            var arguments = IntStream.range(0, node.getNumChildren()).skip(1).boxed().toList().stream().map(i -> {
                // we can only infer the type if it's on this class
                var child = children.get(i);
                var argumentReturnType = table.getParametersTry(node.get("name"));

                if (argumentReturnType.isEmpty() && METHOD_CALL.check(child)) {
                    var childReturnType = table.getReturnType(child.get("name"));
                    return visitForceTemp(child, OptUtils.toOllirType(childReturnType));
                }
                if (argumentReturnType.isPresent() && METHOD_CALL.check(child)) {
                    return visitForceTemp(child, OptUtils.toOllirType(argumentReturnType.get().get(i - 1).getType()));
                }
                if (LIST_ACCESS.check(child)) {
                    return visitForceTemp(child, ".i32");
                }
                return visit(child);
            }).toList();

            arguments.stream().map(OllirExprResult::getComputation).toList().forEach(computation::append);
            if (type != null) {
                computation.append(String.format("%s%s :=%s invokestatic(%s, \"%s\"%s)%s;\n", code, type, type, ref, node.get("name"), arguments.stream().map(OllirExprResult::getCode).reduce("", (a, b) -> a + "," + b), type));
                code += type;
            } else {
                code = String.format("invokestatic(%s, \"%s\"%s).V", ref, node.get("name"), arguments.stream().map(OllirExprResult::getCode).reduce("", (a, b) -> a + "," + b));
            }
        }
        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult methodCallHelper(JmmNode node, StringBuilder computation, String code, String className, String type, boolean forceNoType) {
        var children = node.getChildren();
        var argumentReturnType = table.getParametersTry(node.get("name"));
        var arguments = new ArrayList<OllirExprResult>();

        for (int i = 1; i < node.getNumChildren(); i++) {
            var child = children.get(i);

            if (argumentReturnType.isEmpty() && METHOD_CALL.check(child)) {
                var childReturnType = table.getReturnType(child.get("name"));
                arguments.add(visitForceTemp(child, OptUtils.toOllirType(childReturnType)));
            } else if (argumentReturnType.isPresent()) {
                var childType = argumentReturnType.get().get(i - 1).getType();

                if (childType.hasAttribute("isVarArgs") && childType.getObject("isVarArgs", Boolean.class)) {
                    String arrayType = OptUtils.toOllirType(childType);
                    String elemType = arrayType.replaceFirst("\\.array", "");
                    if (!child.isInstance(ARRAY)) {
                        arguments.add(arrayHelper(arrayType, elemType, children.subList(i, children.size())));
                    } else arguments.add(visit(child));
                    break;
                } else if (METHOD_CALL.check(child)) {
                    arguments.add(visitForceTemp(child, OptUtils.toOllirType(argumentReturnType.get().get(i - 1).getType())));
                } else {
                    arguments.add(visit(child));
                }
            } else {
                arguments.add(visit(child));
            }
        }

        arguments.stream().map(OllirExprResult::getComputation).toList().forEach(computation::append);
        if (type != null && !forceNoType) {
            computation.append(String.format("%s :=%s invokevirtual(%s, \"%s\"%s)%s;\n", code, type, className, node.get("name"), arguments.stream().map(OllirExprResult::getCode).reduce("", (a, b) -> a + "," + b), type));
        } else {
            code = String.format("invokevirtual(%s, \"%s\"%s)%s", className, node.get("name"), arguments.stream().map(OllirExprResult::getCode).reduce("", (a, b) -> a + "," + b), type != null ? type : ".V");
        }
        return new OllirExprResult(code, computation.toString());
    }
}
