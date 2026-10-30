$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$base='http://localhost:8081/SmartStudy'
$password=([IO.File]::ReadAllLines((Join-Path $root '.runtime/db.local.properties')) | Where-Object {$_.StartsWith('SMARTSTU_SEED_PASSWORD=')}).Substring('SMARTSTU_SEED_PASSWORD='.Length)
$checks=0
function Assert($ok,$name){$script:checks++;if(!$ok){throw "FAIL: $name"}}
function Snapshot {
 $value=& (Join-Path $root '.runtime/mysql-8.4.0-winx64/bin/mysql.exe') "--defaults-extra-file=$root/.runtime/mysql-admin.local.ini" --batch --skip-column-names --execute="SELECT SHA2(CAST(payload AS CHAR),256) FROM smartstu.lesson_progress WHERE row_key='student@smartstudy.vn';"
 if($LASTEXITCODE -ne 0){throw 'Snapshot query failed'}
 return [string]$value
}
$login=Invoke-WebRequest ($base+'/login') -SessionVariable session
$csrf=[regex]::Match($login.Content,'name="csrf" value="([^"]+)"').Groups[1].Value
$page=Invoke-WebRequest ($base+'/login') -WebSession $session -Method Post -Body @{csrf=$csrf;email='student@smartstudy.vn';password=$password;role='STUDENT'}
Assert ($page.StatusCode -eq 200) 'login before outage'
$profile=Invoke-WebRequest ($base+'/student/profile') -WebSession $session
$csrf=[regex]::Match($profile.Content,'name="csrf" value="([^"]+)"').Groups[1].Value
$before=Snapshot
try {
 & (Join-Path $PSScriptRoot 'stop-local-mysql.ps1')
 $page=Invoke-WebRequest ($base+'/student/dashboard') -WebSession $session -SkipHttpErrorCheck
 Assert ($page.StatusCode -eq 503) 'read returns 503 during outage'
 $homeResponse=Invoke-WebRequest ($base+'/home') -WebSession $session -SkipHttpErrorCheck
 Assert ($homeResponse.StatusCode -eq 503) 'public JSP database error returns 503, not 500'
 Assert ($page.Content -match '<title>503 · SmartStu</title>') 'branded error page'
 Assert ($page.Content -notmatch 'jdbc:mysql|SQLException|SMARTSTU_DB_PASSWORD|Access denied') 'database internals not exposed'
 $page=Invoke-WebRequest ($base+'/student/lesson') -WebSession $session -Method Post -Body @{csrf=$csrf;action='complete';id='lesson-energy'} -SkipHttpErrorCheck
 Assert ($page.StatusCode -eq 503) 'failed write not acknowledged as success'
} finally { & (Join-Path $PSScriptRoot 'start-local-mysql.ps1') }
$page=Invoke-WebRequest ($base+'/student/dashboard') -WebSession $session -SkipHttpErrorCheck
Assert ($page.StatusCode -eq 200) 'database access recovers after restart'
Assert ((Snapshot) -eq $before) 'failed write did not change committed progress'
Write-Output "PASS $checks database outage/recovery checks"
