package com.flatironschool.javacs;

import com.lexicalscope.jewel.cli.Option;

public interface Cli {

	@Option
	String getTerms();
}
