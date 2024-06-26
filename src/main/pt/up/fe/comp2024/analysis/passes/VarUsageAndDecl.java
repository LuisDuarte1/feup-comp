package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.METHOD;
import static pt.up.fe.comp2024.ast.Kind.OBJECT_TYPE;
import static pt.up.fe.comp2024.ast.TypeUtils.*;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class VarUsageAndDecl extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {

        Optional<JmmNode> currentMethodNode = varDecl.getAncestor(METHOD);
        if (currentMethodNode.isPresent()) {
            String currentMethod = currentMethodNode.get().get("name");

            long localVariables = table.getLocalVariables(currentMethod).stream().filter(var -> var.getName().equals(varDecl.get("name"))).count();
            if (localVariables > 1) {
                var message = String.format("Variable declarations cannot be duplicated");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );

            }
        }
        JmmNode type = varDecl.getChild(0);
        if (OBJECT_TYPE.check(type)) {
            if (!table.getImports().contains(type.get("name")) && !Objects.equals(type.get("name"), table.getClassName())) {
                var message = String.format("Class %s does not exist.", type.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
            }
        }

        Type varType = getTypeFromGrammarType(varDecl.getChild(0));
        if (varType.hasAttribute("isVarArgs") && varType.getObject("isVarArgs", Boolean.class)) {
            // Create error report
            var message = String.format("Variable declarations cannot be vararg");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null)
            );
        }

        return null;
    }


    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {

        String varOrigin = getVarExprOrigin(varRefExpr, table);
        if (Objects.equals(varOrigin, FIELD)) { //A field is being used
            Optional<JmmNode> currentMethodNode = varRefExpr.getAncestor(METHOD);
            //If method is static, throw error
            if (currentMethodNode.isPresent()) {
                String currentMethod = currentMethodNode.get().getKind();
                if (Objects.equals(currentMethod, "Method")) //Main is only static method allowed
                    return null;
            }
            // Create error report
            var message = String.format("Field usage in static method");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varRefExpr),
                    NodeUtils.getColumn(varRefExpr),
                    message,
                    null)
            );
        }
        return null;

    }
}
