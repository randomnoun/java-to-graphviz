package com.example.input;

/** while loop test cases
 * 
 */
public class WhileStatement {

    private static class KnowinWhenToDoThingsService {
        public int getNextAction() {
            return (int) (Math.random() * 4);
        }
    }
    
    public void testWhile() {
        // gv: start of method
        boolean sittinAtTheTable = true;
        while (!sittinAtTheTable) {
            System.out.println("count money");
        }
    }
    
    public void testWhileBreakContinue() {
        // gv: start of method
        boolean theDealIsDone = false;
        boolean sittinAtTheTable = true;
        KnowinWhenToDoThingsService knowinWhenToDoThingsService = new KnowinWhenToDoThingsService();
        while (sittinAtTheTable) {
            int action = knowinWhenToDoThingsService.getNextAction();
            if (action == 0) {
                System.out.println("hold 'em");
            
            } else if (action == 1) {
                System.out.println("fold 'em");
                return;
                
            } else if (action == 2) {
                System.out.println("walk away");
                continue;

            } else if (action == 3) {
                System.out.println("run");
                break;
            
            }
            System.out.println("guitar solo");

            if (theDealIsDone) {
                return;
            }

            System.out.println("inspect eyes of other players");
        }
    }
    
    
}
