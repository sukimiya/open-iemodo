-- V2: Add optimistic lock version column to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
