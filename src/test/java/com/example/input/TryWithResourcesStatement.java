package com.example.input;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** try-with-resources test cases
 * 
 */
public class TryWithResourcesStatement {

    public int testAHoleInTheGround() {
        int whatTheCompanyTakes = -1, whatTheCompanyWants = -1;
        System.out.println("But if I work all day at the blue sky mine");
        try (
            InputStream is = new FileInputStream("blue-sky-mine.txt");
            OutputStream os = new FileOutputStream("whos-gonna-save-me.txt"); 
        ) {
            whatTheCompanyWants = is.read();
            whatTheCompanyTakes = whatTheCompanyWants;
            os.write(null);
            
            return whatTheCompanyTakes;
        } catch (Exception e) {
            System.out.println("Some have sailed from a distant shore");
            
        } finally {
            System.out.println("There'll be food on the table tonight");    
        }
        return whatTheCompanyWants;
    }
    
}
