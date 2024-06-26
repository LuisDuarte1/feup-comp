package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest {

    @Test
    public void symbolTable() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/SymbolTable.jmm"));
        System.out.println("Symbol Table:\n" + result.getSymbolTable().print());
    }

    @Test
    public void varNotDeclared() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarNotDeclared.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void classNotImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ClassNotImported.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void classImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursClassImported.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intPlusObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IntPlusObject.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void boolTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/BoolTimesInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayPlusInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayPlusInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void negateInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursNegateInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void negateBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursNegateBool.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayAccessOnInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayAccessOnInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayIndexNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayIndexNotInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayAccessValid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursArrayAccessValid.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayDeclarationValid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursArrayDeclarationValid.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayDeclarationInvalid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursArrayDeclarationInvalid.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void assignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignIntToBool.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void inheritedMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursInheritedMethod.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentFail.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentPassExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentPassExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentPassImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentPassImports.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void returnArrayAndIndex() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursReturnArrayAndIndex.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void indexAReturnedArray() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursIndexAReturnedArray.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void intInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IntInIfCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInWhileCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void varDeclNames() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursVarDeclName.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToUndeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToUndeclaredMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToMethodAssumedInExtends() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToMethodAssumedInExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodAssumedInImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToMethodAssumedInImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodWrongNumParams() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursCallToMethodWrongNumParams.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IncompatibleArguments.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IncompatibleReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assumeArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssumeArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/Varargs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarargsWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsDeclaration() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursVarargsDeclaration.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursVarargsReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsArray() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursVarargsArray.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsMultipleArrays() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursVarargsMultipleArrays.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsOtherArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursVarargsOtherArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInit() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInit.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInitWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInitWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitAccess() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursArrayInitAccess.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void thisInMethod() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursThisInMethod.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void thisInMain() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursThisInMain.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void thisAsObject() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursThisAsObject.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void fieldInStatic() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursFieldInStatic.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedFunctionInDiffClasses() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/OursDuplicatedFunctionInDiffClasses.jmm"));
        TestUtils.noErrors(result);

    }

}
