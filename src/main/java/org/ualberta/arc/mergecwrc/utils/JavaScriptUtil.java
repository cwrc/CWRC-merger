package org.ualberta.arc.mergecwrc.utils;

import netscape.javascript.JSObject;

/**
 *
 * @author mpm1
 */
public class JavaScriptUtil {
    public static Object callFunction(JSObject js, String function, Object[] params){
        int index = function.indexOf(".");
        
        if(index > 0){
            return callFunction((JSObject)js.getMember(function.substring(0, index)), function.substring(index + 1), params);
        }else{
            return js.call(function, params);
        }
    }
}
