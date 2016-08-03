package com.flatironschool.javacs;
import java.io.IOException;
import static com.lexicalscope.jewel.cli.CliFactory.parseArguments;
import com.lexicalscope.jewel.cli.ArgumentValidationException;

import redis.clients.jedis.Jedis;

public class Search{
	public static void main(String [] args) throws IOException{
		try {
			//gets the arguments that are passed by the user
			Cli cli = parseArguments(Cli.class, args);
			
			//creates a jedis object
			Jedis jedis = JedisMaker.make();
			JedisIndex index = new JedisIndex(jedis);
	
			//Does the search and prints the results
			String term1 = cli.getTerm();
			System.out.println("Query: " + term1);
			WikiSearch search1 = WikiSearch.search(term1, index);
			search1.print();

		} catch(ArgumentValidationException e){
			System.err.println(e.getMessage());	
		}

	}
}

