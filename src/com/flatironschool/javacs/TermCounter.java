package com.flatironschool.javacs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Math;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;


/**
 * Encapsulates a map from search term to frequency (count).
 * total number of times the term appears in the document, total terms in the doc
 * TF(t) = (Number of times term t appears in a document) / (Total number of terms in the document).
 *
 * @author downey
 *
 */
public class TermCounter {
	
	private Map<String, Integer> map;
	private String label;
	
	public TermCounter(String label) {
		this.label = label;
		this.map = new HashMap<String, Integer>();
	}
	
	/*
	*
	*returns URL associated with the TermCounter
	*/
	public String getLabel() {
		return label;
	}

	/**
	*returns the tf value of the specified terms
	*
	*input: term you want to get the tf value for
	*output: tf value of terms
	*/
	public double getTfValue(String term){
		double tf = 0.0;
		int termCount = get(term); //# of times the term appears in the document
		double totalCount = size(); //total # of terms in the document
		tf = (termCount)/(totalCount); //totalCount should not be 0
		return tf;
	}
	
	/**
	 * Returns the total of all counts.
	 * total number of terms in documents
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
	 * Takes a collection of Elements and counts their words.
	 * 
	 * @param paragraphs
	 */
	public void processElements(Elements paragraphs) {
		for (Node node: paragraphs) {
			processTree(node);
		}
	}
	
	/**
	 * Finds TextNodes in a DOM tree and counts their words.
	 * 
	 * @param root
	 */
	public void processTree(Node root) {
		// NOTE: we could use select to find the TextNodes, but since
		// we already have a tree iterator, let's use it.
		for (Node node: new WikiNodeIterable(root)) {
			if (node instanceof TextNode) {
				processText(((TextNode) node).text());
			}
		}
	}

	/**
	 * Splits `text` into words and counts them.
	 * 
	 * @param text  The text to process.
	 */
	public void processText(String text) {
		// replace punctuation with spaces, convert to lower case, and split on whitespace
		String[] array = text.replaceAll("\\pP", " ").toLowerCase().split("\\s+");
		
		for (int i=0; i<array.length; i++) {
			String term = array[i];
			incrementTermCount(term);
		}
		int totalTerms = map.size();
		
		/**
		*may not be necessary to calculate tf for each term...just calculate it on a need basis
		*"when called"
		*calculate tf for each term
		*for(Map.Entry<String, Integer> entry : map.entrySet()){
		*	String term = entry.getKey();
		*	Integer value = entry.getValue();
		*	Float tfValue = value/(float)totalTerms;
		*	tf.put(term, tfValue);
		*}
		**/

	}

	/**
	 * Increments the counter associated with `term`.
	 * 
	 * @param term
	 */
	public void incrementTermCount(String term) {
		// System.out.println(term);
		put(term, get(term) + 1);
	}

	/**
	 * Adds a term to the map with a given count.
	 * 
	 * @param term
	 * @param count
	 */
	public void put(String term, int count) {
		map.put(term, count);
	}

	/**
	 * Returns the count associated with this term, or 0 if it is unseen.
	 * 
	 * @param term
	 * @return
	 */
	public Integer get(String term) {
		Integer count = map.get(term);
		return count == null ? 0 : count;
	}

	/**
	 * Returns the set of terms that have been counted.
	 * 
	 * @return
	 */
	public Set<String> keySet() {
		return map.keySet();
	}
	
	/**
	 * Print the terms and their counts in arbitrary order.
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
		
		TermCounter counter = new TermCounter(url.toString());
		counter.processElements(paragraphs);
		counter.printCounts();
	}
}
