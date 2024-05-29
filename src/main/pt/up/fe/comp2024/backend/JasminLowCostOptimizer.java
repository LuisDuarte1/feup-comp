package pt.up.fe.comp2024.backend;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class JasminLowCostOptimizer {

    public static final List<Function<String, String>> optimizationFuncs = List.of(
        JasminLowCostOptimizer::optimizeRedudantLoadStore,
        JasminLowCostOptimizer::optimizeIntLoadAndStore,
        JasminLowCostOptimizer::optimizeObjectLoadAndStore
    );

    private static String optimizeRedudantLoadStore(String code) {
        String optimizedCode = code;

        optimizedCode = Pattern.compile("iload ([0-9]+)\n[ \t]*istore \\1\n").matcher(optimizedCode)
                .replaceAll("");

        optimizedCode = Pattern.compile("aload ([0-9]+)\n[ \t]*astore \\1\n").matcher(optimizedCode)
                .replaceAll("");
        return optimizedCode;
    }


    public static String optimizeIntLoadAndStore(String code){
        String optimizedCode = code;
        for(int i = 0; i <= 3; i++){
            optimizedCode = Pattern.compile(String.format("iload %d\n", i))
                    .matcher(optimizedCode).replaceAll(String.format("iload_%d\n", i));
        }

        for(int i = 0; i <= 3; i++){
            optimizedCode = Pattern.compile(String.format("istore %d\n", i))
                    .matcher(optimizedCode).replaceAll(String.format("istore_%d\n", i));
        }

        return optimizedCode;
    }

    public static String optimizeObjectLoadAndStore(String code){
        String optimizedCode = code;
        for(int i = 0; i <= 3; i++){
            optimizedCode = Pattern.compile(String.format("aload %d\n", i))
                    .matcher(optimizedCode).replaceAll(String.format("aload_%d\n", i));
        }

        for(int i = 0; i <= 3; i++){
            optimizedCode = Pattern.compile(String.format("astore %d\n", i))
                    .matcher(optimizedCode).replaceAll(String.format("astore_%d\n", i));
        }

        return optimizedCode;
    }

    public static String optimizeJasmin(String code){
        String optimizedCode = code;
        for(var function : optimizationFuncs){
            optimizedCode = function.apply(optimizedCode);
        }
        return optimizedCode;
    }
}
