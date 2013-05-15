package org.datalift.lov.exception;

@SuppressWarnings("serial")
public class LovModuleException extends Exception {

	public LovModuleException(String msg, Exception e) {
		super(msg, e);
	}

}
