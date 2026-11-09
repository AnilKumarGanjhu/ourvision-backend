-- Additional organisation/member information. Existing user profile columns are reused.
ALTER TABLE users ADD COLUMN IF NOT EXISTS loan_status VARCHAR(20) DEFAULT 'CLEAR';
ALTER TABLE users ADD COLUMN IF NOT EXISTS loan_duration VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_loans_taken INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS joining_date DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS father_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS mother_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS marital_status VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS nationality VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS languages_known TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact_person VARCHAR(150);

UPDATE users SET loan_status = 'CLEAR' WHERE loan_status IS NULL OR loan_status = '';
UPDATE users SET total_loans_taken = 0 WHERE total_loans_taken IS NULL;
