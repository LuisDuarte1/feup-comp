package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import static pt.up.fe.comp2024.ast.Kind.VAR_REF_EXPR;

public class ConstantFoldingVisitor extends PreorderJmmVisitor<SymbolTable, Void> {
    private Boolean modified = false;

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        setDefaultValue(() -> null);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode expr1 = binaryExpr.getChild(0);
        JmmNode expr2 = binaryExpr.getChild(1);
        var op = binaryExpr.get("op");

        if (!VAR_REF_EXPR.check(expr1) && !VAR_REF_EXPR.check(expr2)){
            Integer valueExpr1 = Integer.parseInt(expr1.get("value"));
            Integer valueExpr2 = Integer.parseInt(expr2.get("value"));
            int value = 0;
            switch (op) {
                case "+" -> value = valueExpr1 + valueExpr2;
                case "-" -> value = valueExpr1 - valueExpr2;
                case "*" -> value = valueExpr1 * valueExpr2;
                case "/" -> value = valueExpr1 / valueExpr2;
            }

            JmmNodeImpl node = new JmmNodeImpl("IntegerLiteral");
            node.put("value", String.valueOf(value));
            node.setHierarchy(binaryExpr.getHierarchy());
            binaryExpr.replace(node);

            this.setModified(true);
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
