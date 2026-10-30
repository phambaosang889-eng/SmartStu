$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$server=Join-Path $root '.runtime/mysql-8.4.0-winx64/bin/mysqld.exe'
if(!(Test-Path -LiteralPath $server)){throw 'Extract the official MySQL 8.4 ZIP into .runtime first; see docs/DATABASE.md'}
$base=$root.Replace('\','/')
function Wait-WorkspaceMySql {
 # Windows PowerShell 5 treats native stderr as an error record. Failed pings
 # during startup are expected; only the final readiness timeout is fatal.
 $ErrorActionPreference='Continue'
 $deadline=[DateTime]::UtcNow.AddSeconds(30)
 $admin=Join-Path $root '.runtime/mysql-8.4.0-winx64/bin/mysqladmin.exe'
 $privateConfig=Join-Path $root '.runtime/mysql-admin.local.ini'
 if(!(Test-Path -LiteralPath $privateConfig)){throw 'Missing local MySQL readiness configuration; see docs/DATABASE.md'}
 do {
  & $admin "--defaults-extra-file=$privateConfig" --host=127.0.0.1 --port=3307 --connect-timeout=1 ping --silent *> $null
  if($LASTEXITCODE -eq 0){Write-Output 'Workspace MySQL is ready on 127.0.0.1:3307.';return}
  Start-Sleep -Milliseconds 250
 } while([DateTime]::UtcNow -lt $deadline)
 throw 'MySQL did not become ready within 30 seconds. See .runtime/mysql-server.log.'
}
$owned=Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" | Where-Object { $_.ExecutablePath -eq [IO.Path]::GetFullPath($server) }
if($owned){Wait-WorkspaceMySql;exit}
$proc=Start-Process -FilePath $server -ArgumentList @('--no-defaults',('--basedir="'+$base+'/.runtime/mysql-8.4.0-winx64"'),('--datadir="'+$base+'/.runtime/mysql-data"'),'--port=3307','--bind-address=127.0.0.1','--mysqlx=OFF',('--log-error="'+$base+'/.runtime/mysql-server.log"')) -WindowStyle Hidden -PassThru
if($proc.WaitForExit(1500)){throw 'MySQL exited during startup. See .runtime/mysql-server.log.'}
Write-Output ('Local MySQL process: '+$proc.Id+'; 127.0.0.1:3307')
Wait-WorkspaceMySql
