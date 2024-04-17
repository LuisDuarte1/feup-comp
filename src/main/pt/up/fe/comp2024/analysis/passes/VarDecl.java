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
import static pt.up.fe.comp2024.ast.TypeUtils.*;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class Varargs extends AnalysisVisitor {

    //private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        Optional<JmmNode> currentMethodNode = varDecl.getAncestor(METHOD);
        if (currentMethodNode.isPresent()) {
            String currentMethod = currentMethodNode.get().get("name");

            // Var is a declared variable, return
            Optional<Symbol> variable = table.getLocalVariables(currentMethod).stream().filter(varDecl -> varDecl.getName().equals(varRefName)).findFirst();
            if (variable.isPresent()) {

                return null;
            }
        }



        Type varType = getTypeFromGrammarType(varDecl.getChild(0));
        if (varType.hasAttribute("isVarArgs") && varType.getObject("isVarArgs", Boolean.class)) {
            // Create error report
            var message = String.format("Variable declarations cannot be vararg");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null)
            );
        }

        return null;
    }


}
