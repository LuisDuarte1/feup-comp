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
public class Conditions extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_STMT, this::visitIfCondition);
        addVisit(Kind.WHILE_STMT, this::visitWhileCondition);
    }

    private Void visitIfCondition(JmmNode ifStmt, SymbolTable table) {
        JmmNode ifCondition = ifStmt.getChild(0);
        Type conditionType = getExprType(ifCondition, table);
        if (Objects.equals(conditionType.getName(), "boolean"))
            return null;

        // Create error report
        var message = "Expressions in conditions must return a boolean";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(ifStmt),
                NodeUtils.getColumn(ifStmt),
                message,
                null)
        );

        return null;
    }

    private Void visitWhileCondition(JmmNode whileStmt, SymbolTable table) {
        JmmNode whileCondition = whileStmt.getChild(0);
        Type conditionType = getExprType(whileCondition, table);
        if (Objects.equals(conditionType.getName(), "boolean"))
            return null;

        // Create error report
        var message = "Expressions in conditions must return a boolean";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(whileStmt),
                NodeUtils.getColumn(whileStmt),
                message,
                null)
        );

        return null;
    }

}
