package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 */
public class ArithmeticOperation extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        //addVisit(Kind.UNARY_EXPR, this::visitMethodDecl);
    }

    private void createErrorReport(JmmNode binaryExpr, JmmNode expr1, Type typeExpr1, String operator) {
        // Create error report
        var message = String.format("Operand '%s' of type %s is not compatible with operator %s.", expr1.toString(), typeExpr1.getName(), operator);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message,
                null)
        );
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode expr1 = binaryExpr.getChild(0);
        JmmNode expr2 = binaryExpr.getChild(1);

        Type opType = getExprType(binaryExpr, table);

        Type typeExpr1 = getExprType(expr1, table);
        if (typeExpr1 != opType) {
            createErrorReport(binaryExpr, expr1, typeExpr1, binaryExpr.get("op"));
        }

        Type typeExpr2 = getExprType(expr2, table);
        if (typeExpr2 != opType) {
            createErrorReport(binaryExpr, expr2, typeExpr2, binaryExpr.get("op"));
        }

        return null;
    }


}
