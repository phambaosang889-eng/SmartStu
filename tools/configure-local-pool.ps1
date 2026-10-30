$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$config=Join-Path $root '.runtime/db.local.properties'
$properties=@{}
foreach($line in [IO.File]::ReadAllLines($config)){if($line -match '^([^#=]+)=(.*)$'){$properties[$matches[1]]=$matches[2]}}
$driver=Join-Path $root 'web/WEB-INF/lib/mysql-connector-j-8.4.0.jar'
$poolLib=Join-Path $root 'tomcat-validation/lib'
New-Item -ItemType Directory -Force $poolLib | Out-Null
Copy-Item -LiteralPath $driver -Destination $poolLib -Force
$escape={param($value)[Security.SecurityElement]::Escape($value)}
$docBase=& $escape (Join-Path $root 'build/web')
$url=& $escape $properties['SMARTSTU_DB_URL']
$user=& $escape $properties['SMARTSTU_DB_USER']
$secret=& $escape $properties['SMARTSTU_DB_PASSWORD']
$xml=@"
<Context docBase="$docBase" reloadable="true">
 <Resource name="jdbc/SmartStu" auth="Container" type="javax.sql.DataSource"
 factory="org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory" driverClassName="com.mysql.cj.jdbc.Driver"
 url="$url" username="$user" password="$secret" maxTotal="12" maxIdle="6" minIdle="1"
 maxWaitMillis="5000" validationQuery="SELECT 1" testOnBorrow="true" />
</Context>
"@
[IO.File]::WriteAllText((Join-Path $root 'tomcat-validation/conf/Catalina/localhost/SmartStudy.xml'),$xml,[Text.UTF8Encoding]::new($false))
if(!$properties.ContainsKey('SMARTSTU_DB_JNDI')){[IO.File]::AppendAllText($config,"SMARTSTU_DB_JNDI=java:comp/env/jdbc/SmartStu`n")}
Write-Output 'Local Tomcat pool configured; restart the local Tomcat process.'
