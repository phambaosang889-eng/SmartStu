$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$config=[IO.File]::ReadAllLines((Join-Path $root '.runtime/db.local.properties'))
$passwordLine=$config | Where-Object { $_.StartsWith('SMARTSTU_SEED_PASSWORD=') } | Select-Object -First 1
if(!$passwordLine){throw 'Missing local test account password'}
$env:SMARTSTU_TEST_PASSWORD=$passwordLine.Substring('SMARTSTU_SEED_PASSWORD='.Length)
try { & (Join-Path $PSScriptRoot 'http-regression.ps1') }
finally { Remove-Item Env:SMARTSTU_TEST_PASSWORD }
