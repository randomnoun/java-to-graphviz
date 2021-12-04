package com.example.input;

/** comment attribution test cases
 * 
 */
public class CommentAttribution {

    // gv-style: { @import "JavaToGraphviz.css"; }
    
    // keep all the expressionStatements. Do I really want to do this ? Look I suppose so
    // gv-keepNode: expressionStatement block
    
    /* gv-style: {
       }
    */

    /* gv-style: {
    node {
        gv-idFormat: "s_${lineNumber}";
        // gv-labelFormat: "${id}: ${nodeType} lkn: ${lastKeepNodeId}";
        gv-labelFormat: "${lineNumber} (${nodeType})"; 
    }
    */


    // gv: comment just before the method
    public void testAttribution() {
        // gv: start of method
        
        // gv: this comment has it's own node
        
        System.out.println("s1"); // gv: this comment is for s1
        
        // gv:v: this comment is for s2
        System.out.println("s2"); 
        System.out.println("s3");
        // gv:^: this comment is for s3
        
        System.out.println("s4");  System.out.println("s5");  // gv: this comment is for s4 OR IS IT
        System.out.println("s6");  System.out.println("s7");  // gv:<: this comment is for s7
        /* gv:>:this comment is for s8 */ System.out.println("s8");  System.out.println("snot8");
        System.out.println("s9");   /* gv:<--:this comment is for s9 */  System.out.println("s10");
        System.out.println("s11");  /* gv:-->:this comment is for s12 */ System.out.println("s12");
        
        System.out.println("s13"); /* gv: this comment is for s13 */  System.out.println("s14"); // gv: this comment is for s14 OR IS IT
        
        
        
    }
    
}
