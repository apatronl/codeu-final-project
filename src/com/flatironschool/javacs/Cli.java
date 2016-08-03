package com.flatironschool.javacs;

import com.lexicalscope.jewel.cli.Option;

public interface Cli {

	/**
	* Gets a single term to search
	*/
	@Option
	String getTerm();
	String getFilter();
}
