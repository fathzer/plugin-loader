package com.fathzer.plugin.loader;

/** Thrown to indicate that something went wrong during plugin instantiation.
 * 
 */
public class PluginInstantiationException extends Exception {
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param message The detail message.
	 */
	public PluginInstantiationException(String message) {
		super(message);
	}

	/** Constructor.
	 * @param cause the cause (which is saved for later retrieval by the Throwable.getCause() method).
	 * <br>A null value is permitted, and indicates that the cause is unknown.
	 */
	public PluginInstantiationException(Throwable cause) {
		super(cause);
	}
}
