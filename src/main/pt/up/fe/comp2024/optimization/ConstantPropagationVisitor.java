package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ConstantPropagationVisitor extends PreorderJmmVisitor<SymbolTable, Void> {
    private Boolean modified = false;
    private final Map<String, JmmNode> dictionary = new HashMap<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignmentStmt);
        setDefaultValue(() -> null);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        dictionary.clear();
        JmmNode lastChild = method.getChild(method.getNumChildren() - 1);
        method.putObject("returnExpr", lastChild);
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        varDecl.detach();
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        if (!ASSIGN_STMT.check(varRefExpr.getParent()) && varRefExpr != varRefExpr.getParent().getChild(0)) {
            JmmNode value = dictionary.get(varRefExpr.get("name"));
            if (value != null) {
                varRefExpr.replace(value);
                modified = true;
            }
        }

        return null;
    }

    private Void visitAssignmentStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode var = assignStmt.getChild(0);
        JmmNode value = assignStmt.getChild(1);
        if (INTEGER_LITERAL.check(value) || BOOLEAN_LITERAL.check(value)){
            dictionary.put(var.get("name"), value);
            assignStmt.detach();
        }

        return null;
    }

    public void setModified(Boolean bool) {
        this.modified = bool;
    }


    public Boolean getModified() {
        return this.modified;
    }

}
