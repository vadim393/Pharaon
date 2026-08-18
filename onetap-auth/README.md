# OneTap Authentication Server

Cloudflare Worker с D1 базой данных для защиты OneTap клиента.

## Deployed URL
`https://onetap-auth.wishen92.workers.dev`

## API Endpoints

### 1. Check License (Client Authentication)
**POST** `/api/check`

Request:
```json
{
  "discord_username": "Wishen",
  "hwid": "abc123...",
  "ip": "1.2.3.4"
}
```

Success Response (200):
```json
{
  "authorized": true,
  "uid": 1,
  "discord_username": "Wishen",
  "token": "session_token_here",
  "expires_at": 1234567890,
  "hwid_resets_left": 2
}
```

Failure Response (403):
```json
{
  "authorized": false,
  "reason": "NOT_IN_WHITELIST",
  "message": "User not found in license database"
}
```

### 2. Verify Session
**POST** `/api/verify`

Request:
```json
{
  "token": "session_token",
  "hwid": "abc123..."
}
```

### 3. Admin: Add User
**POST** `/api/admin/add-user`

Request:
```json
{
  "discord_username": "Username",
  "uid": 1,
  "expires_at": 1234567890,
  "max_hwid_changes": 3
}
```

### 4. Admin: List Users
**GET** `/api/admin/users`

### 5. Admin: Audit Log
**GET** `/api/admin/audit?limit=100`

## Database Schema

### users
- `id` - Auto increment primary key
- `discord_username` - Discord username (unique)
- `discord_id` - Discord ID (optional)
- `uid` - User ID for client
- `hwid` - Hardware ID (SHA-256 hash)
- `last_ip` - Last login IP
- `last_login` - Last login timestamp
- `created_at` - Account creation timestamp
- `expires_at` - License expiration (null = lifetime)
- `is_active` - Active status (1/0)
- `max_hwid_changes` - Maximum HWID resets allowed
- `hwid_changes_used` - HWID resets used

### sessions
- Session tokens for additional security
- 24 hour expiration

### audit_log
- All authentication attempts and actions

## Security Features

1. **HWID Binding** - First login binds HWID, mismatches are tracked
2. **HWID Reset Limit** - Configurable number of HWID changes per user
3. **Session Tokens** - 24-hour tokens for ongoing validation
4. **Audit Logging** - All auth attempts logged with IP
5. **License Expiration** - Optional expiration dates
6. **IP Tracking** - Monitor login locations
7. **SHA-256 HWID Hashing** - HWIDs stored as hashes

## Commands

### Deploy
```bash
npm run deploy
```

### Local Development
```bash
npm run dev
```

### Apply Schema Changes
```bash
wrangler d1 execute onetap-licenses --file=schema.sql --remote
```

### Add User via CLI
```bash
curl -X POST https://onetap-auth.wishen92.workers.dev/api/admin/add-user \
  -H "Content-Type: application/json" \
  -d '{"discord_username":"Username","uid":1,"max_hwid_changes":3}'
```

### Query Database
```bash
wrangler d1 execute onetap-licenses --command="SELECT * FROM users" --remote
```

## Advantages over Pastebin

1. **Dynamic** - Server-side logic, not static text
2. **Secure** - HWID binding, session tokens, audit logs
3. **Scalable** - Cloudflare edge network
4. **Protected** - Can't be easily copied/bypassed
5. **Trackable** - Full audit trail of all attempts
6. **Flexible** - Easy to add features (expiration, resets, etc)
7. **Free** - Cloudflare Workers free tier is generous

## Next Steps for Better Protection

1. Add admin authentication (API keys or JWT)
2. Implement rate limiting
3. Add IP-based geo-blocking
4. Implement challenge-response authentication
5. Add encrypted communication
6. Implement anti-debugging checks on client
7. Use code obfuscation (Grunt/Proguard)
