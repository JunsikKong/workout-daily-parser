package com.jskong.com;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
	public static void trace(String _log) {
		Log.logging(_log, "TRACE");
	}
	
	public static void debug(String _log) {
		Log.logging(_log, "DEBUG");
	}
	
	public static void info(String _log) {
		Log.logging(_log, "INFO ");
	}
	
	public static void warn(String _log) {
		Log.logging(_log, "WARN ");
	}
	
	public static void error(String _log) {
		Log.logging(_log, "ERROR");
	}
	
	public static void fatal(String _log) {
		Log.logging(_log, "FATAL");
	}
	
	private static void logging(String _log, String _level) {
		LocalDateTime currentTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String formattedTime = currentTime.format(formatter);
		System.out.println("[" + formattedTime + "] [" + _level + "] " + _log);
	}
}
