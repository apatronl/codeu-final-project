#!/bin/bash


echo "Hello Friend! Welcome to a Wikipedia search engine that is almost better than google. What do you want to search for?"
while : ; do	
	echo -n "Enter your terms and press [Enter]: "
	read input
	ant -Darg0=--terms="$input" Search | grep 'http' | grep '=' | tac

done
