$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$runtime=Join-Path $root '.runtime'
$mysql=Join-Path $runtime 'mysql-8.4.0-winx64/bin/mysql.exe'
$adminFile=Join-Path $runtime 'mysql-admin.local.ini'
$configFile=Join-Path $runtime 'db.local.properties'
if(!(Test-Path -LiteralPath $adminFile)){
  $rootSecret=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
  $appSecret=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
  [IO.File]::WriteAllText($adminFile,"[client]`nuser=root`npassword=$rootSecret`nhost=127.0.0.1`nport=3307`n",[Text.UTF8Encoding]::new($false))
  [IO.File]::WriteAllText($configFile,"SMARTSTU_STORAGE=mysql`nSMARTSTU_DB_URL=jdbc:mysql://127.0.0.1:3307/smartstu?connectionTimeZone=UTC&characterEncoding=UTF-8&connectTimeout=5000&socketTimeout=10000`nSMARTSTU_DB_USER=smartstu`nSMARTSTU_DB_PASSWORD=$appSecret`n",[Text.UTF8Encoding]::new($false))
  $setup="ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootSecret'; CREATE DATABASE IF NOT EXISTS smartstu CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER 'smartstu'@'127.0.0.1' IDENTIFIED BY '$appSecret'; GRANT SELECT,INSERT,UPDATE,DELETE ON smartstu.* TO 'smartstu'@'127.0.0.1';"
  $setup | & $mysql --no-defaults --host=127.0.0.1 --port=3307 --user=root
  if($LASTEXITCODE -ne 0){throw 'MySQL account setup failed; inspect local config before retrying.'}
}
Get-Content -Raw -Encoding utf8 (Join-Path $root 'database/smartstu.sql') | & $mysql "--defaults-extra-file=$adminFile" --default-character-set=utf8mb4
if($LASTEXITCODE -ne 0){throw 'Schema import failed'}
& $mysql "--defaults-extra-file=$adminFile" --execute='SELECT VERSION(); SELECT COUNT(*) AS schema_tables FROM information_schema.tables WHERE table_schema="smartstu";'
