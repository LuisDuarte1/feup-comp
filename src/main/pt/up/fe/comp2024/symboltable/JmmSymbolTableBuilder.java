package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {
        List<String> imports = new ArrayList<>();

        //Iterate through imports until first class declaration is reached
        JmmNode classDecl = null;
        for (JmmNode child : root.getChildren()){
            if(child.getKind().equals(CLASS_DECL.toString())){
                classDecl = child;
                break;
            }
            if(child.getKind().equals(IMPORT_DECL.toString())){
                //Adding the last module of an import
                List<String> moduleTree = child.getObjectAsList("modules", String.class);
                imports.add(moduleTree.get(moduleTree.size()-1));
            }
        }
        assert classDecl != null;
        JmmNode finalClassDecl = classDecl;

        SpecsCheck.checkArgument(CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + finalClassDecl);
        String className = classDecl.get("name");

        //Get super class name
        String parentClass = "";
        if(classDecl.getObject("hasParent", Boolean.class))
        {
            parentClass = classDecl.get("parent");
        }

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, imports, parentClass);
    }

    private static Type getTypeFromGrammar(JmmNode type){
        String kind = type.getKind();
        if (kind.equals(INT_ARRAY_TYPE.toString()) || kind.equals(INT_VARARGS_TYPE.toString())){
            return new Type("int", true);
        }
        if(kind.equals(BOOL_TYPE.toString())){
            return new Type("boolean", false);
        }
        if(kind.equals(STR_TYPE.toString())){
            return new Type("String", false);
        }
        if(kind.equals(INT_TYPE.toString())){
            return new Type("int", false);
        }
        if(kind.equals(OBJECT_TYPE.toString())){
            return new Type(type.get("name"), false);
        }

        throw new RuntimeException("Kind not handled by SymbolTableBuilder");
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD)
                .forEach(method -> map.put(method.get("name"),
                        getTypeFromGrammar(getTypeChildren(method).get(0))
                ));

        //TODO: Confirmar o que fazer com o return do main
        if (classDecl.getChildren(MAIN_METHOD).size() == 1){
            map.put("main", new Type("void", false));
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        METHOD_DECLS.forEach((methodDeclType) -> classDecl.getChildren(methodDeclType)
                .forEach(method -> map.put(method.get("name"),
                        method.getChildren(PARAM).stream().map(
                                param -> new Symbol(
                                        getTypeFromGrammar(param.getObject("typename", JmmNode.class)),
                                        param.get("name"))
                        ).toList()
                        )));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        METHOD_DECLS.forEach((methodDeclType) -> classDecl.getChildren(methodDeclType)
                .forEach(method -> map.put(method.get("name"), getLocalsList(method))));

        map.put(classDecl.get("name"), getLocalsList(classDecl));

        return map;
    }
    //TODO: Verificar se o main não é suposto estar na symbol table (assumimos pelo teste que não)
    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();

        METHOD_DECLS.forEach((methodDeclType) -> {
            List<String> curr_methods = classDecl.getChildren(methodDeclType).stream()
                    .map(method -> method.get("name"))
                    .toList();

            methods.addAll(curr_methods);
        });

      return methods;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getTypeFromGrammar(varDecl.getObject("typename", JmmNode.class)), varDecl.get("name")))
                .toList();
    }

}
