package org.ualberta.arc.mergecwrc.utils;

import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.commons.lang3.text.translate.UnicodeEscaper;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil.SectionDiff;

/**
 *
 * @author mpm1
 */
public class JSONUtil {

    public static final CharSequenceTranslator ESCAPE_JAVASCRIPT = new AggregateTranslator(
            new LookupTranslator(
            new String[][]{
                {"'", "\\'"},
                {"/", "\\/"},
                {"\"", "\\\""}
            }),
            new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_ESCAPE()),
            UnicodeEscaper.outsideOf(32, 0x7f));

    public static String convertMultipleMatchListToJson(List<MultipleMatchModel> matches) {
        StringBuilder out = new StringBuilder("[");

        Iterator<MultipleMatchModel> iterator = matches.iterator();

        while (iterator.hasNext()) {
            out.append(convertMultipleMatchToJson(iterator.next()));

            if (iterator.hasNext()) {
                out.append(',');
            }
        }

        out.append(']');

        return out.toString();
    }

    public static String convertMultipleMatchToJson(MultipleMatchModel match) {
        StringBuilder out = new StringBuilder("{");

        setValue(out, "name", match.toString());
        //out.append(',');
        //setValue(out, "inputNode", match.getInputNode());
        out.append(',');
        setValue(out, "possibleMatches", convertQueryResultListToJson(match), false);

        out.append('}');

        return out.toString();
    }

    public static String convertQueryResultListToJson(MultipleMatchModel match) {
        StringBuilder out = new StringBuilder("[");

        Iterator<QueryResult> iterator = match.getPossibleMatches().iterator();

        while (iterator.hasNext()) {
            out.append(convertQueryResultToJson(match, iterator.next()));

            if (iterator.hasNext()) {
                out.append(',');
            }
        }

        out.append(']');

        return out.toString();
    }

    public static String convertQueryResultToJson(MultipleMatchModel match, QueryResult result) {
        StringBuilder out = new StringBuilder("{");

        setValue(out, "name", result.getName());
        out.append(',');
        setValue(out, "node", convertSectionDiffsToJson(match.getPossibleMatchDifference(result)), false);
        out.append(',');
        setValue(out, "score", String.valueOf(result.getScore()), false);

        out.append('}');

        return out.toString();
    }
    
    private static String convertSectionDiffsToJson(List<SectionDiff> diffs){
        StringBuilder out = new StringBuilder("[");
        
        Iterator<SectionDiff> iterator = diffs.iterator();
        while(iterator.hasNext()){
            out.append(convertSectionDiffToJson(iterator.next()));

            if (iterator.hasNext()) {
                out.append(',');
            }
        }
        
        out.append(']');
        
        return out.toString();
    }
    
    private static String convertTextToHtml(String text){
        StringBuilder builder = new StringBuilder();
        
        String[] vals = text.split("\n");
        
        for(int index = 0; index < vals.length; ++index){
            builder.append(StringEscapeUtils.escapeHtml4(vals[index]));
            
            if(index < vals.length - 1){
                builder.append("<br/>");
            }
        }
        
        if(text.trim().endsWith("\n")){
            builder.append("<br/>");
        }
        
        return builder.toString();
    }
    
    private static String convertSectionDiffToJson(SectionDiff diff){
        StringBuilder out = new StringBuilder("{");
        
        setValue(out, "isDifference", Boolean.toString(diff.isDifference()));
        out.append(',');
        
        if(!diff.isDifference()){
            setValue(out, "text", convertTextToHtml(diff.getNewStr()));
        }else{
            setValue(out, "oldText", convertTextToHtml(diff.getOldStr()));
            out.append(',');
            setValue(out, "newText", convertTextToHtml(diff.getNewStr()));
        }
        
        out.append('}');
        
        return out.toString();
    }

    private static void setValue(StringBuilder out, String title, String value) {
        setValue(out, title, value, true);
    }

    private static void setValue(StringBuilder out, String title, String value, boolean asString) {
        out.append('"');
        out.append(title);
        out.append("\":");

        if (asString) {
            out.append('"');
            out.append(ESCAPE_JAVASCRIPT.translate(value));
            out.append('"');
        } else {
            out.append(value);
        }
    }
}
