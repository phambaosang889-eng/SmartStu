$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$pidFile=Join-Path $root '.runtime/tomcat.pid'
$tomcatPid=[int]([IO.File]::ReadAllText($pidFile))
$process=Get-CimInstance Win32_Process -Filter "ProcessId=$tomcatPid"
if(!$process){Write-Output 'Local Tomcat is already stopped.';exit}
if($process.Name -ne 'java.exe' -or !$process.CommandLine.Contains($root+'/tomcat-validation')){throw 'PID does not belong to this workspace Tomcat; refusing to stop it.'}
$tomcatHome='C:/Program Files/Apache Software Foundation/Tomcat 10.1'
& $process.ExecutablePath "-Dcatalina.base=$root/tomcat-validation" "-Dcatalina.home=$tomcatHome" -cp "$tomcatHome/bin/bootstrap.jar;$tomcatHome/bin/tomcat-juli.jar" org.apache.catalina.startup.Bootstrap stop
if($LASTEXITCODE -ne 0){throw 'Graceful Tomcat shutdown failed; inspect the local logs.'}
$running=Get-Process -Id $tomcatPid -ErrorAction SilentlyContinue
if($running){Wait-Process -Id $tomcatPid -Timeout 30}
Write-Output 'Stopped only the local verification Tomcat.'
