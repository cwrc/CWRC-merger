package org.ualberta.arc.mergecwrc.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author mpm1
 */
public class ScoringUtil {

    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
    
    public static float computeLevenshteinPercent(CharSequence str1,
            CharSequence str2){
        int distance = computeLevenshteinDistance(str1, str2);
        
        return 1.0f - (float)(distance << 1)/(float)(str1.length() + str2.length());
    }

    public static int computeLevenshteinDistance(CharSequence str1,
            CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1]
                        + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
                        : 1));
            }
        }

        return distance[str1.length()][str2.length()];
    }

    private static Integer[] arrayKeys(String input, char value) {
        List<Integer> result = new ArrayList<Integer>();

        for (int index = 0; index < input.length(); ++index) {
            if (input.charAt(index) == value) {
                result.add(index);
            }
        }
        
        return result.toArray(new Integer[result.size()]);
    }

    private static boolean checkIsSet(Integer matrix[][], int x, int y) {
        if (x < 0 || y < 0) {
            return false;
        }

        return matrix[x][y] != null;
    }

    private static String getSubString(String string, int index, int length) {
        if (index < 0 || index >= string.length()) {
            return "";
        }
        
        if (length >= string.length() || (index + length) >= string.length()) {
            return string.substring(index);
        }

        return string.substring(index, index + length);
    }

    public static List<SectionDiff> getSimpleDiff(String oldStr, String newStr) {
        List<SectionDiff> difference = new LinkedList<SectionDiff>();
        Integer matrix[][] = new Integer[oldStr.length()][newStr.length()];
        int maxlen = 0;
        int omax = 0;
        int nmax = 0;

        for (int oindex = 0; oindex < oldStr.length(); ++oindex) {
            char ovalue = oldStr.charAt(oindex);
            Integer[] nkeys = arrayKeys(newStr, ovalue);

            for (Integer nindex : nkeys) {
                matrix[oindex][nindex] = checkIsSet(matrix, oindex - 1, nindex - 1) ? matrix[oindex - 1][nindex - 1] + 1 : 1;
                if (matrix[oindex][nindex] > maxlen) {
                    maxlen = matrix[oindex][nindex];
                    omax = oindex + 1 - maxlen;
                    nmax = nindex + 1 - maxlen;
                }
            }
        }

        if (maxlen == 0) {
            difference.add(new SectionDiff(oldStr, newStr, true));
        } else {
            difference.addAll(getSimpleDiff(getSubString(oldStr, 0, omax), getSubString(newStr, 0, nmax)));

            String sameString = getSubString(newStr, nmax, maxlen);
            if (sameString.length() != 0) {
                difference.add(new SectionDiff(sameString, sameString, false));
            }

            difference.addAll(getSimpleDiff(getSubString(oldStr, omax + maxlen, Integer.MAX_VALUE), getSubString(newStr, nmax + maxlen, Integer.MAX_VALUE)));
        }

        return difference;
    }

    public static class SectionDiff {

        private String oldStr, newStr;
        private boolean isDifference;

        public SectionDiff(String oldStr, String newStr, boolean isDifference) {
            this.oldStr = oldStr;
            this.newStr = newStr;
            this.isDifference = isDifference;
        }

        public String getOldStr() {
            return oldStr;
        }

        public String getNewStr() {
            return newStr;
        }

        public boolean isDifference() {
            return isDifference;
        }

        public String getHtml() {
            if (isDifference()) {
                if (oldStr.length() == newStr.length() && oldStr.length() == 0) {
                    return "";
                }

                StringBuilder result = new StringBuilder("<del>");
                result.append(oldStr);
                result.append("</del>");
                result.append("<ins>");
                result.append(newStr);
                result.append("</ins>");

                return result.toString();
            }

            return newStr;
        }

        public String getConsole() {
            if (isDifference()) {
                if (oldStr.length() == newStr.length() && oldStr.length() == 0) {
                    return "";
                }

                StringBuilder result = new StringBuilder((char) 27 + "[31m");
                result.append(oldStr);
                result.append((char) 27 + "[32m");
                result.append(newStr);
                result.append((char) 27 + "[30m");

                return result.toString();
            }

            return (char) 27 + "[30m" + newStr;
        }
    }
}
