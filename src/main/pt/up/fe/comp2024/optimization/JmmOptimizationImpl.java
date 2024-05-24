package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;
public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if(semanticsResult.getConfig().containsKey("optimize")) {
            ConstantFoldingVisitor constantFolding = new ConstantFoldingVisitor();
            ConstantPropagationVisitor constantPropagation = new ConstantPropagationVisitor();
            do {
                constantFolding.setModified(false);
                constantPropagation.setModified(false);
                constantFolding.visit(semanticsResult.getRootNode());
                constantPropagation.visit(semanticsResult.getRootNode());
            } while (constantFolding.getModified() || constantPropagation.getModified());
        }

        return semanticsResult;
    }
}
