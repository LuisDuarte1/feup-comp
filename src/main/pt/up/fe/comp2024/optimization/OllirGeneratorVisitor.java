package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.ClassType;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.*;
//TODO(luisd): fix OllirGneratorVistor

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final String END_TAG = ":\n";
    private final String TAB = "\t";

    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;


    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD, this::visitMethodDecl);
        addVisit(MAIN_METHOD, this::visitMainMethod);
        addVisit(PARAM, this::visitParam);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::defaultVisit);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(EXPR_STMT, this::defaultAddVisit);
        addVisit(IF_STMT, this::visitIfStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        var code = new StringBuilder();

        JmmNode ifCond = node.getObject("ifCond", JmmNode.class);
        JmmNode ifExpr = node.getObject("ifExpr", JmmNode.class);
        JmmNode elseExpr = node.getObject("elseExpr", JmmNode.class);

        var ifTag = OptUtils.getIfTag();
        var enfIfTag = "end" + ifTag;

        var cond = exprVisitor.visit(ifCond);
        code.append(cond.getComputation());
        code.append("if").append(SPACE).append("(");
        code.append(cond.getCode()).append(") ").append("goto ").append(ifTag).append(END_STMT);

        var elseCode = visit(elseExpr);
        code.append(TAB).append(elseCode);
        code.append("goto").append(SPACE).append(enfIfTag).append(END_STMT);

        var thenCode = visit(ifExpr);
        code.append(ifTag).append(END_TAG);
        code.append(TAB).append(thenCode);

        code.append(enfIfTag).append(END_TAG);

        return code.toString();
    }

    private String visitMethodCall(JmmNode node, Void unused) {
        var gen = exprVisitor.visitMethodCall(node, unused);
        return gen.getComputation() + NL + gen.getCode();
    }

    private String defaultAddVisit(JmmNode node, Void unused) {
        var code = new StringBuilder();
        for (var child : node.getChildren()) {
            var childCode = visit(child);
            if (childCode.isEmpty()) continue;
            code.append(childCode).append(";").append(NL);
        }
        return code.toString();
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        var code = new StringBuilder("import ");

        var modules = node.getObject("modules", ArrayList.class);
        for (int i = 0; i < modules.size() - 1; i++) {
            code.append(modules.get(i));
            code.append('.');
        }
        code.append(modules.get(modules.size() - 1));

        code.append(END_STMT);

        return code.toString();
    }


    private String visitVarDeclClass(JmmNode jmmNode) {
        if (OBJECT_TYPE.check(jmmNode.getChild(0))) {

            return ".field public " + jmmNode.get("name") + "." + jmmNode.getChild(0).get("name") + ";" + NL;
        } else {
            return ".field public " + jmmNode.get("name") + OptUtils.toOllirType(jmmNode.getChild(0)) + ";" + NL;

        }
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();


        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        String origin = TypeUtils.getVarExprOrigin(node.getJmmChild(0), table);
        if (Objects.equals(origin, TypeUtils.FIELD)) {
            var rhs = exprVisitor.visitForceTemp(node.getJmmChild(1), typeString);
            code.append(rhs.getComputation());


            code.append(String.format(
                            "putfield(this, %s%s, %s).V",
                            node.getJmmChild(0).get("name"), typeString,
                            rhs.getCode()
                    ))
                    .append(END_STMT);
        } else {
            var lhs = exprVisitor.visit(node.getJmmChild(0));
            var rhs = exprVisitor.visit(node.getJmmChild(1));


            // code to compute the children
            code.append(lhs.getComputation());
            code.append(rhs.getComputation());

            code.append(lhs.getCode());
            code.append(SPACE);

            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);

            code.append(rhs.getCode());


            code.append(END_STMT);
        }

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        return id + typeCode;
    }

    private String visitMainMethod(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method public static main(args.array.String).V");

        code.append(L_BRACKET);
        for (int i = 0; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }
        code.append("ret.V;").append(NL);
        code.append(R_BRACKET);
        return code.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        var paramsList = node.getChildren(PARAM);
        var params = paramsList.stream().map(this::visit).toList();
        StringBuilder paramCode = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i == params.size() - 1) {
                paramCode.append(params.get(i));
                break;
            }
            paramCode.append(params.get(i)).append(',');
        }
        code.append("(" + paramCode + ")");

        // type
        var retType = OptUtils.toOllirType(node.getObject("returnType", JmmNode.class));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var paramsIndex = 0;
        if (!paramsList.isEmpty()) {
            paramsIndex = node.getChildren().indexOf(paramsList.get(paramsList.size() - 1));
        }
        var afterParam = paramsIndex + 1;
        for (int i = afterParam; i < node.getNumChildren() - 1; i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        //parse return statement, if it exists
        var lastChild = node.getChildren().get(node.getNumChildren() - 1);
        if (!retType.equals("V")) {
            var expr = exprVisitor.visitForceTemp(lastChild, retType);
            code.append(expr.getComputation());
            code.append(String.format("ret%s %s;", retType, expr.getCode()));
        } else {
            code.append("ret.V;");
        }
        code.append(NL);

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        if (!Objects.equals(table.getSuper(), "")) {
            code.append(String.format(" extends %s", table.getSuper()));
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            if (VAR_DECL.check(child)) {
                code.append(visitVarDeclClass(child));
            } else {
                code.append(visit(child));
            }

            //TODO: Rework this
            //if (METHOD_DECL.check(child) && needNl) {
            //    code.append(NL);
            //    needNl = false;
            //}

        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        var code = new StringBuilder();
        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }
}
