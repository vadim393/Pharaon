# OneTap User Management Script

$API_URL = "https://onetap-auth.wishen92.workers.dev"

function Show-Menu {
    Write-Host "`n=== OneTap User Management ===" -ForegroundColor Cyan
    Write-Host "1. Add User"
    Write-Host "2. List Users"
    Write-Host "3. View Audit Log"
    Write-Host "4. Query Database"
    Write-Host "5. Exit"
    Write-Host "==============================`n" -ForegroundColor Cyan
}

function Add-User {
    $username = Read-Host "Discord Username"
    $uid = Read-Host "UID (number)"
    $maxResets = Read-Host "Max HWID Changes (default: 3)"
    
    if ([string]::IsNullOrWhiteSpace($maxResets)) {
        $maxResets = 3
    }
    
    $body = @{
        discord_username = $username
        uid = [int]$uid
        max_hwid_changes = [int]$maxResets
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$API_URL/api/admin/add-user" -Method POST -ContentType "application/json" -Body $body
        Write-Host "`n✓ User added successfully!" -ForegroundColor Green
        Write-Host "Username: $username"
        Write-Host "UID: $uid"
        Write-Host "Max HWID Resets: $maxResets"
    } catch {
        Write-Host "`n✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function List-Users {
    try {
        $response = Invoke-RestMethod -Uri "$API_URL/api/admin/users" -Method GET
        Write-Host "`n=== Users ===" -ForegroundColor Cyan
        $response.users | Format-Table -Property discord_username, uid, is_active, hwid_changes_used, max_hwid_changes, last_ip, @{
            Label="Last Login"
            Expression={
                if ($_.last_login) {
                    [DateTimeOffset]::FromUnixTimeMilliseconds($_.last_login).LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss")
                } else {
                    "Never"
                }
            }
        } -AutoSize
    } catch {
        Write-Host "`n✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function View-AuditLog {
    $limit = Read-Host "Number of entries (default: 50)"
    if ([string]::IsNullOrWhiteSpace($limit)) {
        $limit = 50
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$API_URL/api/admin/audit?limit=$limit" -Method GET
        Write-Host "`n=== Audit Log ===" -ForegroundColor Cyan
        $response.logs | Format-Table -Property user_id, action, details, ip, @{
            Label="Timestamp"
            Expression={
                [DateTimeOffset]::FromUnixTimeMilliseconds($_.timestamp).LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss")
            }
        } -AutoSize
    } catch {
        Write-Host "`n✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Query-Database {
    $query = Read-Host "SQL Query"
    
    try {
        $result = wrangler d1 execute onetap-licenses --command="$query" --remote
        Write-Host "`n$result"
    } catch {
        Write-Host "`n✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Main loop
while ($true) {
    Show-Menu
    $choice = Read-Host "Select option"
    
    switch ($choice) {
        "1" { Add-User }
        "2" { List-Users }
        "3" { View-AuditLog }
        "4" { Query-Database }
        "5" { 
            Write-Host "`nGoodbye!" -ForegroundColor Cyan
            exit 
        }
        default { 
            Write-Host "`n✗ Invalid option" -ForegroundColor Red 
        }
    }
    
    Read-Host "`nPress Enter to continue"
}
