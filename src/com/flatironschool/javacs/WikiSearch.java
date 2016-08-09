package com.flatironschool.javacs;

import java.io.IOException;
import java.lang.NullPointerException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Set;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Double> map;
	private JedisIndex jedisIndex = null;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Double> map) {
		this.map = map;
	}

	public WikiSearch(Map<String, Double> map, JedisIndex jedisIndex){
		this.map = map;
		this.jedisIndex = jedisIndex;
	}

	//sets the JedisIndex
	public void setIndex(JedisIndex jedisIndex){
		this.jedisIndex = jedisIndex;
	}

	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Double getRelevance(String url) {
		Double relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	public void print() {
		List<Entry<String, Double>> entries = sort();
		for (Entry<String, Double> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
		Map<String, Double> union = new HashMap<String, Double>(map); //new hashmap same as original map
		for (String term: that.map.keySet()) {
			double relevance = totalRelevance(this.getRelevance(term), that.getRelevance(term));
			union.put(term, relevance);
		}
		return new WikiSearch(union);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
		//maybe longer articles
		Map<String, Double> intersection = new HashMap<String, Double>();
		for (String term: map.keySet()) { //map contains URLs that contain the search term to relevance scores
			if (that.map.containsKey(term)) { //if the same URL contains these two terms
				double relevance = totalRelevance(this.map.get(term), that.map.get(term));
				//just adding the frequency of the search term from each page
				//article length? longer articles may just naturally have a larger frequency for the words
				//if the page has a high frequency for one word, but only 1 count of the other
				intersection.put(term, relevance);
			}
		}
		return new WikiSearch(intersection);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
		Map<String, Double> difference = new HashMap<String, Double>(map);
		for (String term: that.map.keySet()) {
			difference.remove(term);
		}
		return new WikiSearch(difference);
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected double totalRelevance(double rel1, double rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Double>> sort() {
		// NOTE: this can be done more concisely in Java 8.  See
		// http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java

		// make a list of entries
		List<Entry<String, Double>> entries = 
				new LinkedList<Entry<String, Double>>(map.entrySet());
		
		// make a Comparator object for sorting
		Comparator<Entry<String, Double>> comparator = new Comparator<Entry<String, Double>>() {
            @Override
            public int compare(Entry<String, Double> e1, Entry<String, Double> e2) {
                return e1.getValue().compareTo(e2.getValue());
            }
        };
        
        // sort and return the entries
		Collections.sort(entries, comparator);
		return entries;
	}


	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index){
		Map<String, Double> map = new HashMap<String, Double>();
		
		//get list of URLs with term from index
		Set<String> urls = index.getURLs(term);
		for(String url : urls){
			double tfidf = calculateRelevance(url, term, index);
			map.put(url, tfidf);
		}
		return new WikiSearch(map);
	}

		/**
	*
	*sets the relevance score of a URL
	*input: String URL, search terms
	*Output: nothing, but alters map so the URL has the updated relevance
	*
	*/
	public static double calculateRelevance(String url, String term, JedisIndex index){
		double tfidf = 0.0;

		TermCounter tc = index.getTermCounter(url);

		/*if(tc == null){

			System.out.println("tc is null");
		}*/

		double totalIndexed = index.getPagesIndexed();
		double termURLCount = index.getTermURLCount(term);
		//if all of the indexed pages contain the word we searched, our idf value becomes 0

		
		//calculates tf-idf value for the term searched, specific to url
		double tf = tc.getTfValue(term);
		double idf = java.lang.Math.log(totalIndexed/ termURLCount);

		tfidf = tf*idf;
		return tfidf;

	}

	public static void main(String[] args) throws IOException {
		
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		/*index.deleteURLSets();
		index.deleteTermCounters();
		index.deleteAllKeys();*/
		
		//load index
		WikiFetcher wf = new WikiFetcher();

		String url1 = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		Elements paragraphs = wf.readWikipedia(url1);
		index.indexPage(url1, paragraphs);
		
		String url2 = "https://en.wikipedia.org/wiki/Programming_language";
		paragraphs = wf.readWikipedia(url2);
		index.indexPage(url2, paragraphs);

		String url3 = "https://en.wikipedia.org/wiki/Philosophy";
		paragraphs = wf.readWikipedia(url3);
		index.indexPage(url3, paragraphs);

		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();
		
		/*// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();
		
		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
		*/
	}
}