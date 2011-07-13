import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import com.thoughtworks.selenium.DefaultSelenium;


public class SeleniumWrapper {
	static String localip = "";
	static private int PortCounter = 6666;
	private ArrayList<ServerThread> SeleniumThreads;
	ArrayList<String> proxies;
	String currentProxy;
	SeleniumWrapper() throws IOException{
		//Runtime rt = Runtime.getRuntime();
        //rt.exec("taskkill /IM java.exe /F");
        
		SeleniumThreads = new ArrayList<ServerThread>();
	}
	class  ServerThread extends Thread {
		int ServerPort;
		boolean Serverlocal;
		ServerThread(int port, boolean local){
			ServerPort = port;
			Serverlocal = local;
		}
		Process pr;	
	    // This method is called when the thread runs
	    public  void  run() {
	    	BufferedReader in;
			try {
				in = new BufferedReader(new FileReader("Proxy.txt"));
			
			String line;
			proxies = new ArrayList<String>();
			Random generator = new Random();
			 
			while ((line = in.readLine()) != null) 
				proxies.add(line);
	        in.close();
			
	        
			
	         currentProxy = proxies.remove(generator.nextInt(proxies.size()));
	         
	         ArrayList<String> settings = new ArrayList<String>();
	         BufferedReader input;
	 		synchronized(SeleniumWrapper.class){
	         in = new BufferedReader(new FileReader("firefoxprofile\\prefs.js"));
	         boolean ipline = false;
	         boolean portline = false;
				
				
				 
				while ((line = in.readLine()) != null) {
					
					if(Serverlocal){
						if(line.contains("network.proxy.http_port")){
							
							line = "//user_pref(\"network.proxy.http_port\", );";
							
						}else if(line.contains("network.proxy.http")){
							line = "//user_pref(\"network.proxy.http\", \"\");";
							
						}
					}else{
					if(line.contains("network.proxy.http_port")){
						
						line = "user_pref(\"network.proxy.http_port\", " + currentProxy.split(":")[1] + ");";
						portline = true;
					}else if(line.contains("network.proxy.http")){
						line = "user_pref(\"network.proxy.http\", \"" + currentProxy.split(":")[0] + "\");";
						ipline = true;
					}}
					
					settings.add(line);
				}
					
		        in.close();
		        if(!Serverlocal){
		        if(!portline)
		        	settings.add("user_pref(\"network.proxy.http_port\", " + currentProxy.split(":")[1] + ");");
		        if(!ipline)
		        	settings.add("user_pref(\"network.proxy.http\", \"" + currentProxy.split(":")[0] + "\");");
		        }
		 
				BufferedWriter output = new BufferedWriter(new FileWriter("firefoxprofile\\prefs.js"));
				for(String l: settings){
					output.write(l + "\n");
				}
				output.close();
	 		
	         
	         
	        String commend;
	        	commend = "java -jar selenium-server.jar -firefoxProfileTemplate firefoxprofile -port "+ ServerPort;
			//System.out.println("Running: " + commend);
	        Runtime rt = Runtime.getRuntime();
	        pr = rt.exec(commend);
	 		
	        input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
	 		
	        line=null;
	        boolean done = false;
	        while((line=input.readLine()) != null) {
	        	//System.out.println(line);
	            if(line.contains("Started org.openqa.jetty.jetty.Server")){
	            	synchronized(this){
	            		if(Serverlocal)
	            			System.out.println("Robot builded locally");
	            		else
	            			System.out.println("Robot builded with Proxy " + currentProxy);
	            	Thread.sleep(5000);
	            	this.notify();
	            	done = true;
	            	}
	            }
	            if(line.contains("Failed to start")){
	            	System.out.println("Selenium is already running");
	            }
	            if(line.contains("Launching Firefox")){
	            	break;
	            }
	            if(!input.ready()){
	            	Thread.sleep(1000);
	            	//if(done)
	            	//	break;
	            }
	        }
	 		}
	        while((line=input.readLine()) != null) {}
	        	
	        
	 		
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    protected void finalize() throws Throwable
	    {
	      //do finalization here
	      pr.destroy();
	      super.finalize(); //not necessary if extending Object.
	    } 
	}
	
	void  RemoveProxy() throws IOException{
		synchronized(SeleniumWrapper.class){
		BufferedReader in = new BufferedReader(new FileReader("Proxy.txt"));
		
		String line;
		proxies = new ArrayList<String>();
		Random generator = new Random();
		 
		while ((line = in.readLine()) != null) 
			proxies.add(line);
        in.close();
        
        proxies.remove(currentProxy);
        
		BufferedWriter output = new BufferedWriter(new FileWriter("Proxy.txt"));
		for(String l: proxies){
			output.write(l + "\n");
		}
		output.close();
		System.out.println("Proxy " + currentProxy + " deleted");
		}
	}
	 DefaultSelenium AddSeleniumProcess(String URL, boolean local) throws Throwable{
		 
		ServerThread MyServer = null;
		DefaultSelenium selenium = null;
		
		
	 
		int MyPort;
		synchronized(SeleniumWrapper.class){
		MyPort = PortCounter++;
		 }
		try{
		MyServer = new ServerThread(MyPort,local);
		SeleniumThreads.add(MyServer);
		MyServer.start();
		System.out.println("Building Robot...");
		
		synchronized(MyServer){
			MyServer.wait();
		}
		
		
		System.out.println("Starting Robot...");
		if(local)
			selenium = new DefaultSelenium("localhost", MyPort, "*chrome", URL);
		else
			selenium = new DefaultSelenium("localhost", MyPort, "*chrome", URL);
		selenium.start();
		selenium.setTimeout("0");
		System.out.println("Robot QC");
		selenium.open("http://www.cship.info/azenv.php");
		selenium.waitForPageToLoad("30000");
		//System.out.println(selenium.getBodyText());
		if(local){
			localip = selenium.getBodyText().split("REMOTE_ADDR = ")[1].split("REMOTE_PORT = ")[0];
		}
		
		if((/*selenium.getBodyText().contains(currentProxy.split(":")[0]) && */!selenium.getBodyText().contains(localip) )|| local){
			System.out.println("QC Successful!");
			Thread.sleep(5000);
			return selenium;
		}
		else{
			System.out.println("QC Failed! Rebuild Robot");
			selenium.close();
			selenium.stop();
			MyServer.finalize();
			return AddSeleniumProcess(URL,local);
			
		}
		
		} catch(Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            selenium.close();
			selenium.stop();
            MyServer.finalize();
            return AddSeleniumProcess(URL,local);
        }
	}
	protected void finalize() throws Throwable
    {
      //do finalization here
		for(ServerThread t: SeleniumThreads){
			t.finalize();
		}
		
      super.finalize(); //not necessary if extending Object.
    } 
}
