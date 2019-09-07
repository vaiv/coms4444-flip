package flip.sim;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	private static FileWriter fileWriter;
	private static boolean active = false;
	private static boolean v = false;
	
	public static void activate() {
		active = true;
	}

	public static void verbose() {
		v = true;
	}
	
	public static void setLogFile(String filename) {
		try {
			fileWriter = new FileWriter(filename, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void log(String str) {
		if (!active) return;
		dump(str);
	}

	
	public static void record(String str) {
		if (!v) return;
		dump(str);
		
	}

	private static void dump(String str)
	{
		DateFormat dateFormat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS] ");
		Date date = new Date();
		str = dateFormat.format(date) + str + "\n";
		// System.err.print(str);
		System.out.println(str);
		if (fileWriter == null) return;
		try {
			fileWriter.append(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void end()
	{
		if (fileWriter == null) return;
		try {
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		fileWriter = null;
	}
}