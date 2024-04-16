package pt.up.fe.comp2024.analysis.passes;

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

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.getVarExprType;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ArrayAccess extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.NEW_ARRAY, this::visitNewArray);
        addVisit(Kind.LIST_ACCESS, this::visitListAccess);
        addVisit(Kind.ARRAY, this::visitArray);
    }

    private Void visitNewArray(JmmNode array, SymbolTable table) {
        JmmNode arrayNumber = array.getChild(0);

        Type arrayNumberType = getExprType(arrayNumber, table);
        if (Objects.equals(arrayNumberType.getName(), "int")) {
            return null;
        }

        // Create error report
        var message = String.format("Access '%s' isn't a number.", arrayNumber);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(array),
                NodeUtils.getColumn(array),
                message,
                null)
        );

        return null;
    }

    private Void visitListAccess(JmmNode array, SymbolTable table) {
        JmmNode arrayName = array.getChild(0);
        Type arrayNameType = getExprType(arrayName, table);
        if (!arrayNameType.isArray()) {
            // Create error report
            var message = "Access isn't done over an array";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(array),
                    NodeUtils.getColumn(array),
                    message,
                    null)
            );
        }

        JmmNode arrayNumber = array.getChild(1);
        Type arrayNumberType = getExprType(arrayNumber, table);
        if (Objects.equals(arrayNumberType.getName(), "int")) {
            return null;
        }

        // Create error report
        var message2 = String.format("Access '%s' isn't a number.", arrayNumber);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(array),
                NodeUtils.getColumn(array),
                message2,
                null)
        );

        return null;
    }

    private Void visitArray(JmmNode array, SymbolTable table) {
        List<JmmNode> arrayElements = array.getChildren();
        if (!arrayElements.isEmpty()){
            for (JmmNode arrayElement : arrayElements)
                if (!Objects.equals(arrayElement.getKind(), "IntegerLiteral")) {
                    // Create error report
                    var message = "All array elements must have 'int' type";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(array),
                            NodeUtils.getColumn(array),
                            message,
                            null)
                    );
                }
        }
        return null;
    }

}
