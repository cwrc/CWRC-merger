package org.ualberta.arc.mergecwrc;

import java.util.List;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil.SectionDiff;

/**
 *
 * @author mpm1
 */
public class UtilTest {
    public static void main(String[] args){
        /*String[] tests = new String[]{
          "Mark",
          "Marissa",
          "Markus",
          "Marky",
          "Marl",
          "Merv",
          "mark",
          "mArK",
          "M A R K"
        };
        
        for(int index = 0; index < tests.length; ++index){
            String first = tests[index];
            
            for(int compare = index; compare < tests.length; ++compare){
                String second = tests[compare];
                int score = ScoringUtil.computeLevenshteinDistance(first.toUpperCase(), second.toUpperCase());
                
                System.out.println(first + " : " + second + " = " + score);
            }
        }*/
        
        String oldStr = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus suscipit ante interdum ligula posuere dapibus quis a enim. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Praesent adipiscing, felis fringilla molestie consequat, eros mauris volutpat quam, eu pulvinar orci erat in turpis. Mauris blandit cursus tempus. Suspendisse sagittis urna eu lectus imperdiet nec lacinia eros vulputate. Cras eleifend lectus nec nisi congue laoreet. Donec laoreet turpis eu nisi viverra quis tempor ligula tempus. Duis lacinia consectetur volutpat. Mauris ornare commodo lectus. Quisque pharetra commodo dictum. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Sed ante lacus, scelerisque luctus congue quis, auctor in tortor. Proin iaculis, dui quis pulvinar ultricies, orci dui tincidunt dolor, sit amet mattis quam mauris fermentum dolor. Ut pulvinar enim ut est vulputate dictum. In hac habitasse platea dictumst. Sed porta fringilla velit sed tristique.";
        String newStr = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Just adding a change. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Praesent adipiscing, eros mauris volutpat quam, eu pulvinar orci erat in turpis. Mauris blandit cursus tempus. Suspendisse sagittis urna eu lectus imperdiet nec lacinia eros vulputate. Donec laoreet turpis eu nisi viverra quis tempor ligula tempus. Duis lacinia consectetur volutpat. Mauris ornare commodo lectus. Quisque pharetra commodo dictum. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Sed ante lacus, scelerisque luctus congue quis, auctor in tortor. Proin iaculis, dui quis pulvinar ultricies, orci dui tincidunt dolor, sit amet mattis quam mauris fermentum dolor. Ut pulvinar enim ut est vulputate dictum. In hac habitasse platea dictumst. Sed porta fringilla velit sed tristique.\nNunc in suscipit nisi. Nulla ac risus at risus pellentesque scelerisque. Vivamus interdum mattis diam, quis vestibulum est iaculis nec. In consectetur, nisi et laoreet luctus, turpis diam imperdiet tortor, et eleifend neque enim ac nisi. Aliquam erat volutpat. Nunc fermentum lobortis est, id dapibus lectus consectetur nec. Etiam tincidunt varius pulvinar. In gravida lobortis pharetra. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Sed tempor mi quis tellus tincidunt euismod. Suspendisse potenti.";
        
        List<SectionDiff> comparison = ScoringUtil.getSimpleDiff(oldStr, newStr);
        
        for(SectionDiff diff : comparison){
            System.out.print(diff.getConsole());
        }
        System.out.println();
    }
}
