package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.Set;
import java.util.List;

public enum Kind {
    PROGRAM,
    IMPORT_DECL,
    CLASS_DECL,
    VAR_DECL,
    PARAM,
    ID,

    //Type
    INT_ARRAY_TYPE,
    INT_VARARGS_TYPE,
    BOOL_TYPE,
    STR_TYPE,
    INT_TYPE,
    OBJECT_TYPE,

    //MethodDecl
    MAIN_METHOD,
    METHOD,

    //Stmt
    BLOCK_STMT,
    IF_STMT,
    WHILE_STMT,
    EXPR_STMT,
    ASSIGN_STMT,
    LIST_ASSIGN_STMT,

    //Expr
    PRIORITY_EXPR,
    UNARY_EXPR,
    BINARY_EXPR,
    LIST_ACCESS,
    LENGTH_CALL,
    METHOD_CALL,
    NEW_OBJECT,
    NEW_ARRAY,
    ARRAY,
    INTEGER_LITERAL,
    BOOLEAN_LITERAL,
    THIS_LITERAL,
    VAR_REF_EXPR;

    public static final Set<Kind> TYPES = Set.of(INT_ARRAY_TYPE, INT_VARARGS_TYPE, BOOL_TYPE, STR_TYPE, INT_TYPE, OBJECT_TYPE);
    public static final Set<Kind> METHOD_DECLS = Set.of(MAIN_METHOD, METHOD);
    public static final Set<Kind> STATEMENTS = Set.of(BLOCK_STMT, IF_STMT, WHILE_STMT, EXPR_STMT, ASSIGN_STMT, LIST_ASSIGN_STMT);
    public static final Set<Kind> EXPRESSIONS = Set.of(PRIORITY_EXPR, UNARY_EXPR, BINARY_EXPR, LIST_ACCESS, LENGTH_CALL,
            METHOD_CALL, NEW_OBJECT, NEW_ARRAY, ARRAY, INTEGER_LITERAL, BOOLEAN_LITERAL, THIS_LITERAL, VAR_REF_EXPR);

    private final String name;

    public static List<JmmNode> getTypeChildren(JmmNode parent) {
        return parent.getChildren().stream().filter(node -> Kind.fromString(node.getKind()).isType()).toList();
    }

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {

        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * @return true if this kind represents a type, false otherwise
     */
    public boolean isType() {
        return TYPES.contains(this);
    }

    /**
     * @return true if this kind represents a method declaration, false otherwise
     */
    public boolean isMethodDecl() {
        return METHOD_DECLS.contains(this);
    }

    /**
     * @return true if this kind represents a statement, false otherwise
     */
    public boolean isStmt() {
        return STATEMENTS.contains(this);
    }

    /**
     * @return true if this kind represents an expression, false otherwise
     */
    public boolean isExpr() {
        return EXPRESSIONS.contains(this);
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.getKind().equals(getNodeName());
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {

        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {

            // if any matches, return successfully
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
