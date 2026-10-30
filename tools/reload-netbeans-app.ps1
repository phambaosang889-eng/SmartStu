param([string]$TomcatHome='C:/Program Files/Apache Software Foundation/Tomcat 10.1',[string]$BaseUrl='http://localhost:8081')
$ErrorActionPreference='Stop'
# Reuse the already configured local manager identity. Never print or change credentials/roles.
[xml]$users=[IO.File]::ReadAllText((Join-Path $TomcatHome 'conf/tomcat-users.xml'))
$manager=$users.'tomcat-users'.user | Where-Object {($_.roles -split ',').Trim() -contains 'manager-script'} | Select-Object -First 1
if(!$manager){throw 'No existing manager-script account found. Reload SmartStudy from NetBeans instead.'}
$password=ConvertTo-SecureString ([string]$manager.password) -AsPlainText -Force
$credential=[PSCredential]::new([string]$manager.username,$password)
$response=Invoke-WebRequest ($BaseUrl+'/manager/text/reload?path=%2FSmartStudy') -Authentication Basic -Credential $credential -AllowUnencryptedAuthentication -SkipHttpErrorCheck
if($response.StatusCode -ne 200 -or !$response.Content.StartsWith('OK -')){throw ('Application reload failed (HTTP '+$response.StatusCode+'). Use NetBeans Run Project.')}
Write-Output 'SmartStudy reloaded on the existing NetBeans Tomcat; other applications were not restarted.'
