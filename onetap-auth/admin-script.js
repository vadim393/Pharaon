const API_URL = window.location.origin;
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
	html += '<th class="sortable' + (currentSort.column === 'discord_username' ? ' ' + currentSort.direction : '') + '" onclick="sortUsers(\'discord_username\')">User</th>';
	html += '<th class="sortable' + (currentSort.column === 'uid' ? ' ' + currentSort.direction : '') + '" onclick="sortUsers(\'uid\')">UID</th>';
	html += '<th>Status</th>';
	html += '<th>HWID Resets</th>';
	html += '<th class="sortable' + (currentSort.column === 'last_login' ? ' ' + currentSort.direction : '') + '" onclick="sortUsers(\'last_login\')">Last Login (MSK)</th>';
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
			const avatarUrl = `https://cdn.discordapp.com/avatars/${user.discord_id}/${user.discord_avatar}.png?size=64`;
			avatarHtml = `<img src="${avatarUrl}" class="user-avatar" alt="Avatar" onerror="this.style.display='none'">`;
		}
		
		const toggleText = user.is_active ? 'Deactivate' : 'Activate';
		const toggleIcon = user.is_active ? '🔒' : '✅';
		
		html += `<tr>
			<td>
				<div class="user-info">
					${avatarHtml}
					<strong>${user.discord_username}</strong>
				</div>
			</td>
			<td><span class="badge">${user.uid}</span></td>
			<td><span class="status-badge ${statusClass}">${statusText}</span></td>
			<td>${hwid}</td>
			<td>${lastLogin}</td>
			<td>${lastIp}</td>
			<td>
				<div class="action-buttons">
					<button class="btn btn-primary btn-icon" onclick="showEditModal('${user.discord_username}', ${user.uid}, ${user.max_hwid_changes}, ${user.expires_at})">✏️ Edit</button>
					<button class="btn btn-warning btn-icon" onclick="toggleActive('${user.discord_username}')">${toggleIcon} ${toggleText}</button>
					<button class="btn btn-danger btn-icon" onclick="showResetHWID('${user.discord_username}')">🔄 Reset</button>
					<button class="btn btn-danger btn-icon" onclick="showDeleteModal('${user.discord_username}')">🗑️ Delete</button>
				</div>
			</td>
		</tr>`;
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
			
			html += `<tr>
				<td>${time}</td>
				<td><span class="badge">${userId}</span></td>
				<td>${actionBadge}</td>
				<td>${log.details}</td>
				<td>${log.ip}</td>
			</tr>`;
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
	if (!confirm(`Reset HWID for ${username}?\n\nThis will:\n- Clear their HWID\n- Reset HWID change counter to 0\n- Force re-authentication on next login`)) {
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

function closeHwidModal() {
	document.getElementById('hwidModal').style.display = 'none';
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
