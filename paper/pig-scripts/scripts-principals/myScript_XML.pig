
register piggybank.jar;

XML = LOAD '../Data/XML' USING org.apache.pig.piggybank.storage.XMLLoader('record') AS (
    publication: chararray
);

Publications = foreach @ GENERATE
    REGEX_EXTRACT(publication, '<title>(.*)</title>'        , 1) AS title:     chararray,
    REGEX_EXTRACT(publication, '<abstract>(.*)</abstract>'  , 1) AS abstract:  chararray,
    REGEX_EXTRACT(publication, '<year>(.*)</year>'          , 1) AS year:      int,
    REGEX_EXTRACT(publication, '<publisher>(.*)</publisher>', 1) AS publisher: chararray
;


/*
X = GROUP @ ALL;
X = FOREACH @ GENERATE COUNT(Publications);
*/




--describe @;
--dump @;

STORE @ INTO '../csv' USING PigStorage('\t');

