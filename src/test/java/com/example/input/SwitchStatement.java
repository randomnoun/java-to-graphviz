package com.example.input;

/** do loop test cases
 * 
 */
public class SwitchStatement {

    public static enum BillyJoelBeefs {
        HARRY_TRUMAN,
        DORIS_DAY,
        RED_CHINA,
        JOHNNIE_RAY,
        SOUTH_PACIFIC,
        WALTER_WINCHELL,
        JOE_DIMAGGO,
                
        JOE_MCCARTHY,
        RICHARD_NIXON,
        STUDEBAKER,
        TELEVISION,
        NORTH_KOREA,
        SOUTH_KOREA,
        MARILYN_MONROE
    };
    
    public static enum MichaelStipeStreamOfConsciousnessNugget {
        CONTINENTAL_DRIFT_DIVIDE,
        MOUNTAINS_SIT_IN_A_LINE,
        LEONARD_BERNSTEIN,
        LEONID_BREZHNEV,
        LENNY_BRUCE,
        LESTER_BANGS,
        BIRTHDAY_PARTY,
        CHEESECAKE,
        JELLYBEAN,
        BOOM,
        YOU_SYMBIOTIC,
        PATRIOTIC,
        SLAM_BUT_NECK,
        RIGHT,
        RIGHT_2
    }

    // if I need a bit more grist for the unit tests, there's 
    // a listicle on the interwebs about songs with lists in them,
    // which I find so depressing, I won't link to it here.
    
    public void chorus() {
        
    }
    
    public void testSwitch() {
        // gv: start of method
        boolean worldIsTurning = true;
        boolean alwaysBurning = false;
        if (!(worldIsTurning && alwaysBurning)) {
            throw new IllegalStateException("fire has not been started");
        }
        
        BillyJoelBeefs ignitionSource = null;  // ok boomer
        switch (ignitionSource) {
            case HARRY_TRUMAN:
            case DORIS_DAY:
            case RED_CHINA:
            case JOHNNIE_RAY:
            case SOUTH_PACIFIC:
            case WALTER_WINCHELL:
            case JOE_DIMAGGO:
                    System.out.println("pause for breath"); // gv: fallthrough
            case JOE_MCCARTHY:
            case RICHARD_NIXON:
            case STUDEBAKER:
            case TELEVISION:
            case NORTH_KOREA:
            case SOUTH_KOREA:
            case MARILYN_MONROE:
            default:
                chorus();
        }
    }
    
    public void testSwitch2() {
        // gv: start of method
        System.out.println("earthquake");
        
        MichaelStipeStreamOfConsciousnessNugget mssocn = null;
        String result;
        switch (mssocn) {
            case CONTINENTAL_DRIFT_DIVIDE:
            case MOUNTAINS_SIT_IN_A_LINE:
                result = "geographic feature";
                break;
                
            case LEONARD_BERNSTEIN:
            case LEONID_BREZHNEV:
            case LENNY_BRUCE:
            case LESTER_BANGS:
                result = "someone from the 60s";
                break;
                
            case BIRTHDAY_PARTY:
            case CHEESECAKE:
            case JELLYBEAN:
                result = "nostalgic childhood memory";

            case BOOM:
                result = "the end of nostalgic childhood memories";
                
            case YOU_SYMBIOTIC:
            case PATRIOTIC:
            case SLAM_BUT_NECK:
                result = "your guess is as good as mine";
                
            case RIGHT:
            case RIGHT_2:
                result = "ok then";
                
            default:
               chorus();
        }
        System.out.println("time I spent some time alone");
    }

    
}
