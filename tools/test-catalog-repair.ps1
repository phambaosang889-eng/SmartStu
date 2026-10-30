param([string]$BaseUrl='http://localhost:8081/SmartStudy')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$password=([IO.File]::ReadAllLines((Join-Path $root '.runtime/db.local.properties')) | Where-Object {$_.StartsWith('SMARTSTU_SEED_PASSWORD=')}).Substring('SMARTSTU_SEED_PASSWORD='.Length)
$script:checks=0
function Check($condition,$label){if(!$condition){throw "FAIL $label"};$script:checks++}
function Token($html){[regex]::Match($html,'name="csrf" value="([^"]+)"').Groups[1].Value}
function GetPage($session,$path){Invoke-WebRequest ($BaseUrl+$path) -WebSession $session -SkipHttpErrorCheck}
function Login($role){
 $login=Invoke-WebRequest ($BaseUrl+'/login') -SessionVariable session
 $response=Invoke-WebRequest ($BaseUrl+'/login') -WebSession $session -Method Post -Body @{email=($role.ToLower()+'@smartstudy.vn');password=$password;role=$role;csrf=(Token $login.Content)} -SkipHttpErrorCheck
 Check ($response.StatusCode -eq 200 -and $response.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/'+$role.ToLower()+'/dashboard')) "login $role"
 return $session
}
$admin=Login 'ADMIN';$teacher=Login 'TEACHER';$student=Login 'STUDENT'
$topics=GetPage $admin '/admin/topics'
Check ($topics.StatusCode -eq 200) 'topic list loads'
Check ($topics.Content.Contains('<option value="active" selected>Đang sử dụng</option>')) 'active topics selected by default'
$cards=[regex]::Matches($topics.Content,'(?s)<article class="resource-card".*?</article>')
$active=@($cards | Where-Object {$_.Value -match '>Đo lường</a>' -and $_.Value -match 'data-state="published"'})
Check ($active.Count -eq 1) 'exactly one published measurement topic'
$archived=@($cards | Where-Object {$_.Value -match '>Đo lường</a>' -and $_.Value -match 'data-state="archived"'})
Check (@($archived | Where-Object {$_.Value -notmatch 'data-state="archived" hidden'}).Count -eq 0) 'archived duplicates hidden before JavaScript runs'
$csrf=Token $topics.Content
for($i=0;$i -lt 2;$i++){
 $duplicate=Invoke-WebRequest ($BaseUrl+'/admin/topics') -WebSession $admin -Method Post -Body @{csrf=$csrf;action='save';id='';version='0';title='  ĐO LƯỜNG  ';body='Kiểm tra chống trùng';topic='Chủ đề';status='published'} -ContentType 'application/x-www-form-urlencoded; charset=UTF-8' -SkipHttpErrorCheck
 Check ($duplicate.StatusCode -eq 400 -and $duplicate.Content.Contains('đã tồn tại')) 'duplicate topic rejected with useful message'
}
$again=GetPage $admin '/admin/topics'
Check (([regex]::Matches($again.Content,'data-state="published"[^>]*>.*?Đo lường','Singleline')).Count -ge 1) 'original topic retained'
Check (([regex]::Matches($again.Content,'<article class="resource-card"')).Count -eq $cards.Count) 'repeated rejected POST did not add records'
$editor=GetPage $teacher '/teacher/lesson?mode=edit&id=lesson-motion'
Check ($editor.StatusCode -eq 200) 'teacher lesson editor loads'
Check ($editor.Content.Contains('id="lesson-video-manager"')) 'video manager available'
Check (([regex]::Matches($editor.Content,'(?s)<form class="surface editor-form".*?</form>')).Count -eq 1) 'single closed lesson editor form'
Check (([regex]::Matches($editor.Content,'class="video-add-form"')).Count -eq 2) 'two separate video forms retained'
Check ($editor.Content.Contains('refinement.css?v=20261028-editor-2')) 'new layout stylesheet requested'
$css=Invoke-WebRequest ($BaseUrl+'/assets/css/refinement.css?v=20261028-editor-2')
Check ($css.Content.Contains('.workspace-content > .video-manager{width:100%;max-width:920px;margin-left:auto;margin-right:auto;')) 'video and lesson editors share width and centered alignment'
Check ($css.Content.Contains('font-weight:400;font-size:15px;line-height:1.65')) 'editor input text has regular weight'
Check ((GetPage $student '/teacher/lesson?mode=edit&id=lesson-motion').StatusCode -eq 403) 'student denied teacher editor'
Write-Output "PASS $script:checks catalog repair HTTP checks; no records added or deleted."
