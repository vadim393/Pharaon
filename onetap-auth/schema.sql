-- Users table with licenses
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    discord_username TEXT NOT NULL UNIQUE,
    discord_id TEXT,
    discord_avatar TEXT,
    uid INTEGER NOT NULL,
    hwid TEXT,
    last_ip TEXT,
    last_login INTEGER,
    created_at INTEGER NOT NULL,
    expires_at INTEGER,
    is_active INTEGER DEFAULT 1,
    max_hwid_changes INTEGER DEFAULT 3,
    hwid_changes_used INTEGER DEFAULT 0
);

-- Session tokens for additional security
CREATE TABLE IF NOT EXISTS sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token TEXT NOT NULL UNIQUE,
    hwid TEXT NOT NULL,
    ip TEXT,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Audit log
CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,
    details TEXT,
    ip TEXT,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Remote commands table for Discord bot control
CREATE TABLE IF NOT EXISTS remote_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    command TEXT NOT NULL,
    params TEXT,
    status TEXT DEFAULT 'PENDING',
    result TEXT,
    created_at INTEGER NOT NULL,
    executed_at INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_discord ON users(discord_username);
CREATE INDEX IF NOT EXISTS idx_users_hwid ON users(hwid);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_commands_user_status ON remote_commands(user_id, status);
CREATE INDEX IF NOT EXISTS idx_commands_created ON remote_commands(created_at);
