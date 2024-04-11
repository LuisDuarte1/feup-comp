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
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);
    }

    private void createErrorReport(JmmNode expr, JmmNode operand, Type typeExpr1, String operator) {
        String array = typeExpr1.isArray() ? " array" : "";
        // Create error report
        var message = String.format("Operand '%s' of type %s is not compatible with operator %s.", operand.toString(), typeExpr1.getName() + array, operator);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(expr),
                NodeUtils.getColumn(expr),
                message,
                null)
        );
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode expr1 = binaryExpr.getChild(0);
        JmmNode expr2 = binaryExpr.getChild(1);

        Type opType = getExprType(binaryExpr, table);
        binaryExpr.putObject("type", opType);


        Type typeExpr1 = getExprType(expr1, table);
        if (!typeExpr1.equals(opType)) {
            createErrorReport(binaryExpr, expr1, typeExpr1, binaryExpr.get("op"));
        }

        Type typeExpr2 = getExprType(expr2, table);
        if (!typeExpr2.equals(opType)) {
            createErrorReport(binaryExpr, expr2, typeExpr2, binaryExpr.get("op"));
        }

        return null;
    }

    private Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        JmmNode expr = unaryExpr.getChild(0);

        Type opType = getExprType(unaryExpr, table);
        Type typeExpr = getExprType(expr, table);

        unaryExpr.putObject("type", opType);

        if (!typeExpr.equals(opType)) {
            createErrorReport(unaryExpr, expr, typeExpr, unaryExpr.get("op"));
        }

        return null;
    }


}
