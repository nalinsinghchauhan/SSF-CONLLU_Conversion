#!/usr/bin/perl

use strict;
use utf8;

if(@ARGV != 2)
{
	print "USAGE:";
	print "\tperl remove-english-lines.pl <input file> <output file>";
	exit(1);
}

my $stop = "\x{0964}";

my $infile = $ARGV[0];
my $outfile = $ARGV[1];

open(IF, "<:utf8", "$infile") or die("Couldn't open file: $infile.\n");
open(OF, ">:utf8", "$outfile") or die("Couldn't open file: $outfile.\n");

while(my $line=<IF>)
{
	if($line =~ /^([a-zA-Z0-9<>\~\-\_\+\*\,\.\:\;\`\'\"\(\)\[\]\{\}\&\^\$\%\#\@\!\?\\\=]+\t)/)
	{
		my $eng = $1;
#		print STDERR $eng."\n";
	}
	else
	{
		if($line !~ /^Column Names::/)
		{
			$line =~ s/[a-zA-Z<>\~\_\+\*\:\`\'\"\(\)\[\]\{\}\^\$\%\#\@\\\/]//g;
			$line =~ s/[\&\!\?\,\.\;\|\=]/ /g;

			$line =~ s/$stop//g;
		}
		
		print OF $line;
	}
}
