X = LOAD '/user/hue/pig/SysMappingExample/Sources/publications.csv' 
	using PigStorage(';') as (
    id: chararray, 
    title:chararray,
    abstract:   chararray,
    status:   chararray,
    year:   chararray,
    authors:   chararray,
    url_doi:   chararray,    
    publisher:   chararray,    
    search_topic:   chararray
); -- papers


Z = FILTER X BY title matches '.* wireless network .*' OR  
                title matches '.* sensor network .*' OR
                title matches '.* sensors network .*' OR
				abstract matches '.* wireless network .*' OR  
                abstract matches '.* sensor network .*';
              
Y = foreach Z generate id, title;             
 
DUMP Y;