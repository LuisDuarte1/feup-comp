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
import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;
import static pt.up.fe.comp2024.ast.TypeUtils.*;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class MethodCall extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_CALL, this::visitMethodCall);
    }

    private Void visitMethodCall(JmmNode memberCall, SymbolTable table) {
        JmmNode object = memberCall.getObject("object", JmmNode.class);
        Type objectType = TypeUtils.getExprType(object, table);

        if (table.getClassName().equals(objectType.getName()) && table.getMethods().contains(memberCall.get("name"))) {
            List<Symbol> params = table.getParameters(memberCall.get("name"));
            //No parameters to verify and method is in symbol table, return
            if (params.isEmpty()) {
                return null;
            }

            var lastParamType = params.get(params.size() - 1).getType();
            boolean hasVarargs = lastParamType.hasAttribute("isVarArgs") && lastParamType.getObject("isVarArgs", Boolean.class);
            var numNonVarargsParams = hasVarargs ? params.size() - 1 : params.size();

            boolean flag = true;

            for (int i = 0; i < numNonVarargsParams; i++)
                flag &= params.get(i).getType().equals(TypeUtils.getExprType(memberCall.getChild(i + 1), table));
            if (hasVarargs) {
                Type firstVarargsParamType = TypeUtils.getExprType(memberCall.getChild(numNonVarargsParams + 1), table);
                if (firstVarargsParamType.equals(new Type("int", true))) {
                    if ((memberCall.getNumChildren() - 1) == (numNonVarargsParams + 1)) {
                        return null;
                    } else {
                        var message = String.format("Only one int array may be passed as varargs argument of method call '%s()'", memberCall.get("name"));

                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(memberCall),
                                NodeUtils.getColumn(memberCall),
                                message,
                                null)
                        );
                    }
                }

                for (int j = numNonVarargsParams; j < memberCall.getNumChildren() - 1; j++) {
                    Type paramType = TypeUtils.getExprType(memberCall.getChild(j + 1), table);
                    flag &= paramType.equals(new Type("int", false));
                }
                if (flag) return null;
                else {
                    var message = String.format("Passing argument of wrong type to argument of method call '%s()'", memberCall.get("name"));

                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(memberCall),
                            NodeUtils.getColumn(memberCall),
                            message,
                            null)
                    );
                }
            }

            if(flag){
                return null;
            }
        }

        if (table.getImports().contains(memberCall.get("name"))
                || table.getImports().contains(objectType.getName())
                || (table.getClassName().equals(objectType.getName()) && table.getImports().contains(table.getSuper()))){ //Current class extends imported class
            return null;
        }

        if (memberCall.hasAttribute("object") && VAR_REF_EXPR.check(memberCall.getChild(0))) {
            var importName = memberCall.getChild(0).get("name");
            if (table.getImports().contains(importName)) {
                return null;
            }
        }


        var message = String.format("Couldn't resolve member method call '%s()'", memberCall.get("name"));

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(memberCall),
                NodeUtils.getColumn(memberCall),
                message,
                null)
        );
        return null;

    }


}
