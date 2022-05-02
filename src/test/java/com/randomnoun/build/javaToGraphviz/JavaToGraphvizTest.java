package com.randomnoun.build.javaToGraphviz;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.randomnoun.common.ProcessUtil;
import com.randomnoun.common.ProcessUtil.ProcessException;
import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

public class JavaToGraphvizTest {

    private static boolean WRITE_EXPECTED_OUTPUT = true;
    private static boolean WRITE_EXPECTED_OUTPUT_PNG = true;
    
    @BeforeClass 
    public static void beforeAllTestMethods() {
        Log4jCliConfiguration lcc = new Log4jCliConfiguration();
        Properties props = new Properties();
        props.put("log4j.rootCategory", "INFO, CONSOLE");
        lcc.init("[JavaToGraphvizTest]", props);
    }
    
    @Test
    public void testControlFlowStatements() throws IOException {
        testStatement("com.example.input.AllTheControlFlowNodes");
        testStatement("com.example.input.BlogDiagrams");
        testStatement("com.example.input.MultipleGraphs");
        testStatement("com.example.input.UserDefinedSubgraphs");
        
        testStatement("com.example.input.MethodChain");
        testStatement("com.example.input.Constructor");
        
        testStatement("com.example.input.IfStatementFlip");
        testStatement("com.example.input.ContinueBreakRank");
        testStatement("com.example.input.ContinueBreakLabelA"); // comparisons
        testStatement("com.example.input.ContinueBreakLabelB"); // centralSwitch
        
        testStatement("com.example.input.DoStatement");
        testStatement("com.example.input.ForStatement");
        testStatement("com.example.input.IfStatement");
        testStatement("com.example.input.IfStatementExternalCss");
        testStatement("com.example.input.SwitchStatement");
        testStatement("com.example.input.WhileStatement");
        testStatement("com.example.input.SynchronizeStatement");
        testStatement("com.example.input.TryCatchStatement");
        testStatement("com.example.input.TryCatchFinallyStatement");
        testStatement("com.example.input.TryWithResourcesStatement");
        
        testStatement("com.example.input.CommentAttribution");
        
        testStatement("com.example.input.Expressions");
        testStatement("com.example.input.Expressions1");
        testStatement("com.example.input.Expressions2");
        testStatement("com.example.input.Expressions3a");
        testStatement("com.example.input.Expressions3b");
        testStatement("com.example.input.Expressions4");
        testStatement("com.example.input.Expressions5");
        testStatement("com.example.input.Expressions6");
    }
    
    public void testStatement(String className) throws IOException {
        Logger logger = Logger.getLogger(JavaToGraphvizTest.class);
        logger.info(className);
        
        // create 2 diagrams, one with removeNode = true
        int configCombinations = 2;
        // String dotExe = "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe";
        String dotExe = "C:\\Program Files\\Graphviz\\bin\\dot.exe";
        
        for (int i = 0; i < configCombinations; i++) {
            
            File f = new File("src/test/java/" + Text.replaceString(className,  ".",  "/") + ".java");
            FileInputStream fis = new FileInputStream(f);
            String suffix = "";
            
            JavaToGraphviz javaToGraphviz = new JavaToGraphviz();
            javaToGraphviz.setBaseCssUrl("JavaToGraphviz.css"); // defaults to JavaToGraphviz-base.css, which is a bit minimalistic
            Map<String, String> options = new HashMap<>();
            if (i == 0) {
                options.put("enableKeepNodeFilter", "true");
                options.put("defaultKeepNode", "true");
            } else if (i == 1) {
                options.put("enableKeepNodeFilter", "true");
                options.put("defaultKeepNode", "false");
                suffix = "-compact";
            }
            javaToGraphviz.setOptions(options);
            javaToGraphviz.parse(fis, "UTF-8");
            fis.close();
            
            int idx = 0;
            
            boolean hasNext;
            do {
                logger.info("=========================== " + className + "-" + idx + suffix );
                
                StringWriter sw = new StringWriter();
                hasNext = javaToGraphviz.writeGraphviz(sw);
                logger.debug(sw.toString());

                File tf = new File("src/test/resources/expected-output/dot/" + className + "-" + idx + suffix + ".dot");
                File tfPng = new File("src/test/resources/expected-output/png/" + className + "-" + idx + suffix + ".png");
                if (WRITE_EXPECTED_OUTPUT) {
                    FileOutputStream fos = new FileOutputStream(tf);
                    fos.write(sw.toString().getBytes());
                    fos.close();
                    idx++;
                } else {
                    fis = new FileInputStream(tf);
                    String expected = new String(StreamUtil.getByteArray(fis));
                    fis.close();
                    assertEquals("difference in " + className + "-" + idx + suffix + ".dot", expected, sw.toString());
                    idx++;
                }
                if (WRITE_EXPECTED_OUTPUT_PNG) {
                    // call graphviz as well
                    String output;
                    try {
                        output = ProcessUtil.exec(new String[] {
                            dotExe,
                            tf.getPath(),
                            "-Tpng",
                            "-o" + tfPng.getPath()
                        });
                        logger.info("gv output: " + output);
                        if (tfPng.exists() && tfPng.length() < 100) {
                            // compact charts can collapse to zero nodes, which produces an 89 byte empty image
                            tfPng.delete();
                        }
                    } catch (ProcessException e) {
                        logger.info("process failed", e);
                    }
                    
                }
                
            } while (hasNext);
            
        }
    }
        
        
    @Test
    public void testDom() throws IOException {
        testDom("com.example.input.ForStatement");
    }
    
    public void testDom(String className) throws IOException {
        Logger logger = Logger.getLogger(JavaToGraphvizTest.class);
        logger.info(className);
        
        // create 2 files, with and without css applied
        int configCombinations = 2;
        
        for (int i = 0; i < configCombinations; i++) {
            
            File f = new File("src/test/java/" + Text.replaceString(className,  ".",  "/") + ".java");
            FileInputStream fis = new FileInputStream(f);
            String suffix = "";
            
            JavaToGraphviz javaToGraphviz = new JavaToGraphviz();
            javaToGraphviz.setBaseCssUrl("JavaToGraphviz.css"); // defaults to JavaToGraphviz-base.css, which is a bit minimalistic
            if (i == 0) {
                javaToGraphviz.setFormat("dom1");
                suffix = "-dom1";
            } else if (i == 1) {    
                javaToGraphviz.setFormat("dom2");
                suffix = "-dom2";
            }
    
            javaToGraphviz.parse(fis, "UTF-8");
            fis.close();
            
            int idx = 0;
            
            boolean hasNext;
            do {
                StringWriter sw = new StringWriter();
                hasNext = javaToGraphviz.writeGraphviz(sw);
                logger.debug(sw.toString());
                
                File tf = new File("src/test/resources/expected-output/dom/" + className + "-" + idx + suffix + ".dom");
                if (WRITE_EXPECTED_OUTPUT) {
                    FileOutputStream fos = new FileOutputStream(tf);
                    fos.write(sw.toString().getBytes());
                    fos.close();
                    idx++;
                } else {
                    fis = new FileInputStream(tf);
                    String expected = new String(StreamUtil.getByteArray(fis));
                    fis.close();
                    assertEquals("difference in " + className + "-" + idx + suffix + ".dom", expected, sw.toString());
                    idx++;
                }
                if (hasNext) { logger.info("==========================="); }
            } while (hasNext);
            
        }
    }
    
}
