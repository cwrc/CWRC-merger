package org.ualberta.arc.mergecwrc;

/**
 * The default exception to be thrown and caught by the system.
 * 
 * @author mpm1
 */
public class CWRCException extends Exception {

    /**
     * A cwrc error defined by a code and containing a message.
     */
    public static enum Error {

        /** An Unexpected exception occurred. **/
        UNEXPECTED("An Unexpected exception occurred."),
        /** An error occurred while creating the datasource. **/
        DATASOURCE_CREATE("An error occurred while creating the datasource."),
        /** An error occurred while loading the datasource. **/
        DATASOURCE_LOAD("An error occurred while loading the datasource."),
        /** An error occurred while saving the datasource. **/
        DATASOURCE_SAVE("An error occurred while saving the datasource."),
        /** An error occurred while executing the query. **/
        QUERY_ERROR("An error occurred while executing the query."),
        /** An error occurred while reading the variant file. **/
        VARAINT_READ("An error occurred while reading the variant file."),
        /** The given node is invalid. **/
        INVALID_NODE("The given node is invalid.");
        String message;

        Error(String message) {
            this.message = message;
        }

        /**
         * @return Message explaining the error.
         */
        public String getMessage() {
            return message;
        }
    }
    private Error error;

    /**
     * Same as calling CWRCException(null, null).
     */
    public CWRCException() {
        this(null, null);
    }

    /**
     * Same as calling CWRCException(error, null).
     * 
     * @param error The type of error to be displayed.
     */
    public CWRCException(Error error) {
        this(error, null);
    }

    /**
     * Same as calling CWRCException(null, cause).
     * 
     * @param cause The cause of the exception.
     */
    public CWRCException(Exception cause) {
        this(null, cause);
    }

    /**
     * Produces an exception containing the cause and error message. If the error is null, then the error is considered to be of value UNEXPECTED.
     * 
     * @param error The type of error to be displayed.
     * @param cause The cause of the exception.
     */
    public CWRCException(Error error, Exception cause) {
        if (error == null) {
            this.error = Error.UNEXPECTED;
        } else {
            this.error = error;
        }

        if (cause != null) {
            this.initCause(cause);
        }
    }

    @Override
    public String getMessage() {
        return error.name() + " - " + error.message;
    }
    
    public Error getError(){
        return error;
    }
}
