package webScraper;

import java.io.*;
import java.io.IOException;
import java.io.FileWriter;

public final class ErrorLog {
	ErrorLog(String e)
	{
		try{
		File f = new File("errorlog.txt");
		
		if(!f.exists()){
			f.createNewFile();
		}
		
		FileWriter fw = new FileWriter(f.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(e);
		bw.close();
		}
		catch(Exception ex){
			System.out.println("Error log had an error. rip");
		}
	}
}
