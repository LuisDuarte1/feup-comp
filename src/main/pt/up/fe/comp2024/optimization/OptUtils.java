package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.OBJECT_TYPE;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

//import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        //TODO: Check this
        //TYPE.checkOrThrow(typeNode);
        String typeName = typeNode.getKind();
        if (OBJECT_TYPE.check(typeNode)){
            typeName = typeNode.get("name");
        }

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName());
    }

    public static String toOllirType(String typeName) {

        String type = "." + switch (typeName) {
            case "IntType", "int" -> "i32";
            case "BoolType", "boolean" -> "bool";
            default -> typeName;
        };

        return type;
    }


}
