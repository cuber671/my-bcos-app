-- Convert invitation_code.enterprise_id from BIGINT to VARCHAR(36) UUID

-- Step 1: Drop the index on enterprise_id (MySQL requires dropping index before modifying column type)
DROP INDEX idx_enterprise_id ON invitation_code;

-- Step 2: Modify enterprise_id column to VARCHAR(36) to match UUID format
ALTER TABLE invitation_code MODIFY COLUMN enterprise_id VARCHAR(36) NOT NULL;

-- Step 3: Re-create the index on enterprise_id
CREATE INDEX idx_enterprise_id ON invitation_code(enterprise_id);

-- Verification query (run this to verify)
SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_NAME = 'invitation_code' AND COLUMN_NAME = 'enterprise_id';
-- Expected result: varchar(36), NO, MUL (or empty)
