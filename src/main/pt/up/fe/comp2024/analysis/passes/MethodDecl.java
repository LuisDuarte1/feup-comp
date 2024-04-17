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

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.getTypeFromGrammarType;

public class MethodDecl extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD, this::visitMethodDecl);
        addVisit(Kind.MAIN_METHOD, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        String currentMethod = method.get("name");

        List<Symbol> parameters = table.getParameters(currentMethod);
        if (!parameters.isEmpty()) {
            for (int i = 0; i < parameters.size() - 1; i++)
                if (parameters.get(i).getType().hasAttribute("isVarArgs") && parameters.get(i).getType().getObject("isVarArgs", Boolean.class)) {
                    var message = "Vararg type must be the last parameter";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(method),
                            NodeUtils.getColumn(method),
                            message,
                            null)
                    );
                }

            var lastParameter = parameters.get(parameters.size() - 1);

            if (lastParameter.getType().hasAttribute("isVarArgs") && lastParameter.getType().getObject("isVarArgs", Boolean.class)) {
                JmmNode returnExpr = method.getObject("returnExpr", JmmNode.class);

                if (Objects.equals(returnExpr.getKind(), "VarRefExpr")) {
                    if (Objects.equals(returnExpr.get("name"), lastParameter.getName())) {
                        var message = String.format("Method returns cannot be vararg");
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(method),
                                NodeUtils.getColumn(method),
                                message,
                                null)
                        );
                    }
                } else return null;
            }

        }

        if (!Objects.equals(currentMethod, "main")) {
            JmmNode returnType = method.getObject("returnType", JmmNode.class);
            JmmNode returnExpr = method.getObject("returnExpr", JmmNode.class);
            Type returnExprType = getExprType(returnExpr, table);

            if (Objects.equals(returnExprType, null) && Objects.equals(returnExpr.getKind(), "MethodCall"))
                return null;

            if (!Objects.equals(returnExprType.getName(), "imported") && !Objects.equals(getTypeFromGrammarType(returnType), returnExprType)) {
                var t = getTypeFromGrammarType(returnType);
                var message = "Incompatible return type";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
        }

        return null;
    }
}
