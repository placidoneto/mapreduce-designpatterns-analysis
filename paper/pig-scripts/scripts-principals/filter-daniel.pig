
X = LOAD '../inputs/publications.csv' using PigStorage(';') as (

    id: int,
    title:        chararray,
    abstract:   chararray,
    status:   chararray,
    year:   chararray,
    authors:   chararray,
    url_doi:   chararray,    
    publisher:   chararray,    
    search_topic:   chararray
); -- papers


Z = FILTER X BY title matches '.* resource allocation .*' OR  
                title matches '.* resource provisioning .*' OR
				abstract matches '.* resource allocation .*' OR  
                abstract matches '.* resource provisioning .*' OR
                title matches '.* wireless network .*' OR  
                title matches '.* sensor network .*' OR
                title matches '.* sensors network .*' OR
				abstract matches '.* wireless network .*' OR  
                abstract matches '.* sensor network .*' OR
                abstract matches '.* sensors network .*' OR
                title matches '.* virtual machine .*' OR
                abstract matches '.* virtual machine .*' OR
                title matches '.* resource management .*' OR
                abstract matches '.* resource management .*' OR                				                
                title matches '.* elastic environment .*' OR
                abstract matches '.* elastic environment .*' OR
				title matches '.* service replication .*' OR
                abstract matches '.* service replication .*' OR
				title matches '.* grid computing .*' OR
                abstract matches '.* grid computing .*' OR
                title matches '.* resource scheduling .*' OR
                abstract matches '.* resource scheduling .*';
                
cnt = foreach Z generate id, title;
cnt2 = foreach Z generate id, abstract; 
grp = GROUP X by authors;
grp2 = foreach grp generate group, X.id;               
 
--DESCRIBE X;
store cnt into '../outputs/title';
store cnt2 into '../outputs/abstract';
store grp2 into '../outputs/authors-group';
--DUMP grp2;