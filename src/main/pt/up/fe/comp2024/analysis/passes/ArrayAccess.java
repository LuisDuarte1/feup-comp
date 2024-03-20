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

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

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
        //addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }

    private Void visitNewArray(JmmNode array, SymbolTable table) {
        JmmNode arrayNumber = array.getChild(0);

        Type arrayNumberType = getExprType(arrayNumber, table);
        if (arrayNumberType.getName() == "int"){
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
        JmmNode arrayNumber = array.getChild(1);
        Type arrayNumberType = getExprType(arrayNumber, table);
        if (arrayNumberType.getName() == "int"){
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

//    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
//        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");
//
//        // Check if exists a parameter or variable declaration with the same name as the variable reference
//        var varRefName = varRefExpr.get("name");
//
//        if (table.getLocalVariables(currentMethod).stream()
//                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
//            return null;
//        }
//
//        // Var is a field, return
//        if (table.getFields().stream()
//                .anyMatch(param -> param.getName().equals(varRefName))) {
//            return null;
//        }
//
//        // Var is a parameter, return
//        if (table.getParameters(currentMethod).stream()
//                .anyMatch(param -> param.getName().equals(varRefName))) {
//            return null;
//        }
//
//        // Var is a declared variable, return
//        if (table.getLocalVariables(currentMethod).stream()
//                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
//            return null;
//        }
//
//        // Var is an imported class, return
//        if (table.getImports().stream().anyMatch(importClass -> importClass.equals(varRefName))) {
//            return null;
//        }
//
//        // Create error report
//        var message = String.format("Variable '%s' does not exist.", varRefName);
//        addReport(Report.newError(
//                Stage.SEMANTIC,
//                NodeUtils.getLine(varRefExpr),
//                NodeUtils.getColumn(varRefExpr),
//                message,
//                null)
//        );
//
//        return null;
//    }


}
