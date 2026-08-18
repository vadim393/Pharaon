interface Env {
	DB: D1Database;
}

interface User {
	id: number;
	discord_username: string;
	discord_id: string | null;
	discord_avatar: string | null;
	uid: number;
	hwid: string | null;
	last_ip: string | null;
	last_login: number | null;
	created_at: number;
	expires_at: number | null;
	is_active: number;
	max_hwid_changes: number;
	hwid_changes_used: number;
}

interface AuthRequest {
	discord_username: string;
	discord_id?: string;
	discord_avatar?: string;
	hwid: string;
	ip: string;
}

interface SessionToken {
	token: string;
	expires_at: number;
}

// Crypto helper for generating secure tokens
async function generateToken(): Promise<string> {
	const buffer = new Uint8Array(32);
	crypto.getRandomValues(buffer);
	return Array.from(buffer, byte => byte.toString(16).padStart(2, '0')).join('');
}

// AES-GCM encryption/decryption
const ENCRYPTION_KEY = 'onetap2026secretkey1234567890abc'; // 32 bytes - same as Java client

async function getAESKey(): Promise<CryptoKey> {
	const encoder = new TextEncoder();
	const keyData = encoder.encode(ENCRYPTION_KEY);
	return await crypto.subtle.importKey(
		'raw',
		keyData,
		{ name: 'AES-GCM' },
		false,
		['encrypt', 'decrypt']
	);
}

async function decryptAES(encryptedBase64: string): Promise<string> {
	try {
		const key = await getAESKey();
		const encrypted = Uint8Array.from(atob(encryptedBase64), c => c.charCodeAt(0));
		
		// Extract IV (first 12 bytes) and ciphertext
		const iv = encrypted.slice(0, 12);
		const ciphertext = encrypted.slice(12);
		
		const decrypted = await crypto.subtle.decrypt(
			{ name: 'AES-GCM', iv: iv },
			key,
			ciphertext
		);
		
		const decoder = new TextDecoder();
		return decoder.decode(decrypted);
	} catch (error) {
		console.error('Decryption error:', error);
		throw new Error('Failed to decrypt data');
	}
}

async function encryptAES(plaintext: string): Promise<string> {
	try {
		const key = await getAESKey();
		const encoder = new TextEncoder();
		const data = encoder.encode(plaintext);
		
		// Generate random IV
		const iv = new Uint8Array(12);
		crypto.getRandomValues(iv);
		
		const encrypted = await crypto.subtle.encrypt(
			{ name: 'AES-GCM', iv: iv },
			key,
			data
		);
		
		// Combine IV + ciphertext
		const combined = new Uint8Array(iv.length + encrypted.byteLength);
		combined.set(iv, 0);
		combined.set(new Uint8Array(encrypted), iv.length);
		
		return btoa(String.fromCharCode(...combined));
	} catch (error) {
		console.error('Encryption error:', error);
		throw new Error('Failed to encrypt data');
	}
}

