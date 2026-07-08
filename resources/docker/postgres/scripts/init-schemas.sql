-- Create schemas for both applications
-- Each Service owns its own data. No cross-boundary read/writes
create database orders owner app;
create database notifications owner app;