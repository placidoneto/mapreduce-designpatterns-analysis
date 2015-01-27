/*
Register the piggybank jar file to be able to use the UDFs in it
*/
REGISTER 'piggybank.jar';
/*
Assign the aliases ApacheCommonLogLoader and DayExtractor to
piggybank's CommonLogLoader and DateExtractor UDFs
*/
DEFINE ApacheCommonLogLoader org.apache.pig.piggybank.storage.apachelog.CommonLogLoader();
DEFINE DayExtractor org.apache.pig.piggybank.evaluation.util.apachelogparser.DateExtractor('yyyy-MM-dd');
/*
Load the logs dataset using the alias ApacheCommonLogLoader into
the relation logs
*/
logs = LOAD 'datasets/logs/access.log' USING org.apache.pig.piggybank.storage.apachelog.CombinedLogLoader()
		AS (addr: chararray, logname: chararray, user: chararray,
			time: chararray,
			method: chararray, uri: chararray, proto: chararray,
			status: int, bytes: int,
			referer: chararray, useragent: chararray);
/*
* Some processing logic goes here which is deliberately left out
to improve readability
*/
-- Display the contents of the relation logs on the console
DUMP logs;
STORE logs into 'output/logs';