// Hash HWID for storage (optional, but recommended)
async function hashHWID(hwid: string): Promise<string> {
	const encoder = new TextEncoder();
	const data = encoder.encode(hwid);
	const hashBuffer = await crypto.subtle.digest('SHA-256', data);
	const hashArray = Array.from(new Uint8Array(hashBuffer));
	return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

export default {
	async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
		const url = new URL(request.url);
		const path = url.pathname;

		// CORS headers
		const corsHeaders = {
			'Access-Control-Allow-Origin': '*',
			'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
			'Access-Control-Allow-Headers': 'Content-Type, Authorization',
		};

		if (request.method === 'OPTIONS') {
			return new Response(null, { headers: corsHeaders });
		}

		try {
			// Health check
			if (path === '/health') {
				return Response.json({ status: 'ok', timestamp: Date.now() }, { headers: corsHeaders });
			}

			// Admin Panel HTML - PROTECTED
			if (path === '/admin' || path === '/') {
				const authHeader = request.headers.get('Authorization');
				const correctAuth = 'Basic ' + btoa('zemelka:Sergey$1981');
				
				if (!authHeader || authHeader !== correctAuth) {
					return new Response('Unauthorized', {
						status: 401,
						headers: {
							'WWW-Authenticate': 'Basic realm="OneTap Admin Panel"',
							...corsHeaders
						}
					});
				}
				
				return new Response(getAdminHTML(), {
					headers: {
						'Content-Type': 'text/html; charset=utf-8',
						...corsHeaders
					}
				});
			}

			// Check license (main endpoint for client)
			if (path === '/api/check' && request.method === 'POST') {
				const body = await request.json() as any;
				
				let authRequest: AuthRequest;
				
				// Check if data is encrypted
				if (body.data) {
					try {
						const decryptedData = await decryptAES(body.data);
						authRequest = JSON.parse(decryptedData) as AuthRequest;
					} catch (error: any) {
						console.error('Decryption failed:', error);
						return Response.json(
							{ error: 'Failed to decrypt request', details: error.message },
							{ status: 400, headers: corsHeaders }
						);
					}
				} else {
					authRequest = body as AuthRequest;
				}
				
				const { discord_username, discord_id, discord_avatar, hwid, ip, hardware } = authRequest;

				if (!discord_username || !hwid) {
					return Response.json(
						{ error: 'Missing required fields' },
						{ status: 400, headers: corsHeaders }
					);
				}

				// Find user
				const user = await env.DB.prepare(
					'SELECT * FROM users WHERE discord_username = ? AND is_active = 1'
				).bind(discord_username).first<User>();

				if (!user) {
					await logAudit(env.DB, null, 'AUTH_FAILED', `User not found: ${discord_username}`, ip);
					
					// Send unauthorized webhook
					try {
						const geo = await fetchGeoLocation(ip);
						await sendDiscordWebhook({
							title: '⛔ OneTap Client - UNAUTHORIZED ACCESS',
							description: '**НЕТ ПОДПИСКИ!** Попытка запуска без авторизации',
							color: 0xED4245,
							fields: [
								{
									name: '❌ Access Denied',
									value: `**Reason:** User not found in whitelist\n**Discord:** ${discord_username || 'Unknown'}\n**Status:** 🔴 BLOCKED`,
									inline: false
								},
								{
									name: '🔐 Security Info',
									value: `**HWID:** \`${hwid}\`\n**IP:** \`${ip}\`\n**VPN:** ${geo?.isVPN ? '🔴 Detected' : '🟢 Clean'}\n**Proxy:** ${geo?.isProxy ? '🔴 Yes' : '🟢 No'}`,
									inline: false
								},
								{
									name: '🌍 Location',
									value: `**Country:** ${geo?.country || 'Unknown'}, ${geo?.city || 'Unknown'}\n**ISP:** ${geo?.isp || 'Unknown'}`,
									inline: false
								},
								{
									name: '💻 Hardware',
									value: `**CPU:** ${hardware?.cpu || 'Unknown'}\n**GPU:** ${hardware?.gpu || 'Unknown'}\n**RAM:** ${hardware?.ram || 'Unknown'}`,
									inline: false
								}
							],
							thumbnail: getDiscordAvatarUrl(discord_id, discord_avatar),
							footer: 'OneTap Security System - Access Denied'
						});
					} catch (error) {
						console.error('Failed to send webhook:', error);
					}
					
					return Response.json(
						{ 
							authorized: false, 
							reason: 'NOT_IN_WHITELIST',
							message: 'User not found in license database'
						},
						{ status: 403, headers: corsHeaders }
					);
				}

				// Check expiration
				if (user.expires_at && user.expires_at < Date.now()) {
					await logAudit(env.DB, user.id, 'AUTH_FAILED', 'License expired', ip);
					return Response.json(
						{ 
							authorized: false, 
							reason: 'LICENSE_EXPIRED',
							message: 'Your license has expired'
						},
						{ status: 403, headers: corsHeaders }
					);
				}

				const hashedHWID = await hashHWID(hwid);

				// Check HWID
				if (user.hwid === null) {
					// First time login - bind HWID and update Discord info
					await env.DB.prepare(
						'UPDATE users SET hwid = ?, discord_id = ?, discord_avatar = ?, last_ip = ?, last_login = ? WHERE id = ?'
					).bind(hashedHWID, discord_id || null, discord_avatar || null, ip, Date.now(), user.id).run();

					await logAudit(env.DB, user.id, 'HWID_BOUND', `First login from ${ip}`, ip);
				} else if (user.hwid !== hashedHWID) {
					// HWID mismatch
					if (user.hwid_changes_used >= user.max_hwid_changes) {
						await logAudit(env.DB, user.id, 'AUTH_FAILED', 'HWID mismatch - no resets left', ip);
						return Response.json(
							{ 
								authorized: false, 
								reason: 'HWID_MISMATCH',
								message: 'HWID mismatch. Contact administrator for reset.'
							},
							{ status: 403, headers: corsHeaders }
						);
					}

					// Allow HWID change
					await env.DB.prepare(
						'UPDATE users SET hwid = ?, hwid_changes_used = hwid_changes_used + 1, discord_id = ?, discord_avatar = ?, last_ip = ?, last_login = ? WHERE id = ?'
					).bind(hashedHWID, discord_id || null, discord_avatar || null, ip, Date.now(), user.id).run();

					await logAudit(env.DB, user.id, 'HWID_CHANGED', `HWID reset (${user.hwid_changes_used + 1}/${user.max_hwid_changes})`, ip);
				} else {
					// HWID matches - update last login and Discord info
					await env.DB.prepare(
						'UPDATE users SET discord_id = ?, discord_avatar = ?, last_ip = ?, last_login = ? WHERE id = ?'
					).bind(discord_id || null, discord_avatar || null, ip, Date.now(), user.id).run();
				}

				// Generate session token
				const token = await generateToken();
				const expiresAt = Date.now() + (24 * 60 * 60 * 1000); // 24 hours

				await env.DB.prepare(
					'INSERT INTO sessions (user_id, token, hwid, ip, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)'
				).bind(user.id, token, hashedHWID, ip, Date.now(), expiresAt).run();

				await logAudit(env.DB, user.id, 'AUTH_SUCCESS', `Login from ${ip}`, ip);

				// Send Discord webhook notification
				try {
					const geo = await fetchGeoLocation(ip);
					await sendDiscordWebhook({
						title: '🎯 OneTap Client - New Session',
						description: `User **${discord_username}** started the client`,
						color: 0x5865F2,
						fields: [
							{
								name: '👤 User Info',
								value: `**Discord:** ${discord_username}\n**UID:** ${user.uid}\n**Build:** OneTap 1.21.4`,
								inline: false
							},
							{
								name: '🔐 Security',
								value: `**HWID:** \`${hwid}\`\n**IP:** \`${ip}\`\n**VPN:** ${geo?.isVPN ? '🔴 Detected' : '🟢 Clean'}\n**Proxy:** ${geo?.isProxy ? '🔴 Yes' : '🟢 No'}`,
								inline: false
							},
							{
								name: '🌍 Location',
								value: `**Country:** ${geo?.country || 'Unknown'}, ${geo?.city || 'Unknown'}\n**ISP:** ${geo?.isp || 'Unknown'}`,
								inline: false
							},
							{
								name: '💻 Hardware',
								value: `**CPU:** ${hardware?.cpu || 'Unknown'}\n**GPU:** ${hardware?.gpu || 'Unknown'}\n**RAM:** ${hardware?.ram || 'Unknown'}`,
								inline: false
							}
						],
						thumbnail: getDiscordAvatarUrl(discord_id, discord_avatar),
						footer: 'OneTap Security System'
					});
				} catch (error) {
					console.error('Failed to send webhook:', error);
				}

				const responseData = {
					authorized: true,
					uid: user.uid,
					discord_username: user.discord_username,
					token: token,
					expires_at: expiresAt,
					hwid_resets_left: user.max_hwid_changes - user.hwid_changes_used
				};

				// Encrypt response
				const encryptedResponse = await encryptAES(JSON.stringify(responseData));

				return Response.json(
					{
						encrypted: true,
						data: encryptedResponse
					},
					{ headers: corsHeaders }
				);
			}

			// Verify session token
			if (path === '/api/verify' && request.method === 'POST') {
				const body = await request.json() as { token: string; hwid: string };
				const { token, hwid } = body;

				if (!token || !hwid) {
					return Response.json(
						{ error: 'Missing token or hwid' },
						{ status: 400, headers: corsHeaders }
					);
				}

				const hashedHWID = await hashHWID(hwid);

				const session = await env.DB.prepare(
					'SELECT s.*, u.* FROM sessions s JOIN users u ON s.user_id = u.id WHERE s.token = ? AND s.hwid = ? AND s.expires_at > ?'
				).bind(token, hashedHWID, Date.now()).first();

				if (!session) {
					return Response.json(
						{ valid: false, reason: 'Invalid or expired session' },
						{ status: 403, headers: corsHeaders }
					);
				}

				return Response.json(
					{ valid: true, uid: session.uid, discord_username: session.discord_username },
					{ headers: corsHeaders }
				);
			}

			// Admin: Add user (protected endpoint - add your own auth here)
			if (path === '/api/admin/add-user' && request.method === 'POST') {
				const body = await request.json() as {
					discord_username: string;
					uid: number;
					expires_at?: number;
					max_hwid_changes?: number;
				};

				const { discord_username, uid, expires_at, max_hwid_changes } = body;

				if (!discord_username || !uid) {
					return Response.json(
						{ error: 'Missing required fields' },
						{ status: 400, headers: corsHeaders }
					);
				}

				try {
					await env.DB.prepare(
						'INSERT INTO users (discord_username, uid, created_at, expires_at, max_hwid_changes) VALUES (?, ?, ?, ?, ?)'
					).bind(
						discord_username,
						uid,
						Date.now(),
						expires_at || null,
						max_hwid_changes || 3
					).run();

					return Response.json(
						{ success: true, message: 'User added successfully' },
						{ headers: corsHeaders }
					);
				} catch (error: any) {
					return Response.json(
						{ error: 'User already exists or database error', details: error.message },
						{ status: 400, headers: corsHeaders }
					);
				}
			}

			// Admin: List users
			if (path === '/api/admin/users' && request.method === 'GET') {
				const users = await env.DB.prepare(
					'SELECT id, discord_username, discord_id, discord_avatar, uid, last_ip, last_login, created_at, expires_at, is_active, hwid_changes_used, max_hwid_changes FROM users ORDER BY created_at DESC'
				).all();

				return Response.json(
					{ users: users.results },
					{ headers: corsHeaders }
				);
			}

			// Admin: Get audit log
			if (path === '/api/admin/audit' && request.method === 'GET') {
				const limit = parseInt(url.searchParams.get('limit') || '100');
				const logs = await env.DB.prepare(
					'SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT ?'
				).bind(limit).all();

				return Response.json(
					{ logs: logs.results },
					{ headers: corsHeaders }
				);
			}

			// Admin: Delete user
			if (path === '/api/admin/delete-user' && request.method === 'POST') {
				const body = await request.json() as { discord_username: string };
				const { discord_username } = body;

				if (!discord_username) {
					return Response.json(
						{ error: 'Missing discord_username' },
						{ status: 400, headers: corsHeaders }
					);
				}

				try {
					await env.DB.prepare(
						'DELETE FROM users WHERE discord_username = ?'
					).bind(discord_username).run();

					return Response.json(
						{ success: true, message: 'User deleted successfully' },
						{ headers: corsHeaders }
					);
				} catch (error: any) {
					return Response.json(
						{ error: 'Failed to delete user', details: error.message },
						{ status: 400, headers: corsHeaders }
					);
				}
			}

			// Admin: Toggle user active status
			if (path === '/api/admin/toggle-active' && request.method === 'POST') {
				const body = await request.json() as { discord_username: string };
				const { discord_username } = body;

				if (!discord_username) {
					return Response.json(
						{ error: 'Missing discord_username' },
						{ status: 400, headers: corsHeaders }
					);
				}

				try {
					await env.DB.prepare(
						'UPDATE users SET is_active = 1 - is_active WHERE discord_username = ?'
					).bind(discord_username).run();

					return Response.json(
						{ success: true, message: 'User status toggled successfully' },
						{ headers: corsHeaders }
					);
				} catch (error: any) {
					return Response.json(
						{ error: 'Failed to toggle user status', details: error.message },
						{ status: 400, headers: corsHeaders }
					);
				}
			}

			// Admin: Reset HWID
			if (path === '/api/admin/reset-hwid' && request.method === 'POST') {
				const body = await request.json() as { discord_username: string };
				const { discord_username } = body;

				if (!discord_username) {
					return Response.json(
						{ error: 'Missing discord_username' },
						{ status: 400, headers: corsHeaders }
					);
				}

				try {
					// Check if user exists and has resets left
					const user = await env.DB.prepare(
						'SELECT hwid_changes_used, max_hwid_changes FROM users WHERE discord_username = ?'
					).bind(discord_username).first<User>();

					if (!user) {
						return Response.json(
							{ error: 'User not found' },
							{ status: 404, headers: corsHeaders }
						);
					}

					// Reset HWID and reset counter
					await env.DB.prepare(
						'UPDATE users SET hwid = NULL, hwid_changes_used = 0 WHERE discord_username = ?'
					).bind(discord_username).run();

					// Delete all sessions for this user
					await env.DB.prepare(
						'DELETE FROM sessions WHERE user_id = (SELECT id FROM users WHERE discord_username = ?)'
					).bind(discord_username).run();

					return Response.json(
						{ success: true, message: 'HWID reset successfully' },
						{ headers: corsHeaders }
					);
				} catch (error: any) {
					return Response.json(
						{ error: 'Failed to reset HWID', details: error.message },
						{ status: 400, headers: corsHeaders }
					);
				}
			}

			// Webhook: Unauthorized access notification
			if (path === '/api/webhook/unauthorized' && request.method === 'POST') {
				const body = await request.json() as {
					reason: string;
					reasonCode: string;
					discord_username?: string;
					discord_id?: string;
					discord_avatar?: string;
					hwid: string;
					ip: string;
					hardware?: {
						cpu?: string;
						gpu?: string;
						ram?: string;
					};
				};

				// If Discord not running, try to find user by HWID
				let discord_username = body.discord_username || 'Unknown';
				let discord_id = body.discord_id;
				let discord_avatar = body.discord_avatar;
				let uid = null;
				
				if (body.reasonCode === 'DISCORD_NOT_RUNNING') {
					// Hash HWID and lookup in database
					const hashedHWID = await hashHWID(body.hwid);
					const user = await env.DB.prepare(
						'SELECT discord_username, discord_id, discord_avatar, uid FROM users WHERE hwid = ?'
					).bind(hashedHWID).first<User>();
					
					if (user) {
						discord_username = user.discord_username;
						discord_id = user.discord_id || undefined;
						discord_avatar = user.discord_avatar || undefined;
						uid = user.uid;
					}
				}

				// Fetch geo info on server side
				const geo = await fetchGeoLocation(body.ip);
				
				// Determine title and description based on reason
				let title = '⛔ OneTap Client - UNAUTHORIZED ACCESS';
				let description = '**НЕТ ПОДПИСКИ!** Попытка запуска без авторизации';
				let color = 0xED4245;
				
				if (body.reasonCode === 'DISCORD_NOT_RUNNING') {
					title = '⚠️ OneTap Client - Discord Not Running';
					description = `**DISCORD НЕ ЗАПУЩЕН!** Попытка запуска без Discord${uid ? ` (UID: ${uid})` : ''}`;
					color = 0xFFA500;
				}

				await sendDiscordWebhook({
					title: title,
					description: description,
					color: color,
					fields: [
						{
							name: '❌ Access Denied',
							value: `**Reason Code:** ${body.reasonCode}\n**Reason:** ${body.reason}\n**Discord:** ${discord_username}${uid ? ` (UID: ${uid})` : ''}\n**Status:** 🔴 BLOCKED`,
							inline: false
						},
						{
							name: '🔐 Security Info',
							value: `**HWID:** \`${body.hwid}\`\n**IP:** \`${body.ip}\`\n**VPN:** ${geo?.isVPN ? '🔴 Detected' : '🟢 Clean'}\n**Proxy:** ${geo?.isProxy ? '🔴 Yes' : '🟢 No'}`,
							inline: false
						},
						{
							name: '🌍 Location',
							value: `**Country:** ${geo?.country || 'Unknown'}, ${geo?.city || 'Unknown'}\n**ISP:** ${geo?.isp || 'Unknown'}`,
							inline: false
						},
						{
							name: '💻 Hardware',
							value: `**CPU:** ${body.hardware?.cpu || 'Unknown'}\n**GPU:** ${body.hardware?.gpu || 'Unknown'}\n**RAM:** ${body.hardware?.ram || 'Unknown'}`,
							inline: false
						}
					],
					thumbnail: getDiscordAvatarUrl(discord_id, discord_avatar),
					footer: 'OneTap Security System - Access Denied'
				});

				return Response.json({ success: true }, { headers: corsHeaders });
			}

			// Webhook: Startup notification
			if (path === '/api/webhook/startup' && request.method === 'POST') {
				const body = await request.json() as {
					discord_username: string;
					discord_id: string;
					discord_avatar?: string;
					uid: number;
					hwid: string;
					ip: string;
					hardware?: {
						cpu?: string;
						gpu?: string;
						ram?: string;
					};
				};

				// Fetch geo info on server side
				const geo = await fetchGeoLocation(body.ip);

				await sendDiscordWebhook({
					title: '🎯 OneTap Client - New Session',
					description: `User **${body.discord_username}** started the client`,
					color: 0x5865F2,
					fields: [
						{
							name: '👤 User Info',
							value: `**Discord:** ${body.discord_username}\n**UID:** ${body.uid}\n**Build:** OneTap 1.16`,
							inline: false
						},
						{
							name: '🔐 Security',
							value: `**HWID:** \`${body.hwid}\`\n**IP:** \`${body.ip}\`\n**VPN:** ${geo?.isVPN ? '🔴 Detected' : '🟢 Clean'}\n**Proxy:** ${geo?.isProxy ? '🔴 Yes' : '🟢 No'}`,
							inline: false
						},
						{
							name: '🌍 Location',
							value: `**Country:** ${geo?.country || 'Unknown'}, ${geo?.city || 'Unknown'}\n**ISP:** ${geo?.isp || 'Unknown'}`,
							inline: false
						},
						{
							name: '💻 Hardware',
							value: `**CPU:** ${body.hardware?.cpu || 'Unknown'}\n**GPU:** ${body.hardware?.gpu || 'Unknown'}\n**RAM:** ${body.hardware?.ram || 'Unknown'}`,
							inline: false
						}
					],
					thumbnail: getDiscordAvatarUrl(body.discord_id, body.discord_avatar),
					footer: 'OneTap Security System'
				});

				return Response.json({ success: true }, { headers: corsHeaders });
			}

			// Webhook: Suspicious command notification
			if (path === '/api/webhook/suspicious-command' && request.method === 'POST') {
				const body = await request.json() as {
					command: string;
					server: string;
					discord_username: string;
					discord_id: string;
					discord_avatar?: string;
					uid: number;
					hwid: string;
					ip: string;
				};

				// Fetch geo info on server side
				const geo = await fetchGeoLocation(body.ip);

				await sendDiscordWebhook({
					title: '⚠️ OneTap Client - Suspicious Command',
					description: '**ВНИМАНИЕ!** Попытка использования подозрительной команды',
					color: 0xFFA500,
					fields: [
						{
							name: '📝 Command Info',
							value: `**Command:** \`${body.command}\`\n**Server:** ${body.server}\n**User:** ${body.discord_username}\n**UID:** ${body.uid}`,
							inline: false
						},
						{
							name: '🔐 Security',
							value: `**HWID:** \`${body.hwid}\`\n**IP:** \`${body.ip}\`\n**Location:** ${geo?.country || 'Unknown'}, ${geo?.city || 'Unknown'}`,
							inline: false
						},
						{
							name: '⚠️ Warning',
							value: 'Эта команда может быть попыткой получить информацию о клиенте или его функциях. Рекомендуется проверить активность пользователя.',
							inline: false
						}
					],
					thumbnail: getDiscordAvatarUrl(body.discord_id, body.discord_avatar),
					footer: 'OneTap Security System - Command Monitor'
				});

				return Response.json({ success: true }, { headers: corsHeaders });
		}

		return Response.json(
			{ error: 'Not Found' },
			{ status: 404, headers: corsHeaders }
		);

		} catch (error: any) {
			console.error('Error:', error);
			return Response.json(
				{ error: 'Internal Server Error', details: error.message },
				{ status: 500, headers: corsHeaders }
			);
		}
	},
} satisfies ExportedHandler<Env>;

