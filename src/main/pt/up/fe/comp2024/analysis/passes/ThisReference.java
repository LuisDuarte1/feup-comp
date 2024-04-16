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
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.METHOD;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.getVarExprType;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ThisReference extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.THIS_LITERAL, this::visitThis);
    }

    private Void visitThis(JmmNode thisRef, SymbolTable table) {
        Optional<JmmNode> currentMethodNode = thisRef.getAncestor(METHOD);
        if (currentMethodNode.isPresent()) {
            String currentMethod = currentMethodNode.get().getKind();
            if (Objects.equals(currentMethod, "Method"))
                return null;
        }

        // Create error report
        var message = String.format("'this' expression cannot be used in a static method");
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(thisRef),
                NodeUtils.getColumn(thisRef),
                message,
                null)
        );

        return null;
    }

}
