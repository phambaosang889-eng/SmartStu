param([string]$JavaHome='C:/Program Files/Java/jdk-17.0.20',[string]$TomcatHome='C:/Program Files/Apache Software Foundation/Tomcat 10.1')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$classes=Join-Path $root 'build/web/WEB-INF/classes'
if(!(Test-Path -LiteralPath $classes)){throw 'Build the project with Ant before compiling checks.'}
$output=Join-Path $root '.runtime/test-classes'
[IO.Directory]::CreateDirectory($output) | Out-Null
$sources=@(Get-ChildItem -LiteralPath (Join-Path $root 'test') -Filter '*.java' -File | ForEach-Object FullName)
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -cp "$classes;$root/web/WEB-INF/lib/*;$TomcatHome/lib/servlet-api.jar" -d $output $sources
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
Write-Output 'Checks compiled into .runtime/test-classes; production classes remain separate.'
