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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

        long methods = table.getMethods().stream().filter(var -> var.equals(currentMethod)).count();
        if (methods > 1) {
            var message = String.format("Method '%s' is duplicated", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }

        List<Symbol> parameters = table.getParameters(currentMethod);
        Map<String, Long> parameterCounts = parameters.stream()
                .collect(Collectors.groupingBy(Symbol::getName, Collectors.counting()));

        parameterCounts.forEach((paramName, count) -> {
            if (count > 1) {
                var message = String.format("Parameter '%s' is duplicated", paramName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
        });

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

            //Return a varargs param
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
                }
            }
        }
        if (method.getObject("returnType") instanceof JmmNode) { //Declare return type as varargs
            JmmNode declaredReturnNode = method.getObject("returnType", JmmNode.class);
            Type declaredReturnType = getTypeFromGrammarType(declaredReturnNode);

            if (declaredReturnType.hasAttribute("isVarArgs") && declaredReturnType.getObject("isVarArgs", Boolean.class)) {
                var message = "Method returns cannot be vararg";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
        }

        if (!Objects.equals(currentMethod, "main")) {
            JmmNode returnType = method.getObject("returnType", JmmNode.class);
            JmmNode returnExpr = method.getObject("returnExpr", JmmNode.class);
            Type returnExprType = getExprType(returnExpr, table);

            if (Objects.equals(returnExprType, null) && Objects.equals(returnExpr.getKind(), "MethodCall"))
                return null;

            if (!Objects.equals(returnExprType.getName(), "imported") && !Objects.equals(getTypeFromGrammarType(returnType), returnExprType)) {
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
