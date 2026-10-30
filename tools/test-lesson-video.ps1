param([string]$BaseUrl='http://localhost:8081/SmartStudy')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$password=([IO.File]::ReadAllLines((Join-Path $root '.runtime/db.local.properties')) | Where-Object {$_.StartsWith('SMARTSTU_SEED_PASSWORD=')}).Substring('SMARTSTU_SEED_PASSWORD='.Length)
$checks=0
function Check($ok,$label){$script:checks++;if(!$ok){throw "FAIL video: $label"}}
function Token($html){[regex]::Match($html,'name="csrf" value="([^"]+)"').Groups[1].Value}
function Login($role){$page=Invoke-WebRequest ($BaseUrl+'/login') -SessionVariable session;$page=Invoke-WebRequest ($BaseUrl+'/login') -WebSession $session -Method Post -Body @{csrf=(Token $page.Content);email=($role.ToLower()+'@smartstudy.vn');password=$password;role=$role};return $session}
$teacher=Login 'TEACHER';$student=Login 'STUDENT'
$page=Invoke-WebRequest ($BaseUrl+'/teacher/lesson?mode=edit&id=lesson-newton') -WebSession $teacher
$csrf=Token $page.Content
Check ($page.Content -match 'Tải video của bạn lên' -and $page.Content -match 'multipart/form-data') 'teacher upload form exists'
Check ($page.Content -notmatch 'bÃ |giáº') 'video form UTF-8 encoding'
Check ((Invoke-WebRequest ($BaseUrl+'/teacher/lesson-video') -WebSession $student -Method Post -Body @{action='upload-video'} -SkipHttpErrorCheck).StatusCode -eq 403) 'student cannot upload'
Check ((Invoke-WebRequest ($BaseUrl+'/teacher/lesson-video') -WebSession $teacher -Method Post -Body @{lesson='lesson-newton';action='link-video'} -SkipHttpErrorCheck).StatusCode -eq 403) 'CSRF required'
$page=Invoke-WebRequest ($BaseUrl+'/teacher/lesson-video') -WebSession $teacher -Method Post -Body @{csrf=$csrf;lesson='lesson-newton';action='link-video';title='Invalid';url='javascript:alert(1)';source='QA'} -SkipHttpErrorCheck
Check ($page.StatusCode -eq 400) 'unsafe video link rejected'
$invalid=Join-Path $root '.runtime/invalid-video-qa.mp4';[IO.File]::WriteAllText($invalid,'<script>invalid file</script>')
$page=Invoke-WebRequest ($BaseUrl+'/teacher/lesson-video') -WebSession $teacher -Method Post -Form @{csrf=$csrf;lesson='lesson-newton';action='upload-video';title='Invalid file';rights='on';video=(Get-Item $invalid)} -SkipHttpErrorCheck
Check ($page.StatusCode -eq 400) 'MP4 extension spoof rejected'
$fixture=Join-Path $root '.runtime/video-qa.mp4';if(!(Test-Path $fixture)){throw 'Download the MDN test-only video fixture first; see docs/LESSON-VIDEOS.md'}
$title='QA phát video '+[guid]::NewGuid().ToString()
$page=Invoke-WebRequest ($BaseUrl+'/teacher/lesson-video') -WebSession $teacher -Method Post -Form @{csrf=$csrf;lesson='lesson-newton';action='upload-video';title=$title;rights='on';video=(Get-Item $fixture)} -SkipHttpErrorCheck
Check ($page.StatusCode -eq 200 -and $page.Content -match 'Đã cập nhật video') 'real multipart MP4 upload succeeds'
$id=[regex]::Match($page.Content,[regex]::Escape($title)+'(?s:.*?)name="id" value="([a-f0-9-]{36})"').Groups[1].Value
Check ($id.Length -eq 36) 'upload metadata returned'
[IO.File]::WriteAllText((Join-Path $root '.runtime/video-qa-id.txt'),$id)
$watch=$BaseUrl+'/media/lesson-video?id='+$id
$full=Invoke-WebRequest $watch -WebSession $student -SkipHttpErrorCheck
Check ($full.StatusCode -eq 200 -and $full.RawContentLength -eq (Get-Item $fixture).Length) 'enrolled student reads exact uploaded bytes'
$range=Invoke-WebRequest $watch -WebSession $student -Headers @{Range='bytes=0-99'} -SkipHttpErrorCheck
Check ($range.StatusCode -eq 206 -and $range.RawContentLength -eq 100) 'video seeking range returns 206'
Check ($range.Headers['Content-Range'] -match '^bytes 0-99/') 'Content-Range correct'
Check ((Invoke-WebRequest $watch -WebSession $student -Headers @{Range='bytes=999999999-'} -SkipHttpErrorCheck).StatusCode -eq 416) 'invalid range rejected'
$suffix=Invoke-WebRequest $watch -WebSession $student -Headers @{Range='bytes=-50'} -SkipHttpErrorCheck
Check ($suffix.StatusCode -eq 206 -and $suffix.RawContentLength -eq 50) 'suffix range works'
$head=Invoke-WebRequest $watch -Method Head -WebSession $student -SkipHttpErrorCheck
Check ($head.StatusCode -eq 200 -and ($head.Headers['Accept-Ranges'] -join ',') -eq 'bytes') 'HEAD supports browser seeking'
$page=Invoke-WebRequest ($BaseUrl+'/student/lesson?id=lesson-newton') -WebSession $student
Check ($page.Content -match ('media/lesson-video\?id='+$id)) 'student player attached to lesson'
Check ($page.Content -match 'youtube-nocookie.com/embed/gFJgWqZV-aQ') 'curated source embedded'
Write-Output "PASS $checks video HTTP/upload/range checks; uploaded QA clip retained temporarily for browser playback, ID stored privately."
