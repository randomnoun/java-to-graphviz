package com.example.input;

/** method chaining test cases
 * 
 */
public class MethodChain {

    public static class Heart {
        Heart unchain() { return this; }
        Heart cos() { return this; }
        Heart you() { return this; }
        Heart dont() { return this; }
        Heart love() { return this; }
        Heart me() { return this; }
        Heart no() { return this; }
        Heart more() { return this; }
    }
    public static class Fool {
        Fool chain() { return this; }
    }
    public static class FoolGenerator {
        Fool generate() { return new Fool(); }
    }
    
    public void testMethodChain() {
        // chain of fools
        new FoolGenerator().generate().
            chain().
            chain().
            chain();
    }
    

    // gv-subgraph: something
    public void testMethodUnchain() {
        
        Heart h = new Heart();
        h.unchain()
            .cos()
            .you()
            .dont()
            .love()
            .me()
            .no()
            .more(); // gv: let me go { gv-fluent: true; }
    }
    
    

    
}
