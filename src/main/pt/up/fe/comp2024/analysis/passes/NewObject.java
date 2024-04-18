package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.NEW_OBJECT;
import static pt.up.fe.comp2024.ast.TypeUtils.*;

public class NewObject extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(NEW_OBJECT, this::visitNewObject);
    }

    private Void visitNewObject(JmmNode newObject, SymbolTable table) {

        var className = newObject.get("name");

        // Var is an imported class, return
        if (table.getImports().stream().anyMatch(importClass -> importClass.equals(className))) {
            newObject.putObject("type", annotateType(new Type("imported", false), table));
            return null;
        }

        if (Objects.equals(table.getClassName(), className)) return null;

        // Create error report
        var message = String.format("Class '%s' does not exist.", className);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(newObject),
                NodeUtils.getColumn(newObject),
                message,
                null)
        );

        return null;
    }


}
