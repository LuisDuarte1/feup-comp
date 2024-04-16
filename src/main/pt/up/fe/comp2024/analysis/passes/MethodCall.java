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

import static pt.up.fe.comp2024.ast.Kind.METHOD;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.getVarExprType;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class MethodCall extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_CALL, this::visitMethodCall);
    }

    private Void visitMethodCall(JmmNode memberCall, SymbolTable table) {
        JmmNode func = memberCall.getChild(0);
        Type funcType = TypeUtils.getExprType(func, table);

        if (table.getClassName().equals(funcType.getName()) && table.getMethods().contains(memberCall.get("name"))) {
            List<Symbol> params = table.getParameters(memberCall.get("name"));

            if(Objects.equals(params.get(params.size() - 1).getType(), new Type("IntVarargsType", true))) {
                boolean flag = true;
                for(int i = 0; i < params.size(); i++)
                    flag &= ("int".equals(TypeUtils.getExprType(memberCall.getChild(i + 1), table).getName()));
                if (flag) return null;
            }

            if(params.size() == (memberCall.getChildren().size() - 1)) {
                boolean flag = true;
                for(int i = 0; i < params.size(); i++)
                    flag &= (params.get(i).getType() == TypeUtils.getExprType(memberCall.getChild(i + 1), table));
                if (flag) return null;
            }
        }

        if (table.getImports().contains(memberCall.get("name"))
                || table.getImports().contains(funcType.getName())
                || (table.getClassName().equals(funcType.getName()) && table.getImports().contains(table.getSuper()))) {
            return null;
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
