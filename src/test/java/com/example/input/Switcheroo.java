package com.example.input;

/** the new switch features in java 8, 12, 13 and 17
 * 
 */
public class Switcheroo {

    // gv-style: { @import "JavaToGraphviz.css"; }
    
    public void testStringSwitch() {
    	// gv: start of method
    	String night = "Thursday";
        switch (night) {
            case "Monday night":
                System.out.println("50's night");
                break;
            case "Tuesday night":
                System.out.println("60's night");
                break;
            case "Wednesday night":
                System.out.println("70's night");
                break;
            case "Friday night":
                System.out.println("Thursday night");
                break;
            default:
                System.out.println("Unknown");
        }
    }
    
    public void testStringExpression() {
    	// NB: java 12 has different syntax
    	String night = "Thursday";
    	String theme = switch (night) {
            case "Monday":
                yield "50's night";
            case "Tuesday":
            	yield "60's night";
            case "Wednesday":
            	yield "70's night";
            case "Friday":
                yield "Thursday night";
            default:
            	yield "Unknown";
        };
        System.out.println(theme);
    }

    public void testStringExpressionArrow() {
    	// NB: java 12 has different syntax
    	String night = "Thursday";
    	String theme = switch (night) {
            case "Monday" -> "50's night";
            case "Tuesday" -> "60's night";
            case "Wednesday" -> "70's night";
            case "Friday" -> "Thursday night";
            default -> "Unknown";
        };
        System.out.println(theme);
    }

    public void testMultipleStringExpressionArrow() {
    	// NB: java 12 has different syntax
    	String night = "Thursday";
    	String theme = switch (night) {
            case "Monday" -> "50's night";
            case "Tuesday" -> "60's night";
            case "Wednesday" -> "70's night";
            case "Friday" -> "Thursday night";
            case "Thursday", "Saturday", "Sunday" -> "Unknown";
            default -> throw new IllegalStateException("hrm");
        };
        System.out.println(theme);
    }

    /*
    public void testPatternMatching() {
    	// java 17, according to https://medium.com/@javatechie/the-evolution-of-switch-statement-from-java-7-to-java-17-4b5eee8d29b7
    	// but it's a preview feawture, so needs to be enabled
    	// see https://wiki.eclipse.org/Java17/Examples#:~:text=Pattern%20Matching%20for%20switch%20is,enabled%20for%20the%20project%2Fworkspace.
    	// and https://www.baeldung.com/java-preview-features
    	Object obj = "Is it a string?";
    	String thing = switch (obj) {
	        case Integer i -> "It is an integer";
	        case String s -> "It is a string";
	        default -> "It is none of the known data types";
	    };
        System.out.println(thing);
    }
    */
    

    
    

    
}
