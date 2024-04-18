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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.METHOD;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.getVarExprType;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class FieldsAndImports extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.PROGRAM, this::visitProgram);
    }

    private Void visitProgram(JmmNode program, SymbolTable table) {
        List<String> imports = table.getImports();
        Map<String, Long> importCounts = imports.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        importCounts.forEach((importName, count) -> {
            if (count > 1) {
                var message = String.format("Import '%s' is duplicated", importName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        -1,
                        -1,
                        message,
                        null)
                );
            }
        });

        List<Symbol> fields = table.getFields();
        Map<String, Long> fieldCounts = fields.stream()
                .collect(Collectors.groupingBy(Symbol::getName, Collectors.counting()));

        fieldCounts.forEach((fieldName, count) -> {
            if (count > 1) {
                var message = String.format("Field '%s' is duplicated", fieldName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        -1,
                        -1,
                        message,
                        null)
                );
            }
        });

        return null;
    }

}
