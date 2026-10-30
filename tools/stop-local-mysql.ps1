$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$server=[IO.Path]::GetFullPath((Join-Path $root '.runtime/mysql-8.4.0-winx64/bin/mysqld.exe'))
$owned=Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" | Where-Object { $_.ExecutablePath -eq $server }
if(!$owned){Write-Output 'Local MySQL is already stopped.';exit}
& (Join-Path $root '.runtime/mysql-8.4.0-winx64/bin/mysqladmin.exe') "--defaults-extra-file=$root/.runtime/mysql-admin.local.ini" shutdown
if($LASTEXITCODE -ne 0){throw 'MySQL shutdown failed'}
foreach($process in $owned){$running=Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue;if($running){Wait-Process -Id $process.ProcessId -Timeout 30}}
Write-Output 'MySQL finished flushing and releasing the local data directory.'
