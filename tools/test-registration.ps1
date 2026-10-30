param([string]$BaseUrl='http://localhost:8081/SmartStudy')
$ErrorActionPreference='Stop';$checks=0
function Check($ok,$label){$script:checks++;if(!$ok){throw "FAIL: $label"}}
function Token($page){[regex]::Match($page.Content,'name="csrf" value="([^"]+)"').Groups[1].Value}
function Post($session,$path,$body){Invoke-WebRequest ($BaseUrl+$path) -Method Post -WebSession $session -Body $body -ContentType 'application/x-www-form-urlencoded; charset=UTF-8' -SkipHttpErrorCheck}
$login=Invoke-WebRequest ($BaseUrl+'/login') -SessionVariable session
Check ($login.Content -match '/register">Tạo tài khoản học sinh') 'login links to registration'
$page=Invoke-WebRequest ($BaseUrl+'/register') -WebSession $session
Check ($page.StatusCode -eq 200 -and $page.Content -match 'name="confirmPassword"') 'registration page available to visitors'
$csrf=Token $page;$email='register-'+[Guid]::NewGuid().ToString('N')+'@smartstu.test';$password='Local-Registration-QA-2026!'
$body=@{csrf=$csrf;name='Học sinh kiểm thử';email=$email;password=$password;confirmPassword=$password}
Check ((Post $session '/register' @{name='Test';email=$email;password=$password;confirmPassword=$password}).StatusCode -eq 403) 'registration requires csrf'
foreach($case in @(@{field='name';value=''},@{field='email';value='not-an-email'},@{field='password';value='short'},@{field='confirmPassword';value='does-not-match'})){
 $invalid=$body.Clone();$invalid[$case.field]=$case.value;$response=Post $session '/register' $invalid
 Check ($response.StatusCode -eq 400) ('server rejects invalid '+$case.field)
 Check ($response.Content -notmatch 'value="Local-Registration-QA-2026!"') 'password not echoed in validation response'
}
foreach($role in @('ADMIN','TEACHER')){$invalid=$body.Clone();$invalid.role=$role;Check ((Post $session '/register' $invalid).StatusCode -eq 400) ('cannot self-register as '+$role)}
$response=Post $session '/register' $body
Check ($response.StatusCode -eq 200 -and $response.Content -match 'Tạo tài khoản thành công') 'real account created and success shown on login'
Check ($response.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/login')) 'registration redirects to login after commit'
$csrf=Token $response;$body.csrf=$csrf
Check ((Post $session '/register' $body).StatusCode -eq 400) 'duplicate email rejected'
$duplicate=$body.Clone();$duplicate.email=$email.ToUpper();Check ((Post $session '/register' $duplicate).StatusCode -eq 400) 'case variant duplicate rejected'
Check ((Post $session '/login' @{csrf=$csrf;email=$email;password='wrong-password';role='STUDENT'}).Content -match 'không đúng') 'wrong password rejected'
$response=Post $session '/login' @{csrf=$csrf;email=$email;password=$password;role='ADMIN'}
Check ($response.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/login')) 'student cannot choose admin at login'
$response=Post $session '/login' @{csrf=$csrf;email=$email;password=$password;role='STUDENT'}
Check ($response.StatusCode -eq 200 -and $response.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/student/dashboard')) 'new account signs in to real student dashboard'
Check ($response.Content -match 'Học sinh kiểm thử') 'Vietnamese name persisted correctly'
Check ((Invoke-WebRequest ($BaseUrl+'/admin/users') -WebSession $session -SkipHttpErrorCheck).StatusCode -eq 403) 'registered account cannot access admin'
Check ((Invoke-WebRequest ($BaseUrl+'/teacher/courses') -WebSession $session -SkipHttpErrorCheck).StatusCode -eq 403) 'registered account cannot access teacher'
$profile=Invoke-WebRequest ($BaseUrl+'/student/profile') -WebSession $session
Check ($profile.Content -match $email) 'new account profile loaded from database'
$snapshotPath=Join-Path (Split-Path $PSScriptRoot) '.runtime/registered-qa-email.txt'
[IO.File]::AppendAllText($snapshotPath,$email+"`n")
Write-Output "PASS $checks registration/login checks; QA email recorded privately for persistence verification."
