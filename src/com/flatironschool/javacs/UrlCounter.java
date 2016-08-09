package com.flatironschool.javacs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * Encapsulates a map from search term to frequency (count).
 * 
 * @author downey
 *
 */
public class UrlCounter {
	
	private Map<String, Integer> map;
	private String label;
	
	public UrlCounter(String label) {
		this.label = label;
		this.map = new HashMap<String, Integer>();
	}
	
	public String getLabel() {
		return label;
	}
	
	/**
	 * Returns the total of all counts.
	 * 
	 * @return
	*/
	public int size() {
		int total = 0;
		for (Integer value: map.values()) {
			total += value;
		}
		return total;
	}

	/**
	 * Takes a collection of Elements and counts their urls.
	 * 
	 * @param paragraphs
	 */
	public void processElements(Elements paragraphs) {
		Elements links = paragraphs.select("a[href]");
                for(Element link: links){
                      	String abs_url = link.attr("abs:href"); 
			if(abs_url.startsWith("https://en.wikipedia.org")
					&& !abs_url.contains("#cite_note")){
				incrementUrlCount(abs_url);
                	}
		}
	}

	/**
	 * Increments the counter associated with `abs_url`.
	 * 
	 * @param term
	 */
	public void incrementUrlCount(String abs_url) {
		// System.out.println(term);
		put(abs_url, get(abs_url) + 1);
	}

	/**
	 * Adds a url to the map with a given count.
	 * 
	 * @param term
	 * @param count
	 */
	public void put(String abs_url, int count) {
		map.put(abs_url, count);
	}

	/**
	 * Returns the count associated with this url, or 0 if it is unseen.
	 * 
	 * @param term
	 * @return
	 */
	public Integer get(String abs_url) {
		Integer count = map.get(abs_url);
		return count == null ? 0 : count;
	}

	/**
	 * Returns the set of urls that have been counted.
	 * 
	 * @return
	 */
	public Set<String> keySet() {
		return map.keySet();
	}
	
	/**
	 * Print the url and their counts in arbitrary order.
	 */
	public void printCounts() {
		for (String key: keySet()) {
			Integer count = get(key);
			System.out.println(key + ", " + count);
		}
		System.out.println("Total of all counts = " + size());
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		
		WikiFetcher wf = new WikiFetcher();
		Elements paragraphs = wf.fetchWikipedia(url);
		
		UrlCounter counter = new UrlCounter(url.toString());
		counter.processElements(paragraphs);
		counter.printCounts();
	}
}
