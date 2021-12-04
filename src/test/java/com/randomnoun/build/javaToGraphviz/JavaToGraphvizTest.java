package com.randomnoun.build.javaToGraphviz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

public class JavaToGraphvizTest {

    private static boolean WRITE_EXPECTED_OUTPUT = true;
    
    @BeforeClass 
    public static void beforeAllTestMethods() {
        Log4jCliConfiguration lcc = new Log4jCliConfiguration();
        Properties props = new Properties();
        props.put("log4j.rootCategory", "INFO, CONSOLE");
        lcc.init("[JavaToGraphvizTest]", props);
    }
    
    @Test
    public void testControlFlowStatements() throws IOException {
        
        // ok so switch is broken now
        testStatement("com.example.input.ContinueBreakLabelA");
        testStatement("com.example.input.ContinueBreakLabelB");
        
        /*
        testStatement("com.example.input.MethodChain");
        
        testStatement("com.example.input.IfStatementFlip");
        testStatement("com.example.input.ContinueBreakRank");
        testStatement("com.example.input.ContinueBreakLabel");
        
        testStatement("com.example.input.DoStatement");
        testStatement("com.example.input.ForStatement");
        testStatement("com.example.input.IfStatement");
        testStatement("com.example.input.IfStatementExternalCss");
        testStatement("com.example.input.SwitchStatement");
        testStatement("com.example.input.WhileStatement");
        testStatement("com.example.input.SynchronizeStatement");
        testStatement("com.example.input.TryCatchStatement");
        
        testStatement("com.example.input.MultipleGraphs"); 
        testStatement("com.example.input.UserDefinedSubgraphs");
        testStatement("com.example.input.CommentAttribution");
        
        testStatement("com.example.input.Expressions");
        testStatement("com.example.input.Expressions1");
        testStatement("com.example.input.Expressions2");
        testStatement("com.example.input.Expressions3a");
        testStatement("com.example.input.Expressions3b");
        testStatement("com.example.input.Expressions4");
        testStatement("com.example.input.Expressions5");
        testStatement("com.example.input.Expressions6");
        */

    }
    
    public void testStatement(String className) throws IOException {
        Logger logger = Logger.getLogger(JavaToGraphvizTest.class);
        logger.info(className);
        
        File f = new File("src/test/java/" + Text.replaceString(className,  ".",  "/") + ".java");
        FileInputStream fis = new FileInputStream(f);
        
        // InputStream is = JavaToGraphviz4.class.getResourceAsStream("/test.java");
        // InputStream is = JavaToGraphviz.class.getResourceAsStream("/Statements.java");
        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        JavaToGraphviz javaToGraphviz = new JavaToGraphviz();
        javaToGraphviz.setBaseCssUrl("JavaToGraphviz.css"); // defaults to JavaToGraphviz-base.css, which is a bit minimalistic
        // javaToGraphviz.setRemoveNode(true);
        javaToGraphviz.parse(fis, "UTF-8");
        fis.close();
        
        int idx = 0;
        
        boolean hasNext;
        do {
            StringWriter sw = new StringWriter();
            hasNext = javaToGraphviz.writeGraphviz(sw);
            logger.debug(sw.toString());
            
            if (WRITE_EXPECTED_OUTPUT) {
                File tf = new File("src/test/resources/expected-output/" + className + "-" + idx + ".dot");            
                FileOutputStream fos = new FileOutputStream(tf);
                fos.write(sw.toString().getBytes());
                fos.close();
                idx++;
            }
            
            if (hasNext) { logger.info("==========================="); }
        } while (hasNext);
        
    }

}