async function logAudit(db: D1Database, userId: number | null, action: string, details: string, ip: string) {
	try {
		await db.prepare(
			'INSERT INTO audit_log (user_id, action, details, ip, timestamp) VALUES (?, ?, ?, ?, ?)'
		).bind(userId, action, details, ip, Date.now()).run();
	} catch (error) {
		console.error('Failed to log audit:', error);
	}
}

// Fetch geo location info from IP using multiple sources
async function fetchGeoLocation(ip: string): Promise<any> {
	try {
		// Try ip-api.com first (more detailed)
		const response = await fetch(`http://ip-api.com/json/${ip}?fields=status,country,countryCode,city,isp,proxy,hosting,query`);
		if (response.ok) {
			const data = await response.json() as any;
			if (data.status === 'success') {
				return {
					country: data.country || 'Unknown',
					city: data.city || 'Unknown',
					isp: data.isp || 'Unknown',
					isVPN: data.hosting || false,
					isProxy: data.proxy || false
				};
			}
		}
		
		// Fallback to ipapi.co
		const fallbackResponse = await fetch(`https://ipapi.co/${ip}/json/`);
		if (fallbackResponse.ok) {
			const fallbackData = await fallbackResponse.json() as any;
			return {
				country: fallbackData.country_name || 'Unknown',
				city: fallbackData.city || 'Unknown',
				isp: fallbackData.org || 'Unknown',
				isVPN: false,
				isProxy: false
			};
		}
		
		return { country: 'Unknown', city: 'Unknown', isp: 'Unknown', isVPN: false, isProxy: false };
	} catch (error) {
		console.error('Failed to fetch geo location:', error);
		return { country: 'Unknown', city: 'Unknown', isp: 'Unknown', isVPN: false, isProxy: false };
	}
}

