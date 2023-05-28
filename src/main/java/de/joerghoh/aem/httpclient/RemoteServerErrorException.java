package de.joerghoh.aem.httpclient;



/**
 * The remote system throw a server error (statuscode 5xx)
 *
 */

public class RemoteServerErrorException extends Exception {
	
	
	int errorCode = 0;
	
	public RemoteServerErrorException(String msg, int errorCode) {
		super(msg);
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	

}
