//Someday I'll have the motivation to split this up into multiple package sets
//Idealy, all the parsing tools will be grouped, and all the connection tools will
//be seperate.

package netbeanscrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.*;

public class WebCrawler {
	String currentSite;
  int currentDomainID;
	
	public WebCrawler()
	{
            boolean crawling = true;

            //Pulls the top address off of the queue
            try{
                currentSite = grabTopAddress();
            }
            catch(Exception e)
            {
                currentSite = "https://www.reddit.com/";
            }

            //If this errors out, or the queue is empty, it defaults to reddit
            if(currentSite == "NULL")
            {
                currentSite = "https://www.reddit.com/";
            }

            while(crawling)
            {
                System.out.println("Currently Scraping:" + this.currentSite);
                CrawlData returned = scrapeSite(this.currentSite);

                System.out.println("Links: " + returned.getLinks());
                System.out.println("Images: " + returned.getImages());

                returned.writeAllConnections();
                exportToDatabase(returned);
                currentSite = grabTopAddress();
                currentDomainID = domainSend(currentSite);
            }
	}
	
	String grabTopAddress()
	{
		serverCreds sc = new serverCreds();
		System.out.println("Connecting to Database...");
                
		String next = "NULL";
		try {
                    Class.forName(sc.driver);
                    
                    Connection conn = DriverManager.getConnection(sc.serv+sc.dbName,sc.userName, sc.password);
                    
                    Statement st = conn.createStatement();
                    ResultSet res = st.executeQuery("SELECT * FROM inQueue where isread = 0 LIMIT 1");
                    int nextID = -4;

                    while(res.next())
                    {
                            nextID = res.getInt("id");
                            String url = res.getString("url");
                            next = url;
                            System.out.println("TOP LINE IN QUEUE: " + url + nextID + "ID: " + res.getInt("id"));
                    }

                    st.executeUpdate("UPDATE inQueue SET isread = 1 WHERE idinQueue = " + nextID);
                    conn.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return next;
	}
	
	CrawlData scrapeSite(String surl)
	{
            URL url;
            InputStream is = null;
            BufferedReader br;
            String line;
            CrawlData cd = new CrawlData();
		
            try {
                url = new URL(surl);
                is = url.openStream();
                br = new BufferedReader(new InputStreamReader(is));
			
                while((line= br.readLine()) != null){
                    System.out.println(line);				
                    char[] thisLine = line.toCharArray();
                    for(int i = 0; i<thisLine.length; i++)
                    {
                        if(thisLine[i] == '<')
                        {
                            if(thisLine[i+1] == 'a' || thisLine[i+1] == 'A')
                            {
                                cd.addToLinks();
                            }
                            else if(thisLine[i+1] == 'i' && thisLine[i+2] == 'm')
                            {
                                cd.addToImages();
                            }
                        }
                    }

                    Pattern p = Pattern.compile("href=\"([^\"]*)\"");
                    Matcher m = p.matcher(line);

                    while(m.find())
                    {
                        for(int l = 0; l<=m.groupCount(); l++)
                        {
                            String groupStr = formatLink(m.group(l));
                            if(groupStr != "NULL")
                            cd.addNewConnection(groupStr);
                        }
                    }
                }
            }
            catch(MalformedURLException mue)
            {
                    mue.printStackTrace();
            }
            catch(IOException ioe)
            {
                    ioe.printStackTrace();
            }
            finally{
            }
		
            return cd;
	}
	
	String formatLink(String link)
	{
            link = link.replace("href=", "");
            link = link.replace("\"", "");
		
		char[] linkChar = link.toCharArray();
		if(linkChar.length>2)
		{
			try{
				if(linkChar[0] == '/' && linkChar[1] == '/')
				{
					link = "http:" + link;
				}
				else if((linkChar[0] == '/' && linkChar[1] != '/') && (this.currentSite.toCharArray()[this.currentSite.toCharArray().length-1] == '/'))
				{
					link = this.currentSite + link.substring(1);
				}
				else if((linkChar[0] == '/' && linkChar[1] != '/') && (this.currentSite.toCharArray()[this.currentSite.toCharArray().length] != '/'))
				{
					link = this.currentSite +link;
				}
				else if(link.substring(0, link.indexOf(':'))== "javascript")
				{
					link = "NULL";
				}
				else if(linkChar[0] == '?')
				{
					link = "NULL";
				}
				else if(linkChar[0] == '#')
				{
					link = "NULL";
				}
				
				if(link.contains(".ico") || link.contains(".png") || link.contains(".gif") || link.contains(".jpg") || link.contains(".jpeg") || link.contains(".css"))
				{
					link = "NULL";
				}
			}
			catch(Exception e){
                            System.out.println("Error formatting link:::Skipping: " + link);
                            System.out.println(e);
                            ErrorLog er = new ErrorLog(e.toString());
			}
		}
		else
		{
			link = "NULL";
		}
		
            return link;
	}
	
	void exportToDatabase(CrawlData cd)
	{
            //Newly scraped data is being sent to the database here. It's 
            //implied that we already know the domainID of the current site
            //already.
            
            serverCreds sc = new serverCreds();

            try{
                Class.forName(sc.driver);
                Connection conn = DriverManager.getConnection(sc.serv+sc.dbName,sc.userName, sc.password);
			
                Statement st = conn.createStatement();
			
                for(int i = 0; i<cd.getConnectionsLength(); i++)
                {
                    boolean unique = testForUnique(cd.connections.get(i));
                    
                    if(unique){
                        int domainID = domainSend(cd.connections.get(i));
                        //if domainSend errors out or for whatever reason it
                        //can't return a proper id, it returns -1 as a flag
                        //to stop the rest of this process
                        
                        if(domainID != -1) 
                        {
                            //1)We need to submit the new URL into the
                            //long term log
                            String insLinkToLog = "INSERT INTO `WebScrape`.`scrapedLog` (`domainid`, `url`) VALUES ('" + domainID + "', '" + cd.connections.get(i) +"');";
                            System.out.println(insLinkToLog);
                            st.executeUpdate(insLinkToLog);
                            //2)Then add it to the queue, so that the new
                            //URL will eventually be scraped
                            String insLinkToQueue = "INSERT INTO `WebScrape`.`inQueue` (`url`, `logid`) VALUES ('" + cd.connections.get(i) +"', '"+ domainID +"');";
                            System.out.println("");
                            st.executeUpdate(insLinkToQueue);
                            //3)Then build the relational row between currentSite
                            //and the site we're logging now
                            String insRelational = "INSERT INTO `WebScrape`.`logRelational` (`root`, `branch`) VALUES('" + this.currentDomainID + "', '"+ domainID +"');";
                            System.out.println("Create Relational: " + insRelational);
                            st.executeUpdate(insRelational);
                        }
                    }
                }
                conn.close();
            }
		
            catch (Exception e){
                e.printStackTrace();
            }
	}
	
	boolean testForUnique(String url)
	{
		boolean unique = true;
		serverCreds sc = new serverCreds();
		
		try{
                    Class.forName(sc.driver);
                    Connection conn = DriverManager.getConnection(sc.serv+sc.dbName,sc.userName, sc.password);

                    Statement st = conn.createStatement();
                    ResultSet res = st.executeQuery("SELECT * FROM inQueue where url = '" + url + "'");
                    int nextID = -4;
                    int count = 0;
                    while(res.next())
                    {
                        count++;
                    }

                    if(count>0)
                    {
                        unique = false;
                    }
                    conn.close();
		}
		catch(Exception e)
		{
                    e.printStackTrace();
		}
		return unique;
	}
	
	int domainSend(String url) //I really need to start writing psuedo code before each method or something
	{
            serverCreds sc = new serverCreds();
            int domainID = -1;

            String domain = parseForDomain(url);

            try{
                Class.forName(sc.driver);
                Connection conn = DriverManager.getConnection(sc.serv+sc.dbName, sc.userName, sc.password);

                Statement st = conn.createStatement();
                String domainCheck = "SELECT * FROM domains where domainName = '" + domain + "' LIMIT 1";
                ResultSet res = st.executeQuery(domainCheck);

                while(res.next())
                {
                    domainID = res.getInt("iddomains");
                }

                
                if(domainID == -1) //WHAT IS THIS?? IF DOMAINID = -1? ISNT IT IF DOMAINID!=-1?
                {
                    Statement stm = conn.createStatement();
                    String dSend = "INSERT INTO `WebScrape`.`domains` (`domainName`) VALUES ('" + domain + "');";
                    System.out.println("Domain being Submitted: " + dSend);
                    domainID = stm.executeUpdate(dSend, Statement.RETURN_GENERATED_KEYS);
                    System.out.println("Returned Key: " + domainID);
                    //Error being thrown:
                    //Unknown column '' in 'field list'
                    //domainSend printing "INSERT INTO `WebScrape`.`domains` (`domainName`) VALUES(``);
                }
               

                conn.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return domainID;
	}
        
    String parseForDomain(String url)
    {
        char[] charURL = url.toCharArray();
        char[] charJustDomain = new char[0];
        if((charURL[0] == 'h' && charURL[1] == 't' && charURL[2] == 't' && charURL[3] == 'p' && charURL[4] == ':')||(charURL[0] == 'h' && charURL[1] == 't' && charURL[2] == 't' && charURL[3] == 'p' && charURL[4] == 's'))
        {
            for(int i = 0; i<=url.length(); i++)
            {
                if(charURL[i] == '.')
                if ((charURL[i+1] == 'c' && charURL[i+2] == 'o' && charURL[i+3] == 'm') ||
                    (charURL[i+1] == 'n' && charURL[i+2] == 'e' && charURL[i+3] == 't') ||
                    (charURL[i+1] == 'o' && charURL[i+2] == 'r' && charURL[i+3] == 'g') ||
                    (charURL[i+1] == 'b' && charURL[i+2] == 'i' && charURL[i+3] == 'z'))
                    {
                        charJustDomain = new char[i+4];
                        for(int j = 0; j<=i+3; j++)
                        {
                            charJustDomain[j] = charURL[j];
                        }
                        break;
                    }
            }
        }
      
        String domain = new String(charJustDomain);
        return domain;
    }
}
