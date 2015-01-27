
Publications = LOAD '../Data/CSV' using PigStorage(',') as (

    document_title: chararray,
    authors:        chararray,
    authors_affiliations:   chararray,
    publication_title:      chararray,
    publication_date:       chararray,
    publication_year:       int,
    volume:     int,
    issue:      int,
    start_page: int,
    end_page:   int,
    abstract:   chararray,
    ISSN:       chararray,
    ISBN:       chararray,
    EISBN:      chararray,
    DOI:        chararray,
    pdf_link:           chararray,
    author_keywords:    chararray,
    ieee_terms:         chararray,
    inspec_controlled_terms:        chararray,
    inspec_non_controlled_terms:    chararray,
    doe_terms:  chararray,
    pacs_terms: chararray,
    mesh_terms: chararray,
    article_citation_count: int,
    patent_citation_count:  int,
    reference_count:        int,
    copyright_year:         int,
    online_date:            chararray,
    date_added_to_xplore:   chararray,
    meeting_date:   chararray,
    publisher:      chararray,
	sponsors:       chararray,
	document_identifier: chararray

); -- Publications

------------------------------------------------------------
-- Number of articles
------------------------------------------------------------
/*
Publications_ALL = GROUP Publications ALL;
Publications_Count = FOREACH @ GENERATE COUNT($1);
*/


------------------------------------------------------------
-- Select publications addressing :
--      QoS &
--      Policy Based Programming &
--      Web Service Programming
------------------------------------------------------------

--
-- Cluster publications by topic
--
SPLIT Publications INTO

    about_QoS IF (
        document_title  matches '.* non-functional .*' OR
        document_title  matches '.* non functional .*' OR
        document_title  matches '.* quality .*' OR
        document_title  matches '.* QoS .*' OR
        document_title  matches '.* NF .*'
    ),

    about_Policy IF (
        document_title  matches '.* property .*'    OR
        document_title  matches '.* requirement .*' OR
        document_title  matches '.* aspect .*'      OR
        document_title  matches '.* attribute .*'   OR
        document_title  matches '.* parameter .*'   OR
        document_title  matches '.* concern .*'     OR
        document_title  matches '.* constraint .*'  OR
        document_title  matches '.* approach .*'    OR
        document_title  matches '.* policy .*'      OR
        document_title  matches '.* contract .*'
    ),

    about_Services IF (
        document_title  matches '.* web service .*'    OR
        document_title  matches '.* service composition .*' OR
        document_title  matches '.* service based application .*' OR
        document_title  matches '.* service-based application .*'
    )
;

--
-- Find publications addressing ALL topics
--

Publications_about_QoS      = FOREACH about_QoS      GENERATE document_title;
Publications_about_Policy   = FOREACH about_Policy   GENERATE document_title;
Publications_about_Services = FOREACH about_Services GENERATE document_title;

Selected_Publications = JOIN
    Publications                by document_title,
    Publications_about_QoS      by document_title,
    Publications_about_Policy   by document_title,
    Publications_about_Services by document_title
;

Selected_Publications = FOREACH @ GENERATE
    Publications::document_title as document_title
;

dump @;
