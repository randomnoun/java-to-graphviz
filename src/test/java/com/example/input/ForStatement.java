package com.example.input;

import java.util.ArrayList;
import java.util.List;

/** for loop test cases
 * 
 */
public class ForStatement {

    // gv-style: { @import "JavaToGraphviz.css"; }
    
    // uncomment to test
    // gv-stylex: { @import url("https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/main/resources/JavaToGraphviz.css"); }
    // gv-stylex: { @import url("something.txt"); }
    // gv-stylex: { @import "something.txt"; }
    // gv-stylex: { @import "https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/main/resources/JavaToGraphviz.css"; }
    
    public void testFor() {
        // gv: start of method 
        for (int i=0; i<12; i++) {
            System.out.println(i); // gv: in loop
        }
        System.out.println("twelve little ladybugs. at the ladybug picnic."); // gv: after loop 
    }

    public void testEnhancedFor() {
        // gv: start of method
        List<String> things = new ArrayList<>();
        things.add("raindrops on roses and whiskers on kittens");
        things.add("bright copper kettles and warm woolen mittens");
        things.add("brown paper packages tied up with string");
        int thingCount = 0;
        for (String t : things) {
            // NB: inaccurate thing count due to plurals and conjunctions in thing list
            // @TODO add NLP processing
            thingCount ++; // gv: in loop  
        }
        // gv: after loop
        System.out.println("these are " + 
            (thingCount == 1 ? "one of" :
            (thingCount == 2 ? "a couple of " :
            (thingCount == 3 ? "a few of" :
            "DOES NOT COMPUTE" ))) + " my favourite things"); 
    }

    
}
