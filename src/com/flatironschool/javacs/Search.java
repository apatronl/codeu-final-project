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

			//Does the search and prints the results
			String terms = cli.getTerm();
			System.out.println("Query: " + terms);

			// String[] termsArr = terms.split(",");
			// System.out.println(termsArr[0] + " " + termsArr[1]);
			String[] termsArr = Parser.parseTerms(terms);
			System.out.println(termsArr[0]);

			//Get filter (and, or, minus)
			String filter = cli.getFilter();

			WikiSearch search1 = WikiSearch.search(termsArr[0], index);
			WikiSearch search2 = WikiSearch.search(termsArr[1], index);
		//	search1.print();

			if (filter.equals("and")) {
				System.out.println("AND");
				WikiSearch and = search1.and(search2);
				and.print();
			}

		} catch(ArgumentValidationException e){
			System.err.println(e.getMessage());
		}

	}
}
