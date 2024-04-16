package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.rmi.server.RemoteObjectInvocationHandler;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.TypeUtils.*;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD, this::visitMethodDecl);
        addVisit(Kind.MAIN_METHOD, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a field, return
        Optional<Symbol> field = table.getFields().stream().filter(param -> param.getName().equals(varRefName)).findFirst();
        if (field.isPresent()) {
            varRefExpr.putObject("type", annotateType(field.get().getType(), table));
            return null;
        }

        // Var is a parameter, return
        Optional<Symbol> parameter = table.getParameters(currentMethod).stream().filter(param -> param.getName().equals(varRefName)).findFirst();
        if (parameter.isPresent()) {
            varRefExpr.putObject("type", annotateType(parameter.get().getType(), table));
            return null;
        }

        // Var is a declared variable, return
        Optional<Symbol> variable = table.getLocalVariables(currentMethod).stream().filter(varDecl -> varDecl.getName().equals(varRefName)).findFirst();
        if (variable.isPresent()) {
            varRefExpr.putObject("type", annotateType(variable.get().getType(), table));
            return null;
        }

        // Var is an imported class, return
        if (table.getImports().stream().anyMatch(importClass -> importClass.equals(varRefName))) {
            varRefExpr.putObject("type", annotateType(new Type("imported", false), table));
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }


}
