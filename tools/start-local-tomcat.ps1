param([string]$JavaHome='C:/Program Files/Java/jdk-17.0.20',[string]$TomcatHome='C:/Program Files/Apache Software Foundation/Tomcat 10.1')
$ErrorActionPreference='Stop'
$listener=Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue | Select-Object -First 1
if($listener){
 try {$existingPage=Invoke-WebRequest 'http://localhost:8081/SmartStudy/login' -TimeoutSec 10 -SkipHttpErrorCheck} catch {throw 'Port 8081 is occupied. Use the running server from NetBeans; do not start another Tomcat.'}
 if($existingPage.StatusCode -eq 200 -and $existingPage.Content -match 'SmartStudy|SmartStu'){Write-Output 'SmartStudy is already running on port 8081. Reusing the existing server; no second Tomcat started.';exit}
 throw 'Port 8081 is occupied by another application. Stop the appropriate server from NetBeans or choose another port.'
}
$root=Split-Path $PSScriptRoot
$config=Join-Path $root '.runtime/db.local.properties'
if(!(Test-Path -LiteralPath $config)){throw 'Configure .runtime/db.local.properties first; see docs/DATABASE.md'}
$existing=Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {$_.CommandLine -and $_.CommandLine.Contains($root+'/tomcat-validation') -and $_.CommandLine.Contains('Bootstrap start')}
if($existing){Write-Output 'The workspace Tomcat process is already running.';exit}
$java=Join-Path $JavaHome 'bin/java.exe'
$args=@('"-DSMARTSTU_CONFIG_FILE='+$config+'"','"-Dcatalina.base='+$root+'/tomcat-validation"','"-Dcatalina.home='+$TomcatHome+'"','"-Djava.io.tmpdir='+$root+'/tomcat-validation/temp"','-Dfile.encoding=UTF-8','-cp','"'+$TomcatHome+'/bin/bootstrap.jar;'+$TomcatHome+'/bin/tomcat-juli.jar"','org.apache.catalina.startup.Bootstrap','start')
$proc=Start-Process -FilePath $java -ArgumentList $args -WindowStyle Hidden -RedirectStandardOutput (Join-Path $root '.runtime/tomcat-out.log') -RedirectStandardError (Join-Path $root '.runtime/tomcat-error.log') -PassThru
[IO.File]::WriteAllText((Join-Path $root '.runtime/tomcat.pid'),[string]$proc.Id)
if($proc.WaitForExit(1500)){throw 'Tomcat exited during startup. See .runtime/tomcat-error.log.'}
Write-Output ('Tomcat process: '+$proc.Id+'; http://localhost:8081/SmartStudy')
