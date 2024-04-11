package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.TypeUtils.areTypesAssignable;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class AssignmentType extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignmentStmt);
    }

    private Void visitAssignmentStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode expr1 = assignStmt.getChild(0);
        JmmNode expr2 = assignStmt.getChild(1);

        Type typeExpr1 = getExprType(expr1, table);
        Type typeExpr2 = getExprType(expr2, table);

        if (!areTypesAssignable(typeExpr2, typeExpr1)) {
            String array1 = typeExpr1.isArray() ? " array" : "";
            String array2 = typeExpr2.isArray() ? " array" : "";

            // Create error report
            var message = String.format("Assignment of '%s' of type %s to %s of type %s is not allowed.", expr1.toString(), typeExpr1.getName() + array1, expr2.toString(), typeExpr2.getName() + array2);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
