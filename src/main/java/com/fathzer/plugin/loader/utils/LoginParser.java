package com.fathzer.plugin.loader.utils;

import java.net.PasswordAuthentication;

/** A User:password login parser.
 */
@SuppressWarnings("java:S1610")
public abstract class LoginParser {
	private LoginParser() {
		super();
	}
	
	/** Converts a user:pwd formatted string to a PasswordAuthentication login. 
	 * @param userAndPwd The string. Its format should be user:pwd. pwd can be omitted.
	 * @return The login or null if the input String is empty or null
	 */
	public static PasswordAuthentication fromString(String userAndPwd) {
		if (userAndPwd!=null && !userAndPwd.trim().isEmpty()) {
			final int index = userAndPwd.indexOf(':');
			return index<0 ? new PasswordAuthentication(userAndPwd, new char[0]) : new PasswordAuthentication(userAndPwd.substring(0,index), userAndPwd.substring(index+1).toCharArray());
		} else {
			return null;
		}
	}
	
	/** Converts a PasswordAuthentication login to a String.
	 * <br>The password is replaced by stars in the string.
	 * @param login The login.
	 * @return a String
	 */
	public static String toString(PasswordAuthentication login) {
		StringBuilder builder = new StringBuilder();
		builder.append(login.getUserName());
		if (login.getPassword().length!=0) {
			builder.append(":*******");
		}
		return builder.toString(); 
	}
}
