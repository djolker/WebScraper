package netbeanscrawler;

import java.util.*;

public class CrawlData {

	int links;
	int images;
	List<String> connections = new ArrayList();
	
	public int getLinks()
	{
		return links;
	}
	
	public void setLinks(int links)
	{
		if(links>=0)
		{
			this.links = links;
		}
	}
	
	public int getImages()
	{	
		return images;
	}
	
	public void setImages(int images)
	{
		if(images>=0)
		{
			this.images = images;
		}
	}
	
	public void addToLinks()
	{
		this.links++;
	}
	
	public void addToImages()
	{
		this.images++;
	}
	
	public String getNextListItem() 
	{
		if(connections.size() > 0)
		{
			return this.connections.get(0);
		}
		
		return "NO_CONNECTIONS_LEFT";
		
	}
	
	public void addNewConnection(String href)
	{
		connections.add(href);
	}
	
	public int getConnectionsLength()
	{
		return this.connections.size();
	}
	
        public void writeAllConnections()
	{
		removeRedundancies();
		
		for(int i = 0; i<this.connections.size(); i++)
		{
			System.out.println(connections.get(i));
		}
	}
	
	private void removeRedundancies()
	{
		List<String> tempList = new ArrayList();
		for(int i = 0; i < this.connections.size(); i++)
		{
			if(!tempList.contains(connections.get(i)))
			{
				tempList.add(connections.get(i));
			}
		}
		
		this.connections = tempList;
	}
}