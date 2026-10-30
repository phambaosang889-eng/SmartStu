$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
& (Join-Path $PSScriptRoot 'compile-checks.ps1')
$schema='smartstu_schema_'+[Guid]::NewGuid().ToString('N').Substring(0,8)
$adminFile=Join-Path $root '.runtime/mysql-admin.local.ini'
$mysql=Join-Path $root '.runtime/mysql-8.4.0-winx64/bin/mysql.exe'
$sql=[IO.File]::ReadAllText((Join-Path $root 'database/smartstu.sql')).Replace('CREATE DATABASE IF NOT EXISTS smartstu ','CREATE DATABASE IF NOT EXISTS '+$schema+' ').Replace('USE smartstu;','USE '+$schema+';')
$seed=[IO.File]::ReadAllText((Join-Path $root 'database/seed-local.sql')).Replace('USE smartstu;','USE '+$schema+';')
# Run initialization twice to check repeatability. No existing schema is targeted.
($sql+"`n"+$seed+"`n"+$sql+"`n"+$seed) | & $mysql "--defaults-extra-file=$adminFile" --default-character-set=utf8mb4
if($LASTEXITCODE -ne 0){throw 'Fresh schema/seed validation failed'}
$admin=@{};foreach($line in [IO.File]::ReadAllLines($adminFile)){if($line -match '^([^#=]+)=(.*)$'){$admin[$matches[1]]=$matches[2]}}
$seedPassword=([IO.File]::ReadAllLines((Join-Path $root '.runtime/db.local.properties')) | Where-Object {$_.StartsWith('SMARTSTU_SEED_PASSWORD=')}).Substring('SMARTSTU_SEED_PASSWORD='.Length)
$config="SMARTSTU_DB_URL=jdbc:mysql://127.0.0.1:3307/$schema`?connectionTimeZone=UTC&characterEncoding=UTF-8`nSMARTSTU_DB_USER=root`nSMARTSTU_DB_PASSWORD="+$admin['password']+"`nSMARTSTU_SEED_PASSWORD=$seedPassword`n"
$configPath=Join-Path $root '.runtime/schema-test.local.properties'
[IO.File]::WriteAllText($configPath,$config,[Text.UTF8Encoding]::new($false))
& 'C:/Program Files/Java/jdk-17.0.20/bin/java.exe' "-DSMARTSTU_CONFIG_FILE=$configPath" -DSMARTSTU_DB_JNDI= -cp "$root/build/web/WEB-INF/classes;$root/.runtime/test-classes;$root/web/WEB-INF/lib/*" RelationalWorkflowTest
if($LASTEXITCODE -ne 0){throw 'Fresh schema JDBC workflow failed'}
& $mysql "--defaults-extra-file=$adminFile" --execute="SELECT COUNT(*) AS accounts FROM $schema.users; SELECT COUNT(*) AS objects FROM information_schema.tables WHERE table_schema='$schema'; SELECT COUNT(*) AS foreign_keys FROM information_schema.table_constraints WHERE constraint_schema='$schema' AND constraint_type='FOREIGN KEY';"
Write-Output "PASS fresh schema + seed imported twice; verification database retained: $schema"
