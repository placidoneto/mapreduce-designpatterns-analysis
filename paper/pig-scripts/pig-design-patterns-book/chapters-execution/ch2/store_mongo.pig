/*
Register the mongo jar files and piggybank jar to be able to use the UDFs
*/
REGISTER '../../lib/mongo.jar';
REGISTER '../../lib/mongo_hadoop_pig.jar';
REGISTER '../../lib/piggybank.jar';

/*
Assign the alias MongoStorage to MongoStorage class
*/
DEFINE MongoStorage com.mongodb.hadoop.pig.MongoStorage(); 

/*
Load the contents of files starting with NASDAQ_daily_prices_ into a Pig relation stock_data
*/
stock_data= LOAD '../../datasets/mongo/NASDAQ_daily_prices_*' USING org.apache.pig.piggybank.storage.CSVLoader() as (exchange:chararray, stock_symbol:chararray, date:chararray, stock_price_open:chararray, stock_price_high:chararray, stock_price_low:chararray, stock_price_close:chararray, stock_volume:chararray, stock_price_adj_close:chararray);

/*
* Some processing logic goes here which is deliberately left out to improve readability
*/

/*
Store data to MongoDB by specifying the MongoStorage serializer.  The MongoDB URI nasdaqDB.store_stock is the document collection created to hold this data.
*/

--dump stock_data;
--STORE stock_data into '../../output/mongo';
STORE stock_data INTO 'mongodb://localhost:27017/data/db/output/mongo/nasdaqDB.store_stock' using MongoStorage(); 
