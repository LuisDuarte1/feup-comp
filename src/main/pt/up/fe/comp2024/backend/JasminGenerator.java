package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.stringtemplate.v4.ST;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCallInstruction);
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private String generateCallInstruction(CallInstruction callInstruction){
        var code = new StringBuilder();
        switch (callInstruction.getInvocationType()){
            case NEW -> {
                code.append(String.format("new %s", ((ClassType) callInstruction.getReturnType()).getName())).append(NL);
                code.append("dup").append(NL);
            }
            case invokespecial -> {
                code.append(
                        callInstruction.getArguments().stream()
                                .map(generators::apply)
                                .reduce("", (a , b) -> {return a + NL + b;})
                ).append(NL);
                final ClassType classType = (ClassType) ((Operand) callInstruction.getCaller()).getType();
                var className = currentMethod.getOllirClass().getImports().stream()
                        .filter(((val) -> val.endsWith(classType.getName())))
                        .findFirst().orElse(classType.getName()).replace(".", "/");
                var methodName = ((LiteralElement) callInstruction.getMethodName()).getLiteral().replace("\"", "");

                var paramsType = callInstruction.getArguments().stream().map(Element::getType).map(this::getJasminTypeOfElement)
                        .reduce("", (subtotal, element) -> subtotal + element);
                var returnType = getJasminTypeOfElement(callInstruction.getReturnType());

                code.append(String.format("invokespecial %s",
                        String.format("%s(%s)%s", className+"/"+methodName,paramsType, returnType))).append(NL);
            }
            case invokestatic -> {
                code.append(
                        callInstruction.getArguments().stream()
                                .map(generators::apply)
                                .reduce("", (a , b) -> {return a + NL + b;})
                ).append(NL);

                var className = currentMethod.getOllirClass().getImports().stream()
                        .filter(((val) -> val.endsWith(((Operand) callInstruction.getCaller()).getName())))
                        .findFirst().orElse(((Operand) callInstruction.getCaller()).getName())
                        .replace(".", "/");
                var methodName = ((LiteralElement) callInstruction.getMethodName()).getLiteral().replace("\"", "");
                var paramsType = callInstruction.getArguments().stream().map(Element::getType).map(this::getJasminTypeOfElement)
                        .reduce("", (subtotal, element) -> subtotal + element);
                var returnType = getJasminTypeOfElement(callInstruction.getReturnType());

                code.append(String.format("invokestatic %s",
                        String.format("%s(%s)%s", className+"/"+methodName,paramsType, returnType))).append(NL);
            }
            case invokevirtual -> {
                var className = ((Operand) callInstruction.getCaller()).getName();
                if (Objects.equals(className, "this")) {
                    code.append("aload_0").append(NL);
                }
                else code.append(String.format("aload %d", currentMethod.getVarTable().get(className).getVirtualReg()));
                className = currentMethod.getOllirClass().getImports().stream()
                        .filter((val) -> val.endsWith(((ClassType) callInstruction.getCaller().getType()).getName()))
                        .findFirst().orElse(((ClassType) callInstruction.getCaller().getType()).getName())
                        .replace(".", "/");
                code.append(
                        callInstruction.getArguments().stream()
                                .map(generators::apply)
                                .reduce("", (a , b) -> {return a + NL + b;})
                ).append(NL);

                var methodName = ((LiteralElement) callInstruction.getMethodName()).getLiteral().replace("\"", "");
                var paramsType = callInstruction.getArguments().stream().map(Element::getType).map(this::getJasminTypeOfElement)
                        .reduce("", (subtotal, element) -> subtotal + element);
                var returnType = getJasminTypeOfElement(callInstruction.getReturnType());
                code.append(String.format("invokevirtual %s",
                        String.format("%s(%s)%s", className+"/"+methodName,paramsType, returnType))).append(NL);

            }
        }
        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction){
        var code = new StringBuilder();

        var className = currentMethod.getOllirClass().getClassName();
        String finalClassName = className;
        className = currentMethod.getOllirClass().getImports().stream()
                .filter((val) -> val.endsWith(finalClassName))
                .findFirst().orElse(finalClassName)
                .replace(".", "/");
        if (!Objects.equals(((Operand) getFieldInstruction.getOperands().get(0)).getName(), "this")){
            code.append(String.format("aload %s",
                    currentMethod.getVarTable().get(
                            ((Operand) getFieldInstruction.getOperands().get(0)).getName()).getVirtualReg()
            ));
        } else {
            code.append("aload_0");
        }
        code.append(NL);

        code.append(String.format("getfield %s %s",
                        className + "/" +getFieldInstruction.getField().getName(),
                        getJasminTypeOfElement(getFieldInstruction.getField().getType())))
                .append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction){
        var code = new StringBuilder();

        var className = currentMethod.getOllirClass().getClassName();
        String finalClassName = className;
        className = currentMethod.getOllirClass().getImports().stream()
                .filter((val) -> val.endsWith(finalClassName))
                .findFirst().orElse(finalClassName)
                .replace(".", "/");
        if (!Objects.equals(((Operand) putFieldInstruction.getOperands().get(0)).getName(), "this")){
            code.append(String.format("aload %s",
                    currentMethod.getVarTable().get(
                            ((Operand) putFieldInstruction.getOperands().get(0)).getName()).getVirtualReg()
            ));
        } else {
            code.append("aload_0");
        }

        code.append(NL);

        code.append(generators.apply(putFieldInstruction.getValue()));

        code.append(String.format("putfield %s %s",
                        className + "/" + putFieldInstruction.getField().getName(),
                        getJasminTypeOfElement(putFieldInstruction.getField().getType())))
                .append(NL);

        return code.toString();
    }

    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        code.append(".super java/lang/Object").append(NL);

        ollirResult.getOllirClass().getFields()
                .stream().map((val) -> String.format(".field %s %s %s\n",
                        val.getFieldAccessModifier().name().equalsIgnoreCase("default")
                                ? "public" : val.getFieldAccessModifier().name().toLowerCase(),
                        val.getFieldName(),
                        getJasminTypeOfElement(val.getFieldType())))
                .toList().forEach(code::append);

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }
        System.out.println(code);
        return code.toString();
    }

    private String getJasminTypeOfElement(Type element){
        return switch (element.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "Ljava/lang/String;";
            case ARRAYREF ->
                    "[".repeat(((ArrayType) element).getNumDimensions())
                            + getJasminTypeOfElement(((ArrayType) element).getElementType());
            case VOID -> "V";
            case CLASS,OBJECTREF -> String.format("L%s;", currentMethod.getOllirClass().getImports().stream()
                    .filter((val) -> val.endsWith(((ClassType) element).getName()))
                    .findFirst().orElse(((ClassType) element).getName())
                    .replace(".", "/"));
            default -> throw new RuntimeException(
                    String.format("Jasmin type not handled: %s", element.getTypeOfElement().name())
            );
        };
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";
        if (method.isStaticMethod()){
            modifier = modifier + "static" + " ";
        }

        var methodName = method.getMethodName();
        var paramsType = method.getParams().stream().map(Element::getType).map(this::getJasminTypeOfElement)
                .reduce("", (subtotal, element) -> subtotal + element);
        var returnType = getJasminTypeOfElement(method.getReturnType());
        code.append("\n.method ").append(modifier).append(methodName)
                .append(String.format("(%s)%s", paramsType, returnType))
                .append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }
        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        var type = currentMethod.getVarTable().get(operand.getName()).getVarType();

        switch (type.getTypeOfElement()){
            case INT32, BOOLEAN -> code.append("istore ").append(reg).append(NL);
            case CLASS -> code.append("astore ").append(reg).append(NL);
            case OBJECTREF -> code.append("astore ").append(reg).append(NL);
            default -> throw new RuntimeException(
                    String.format("Assign type %s not handled", type.getTypeOfElement().name())
            );
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return switch (operand.getType().getTypeOfElement()){
            case INT32,BOOLEAN -> "iload ";
            case OBJECTREF,CLASS -> "aload ";
            case THIS -> "aload_0";
            default ->
                    throw new NotImplementedException(
                            String.format("Operand %s is not implemented",
                                    operand.getType().getTypeOfElement().name()));
        }+ reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            // TODO(luisd): test div and sub due to not being commutative
            case DIV -> "idiv";
            case SUB -> "isub";
            case AND, ANDB -> "iand";
            case OR, ORB -> "ior";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        switch (returnInst.getReturnType().getTypeOfElement()){
            case VOID -> code.append("return");
            case INT32, BOOLEAN -> {
                code.append(generators.apply(returnInst.getOperand()));
                code.append("ireturn");
            }
            case CLASS -> {
                code.append(generators.apply(returnInst.getOperand()));
                code.append("areturn");
            }
            default -> throw new RuntimeException(
                    String.format("Return type %s not handled", returnInst.getReturnType().getTypeOfElement().name()));

        }

        return code.toString();
    }

}
