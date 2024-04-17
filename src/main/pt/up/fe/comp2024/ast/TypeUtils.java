package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;

import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;


public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOL_TYPE_NAME = "boolean";
    private static final String STR_TYPE_NAME = "String";


    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getStrTypeName() {
        return STR_TYPE_NAME;
    }

    public static String getBoolTypeName() {
        return BOOL_TYPE_NAME;
    }

    public static Type annotateType(Type type, SymbolTable table) {
        if (type.getName().equals(table.getClassName())) {
            type.putObject("parent", table.getSuper());
        }
        if (table.getImports().contains(type.getName())) {
            type.putObject("imported", true);
        }
        return type;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        //Node was annotated during semantic analysis
        if (expr.hasAttribute("type")) {
            return expr.getObject("type", Type.class);
        }
        //Node was not annotated
        else {
            Kind kind = Kind.fromString(expr.getKind());
            Type type = switch (kind) {
                case PRIORITY_EXPR -> getExprType(expr.getChild(0), table);
                case UNARY_EXPR -> getUnaryExprType(expr);
                case BINARY_EXPR -> getBinExprType(expr);
                case LIST_ACCESS -> {
                    var array = expr.getChild(0);
                    Type arrayType;
                    if(ARRAY.check(array)){
                        arrayType = getExprType(array.getChild(0), table);
                    } //If it's an in place array
                    else arrayType = getVarExprType(expr.getChild(0), table);
                    yield new Type(arrayType.getName(), false); //Same type as array but the access itself is not an array
                }
                case LENGTH_CALL -> new Type(INT_TYPE_NAME, false);
                case METHOD_CALL -> {
                    var localType = table.getReturnType(expr.get("name"));
                    if (localType != null) {
                        yield localType;
                    } else {
                        var object = expr.getObject("object", JmmNode.class);
                        if (table.getImports().contains(getExprType(object, table).getName())) //Class of the method is imported
                        {
                            yield new Type("imported", false);
                        }
                    }
                    throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
                }
                case NEW_OBJECT -> annotateType(new Type(expr.get("name"), false), table);
                case NEW_ARRAY, ARRAY -> new Type(INT_TYPE_NAME, true);
                case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
                case BOOLEAN_LITERAL -> new Type(BOOL_TYPE_NAME, false);
                case THIS_LITERAL -> annotateType(new Type(table.getClassName(), false), table);
                case VAR_REF_EXPR -> getVarExprType(expr, table);
                default ->
                        throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
            };

            return type;
        }
    }

    public static Type getTypeFromGrammarType(JmmNode type) {
        Kind kind = Kind.fromString(type.getKind());

        Type result = switch (kind) {
            case INT_ARRAY_TYPE -> new Type(INT_TYPE_NAME, true);
            case INT_VARARGS_TYPE -> {
                var temp = new Type(INT_TYPE_NAME, true);
                temp.putObject("isVarArgs", true);
                yield temp;
            }
            case BOOL_TYPE -> new Type(BOOL_TYPE_NAME, false);
            case STR_TYPE -> new Type(STR_TYPE_NAME, false);
            case INT_TYPE -> new Type(INT_TYPE_NAME, false);
            case OBJECT_TYPE -> new Type(type.get("name"), false);
            default -> throw new RuntimeException("Can't compute type for type kind '" + kind + "'");
        };

        return result;
    }

    /**
     * @param binaryExpr
     * @return Type of binary expression operands
     */
    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-", "<" -> new Type(INT_TYPE_NAME, false);
            case "&&" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    /**
     * @param unaryExpr
     * @return Type of unary expression operands
     */
    private static Type getUnaryExprType(JmmNode unaryExpr) {
        String operator = unaryExpr.get("op");
        if (Objects.equals(operator, "!")) return new Type(BOOL_TYPE_NAME, false);
        else throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + unaryExpr + "'");
    }

    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        //Node was annotated during semantic analysis
        if (varRefExpr.hasAttribute("type")) {
            return varRefExpr.getObject("type", Type.class);
        }
        //Node was not annotated
        else {
            var varRefName = varRefExpr.get("name");

            Optional<JmmNode> currentMethodNode = varRefExpr.getAncestor(METHOD);
            Optional<JmmNode> currentClassNode = varRefExpr.getAncestor(CLASS_DECL);

            if (currentMethodNode.isPresent()) {
                String currentMethod = currentMethodNode.get().get("name");

                // Var is a parameter
                Optional<Symbol> parameter = table.getParameters(currentMethod).stream().filter(param -> param.getName().equals(varRefName)).findFirst();
                if (parameter.isPresent()) {
                    varRefExpr.putObject("type", parameter.get().getType());
                    return annotateType(parameter.get().getType(), table);
                }

                // Var is a declared variable
                Optional<Symbol> variable = table.getLocalVariables(currentMethod).stream().filter(varDecl -> varDecl.getName().equals(varRefName)).findFirst();
                if (variable.isPresent()) {
                    varRefExpr.putObject("type", variable.get().getType());
                    return annotateType(variable.get().getType(), table);
                }
            }
            if (currentClassNode.isPresent()) {
                // Var is a field
                Optional<Symbol> field = table.getFields().stream().filter(param -> param.getName().equals(varRefName)).findFirst();
                if (field.isPresent()) {
                    varRefExpr.putObject("type", field.get().getType());
                    return annotateType(field.get().getType(), table);
                }
            }
            throw new RuntimeException("Could not access " + varRefExpr + " parent method or class");
        }
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        if (sourceType.equals(destinationType)) {
            return true;
        } else if (sourceType.hasAttribute("parent") && (sourceType.getObject("parent").equals(destinationType.getName())))
            return true;
        return sourceType.hasAttribute("imported") && destinationType.hasAttribute("imported");
    }
}
