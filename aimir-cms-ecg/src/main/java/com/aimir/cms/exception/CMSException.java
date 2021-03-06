
package com.aimir.cms.exception;

import javax.xml.ws.WebFault;

import com.aimir.cms.model.ErrorParam;


/**
 * This class was generated by Apache CXF 2.5.2
 * 2013-01-11T13:33:56.627+09:00
 * Generated source version: 2.5.2
 */

@WebFault(name = "Exception", targetNamespace = "http://server.ws.cms.aimir.com/")
public class CMSException extends java.lang.Exception {

	private static final long serialVersionUID = 7620679159768502449L;
	private Exception exception;

    public CMSException() {
        super();
    }
    
    public CMSException(Exception exception) {     
        this.exception = exception;
    }
    
    public CMSException(int errorType, String errorMessage) {
        super(errorMessage);
        this.exception = new Exception();
    	ErrorParam errorParam = new ErrorParam();
    	errorParam.setErrorId((long) errorType);
    	errorParam.setErrorMsg(errorMessage);
    	this.exception.setErrorParam(errorParam);
    }
    
    public CMSException(String message) {
        super(message);
    }
    
    public CMSException(String message, Throwable cause) {
        super(message, cause);
    }

    public CMSException(String message, Exception exception) {
        super(message);
        this.exception = exception;
    }

    public CMSException(String message, Exception exception, Throwable cause) {
        super(message, cause);
        this.exception = exception;
    }

    public com.aimir.cms.exception.Exception getFaultInfo() {
        return this.exception;
    }
}
