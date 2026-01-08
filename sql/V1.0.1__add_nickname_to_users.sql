-- Add nickname column to users table
ALTER TABLE users ADD COLUMN nickname VARCHAR(50) COMMENT '昵称' AFTER username;