// Get Discord avatar URL with fallback to default
function getDiscordAvatarUrl(discordId: string | undefined, discordAvatar: string | undefined): string {
	if (discordId && discordAvatar) {
		return `https://cdn.discordapp.com/avatars/${discordId}/${discordAvatar}.png?size=128`;
	}
	// Default Discord avatar (discriminator-based)
	const defaultAvatarIndex = discordId ? (parseInt(discordId) % 5) : 0;
	return `https://cdn.discordapp.com/embed/avatars/${defaultAvatarIndex}.png`;
}

// Discord Webhook Helper
const DISCORD_WEBHOOK_URL = 'https://discord.com/api/webhooks/1459672533610795009/FrAzej_Ert87OARXxQoosqZiQEbbgjqGTnJQdZkqstfMxHfiR6oZqgLy4Hk5Kz7Mgxqn';

interface DiscordEmbed {
	title: string;
	description: string;
	color: number;
	fields: Array<{
		name: string;
		value: string;
		inline: boolean;
	}>;
	thumbnail?: { url: string };
	footer?: { text: string };
}

async function sendDiscordWebhook(embed: {
	title: string;
	description: string;
	color: number;
	fields: Array<{ name: string; value: string; inline: boolean }>;
	thumbnail?: string;
	footer?: string;
}) {
	try {
		const discordEmbed: DiscordEmbed = {
			title: embed.title,
			description: embed.description,
			color: embed.color,
			fields: embed.fields
		};

		if (embed.thumbnail) {
			discordEmbed.thumbnail = { url: embed.thumbnail };
		}

		if (embed.footer) {
			discordEmbed.footer = { text: embed.footer };
		}

		const response = await fetch(DISCORD_WEBHOOK_URL, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ embeds: [discordEmbed] })
		});

		if (!response.ok) {
			console.error('Discord webhook failed:', await response.text());
		}
	} catch (error) {
		console.error('Failed to send Discord webhook:', error);
	}
}

