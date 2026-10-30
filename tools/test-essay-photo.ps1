param([string]$BaseUrl='http://localhost:8081/SmartStudy',[switch]$LiveAI)
$ErrorActionPreference='Stop';$root=Split-Path $PSScriptRoot;$script:checks=0
$secret=([IO.File]::ReadAllLines((Join-Path $root '.runtime/db.local.properties')) | Where-Object {$_.StartsWith('SMARTSTU_SEED_PASSWORD=')}).Substring('SMARTSTU_SEED_PASSWORD='.Length)
function Check($ok,$label){if(!$ok){throw "FAIL $label"};$script:checks++}
function Token($html){[regex]::Match($html,'name="csrf" value="([^"]+)"').Groups[1].Value}
function GetPage($session,$path){Invoke-WebRequest ($BaseUrl+$path) -WebSession $session -SkipHttpErrorCheck}
function Post($session,$path,$body){Invoke-WebRequest ($BaseUrl+$path) -WebSession $session -Method Post -Body $body -ContentType 'application/x-www-form-urlencoded; charset=UTF-8' -SkipHttpErrorCheck}
function Login($role){$page=Invoke-WebRequest ($BaseUrl+'/login') -SessionVariable session;$r=Post $session '/login' @{csrf=(Token $page.Content);role=$role;email=($role.ToLower()+'@smartstudy.vn');password=$secret};Check ($r.StatusCode -eq 200 -and $r.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/'+$role.ToLower()+'/dashboard')) "login $role";return $session}
$student=Login 'STUDENT';$teacher=Login 'TEACHER';$admin=Login 'ADMIN'
$page=GetPage $student '/student/essays?id=newton';$csrf=Token $page.Content
Check ($page.StatusCode -eq 200 -and $page.Content.Contains('essay-photo-panel')) 'photo workflow UI present'
$image=Get-Item (Join-Path $root '.runtime/essay-photo-qa.png')
$repeatPath=Join-Path $root '.runtime/essay-photo-qa-repeat.png';[IO.File]::WriteAllBytes($repeatPath,[IO.File]::ReadAllBytes($image.FullName));$repeatImage=Get-Item $repeatPath
$bad=Post $student '/student/essay-photo' @{action='upload';assignment='newton'}
Check ($bad.StatusCode -eq 403) 'upload requires CSRF'
$fakePath=Join-Path $root '.runtime/essay-invalid.jpg';[IO.File]::WriteAllText($fakePath,'<svg onload="alert(1)"></svg>')
$bad=Invoke-WebRequest ($BaseUrl+'/student/essay-photo') -WebSession $student -Method Post -Form @{csrf=$csrf;action='upload';assignment='newton';photoFile=(Get-Item $fakePath)} -SkipHttpErrorCheck
Check ($bad.StatusCode -eq 400) 'spoofed image rejected'
$uploaded=Invoke-WebRequest ($BaseUrl+'/student/essay-photo') -WebSession $student -Method Post -Form @{csrf=$csrf;action='upload';assignment='newton';draftAnswer='';photoFile=$image} -SkipHttpErrorCheck
Check ($uploaded.StatusCode -eq 200) 'upload stores image'
$photo=[regex]::Match($uploaded.Content,'/media/essay-photo\?id=([a-f0-9-]+)').Groups[1].Value;Check (![string]::IsNullOrWhiteSpace($photo)) 'private image ID returned'
$repeat=Invoke-WebRequest ($BaseUrl+'/student/essay-photo') -WebSession $student -Method Post -Form @{csrf=$csrf;action='upload';assignment='newton';draftAnswer='';photoFile=$repeatImage} -SkipHttpErrorCheck
Check ($repeat.StatusCode -eq 200 -and $repeat.Content.Contains('/media/essay-photo?id='+$photo)) 'same draft image upload is idempotent'
$media=GetPage $student ('/media/essay-photo?id='+$photo);Check ($media.StatusCode -eq 200 -and $media.Headers.'Content-Type' -like '*image/jpeg*') 'owner can view JPEG'
Check ($media.Headers.'Cache-Control' -like '*no-store*') 'private image not cached'
Check ((GetPage $teacher ('/media/essay-photo?id='+$photo)).StatusCode -eq 404) 'teacher cannot read unsubmitted photo'
Check ((GetPage $admin ('/media/essay-photo?id='+$photo)).StatusCode -eq 404) 'admin cannot read private photo'
Check ((Post $student '/student/essay-photo' @{csrf=$csrf;action='scan';photo=$photo}).StatusCode -eq 400) 'AI scan requires consent'
if($LiveAI){
 $scan=Post $student '/student/essay-photo' @{csrf=$csrf;action='scan';photo=$photo;aiConsent='yes';draftAnswer=''}
 if($scan.StatusCode -ne 200){$scan.Content | Set-Content (Join-Path $root '.runtime/essay-ocr-error.html') -Encoding utf8; Write-Output "OCR HTTP $($scan.StatusCode)"}
 Check ($scan.StatusCode -eq 200 -and $scan.Content.Contains('Nội dung AI đọc được')) 'Gemini OCR returns saved transcription'
 $text=[Net.WebUtility]::HtmlDecode([regex]::Match($scan.Content,'(?s)<textarea class="essay-ocr-text"[^>]*>(.*?)</textarea>').Groups[1].Value)
 Check ($text -match '20' -and $text -match '10') 'OCR retains deliberately incorrect calculation rather than solving it'
}
$page=GetPage $student '/student/essays?id=newton';$token=[regex]::Match($page.Content,'name="token" value="([^"]+)"').Groups[1].Value
$result=Post $student '/student/essays' @{csrf=$csrf;action='submit';id='newton';token=$token;answer=''}
Check ($result.StatusCode -eq 200) 'photo-only submission accepted'
$submission=[regex]::Match($result.BaseResponse.RequestMessage.RequestUri.Query,'submission=([^&]+)').Groups[1].Value;Check (![string]::IsNullOrWhiteSpace($submission)) 'submission ID'
Check ($result.Content.Contains('/media/essay-photo?id='+$photo)) 'submitted work retains photo'
$again=Post $student '/student/essays' @{csrf=$csrf;action='submit';id='newton';token=$token;answer=''}
Check ($again.BaseResponse.RequestMessage.RequestUri.Query.Contains($submission)) 'repeated submit creates no duplicate'
Check ((Post $student '/student/essay-photo' @{csrf=$csrf;action='remove';photo=$photo}).StatusCode -eq 400) 'submitted photo cannot be removed'
Check ((GetPage $teacher ('/media/essay-photo?id='+$photo)).StatusCode -eq 200) 'assigned teacher can view submitted photo'
$teacherPage=GetPage $teacher ('/teacher/essays?submission='+$submission);$teacherCsrf=Token $teacherPage.Content
if($LiveAI){
 $graded=Post $teacher '/teacher/essays' @{csrf=$teacherCsrf;action='ai-review';submission=$submission}
 Check ($graded.StatusCode -eq 200 -and $graded.Content.Contains('Đề xuất AI:')) 'Gemini suggests rubric scores and feedback'
 Check ($graded.Content -match 'F/m|F / m|chia|phép chia') 'feedback identifies the incorrect multiply/divide step'
}
$studentResult=GetPage $student ('/student/essays?submission='+$submission)
Check (!$studentResult.Content.Contains('Đề xuất AI:') -and !$studentResult.Content.Contains('Kết quả giáo viên đã công bố')) 'AI grade remains private until teacher approval'
@{photo=$photo;submission=$submission;liveAI=[bool]$LiveAI} | ConvertTo-Json | Set-Content (Join-Path $root '.runtime/essay-photo-qa-result.json') -Encoding utf8
Write-Output "PASS $script:checks essay-photo HTTP checks. Synthetic submission retained for visual review: $submission"
