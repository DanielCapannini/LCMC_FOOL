package test;

import org.junit.jupiter.api.Test;

public class OperatorsTest {
    // 0=FALSE
    // 1=TRUE

    private static final String ROOT_MINUS_TEST_FILES = "res/test/operators/minus/";
    private static final String ROOT_DIV_TEST_FILES = "res/test/operators/div/";
    private static final String ROOT_GE_TEST_FILES = "res/test/operators/greaterEquals/";
    private static final String ROOT_LE_TEST_FILES = "res/test/operators/lessEquals/";
    private static final String ROOT_NOT_TEST_FILES = "res/test/operators/not/";
    private static final String ROOT_AND_TEST_FILES = "res/test/operators/and/";
    private static final String ROOT_OR_TEST_FILES = "res/test/operators/or/";

    private static final String ROOT_OO = "res/test/oo/";

    @Test
    public void test5Minus3() {
        String fileName = ROOT_MINUS_TEST_FILES + "5minus3.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:int = 5 - 3; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing 10 --2
    @Test
    public void test10DoubleMinus2() {
        String fileName = ROOT_MINUS_TEST_FILES + "10doubleMinus2.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:int = 10 - -2; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing 10 / 5
    @Test
    public void test10Div5() {
        String fileName = ROOT_DIV_TEST_FILES + "10div5.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:int = 10 / 5; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing 10 / 3
    @Test
    public void test10Div3() {
        String fileName = ROOT_DIV_TEST_FILES + "10div3.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:int = 10 / 3; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing lessEquals 4 <= 2
    @Test
    public void test4LessEquals2() {
        String fileName = ROOT_LE_TEST_FILES + "4lessEquals2.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = 4 <= 2; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing lessEquals 4 <= 5
    @Test
    public void test4LessEquals5() {
        String fileName = ROOT_LE_TEST_FILES + "4lessEquals5.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = 4 <= 5; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing lessEquals 4 <= 4
    @Test
    public void test4LessEquals4() {
        String fileName = ROOT_LE_TEST_FILES + "4lessEquals4.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = 4 <= 4; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing greaterEquals 4 >= 5
    @Test
    public void test4GreaterEquals5() {
        String fileName = ROOT_GE_TEST_FILES + "4greaterEquals5.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = 4 >= 5; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing greaterEquals 4 >= 4
    @Test
    public void test4GreaterEquals4() {
        String fileName = ROOT_GE_TEST_FILES + "4greaterEquals4.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = 4 >= 4; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing greaterEquals 5 >= 4
    @Test
    public void test5GreaterEquals4() {
        String fileName = ROOT_GE_TEST_FILES + "5greaterEquals4.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = 5 >= 4; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing or var x:bool = false||false
    @Test
    public void testFalseOrFalse() {
        String fileName = ROOT_OR_TEST_FILES + "falseOrFalse.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = false||false; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing or var x:bool = false||true
    @Test
    public void testFalseOrTrue() {
        String fileName = ROOT_OR_TEST_FILES + "falseOrTrue.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = false||true; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing or var x:bool = true||false
    @Test
    public void testTrueOrFalse() {
        String fileName = ROOT_OR_TEST_FILES + "TrueOrFalse.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = true||false; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing or var x:bool = true||true
    @Test
    public void testTrueOrTrue() {
        String fileName = ROOT_OR_TEST_FILES + "trueOrTrue.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = true||true; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing and var x:bool = false&&false
    @Test
    public void testFalseAndFalse() {
        String fileName = ROOT_AND_TEST_FILES + "falseAndFalse.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = false&&false; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing and var x:bool = false&&true
    @Test
    public void testFalseAndTrue() {
        String fileName = ROOT_AND_TEST_FILES + "falseAndTrue.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = false&&true; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing and var x:bool = true&&false
    @Test
    public void testTrueAndFalse() {
        String fileName = ROOT_AND_TEST_FILES + "trueAndFalse.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = true&&false; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing and var x:bool = true&&true
    @Test
    public void testTrueAndTrue() {
        String fileName = ROOT_AND_TEST_FILES + "trueAndTrue.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = true&&true; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing not var x:bool = !true
    @Test
    public void testNotTrue() {
        String fileName = ROOT_NOT_TEST_FILES + "notTrue.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = !true; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    // testing not var x:bool = !false
    @Test
    public void testNotFalse() {
        String fileName = ROOT_NOT_TEST_FILES + "notFalse.fool";

        ToolsForTest tools = new ToolsForTest();
        tools.createFOOLFile(fileName, "let var x:bool = !false; in print(x);");
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }

    //
    @Test
    public void testQuickSort() {
        String fileName = ROOT_OO + "bankloan.fool";
        ToolsForTest tools = new ToolsForTest();
        tools.buildASTAndSVMAndCheckErrors(fileName, false);
        tools.runningSVM();
    }


}