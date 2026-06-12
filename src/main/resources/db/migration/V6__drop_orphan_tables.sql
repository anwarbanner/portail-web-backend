-- Tables 'standards' and 'categories' were created in V1/V2 but never mapped
-- to any Java entity. Dropping them to clean up the schema.
-- 'standards' must be dropped first (FK references 'categories').
DROP TABLE IF EXISTS standards;
DROP TABLE IF EXISTS categories;