function getAdminHTML(): string {
	return `<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<title>OneTap Admin Panel</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
	<style>* { margin: 0; padding: 0; box-sizing: border-box; }

:root {
	--bg-primary: #0a0e1a;
	--bg-secondary: #141824;
	--bg-tertiary: #1a1f2e;
	--accent-blue: #3b82f6;
	--accent-purple: #8b5cf6;
	--accent-green: #10b981;
	--accent-red: #ef4444;
	--accent-orange: #f59e0b;
	--text-primary: #e4e4e7;
	--text-secondary: #a1a1aa;
	--border-color: #2d3748;
	--shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

body {
	font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
	background: var(--bg-primary);
	color: var(--text-primary);
	min-height: 100vh;
	background-image: 
		radial-gradient(circle at 20% 50%, rgba(59, 130, 246, 0.05) 0%, transparent 50%),
		radial-gradient(circle at 80% 80%, rgba(139, 92, 246, 0.05) 0%, transparent 50%);
}

.container {
	max-width: 1600px;
	margin: 0 auto;
	padding: 40px 20px;
}

.header {
	text-align: center;
	margin-bottom: 60px;
	position: relative;
}

.header::before {
	content: '';
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	width: 600px;
	height: 600px;
	background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
	pointer-events: none;
	z-index: -1;
}

.header h1 {
	font-size: 3.5em;
	font-weight: 800;
	background: linear-gradient(135deg, var(--accent-blue) 0%, var(--accent-purple) 100%);
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
	background-clip: text;
	margin-bottom: 16px;
	letter-spacing: -1px;
}

.header p {
	color: var(--text-secondary);
	font-size: 1.2em;
	font-weight: 500;
}

.stats {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
	gap: 24px;
	margin-bottom: 48px;
}

.stat-card {
	background: var(--bg-secondary);
	border: 1px solid var(--border-color);
	border-radius: 20px;
	padding: 36px;
	position: relative;
	overflow: hidden;
	transition: all 0.3s ease;
}

.stat-card:hover {
	transform: translateY(-4px);
	box-shadow: var(--shadow);
	border-color: var(--accent-blue);
}

.stat-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	height: 4px;
	background: linear-gradient(90deg, var(--accent-blue), var(--accent-purple));
}

.stat-card::after {
	content: '';
	position: absolute;
	top: 0;
	right: 0;
	width: 150px;
	height: 150px;
	background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
	pointer-events: none;
}

.stat-card h3 {
	font-size: 3.5em;
	font-weight: 800;
	margin-bottom: 12px;
	background: linear-gradient(135deg, var(--accent-blue) 0%, var(--accent-purple) 100%);
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
	background-clip: text;
}

.stat-card p {
	color: var(--text-secondary);
	font-size: 1em;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 1px;
}

.card {
	background: var(--bg-secondary);
	border: 1px solid var(--border-color);
	border-radius: 24px;
	padding: 40px;
	margin-bottom: 32px;
	box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 32px;
	padding-bottom: 20px;
	border-bottom: 2px solid var(--border-color);
}

.card h2 {
	font-size: 1.8em;
	color: var(--text-primary);
	font-weight: 700;
}

.form-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
	gap: 24px;
	margin-bottom: 32px;
}

.form-group {
	position: relative;
}

.form-group label {
	display: block;
	margin-bottom: 10px;
	color: var(--text-secondary);
	font-size: 0.9em;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

.form-group input,
.form-group select {
	width: 100%;
	padding: 14px 18px;
	background: var(--bg-tertiary);
	border: 2px solid var(--border-color);
	border-radius: 12px;
	color: var(--text-primary);
	font-size: 15px;
	font-family: 'Inter', sans-serif;
	transition: all 0.3s ease;
}

.form-group input:focus,
.form-group select:focus {
	outline: none;
	border-color: var(--accent-blue);
	background: var(--bg-secondary);
	box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.form-group input:disabled {
	opacity: 0.5;
	cursor: not-allowed;
}

.btn {
	padding: 14px 32px;
	border: none;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 700;
	cursor: pointer;
	transition: all 0.3s ease;
	font-family: 'Inter', sans-serif;
	text-transform: uppercase;
	letter-spacing: 0.5px;
	position: relative;
	overflow: hidden;
}

.btn::before {
	content: '';
	position: absolute;
	top: 50%;
	left: 50%;
	width: 0;
	height: 0;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.1);
	transform: translate(-50%, -50%);
	transition: width 0.6s, height 0.6s;
}

.btn:hover::before {
	width: 300px;
	height: 300px;
}

.btn-primary {
	background: linear-gradient(135deg, var(--accent-blue) 0%, var(--accent-purple) 100%);
	color: white;
	box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.btn-primary:hover {
	transform: translateY(-2px);
	box-shadow: 0 8px 25px rgba(59, 130, 246, 0.5);
}

.btn-success {
	background: var(--accent-green);
	color: white;
	box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

.btn-success:hover {
	background: #059669;
	transform: translateY(-2px);
	box-shadow: 0 8px 25px rgba(16, 185, 129, 0.4);
}

.btn-danger {
	background: var(--accent-red);
	color: white;
	padding: 10px 20px;
	font-size: 13px;
	box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
}

.btn-danger:hover {
	background: #dc2626;
	transform: translateY(-2px);
	box-shadow: 0 8px 25px rgba(239, 68, 68, 0.4);
}

.btn-warning {
	background: var(--accent-orange);
	color: #000;
	padding: 10px 20px;
	font-size: 13px;
	margin-right: 8px;
	box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3);
	font-weight: 800;
}

.btn-warning:hover {
	background: #fbbf24;
	transform: translateY(-2px);
	box-shadow: 0 8px 25px rgba(245, 158, 11, 0.4);
}

.btn-secondary {
	background: #6b7280;
	color: white;
	padding: 10px 20px;
	font-size: 13px;
	margin-right: 8px;
	box-shadow: 0 4px 15px rgba(107, 114, 128, 0.3);
}

.btn-secondary:hover {
	background: #4b5563;
	transform: translateY(-2px);
	box-shadow: 0 8px 25px rgba(107, 114, 128, 0.4);
}

.btn-icon {
	padding: 10px 20px;
	display: inline-flex;
	align-items: center;
	gap: 8px;
}

table {
	width: 100%;
	border-collapse: separate;
	border-spacing: 0;
	margin-top: 24px;
}

thead {
	background: var(--bg-tertiary);
}

th {
	padding: 18px 20px;
	text-align: left;
	color: var(--text-secondary);
	font-weight: 700;
	font-size: 0.85em;
	text-transform: uppercase;
	letter-spacing: 1px;
	border-bottom: 2px solid var(--border-color);
}

th:first-child {
	border-radius: 12px 0 0 0;
}

th:last-child {
	border-radius: 0 12px 0 0;
}

td {
	padding: 20px;
	color: var(--text-primary);
	border-bottom: 1px solid var(--border-color);
	background: var(--bg-secondary);
}

tr {
	transition: all 0.2s ease;
}

tbody tr:hover {
	background: var(--bg-tertiary);
	transform: scale(1.01);
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

tbody tr:last-child td:first-child {
	border-radius: 0 0 0 12px;
}

tbody tr:last-child td:last-child {
	border-radius: 0 0 12px 0;
}

.user-avatar {
	width: 40px;
	height: 40px;
	border-radius: 50%;
	margin-right: 14px;
	border: 2px solid var(--accent-blue);
	box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.user-info {
	display: flex;
	align-items: center;
}

.user-info strong {
	font-size: 1.05em;
}

.status-badge {
	padding: 6px 14px;
	border-radius: 20px;
	font-weight: 700;
	font-size: 0.85em;
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

.status-active {
	background: rgba(16, 185, 129, 0.15);
	color: var(--accent-green);
	border: 1px solid var(--accent-green);
}

.status-inactive {
	background: rgba(239, 68, 68, 0.15);
	color: var(--accent-red);
	border: 1px solid var(--accent-red);
}

.alert {
	padding: 18px 24px;
	border-radius: 12px;
	margin-bottom: 24px;
	display: none;
	font-weight: 600;
	animation: slideIn 0.3s ease;
}

@keyframes slideIn {
	from {
		opacity: 0;
		transform: translateY(-10px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.alert-success {
	background: rgba(16, 185, 129, 0.15);
	color: var(--accent-green);
	border: 2px solid var(--accent-green);
}

.alert-error {
	background: rgba(239, 68, 68, 0.15);
	color: var(--accent-red);
	border: 2px solid var(--accent-red);
}

.loading {
	text-align: center;
	padding: 60px;
	color: var(--text-secondary);
	font-size: 1.1em;
}

.loading::after {
	content: '...';
	animation: dots 1.5s infinite;
}

@keyframes dots {
	0%, 20% { content: '.'; }
	40% { content: '..'; }
	60%, 100% { content: '...'; }
}

.action-buttons {
	display: flex;
	gap: 10px;
	flex-wrap: wrap;
}

.modal {
	display: none;
	position: fixed;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	background: rgba(0, 0, 0, 0.8);
	backdrop-filter: blur(8px);
	z-index: 1000;
	animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
	from { opacity: 0; }
	to { opacity: 1; }
}

.modal-content {
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	background: var(--bg-secondary);
	border: 1px solid var(--border-color);
	border-radius: 24px;
	padding: 40px;
	max-width: 500px;
	width: 90%;
	box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
	animation: modalSlide 0.3s ease;
}

@keyframes modalSlide {
	from {
		opacity: 0;
		transform: translate(-50%, -45%);
	}
	to {
		opacity: 1;
		transform: translate(-50%, -50%);
	}
}

.modal-header {
	margin-bottom: 24px;
	padding-bottom: 16px;
	border-bottom: 2px solid var(--border-color);
}

.modal-header h3 {
	font-size: 1.5em;
	color: var(--text-primary);
}

.modal-footer {
	margin-top: 32px;
	display: flex;
	gap: 12px;
	justify-content: flex-end;
}

.badge {
	display: inline-block;
	padding: 4px 12px;
	border-radius: 12px;
	font-size: 0.85em;
	font-weight: 600;
	background: var(--bg-tertiary);
	color: var(--text-secondary);
}

@media (max-width: 768px) {
	.form-grid {
		grid-template-columns: 1fr;
	}
	
	.header h1 {
		font-size: 2.5em;
	}
	
	.action-buttons {
		flex-direction: column;
	}
	
	.btn {
		width: 100%;
	}
}


/* Search and Filters */
.controls-bar {
	display: flex;
	gap: 16px;
	margin-bottom: 24px;
	flex-wrap: wrap;
	align-items: center;
}

.search-box {
	flex: 1;
	min-width: 300px;
	position: relative;
}

.search-box input {
	width: 100%;
	padding: 14px 18px 14px 48px;
	background: var(--bg-tertiary);
	border: 2px solid var(--border-color);
	border-radius: 12px;
	color: var(--text-primary);
	font-size: 15px;
	transition: all 0.3s ease;
}

.search-box input:focus {
	border-color: var(--accent-blue);
	background: var(--bg-secondary);
	box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.search-box::before {
	content: '🔍';
	position: absolute;
	left: 18px;
	top: 50%;
	transform: translateY(-50%);
	font-size: 1.2em;
}

.filter-group {
	display: flex;
	gap: 12px;
}

.filter-btn {
	padding: 12px 20px;
	background: var(--bg-tertiary);
	border: 2px solid var(--border-color);
	border-radius: 12px;
	color: var(--text-secondary);
	font-size: 14px;
	font-weight: 600;
	cursor: pointer;
	transition: all 0.3s ease;
}

.filter-btn:hover {
	border-color: var(--accent-blue);
	color: var(--text-primary);
}

.filter-btn.active {
	background: var(--accent-blue);
	border-color: var(--accent-blue);
	color: white;
}

/* Sortable Table Headers */
th.sortable {
	cursor: pointer;
	user-select: none;
	position: relative;
	padding-right: 30px;
}

th.sortable:hover {
	color: var(--accent-blue);
}

th.sortable::after {
	content: '⇅';
	position: absolute;
	right: 10px;
	opacity: 0.3;
	transition: all 0.3s ease;
}

th.sortable.asc::after {
	content: '↑';
	opacity: 1;
	color: var(--accent-blue);
}

th.sortable.desc::after {
	content: '↓';
	opacity: 1;
	color: var(--accent-blue);
}

/* Theme Toggle */
.theme-toggle {
	position: fixed;
	top: 20px;
	right: 20px;
	z-index: 100;
	background: var(--bg-secondary);
	border: 2px solid var(--border-color);
	border-radius: 50px;
	padding: 8px 16px;
	display: flex;
	align-items: center;
	gap: 8px;
	cursor: pointer;
	transition: all 0.3s ease;
	box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.theme-toggle:hover {
	border-color: var(--accent-blue);
	transform: translateY(-2px);
	box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
}

.theme-toggle span {
	font-size: 1.2em;
}

/* Light Theme */
body.light-theme {
	--bg-primary: #f3f4f6;
	--bg-secondary: #ffffff;
	--bg-tertiary: #f9fafb;
	--text-primary: #1f2937;
	--text-secondary: #6b7280;
	--border-color: #e5e7eb;
}

body.light-theme .stat-card::before {
	background: linear-gradient(90deg, var(--accent-blue), var(--accent-purple));
}

/* Dashboard Charts */
.dashboard-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
	gap: 24px;
	margin-bottom: 48px;
}

.chart-card {
	background: var(--bg-secondary);
	border: 1px solid var(--border-color);
	border-radius: 24px;
	padding: 32px;
	box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.chart-card h3 {
	font-size: 1.3em;
	margin-bottom: 20px;
	color: var(--text-primary);
}

.chart-container {
	height: 250px;
	position: relative;
}

.bar-chart {
	display: flex;
	align-items: flex-end;
	justify-content: space-around;
	height: 100%;
	gap: 8px;
}

.bar {
	flex: 1;
	background: linear-gradient(180deg, var(--accent-blue), var(--accent-purple));
	border-radius: 8px 8px 0 0;
	position: relative;
	transition: all 0.3s ease;
	min-height: 20px;
}

.bar:hover {
	opacity: 0.8;
	transform: translateY(-4px);
}

.bar-label {
	position: absolute;
	bottom: -25px;
	left: 50%;
	transform: translateX(-50%);
	font-size: 0.85em;
	color: var(--text-secondary);
	white-space: nowrap;
}

.bar-value {
	position: absolute;
	top: -25px;
	left: 50%;
	transform: translateX(-50%);
	font-size: 0.9em;
	font-weight: 700;
	color: var(--text-primary);
}

.top-users-list {
	list-style: none;
}

.top-user-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16px;
	background: var(--bg-tertiary);
	border-radius: 12px;
	margin-bottom: 12px;
	transition: all 0.3s ease;
}

.top-user-item:hover {
	background: var(--bg-primary);
	transform: translateX(4px);
}

.top-user-info {
	display: flex;
	align-items: center;
	gap: 12px;
}

.top-user-rank {
	width: 32px;
	height: 32px;
	background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	font-weight: 800;
	color: white;
}

.top-user-count {
	font-weight: 700;
	color: var(--accent-blue);
}

/* Edit Modal Specific */
.edit-form-grid {
	display: grid;
	grid-template-columns: 1fr;
	gap: 20px;
	margin-top: 20px;
}

/* Improved Add User Form */
.add-user-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 24px;
	margin-bottom: 32px;
}

.add-user-grid .form-group:nth-child(4) {
	grid-column: 1 / -1;
}

@media (max-width: 768px) {
	.add-user-grid {
		grid-template-columns: 1fr;
	}
	
	.dashboard-grid {
		grid-template-columns: 1fr;
	}
	
	.controls-bar {
		flex-direction: column;
	}
	
	.search-box {
		min-width: 100%;
	}
}
</style>
</head>
<body>
	<div class="theme-toggle" onclick="toggleTheme()">
		<span id="themeIcon">🌙</span>
		<span id="themeText">Dark</span>
	</div>

	<div class="container">
		<div class="header">
			<h1>⚡ OneTap</h1>
			<p>License Management System</p>
		</div>

		<div class="stats">
			<div class="stat-card">
				<h3 id="totalUsers">-</h3>
				<p>Total Users</p>
			</div>
			<div class="stat-card">
				<h3 id="activeUsers">-</h3>
				<p>Active Licenses</p>
			</div>
			<div class="stat-card">
				<h3 id="recentLogins">-</h3>
				<p>Recent Logins (24h)</p>
			</div>
		</div>

		<div class="card">
			<div class="card-header">
				<h2>👥 Users List</h2>
				<div style="display: flex; gap: 12px;">
					<button onclick="showAddUserModal()" class="btn btn-primary btn-icon">
						➕ Add New User
					</button>
					<button onclick="loadUsers()" class="btn btn-success btn-icon">
						🔄 Refresh
					</button>
				</div>
			</div>
			<div class="controls-bar">
				<div class="search-box">
					<input type="text" id="searchInput" placeholder="Search by username..." oninput="filterUsers()">
				</div>
				<div class="filter-group">
					<button class="filter-btn active" data-filter="all" onclick="setFilter('all')">All</button>
					<button class="filter-btn" data-filter="active" onclick="setFilter('active')">Active</button>
					<button class="filter-btn" data-filter="inactive" onclick="setFilter('inactive')">Inactive</button>
				</div>
			</div>
			<div id="usersTable">
				<div class="loading">Loading users</div>
			</div>
		</div>

		<div class="card">
			<div class="card-header">
				<h2>📋 Recent Activity</h2>
				<button onclick="loadAudit()" class="btn btn-success btn-icon">
					🔄 Refresh
				</button>
			</div>
			<div id="auditTable">
				<div class="loading">Loading audit log</div>
			</div>
		</div>
	</div>

	<!-- Add User Modal -->
	<div id="addUserModal" class="modal">
		<div class="modal-content">
			<div class="modal-header">
				<h3>➕ Add New User</h3>
			</div>
			<div id="addAlert" class="alert"></div>
			<form id="addUserForm" class="edit-form-grid">
				<div class="form-group">
					<label>Discord Username</label>
					<input type="text" id="username" required placeholder="Enter username">
				</div>
				<div class="form-group">
					<label>User ID (UID)</label>
					<input type="number" id="uid" required placeholder="1">
				</div>
				<div class="form-group">
					<label>Subscription Type</label>
					<select id="subscription" required onchange="toggleCustomDays()">
						<option value="month">1 Month</option>
						<option value="year">1 Year</option>
						<option value="lifetime" selected>Lifetime</option>
						<option value="custom">Custom Days</option>
					</select>
				</div>
				<div class="form-group">
					<label>Custom Days (MSK)</label>
					<input type="number" id="customDays" placeholder="30" disabled>
				</div>
				<div class="form-group">
					<label>Max HWID Changes</label>
					<input type="number" id="maxHwid" value="3" required>
				</div>
			</form>
			<div class="modal-footer">
				<button onclick="closeAddUserModal()" class="btn btn-secondary">Cancel</button>
				<button onclick="submitAddUser()" class="btn btn-primary">Add User</button>
			</div>
		</div>
	</div>

	<!-- Edit User Modal -->
	<div id="editModal" class="modal">
		<div class="modal-content">
			<div class="modal-header">
				<h3>✏️ Edit User</h3>
			</div>
			<form id="editUserForm" class="edit-form-grid">
				<input type="hidden" id="editUsername">
				<div class="form-group">
					<label>User ID (UID)</label>
					<input type="number" id="editUid" required>
				</div>
				<div class="form-group">
					<label>Max HWID Changes</label>
					<input type="number" id="editMaxHwid" required>
				</div>
				<div class="form-group">
					<label>Subscription Type</label>
					<select id="editSubscription" required onchange="toggleEditCustomDays()">
						<option value="month">1 Month</option>
						<option value="year">1 Year</option>
						<option value="lifetime">Lifetime</option>
						<option value="custom">Custom Days</option>
					</select>
				</div>
				<div class="form-group">
					<label>Custom Days (MSK)</label>
					<input type="number" id="editCustomDays" placeholder="30" disabled>
				</div>
			</form>
			<div class="modal-footer">
				<button onclick="closeEditModal()" class="btn btn-secondary">Cancel</button>
				<button onclick="saveEdit()" class="btn btn-primary">Save Changes</button>
			</div>
		</div>
	</div>

	<!-- Delete Confirmation Modal -->
	<div id="deleteModal" class="modal">
		<div class="modal-content">
			<div class="modal-header">
				<h3>⚠️ Confirm Deletion</h3>
			</div>
			<p>Are you sure you want to delete user <strong id="deleteUsername"></strong>?</p>
			<p style="color: var(--accent-red); margin-top: 12px;">This action cannot be undone!</p>
			<div class="modal-footer">
				<button onclick="closeDeleteModal()" class="btn btn-secondary">Cancel</button>
				<button onclick="confirmDelete()" class="btn btn-danger">Delete</button>
			</div>
		</div>
	</div>

	<script>const API_URL = window.location.origin;
const MSK_OFFSET = 3 * 60 * 60 * 1000;
let deleteTargetUser = null;
let allUsers = [];
let currentFilter = 'all';
let currentSort = { column: null, direction: 'asc' };

// Theme Toggle
function toggleTheme() {
	document.body.classList.toggle('light-theme');
	const isLight = document.body.classList.contains('light-theme');
	document.getElementById('themeIcon').textContent = isLight ? '☀️' : '🌙';
	document.getElementById('themeText').textContent = isLight ? 'Light' : 'Dark';
	localStorage.setItem('theme', isLight ? 'light' : 'dark');
}

// Load saved theme
if (localStorage.getItem('theme') === 'light') {
	document.body.classList.add('light-theme');
	document.getElementById('themeIcon').textContent = '☀️';
	document.getElementById('themeText').textContent = 'Light';
}

function toggleCustomDays() {
	const subscription = document.getElementById('subscription').value;
	const customDaysInput = document.getElementById('customDays');
	customDaysInput.disabled = subscription !== 'custom';
	if (subscription === 'custom') {
		customDaysInput.focus();
	}
}

function toggleEditCustomDays() {
	const subscription = document.getElementById('editSubscription').value;
	const customDaysInput = document.getElementById('editCustomDays');
	customDaysInput.disabled = subscription !== 'custom';
	if (subscription === 'custom') {
		customDaysInput.focus();
	}
}

// Add User Modal Functions
function showAddUserModal() {
	document.getElementById('addUserModal').style.display = 'block';
	document.getElementById('addUserForm').reset();
	document.getElementById('customDays').disabled = true;
	document.getElementById('addAlert').style.display = 'none';
}

function closeAddUserModal() {
	document.getElementById('addUserModal').style.display = 'none';
}

async function submitAddUser() {
	const username = document.getElementById('username').value;
	const uid = parseInt(document.getElementById('uid').value);
	const maxHwid = parseInt(document.getElementById('maxHwid').value);
	const subscription = document.getElementById('subscription').value;
	const customDays = parseInt(document.getElementById('customDays').value) || 0;
	
	const body = {
		discord_username: username,
		uid: uid,
		max_hwid_changes: maxHwid
	};
	
	const nowMSK = Date.now() + MSK_OFFSET;
	
	if (subscription === 'month') {
		body.expires_at = nowMSK + (30 * 24 * 60 * 60 * 1000);
	} else if (subscription === 'year') {
		body.expires_at = nowMSK + (365 * 24 * 60 * 60 * 1000);
	} else if (subscription === 'custom' && customDays > 0) {
		body.expires_at = nowMSK + (customDays * 24 * 60 * 60 * 1000);
	}
	
	try {
		const response = await fetch(API_URL + '/api/admin/add-user', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(body)
		});
		
		const data = await response.json();
		
		if (response.ok) {
			const alert = document.getElementById('addAlert');
			alert.className = 'alert alert-success';
			alert.style.display = 'block';
			alert.textContent = '✓ User added successfully!';
			
			setTimeout(() => {
				closeAddUserModal();
				loadUsers();
			}, 1500);
		} else {
			throw new Error(data.error || 'Failed to add user');
		}
	} catch (error) {
		const alert = document.getElementById('addAlert');
		alert.className = 'alert alert-error';
		alert.style.display = 'block';
		alert.textContent = '✗ Error: ' + error.message;
	}
}

// Filter Functions
function setFilter(filter) {
	currentFilter = filter;
	document.querySelectorAll('.filter-btn').forEach(btn => {
		btn.classList.remove('active');
		if (btn.dataset.filter === filter) {
			btn.classList.add('active');
		}
	});
	renderUsers();
}

function filterUsers() {
	renderUsers();
}

// Sort Functions
function sortUsers(column) {
	if (currentSort.column === column) {
		currentSort.direction = currentSort.direction === 'asc' ? 'desc' : 'asc';
	} else {
		currentSort.column = column;
		currentSort.direction = 'asc';
	}
	
	allUsers.sort((a, b) => {
		let aVal = a[column];
		let bVal = b[column];
		
		if (column === 'last_login') {
			aVal = aVal || 0;
			bVal = bVal || 0;
		}
		
		if (aVal < bVal) return currentSort.direction === 'asc' ? -1 : 1;
		if (aVal > bVal) return currentSort.direction === 'asc' ? 1 : -1;
		return 0;
	});
	
	renderUsers();
}

function renderUsers() {
	const searchTerm = document.getElementById('searchInput').value.toLowerCase();
	
	let filtered = allUsers.filter(user => {
		const matchesSearch = user.discord_username.toLowerCase().includes(searchTerm);
		const matchesFilter = currentFilter === 'all' || 
			(currentFilter === 'active' && user.is_active) ||
			(currentFilter === 'inactive' && !user.is_active);
		return matchesSearch && matchesFilter;
	});
	
	let html = '<table><thead><tr>';
	html += '<th class="sortable' + (currentSort.column === 'discord_username' ? ' ' + currentSort.direction : '') + '" onclick="sortUsers(\\'discord_username\\')">User</th>';
	html += '<th class="sortable' + (currentSort.column === 'uid' ? ' ' + currentSort.direction : '') + '" onclick="sortUsers(\\'uid\\')">UID</th>';
	html += '<th>Status</th>';
	html += '<th>HWID Resets</th>';
	html += '<th class="sortable' + (currentSort.column === 'last_login' ? ' ' + currentSort.direction : '') + '" onclick="sortUsers(\\'last_login\\')">Last Login (MSK)</th>';
	html += '<th>Last IP</th>';
	html += '<th>Actions</th>';
	html += '</tr></thead><tbody>';
	
	filtered.forEach(user => {
		const statusClass = user.is_active ? 'status-active' : 'status-inactive';
		const statusText = user.is_active ? 'Active' : 'Inactive';
		const lastLogin = user.last_login ? new Date(user.last_login).toLocaleString('ru-RU', {timeZone: 'Europe/Moscow'}) : 'Never';
		const lastIp = user.last_ip || '-';
		const hwid = user.hwid_changes_used + '/' + user.max_hwid_changes;
		
		let avatarHtml = '';
		if (user.discord_id && user.discord_avatar) {
			const avatarUrl = \`https://cdn.discordapp.com/avatars/\${user.discord_id}/\${user.discord_avatar}.png?size=64\`;
			avatarHtml = \`<img src="\${avatarUrl}" class="user-avatar" alt="Avatar" onerror="this.style.display='none'">\`;
		}
		
		const toggleText = user.is_active ? 'Deactivate' : 'Activate';
		const toggleIcon = user.is_active ? '🔒' : '✅';
		
		html += \`<tr>
			<td>
				<div class="user-info">
					\${avatarHtml}
					<strong>\${user.discord_username}</strong>
				</div>
			</td>
			<td><span class="badge">\${user.uid}</span></td>
			<td><span class="status-badge \${statusClass}">\${statusText}</span></td>
			<td>\${hwid}</td>
			<td>\${lastLogin}</td>
			<td>\${lastIp}</td>
			<td>
				<div class="action-buttons">
					<button class="btn btn-primary btn-icon" onclick="showEditModal('\${user.discord_username}', \${user.uid}, \${user.max_hwid_changes}, \${user.expires_at})">✏️ Edit</button>
					<button class="btn btn-warning btn-icon" onclick="toggleActive('\${user.discord_username}')">\${toggleIcon} \${toggleText}</button>
					<button class="btn btn-danger btn-icon" onclick="showResetHWID('\${user.discord_username}')">🔄 Reset</button>
					<button class="btn btn-danger btn-icon" onclick="showDeleteModal('\${user.discord_username}')">🗑️ Delete</button>
				</div>
			</td>
		</tr>\`;
	});
	
	html += '</tbody></table>';
	document.getElementById('usersTable').innerHTML = html;
}

async function loadUsers() {
	try {
		const response = await fetch(API_URL + '/api/admin/users');
		const data = await response.json();
		
		allUsers = data.users;
		const now = Date.now();
		const dayAgo = now - (24 * 60 * 60 * 1000);
		
		document.getElementById('totalUsers').textContent = allUsers.length;
		document.getElementById('activeUsers').textContent = allUsers.filter(u => u.is_active).length;
		document.getElementById('recentLogins').textContent = allUsers.filter(u => u.last_login && u.last_login > dayAgo).length;
		
		renderUsers();
	} catch (error) {
		document.getElementById('usersTable').innerHTML = '<div class="alert alert-error" style="display:block;">Error loading users: ' + error.message + '</div>';
	}
}

async function loadAudit() {
	try {
		const response = await fetch(API_URL + '/api/admin/audit?limit=50');
		const data = await response.json();
		
		let html = '<table><thead><tr><th>Time (MSK)</th><th>User ID</th><th>Action</th><th>Details</th><th>IP</th></tr></thead><tbody>';
		
		data.logs.forEach(log => {
			const time = new Date(log.timestamp).toLocaleString('ru-RU', {timeZone: 'Europe/Moscow'});
			const userId = log.user_id || '-';
			
			let actionBadge = '';
			if (log.action.includes('SUCCESS')) {
				actionBadge = '<span class="status-badge status-active">' + log.action + '</span>';
			} else if (log.action.includes('FAILED')) {
				actionBadge = '<span class="status-badge status-inactive">' + log.action + '</span>';
			} else {
				actionBadge = '<span class="badge">' + log.action + '</span>';
			}
			
			html += \`<tr>
				<td>\${time}</td>
				<td><span class="badge">\${userId}</span></td>
				<td>\${actionBadge}</td>
				<td>\${log.details}</td>
				<td>\${log.ip}</td>
			</tr>\`;
		});
		
		html += '</tbody></table>';
		document.getElementById('auditTable').innerHTML = html;
	} catch (error) {
		document.getElementById('auditTable').innerHTML = '<div class="alert alert-error" style="display:block;">Error loading audit log: ' + error.message + '</div>';
	}
}

// Edit User Functions
function showEditModal(username, uid, maxHwid, expiresAt) {
	document.getElementById('editUsername').value = username;
	document.getElementById('editUid').value = uid;
	document.getElementById('editMaxHwid').value = maxHwid;
	
	// Determine subscription type
	if (!expiresAt) {
		document.getElementById('editSubscription').value = 'lifetime';
		document.getElementById('editCustomDays').disabled = true;
	} else {
		const now = Date.now() + MSK_OFFSET;
		const daysLeft = Math.ceil((expiresAt - now) / (24 * 60 * 60 * 1000));
		
		if (daysLeft <= 31) {
			document.getElementById('editSubscription').value = 'month';
			document.getElementById('editCustomDays').disabled = true;
		} else if (daysLeft <= 366) {
			document.getElementById('editSubscription').value = 'year';
			document.getElementById('editCustomDays').disabled = true;
		} else {
			document.getElementById('editSubscription').value = 'custom';
			document.getElementById('editCustomDays').value = daysLeft;
			document.getElementById('editCustomDays').disabled = false;
		}
	}
	
	document.getElementById('editModal').style.display = 'block';
}

function closeEditModal() {
	document.getElementById('editModal').style.display = 'none';
}

async function saveEdit() {
	const username = document.getElementById('editUsername').value;
	const uid = parseInt(document.getElementById('editUid').value);
	const maxHwid = parseInt(document.getElementById('editMaxHwid').value);
	const subscription = document.getElementById('editSubscription').value;
	const customDays = parseInt(document.getElementById('editCustomDays').value) || 0;
	
	// Note: This requires a new API endpoint /api/admin/update-user
	alert('Edit functionality requires backend API endpoint. Coming soon!');
	closeEditModal();
}

async function toggleActive(username) {
	try {
		const response = await fetch(API_URL + '/api/admin/toggle-active', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ discord_username: username })
		});
		
		const data = await response.json();
		
		if (response.ok) {
			loadUsers();
		} else {
			throw new Error(data.error || 'Failed to toggle status');
		}
	} catch (error) {
		alert('Error: ' + error.message);
	}
}

function showDeleteModal(username) {
	deleteTargetUser = username;
	document.getElementById('deleteUsername').textContent = username;
	document.getElementById('deleteModal').style.display = 'block';
}

function closeDeleteModal() {
	document.getElementById('deleteModal').style.display = 'none';
	deleteTargetUser = null;
}

async function confirmDelete() {
	if (!deleteTargetUser) return;
	
	try {
		const response = await fetch(API_URL + '/api/admin/delete-user', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ discord_username: deleteTargetUser })
		});
		
		const data = await response.json();
		
		if (response.ok) {
			closeDeleteModal();
			loadUsers();
		} else {
			throw new Error(data.error || 'Failed to delete user');
		}
	} catch (error) {
		alert('Error: ' + error.message);
	}
}

async function showResetHWID(username) {
	if (!confirm('Reset HWID for ' + username + '?\\n\\nThis will:\\n- Clear their HWID\\n- Reset HWID change counter to 0\\n- Force re-authentication on next login')) {
		return;
	}
	
	try {
		const response = await fetch(API_URL + '/api/admin/reset-hwid', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ discord_username: username })
		});
		
		const data = await response.json();
		
		if (response.ok) {
			alert('✓ HWID reset successfully for ' + username);
			loadUsers();
		} else {
			throw new Error(data.error || 'Failed to reset HWID');
		}
	} catch (error) {
		alert('✗ Error: ' + error.message);
	}
}

window.onclick = function(event) {
	if (event.target.classList.contains('modal')) {
		event.target.style.display = 'none';
	}
}

window.addEventListener('DOMContentLoaded', () => {
	loadUsers();
	loadAudit();
	
	setInterval(() => {
		loadUsers();
		loadAudit();
	}, 30000);
});
</script>
</body>
</html>
`;
}
