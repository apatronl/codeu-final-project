package com.flatironschool.javacs;
import java.io.IOException;
import static com.lexicalscope.jewel.cli.CliFactory.parseArguments;
import com.lexicalscope.jewel.cli.ArgumentValidationException;

import redis.clients.jedis.Jedis;

public class Search {
	public static void main(String [] args) throws IOException{
		try {
			//gets the arguments that are passed by the user
			Cli cli = parseArguments(Cli.class, args);

			//creates a jedis object
			Jedis jedis = JedisMaker.make();
			JedisIndex index = new JedisIndex(jedis);
			JedisIndex.loadIndex(index);
			//Does the search and prints the results
			String terms = cli.getTerms().toLowerCase();
			System.out.println("Searching.....");
			String[] termsArr = Parser.parseTerms(terms);
			
			WikiSearch search1 = WikiSearch.search(termsArr[0], index);
			WikiSearch search2 = null;
			for(int i = 1; i < termsArr.length; i++){
				if(termsArr[i].equals("and")){
					search2 = WikiSearch.search(termsArr[i + 1], index);
					search1 = search1.and(search2);
					i++;
				}
				else if(termsArr[i].equals("-")){
					search2 = WikiSearch.search(termsArr[i + 1], index);
					search1 = search1.minus(search2);
					i++;
				}
				else{
					search2 = WikiSearch.search(termsArr[i], index);
					search1 = search1.or(search2);
				}
			}
			search1.print();
		} catch(ArgumentValidationException e){
			System.err.println(e.getMessage());
		}

	}
}
