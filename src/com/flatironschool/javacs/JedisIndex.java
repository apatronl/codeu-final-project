package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * Represents a Redis-backed web search index.
 * IDF(t) = log_e(Total number of documents / Number of documents with term t in it).
 *    -use getPagesIndexed() to get total number of documents
 *    -use getTermURLCount(term) to get # of documents with the term in it
 * we don't actually push the termCounter to redis...just the info from the termCounter
 *    -create a hashmap containing all the termcounters? the label of each termCounter is its URL
 *	  -TF(t) = (Number of times term t appears in a document) / (Total number of terms in the document).
 *    -from each termcounter...getTfValue(term)
 *    -getTermCounter
 * wiki search puts all the values together to get relevance score
 */
public class JedisIndex {

	private Jedis jedis;
	private int pagesIndexed = 0; //the total number of documents
	private ArrayList<String> termCounterIDs = new ArrayList<String>();
	private Map<String, TermCounter> termCounters = new HashMap<String, TermCounter>();
		//hashmap of urls to termcounters


	/**
	 * Constructor.
	 * 
	 * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
	}

	/**
	*
	*input: String URL
	*output: TermCounter associated with URL
	*
	**/

	public TermCounter getTermCounter(String url){
		return termCounters.get(url);
	}
	
	/**
	*
	*returns the number of pages that have been indexed
	*
	**/

	public int getPagesIndexed(){
		return pagesIndexed;
	}
	
	/**
	*returns the number of URLS that contain the inputted term
	*
	**/
	public int getTermURLCount(String term){
		//Set<String> set
		Set<String> termURLs = getURLs(term);
		return termURLs.size();

	}

	/**
	 * Returns the Redis key for a given search term.
	 * 
	 * @return Redis key.
	 */
	private String urlSetKey(String term) {
		return "URLSet:" + term;
	}


	/**
	 * Returns the Redis key for a URL's TermCounter.
	 * 
	 * @return Redis key.
	 */
	private String termCounterKey(String url) {
		String termCounterID = "TermCounter:" + url;
		if(termCounterIDs.contains(termCounterID) == false){ //if this is a new termCounterKey
			termCounterIDs.add(termCounterID); //add it to the the list
			pagesIndexed++; //increase the pagesIndexed count by 1. (there is 1 term counter for each page)
		}
		return termCounterID;
	}

	/**
	 * Checks whether we have a TermCounter for a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public boolean isIndexed(String url) {
		String redisKey = termCounterKey(url);
		return jedis.exists(redisKey);
	}
	
	/**
	 * Adds a URL to the set associated with `term`.
	 * 
	 * @param term
	 * @param tc
	 */
	public void add(String term, TermCounter tc) {
		jedis.sadd(urlSetKey(term), tc.getLabel());
	}

	/**
	 * Looks up a search term and returns a set of URLs.
	 * Number of documents with term t in it
	 * 
	 * @param term
	 * @return Set of URLs.
	 */
	public Set<String> getURLs(String term) {
		Set<String> set = jedis.smembers(urlSetKey(term));
		return set;
	}

	/**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Integer> getCounts(String term) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Set<String> urls = getURLs(term);
		for (String url: urls) {
			Integer count = getCount(url, term);
			map.put(url, count);
		}
		return map;
	}

	/**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Integer> getCountsFaster(String term) {
		// convert the set of strings to a list so we get the
		// same traversal order every time
		List<String> urls = new ArrayList<String>();
		urls.addAll(getURLs(term));

		// construct a transaction to perform all lookups
		Transaction t = jedis.multi();
		for (String url: urls) {
			String redisKey = termCounterKey(url);
			t.hget(redisKey, term);
		}
		List<Object> res = t.exec();

		// iterate the results and make the map
		Map<String, Integer> map = new HashMap<String, Integer>();
		int i = 0;
		for (String url: urls) {
			Integer count = new Integer((String) res.get(i++));
			map.put(url, count);
		}
		return map;
	}

	/**
	 * Returns the number of times the given term appears at the given URL.
	 * 
	 * @param url
	 * @param term
	 * @return
	 */
	public Integer getCount(String url, String term) {
		String redisKey = termCounterKey(url);
		String count = jedis.hget(redisKey, term);
		return new Integer(count);
	}

	/**
	 * Add a page to the index.
	 * 
	 * @param url         URL of the page.
	 * @param paragraphs  Collection of elements that should be indexed.
	 */
	public void indexPage(String url, Elements paragraphs) {
		
		// make a TermCounter and count the terms in the paragraphs
		TermCounter tc = new TermCounter(url);
		tc.processElements(paragraphs);

		//add TermCounter to our hashmap.
		termCounters.put(url, tc);		

		String hashname = termCounterKey(url);
		
		// push the contents of the TermCounter to Redis
		pushTermCounterToRedis(tc, hashname);
	}

	/**
	 * Pushes the contents of the TermCounter to Redis.
	 * 
	 * @param tc
	 * @return List of return values from Redis.
	 */
	public List<Object> pushTermCounterToRedis(TermCounter tc, String hashname) {
		Transaction t = jedis.multi();
		
		String url = tc.getLabel();
		
		// if this page has already been indexed; delete the old hash
		//since it's a transaction we can't use an if statement - will get an error
		//how do we keep track of the total number of indexed pages

		//check if hashname exists already?
		t.del(hashname);


		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term: tc.keySet()) {
			Integer count = tc.get(term);
			t.hset(hashname, term, count.toString());
			t.sadd(urlSetKey(term), url);
		}
		List<Object> res = t.exec();
		return res;
	}

	/**
	 * Prints the contents of the index.
	 * 
	 * Should be used for development and testing, not production.
	 */
	public void printIndex() {
		// loop through the search terms
		for (String term: termSet()) {
			System.out.println(term);
			
			// for each term, print the pages where it appears
			Set<String> urls = getURLs(term);
			for (String url: urls) {
				Integer count = getCount(url, term);
				System.out.println("    " + url + " " + count);
			}
		}
	}

	/**
	 * Returns the set of terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termSet() {
		Set<String> keys = urlSetKeys();
		Set<String> terms = new HashSet<String>();
		for (String key: keys) {
			String[] array = key.split(":");
			if (array.length < 2) {
				terms.add("");
			} else {
				terms.add(array[1]);
			}
		}
		return terms;
	}

	/**
	 * Returns URLSet keys for the terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> urlSetKeys() {
		return jedis.keys("URLSet:*");
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termCounterKeys() {
		return jedis.keys("TermCounter:*");
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
		pagesIndexed = 0;
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all keys from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys("*");
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();

	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);
		
		//index.deleteTermCounters();
		//index.deleteURLSets();
		//index.deleteAllKeys();
		loadIndex(index);
		
		Map<String, Integer> map = index.getCountsFaster("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}

	/**
	 * Stores two pages in the index for testing purposes.
	 * 
	 * @return
	 * @throws IOException
	 */
	private static void loadIndex(JedisIndex index) throws IOException {
		WikiFetcher wf = new WikiFetcher();

		String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		Elements paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
		
		url = "https://en.wikipedia.org/wiki/Programming_language";
		paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
	}
}
