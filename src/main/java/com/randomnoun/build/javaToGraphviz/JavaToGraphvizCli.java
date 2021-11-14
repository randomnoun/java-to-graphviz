package com.randomnoun.build.javaToGraphviz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/** CLI for JavaToGraphviz
 * 
 */
public class JavaToGraphvizCli {

    Logger logger = Logger.getLogger(JavaToGraphvizCli.class);


    public static String usage() {
        return 
          "Usage: \n" +
          "  java " + JavaToGraphvizCli.class.getName() + " [options] filename filename ... \n" +
          "where [options] are:\n" +
          " -h -?                    displays this help text\n" +
          " --verbose  -v            increase verbosity ( info level )\n" +
          " --verbose2 -vv           increase verbosity a bit more ( debug level )\n" +
          " --format   -f dot|dom1|dom2 output format: DOT diagram, pre-styling DOM, post-styling DOM\n" +
          " --output   -o filename   send output to filename; use {f} for base filename, {i} for diagram index\n" +
          "                          e.g. \"{f}-{i}.dot\" for dot output, \"{f}.html\" for dom output\n" +
          "                          default is stdout\n" +
          " --source   -s version    set Java source language version ( 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7,\n" +
          "                          8, 9, 10, 11, 12, 13, 14, 15, 16)\n" +
          "\n" +
          " --edger    -e name       add edger to list of edgers\n" +
          "                          available edgers: control-flow, ast\n" +
          " --remove   -r            perform node / edge removal\n" +
          "\n" +
          " --base-css -b url        replace base css with url ( relative to file://./ )\n" +
          " --user-css -u url        add url to list of user css list\n" +
          " --css      -c {rules}    add css rules\n";
    }
    
    public static void main(String args[]) throws IOException {
        List<String> filenames = new ArrayList<>();
        int verbose = 0;
        String format = "dot";
        String outputTemplate = null;
        
        String sourceVersion = "11";
        List<String> edgers = new ArrayList<>();
        boolean remove = false;
        String baseCssUrl = "something";
        List<String> userCssUrls = new ArrayList<>();
        List<String> userCssRules = new ArrayList<>();
        List<File> inputFiles = new ArrayList<>();
        
        if (args.length < 1) { 
            System.out.println(usage());
            throw new IllegalArgumentException("Expected input filename or options");
        }
        int argIndex = 0;
        while (argIndex < args.length && args[argIndex].startsWith("-")) {
            String a = args[argIndex];
            if (a.equals("--verbose") || a.equals("-v")) {
                verbose = 1;
                argIndex ++;
            } else if (a.equals("--verbose2") || a.equals("-vv")) {
                verbose = 2;
                argIndex ++;
            } else if (a.equals("--format") || a.equals("-f")) {
                format = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--output") || a.equals("-o")) {
                outputTemplate = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--source") || a.equals("-s")) {
                sourceVersion = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--edger") || a.equals("-e")) {
                edgers.add(args[argIndex + 1]);
                argIndex += 2;
            } else if (a.equals("--remove") || a.equals("-r")) {
                remove = true;
                argIndex ++; 
            } else if (a.equals("--base-css") || a.equals("-b")) {
                baseCssUrl = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--user-css") || a.equals("-u")) {
                userCssUrls.add(args[argIndex + 1]);
                argIndex += 2;
            } else if (a.equals("--css") || a.equals("-c")) {
                userCssRules.add(args[argIndex + 1]);
                argIndex += 2;
            } else if (args[argIndex].equals("-h") || args[argIndex].equals("-?")) {
                System.out.println(usage());
                System.exit(0);
            } else {
                // here beginneth the filenames
                break;
            }
        }
        if (argIndex == args.length) {
            System.out.println(usage());
            throw new IllegalArgumentException("Expected input filenames");
        }
        while (argIndex < args.length) {
            String f = args[argIndex];
            argIndex++;
            File file = new File(f);
            if (!file.exists()) {
                throw new IllegalArgumentException("Input file '" + f + "' does not exist");
            } else if (!file.canRead()) {
                throw new IllegalArgumentException("Input file '" + f + "' is not readable");
            }
            inputFiles.add(file);
        }
        // option defaults and validation
        if (!(format.equals("dot") || format.equals("dom1") || format.equals("dom2"))) {
            throw new IllegalArgumentException("Unknown format '" + format + "'; expected 'dot', 'dom1' or 'dom2'");
        }
        /*
        if (outputTemplate == null) {
            if (format.equals("dot")) {
                outputTemplate = "{f}-{i}.dot";
            } else {
                outputTemplate = "{f}-{i}.html";
            }
        }
        */
        if (edgers.isEmpty()) { 
            edgers.add("control-flow");
        } 
        for (String e : edgers) {
            // something
        }
        

        Log4jCliConfiguration lcc = new Log4jCliConfiguration();
        Properties props = new Properties();
        props.put("log4j.logger", "FATAL");
        if (verbose == 1) {
            props.put("log4j.logger", "INFO");
        } else if (verbose == 2) {
            props.put("log4j.logger", "DEBUG");
        }
        lcc.init("[JavaToGraphviz]", props);
        Logger logger = Logger.getLogger(JavaToGraphvizCli.class);

        
        for (File f : inputFiles) {
            JavaToGraphviz javaToGraphviz = new JavaToGraphviz();
            javaToGraphviz.setFormat(format);
            javaToGraphviz.setSourceVersion(sourceVersion);
            javaToGraphviz.setEdgerNames(edgers);
            javaToGraphviz.setRemoveNode(remove);
            
            if (baseCssUrl != null) {
                javaToGraphviz.setBaseCssUrl(baseCssUrl);
            }
            javaToGraphviz.setUserCssUrls(userCssUrls);
            javaToGraphviz.setUserCssRules(userCssRules);
            
            // javaToGraphviz.setIncludeThrowEdges(false); // collapse throw edges
            // javaToGraphviz.setIncludeThrowNodes(false); // collapse throw nodes
    
            // String className = "com.example.input.SwitchStatement2";
            // File f = new File("src/test/java/" + Text.replaceString(className,  ".",  "/") + ".java");
            logger.debug("Reading " + f.getCanonicalPath());            
            FileInputStream is = new FileInputStream(f);
            javaToGraphviz.parse(is, "UTF-8");
            is.close();
            String n = f.getName();
            if (n.indexOf(".") != -1) { n = n.substring(0, n.indexOf(".")); }
            
            int diagramIndex = 0;
            boolean hasNext;
            do {
                String on = outputTemplate;
                if (on == null) {
                    StringWriter sw = new StringWriter();
                    hasNext = javaToGraphviz.writeGraphviz(sw);
                    System.out.println(sw.toString());
                    if (hasNext) { System.out.println("==========================="); }
                } else {
                    on = Text.replaceString(on,  "{f}", n);
                    on = Text.replaceString(on,  "{i}", String.valueOf(diagramIndex));
                    logger.debug("Writing " + on);   
                    FileOutputStream fos = new FileOutputStream(on);
                    PrintWriter pw = new PrintWriter(fos);
                    hasNext = javaToGraphviz.writeGraphviz(pw);
                    pw.flush();
                    fos.close();
                }
                diagramIndex++;
            } while (hasNext);
        }
        
    }
   
	

}