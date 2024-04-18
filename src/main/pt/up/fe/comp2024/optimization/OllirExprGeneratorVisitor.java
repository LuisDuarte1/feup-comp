package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.ClassType;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;
import java.util.regex.Pattern;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(UNARY_EXPR, this::visitUnaryExpr);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(NEW_OBJECT, this::visitNewObject);
        setDefaultVisit(this::defaultVisit);
    }

    public OllirExprResult visitForceTemp(JmmNode node, String type){
        var computation = new StringBuilder();
        String register = OptUtils.getTemp();
        var nodeComp = visit(node);

        computation.append(nodeComp.getComputation());

        final Pattern pattern = Pattern.compile("(.*)(\\.[a-z0-9A-Z]*)");
        var matcher = pattern.matcher(nodeComp.getCode());
        if(!matcher.find()) throw new RuntimeException("Could find match for returned code");
        var returnedRegister = matcher.group(1);

        computation.append(String.format("%s%s :=%s %s%s;",
                register,
                type,
                type,
                returnedRegister,
                type
                )).append("\n");

        return new OllirExprResult(register+type, computation);

    }

    protected OllirExprResult visitNewObject(JmmNode node, Void unused){
        var computation = new StringBuilder();
        String register = OptUtils.getTemp();
        String name = node.get("name");
        computation.append(String.format("%s.%s :=.%s new(%s).%s;",
                register, name, name, name, name)).append("\n");
        var arguments = node.getChildren().stream().map(this::visit).toList();
        arguments.stream().map(OllirExprResult::getComputation).toList().forEach(computation::append);
        computation.append(String.format("invokespecial(%s.%s,\"<init>\"%s).V;",
                        register,
                        name,
                        arguments.stream().map(OllirExprResult::getCode).reduce("", (a,b) -> a + "," + b)
                        ))
                .append("\n");
        return new OllirExprResult(register+"."+name, computation);
    }


    protected OllirExprResult visitUnaryExpr(JmmNode node, Void unused){

        String code = node.get("op") +
                // TODO (luisd): Maybe throw if there are multiple children?
                node.getJmmChild(0);

        return new OllirExprResult(code);

    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode());
        computation.append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var computation = new StringBuilder();
        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        if (table.getFields().stream().anyMatch((val) -> Objects.equals(val.getName(), id))){
            var fieldTmp = OptUtils.getTemp();

            computation.append(String.format(
                    "%s%s :=%s getfield(this, %s%s)%s;",
                    fieldTmp, ollirType,
                    ollirType,
                    id,ollirType,
                    ollirType
            ))
            .append("\n");

            return new OllirExprResult(fieldTmp+ollirType, computation);
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

    public String getTypeFromParent(JmmNode node){
        JmmNode currentNode = node;
        while (!(currentNode.hasAttribute("type") || METHOD.check(currentNode) || MAIN_METHOD.check(currentNode))){
            currentNode = currentNode.getParent(); // it always exists here
        }
        if(currentNode.hasAttribute("type")){
            return OptUtils.toOllirType(currentNode.getObject("type", Type.class));
        }
        return null;
    }


    public OllirExprResult visitMethodCall(JmmNode node, Void unused){
        var computation = new StringBuilder();
        String code = OptUtils.getTemp();
        String type = getTypeFromParent(node);
        if (table.getMethods().contains(node.get("name")) && THIS_LITERAL.check(node.getJmmChild(0))) {
            return methodCallHelper(node, computation, code+type, "this", type);
        }
        else if (table.getMethods().contains(node.get("name"))){
            var fieldComp = visit(node.getChild(0));
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code+type, fieldComp.getCode(), type);
        }
        var ref = node.getChild(0).get("name");
        // it mean it's an class field
        if(table.getFields().stream().anyMatch((value) -> Objects.equals(value.getName(), ref))){
            var fieldComp = visit(node.getChild(0));
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code+type, fieldComp.getCode(), type);
        }

        //we then check if it belongs to a local variable
        var methodName = "";
        JmmNode currNode = node;
        while(!(METHOD.check(currNode) || MAIN_METHOD.check(currNode))){
            currNode = currNode.getParent();
        }
        methodName = currNode.get("name");

        if(table.getLocalVariables(methodName).stream().anyMatch((val) -> Objects.equals(val.getName(), ref))){
            var fieldComp = visit(node.getChild(0));
            computation.append(fieldComp.getComputation());
            return methodCallHelper(node, computation, code+type, fieldComp.getCode(), type);
        }

        if (table.getImports().contains(ref)){
            var arguments = node.getChildren().stream().skip(1).map((child) -> {
                // we can only infer the type if it's on this class
                if (METHOD_CALL.check(child) && table.getMethods().contains(child.get("name"))){
                    var childReturnType = table.getReturnType(child.get("name"));
                    return visitForceTemp(child, OptUtils.toOllirType(childReturnType));
                }
                return visit(child);
            }).toList();
            arguments.stream().map(OllirExprResult::getComputation).toList().forEach(computation::append);
            if(type != null){
                computation.append(String.format("%s%s :=%s invokestatic(%s, \"%s\"%s)%s;\n",
                        code, type, type,
                        ref,
                        node.get("name"),
                        arguments.stream().map(OllirExprResult::getCode).reduce("", (a,b) -> a + "," + b),
                        type
                ));
            } else {
                code = String.format("invokestatic(%s, \"%s\"%s).V",
                        ref,
                        node.get("name"),
                        arguments.stream().map(OllirExprResult::getCode).reduce("", (a,b) -> a + "," + b));
            }
        }
        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult methodCallHelper(JmmNode node, StringBuilder computation, String code, String className,
                                             String type) {
        var arguments = node.getChildren().stream().skip(1).map((child) -> {
            // we can only infer the type if it's on this class
            if (METHOD_CALL.check(child) && table.getMethods().contains(child.get("name"))){
                var childReturnType = table.getReturnType(child.get("name"));
                return visitForceTemp(child, OptUtils.toOllirType(childReturnType));
            }
            return visit(child);
        }).toList();
        var object = node
                .getObject("object", JmmNode.class);
        var objectType = THIS_LITERAL.check(object) ? "this" : object.getObject("type", Type.class).getName();
        arguments.stream().map(OllirExprResult::getComputation).toList().forEach(computation::append);
        if(type != null){
            computation.append(String.format("%s%s :=%s invokevirtual(%s, \"%s\"%s)%s;\n",
                    code, type, type,
                    className,
                    node.get("name"),
                    arguments.stream().map(OllirExprResult::getCode).reduce("", (a,b) -> a + "," + b),
                    type
            ));
        } else {
            code = String.format("invokevirtual(%s, \"%s\"%s).V",
                    className,
                    node.get("name"),
                    arguments.stream().map(OllirExprResult::getCode).reduce("", (a,b) -> a + "," + b));
        }
        return new OllirExprResult(code, computation.toString());
    }
}
