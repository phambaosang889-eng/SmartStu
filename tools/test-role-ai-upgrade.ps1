param([string]$BaseUrl='http://localhost:8081/SmartStudy',[string]$Password=$env:SMARTSTU_TEST_PASSWORD)
$ErrorActionPreference='Stop'
if([string]::IsNullOrWhiteSpace($Password)){throw 'Supply -Password or SMARTSTU_TEST_PASSWORD for the local fixture.'}
$script:checks=0
function Check($ok,[string]$label){$script:checks++;if(-not $ok){throw "FAIL: $label"}}
function ReadPage($session,[string]$path){Invoke-WebRequest -Uri ($BaseUrl+$path) -WebSession $session -SkipHttpErrorCheck}
function Token([string]$html){$m=[regex]::Match($html,'name="csrf"\s+value="([^"]+)"');if(-not $m.Success){throw 'CSRF token absent'};$m.Groups[1].Value}
function Post($session,[string]$path,$body){Invoke-WebRequest -Uri ($BaseUrl+$path) -Method Post -WebSession $session -Body $body -ContentType 'application/x-www-form-urlencoded; charset=UTF-8' -SkipHttpErrorCheck}
function Form([string]$html,[string]$needle){foreach($m in [regex]::Matches($html,'(?s)<form\b[^>]*>.*?</form>')){if($m.Value -match $needle){return $m.Value}};throw "Form not found: $needle"}
function Fields([string]$form){
    $fields=@{}
    foreach($m in [regex]::Matches($form,'<input\b[^>]*>')){$tag=$m.Value;if($tag -match 'type="(?:checkbox|radio|submit)"'){continue};$name=[regex]::Match($tag,'\bname="([^"]+)"');if($name.Success){$value=[regex]::Match($tag,'\bvalue="([^"]*)"').Groups[1].Value;$fields[$name.Groups[1].Value]=[Net.WebUtility]::HtmlDecode($value)}}
    foreach($m in [regex]::Matches($form,'(?s)<textarea\b[^>]*name="([^"]+)"[^>]*>(.*?)</textarea>')){$fields[$m.Groups[1].Value]=[Net.WebUtility]::HtmlDecode($m.Groups[2].Value)}
    foreach($m in [regex]::Matches($form,'(?s)<select\b[^>]*name="([^"]+)"[^>]*>(.*?)</select>')){$options=[regex]::Matches($m.Groups[2].Value,'(?s)<option\b([^>]*)>(.*?)</option>');$option=$options | Where-Object {$_.Groups[1].Value -match '\bselected\b'} | Select-Object -First 1;if(-not $option){$option=$options[0]};$value=[regex]::Match($option.Groups[1].Value,'value="([^"]*)"');$fields[$m.Groups[1].Value]=[Net.WebUtility]::HtmlDecode($(if($value.Success){$value.Groups[1].Value}else{$option.Groups[2].Value}))}
    return $fields
}
function QueryId($response,[string]$name){[regex]::Match($response.BaseResponse.RequestMessage.RequestUri.Query,"(?:\?|&)$name=([^&]+)").Groups[1].Value}
function Login([string]$role){$loginPage=Invoke-WebRequest -Uri ($BaseUrl+'/login') -SessionVariable session -SkipHttpErrorCheck;$r=Post $session '/login' @{email=($role.ToLower()+'@smartstudy.vn');password=$Password;role=$role;csrf=(Token $loginPage.Content)};Check ($r.StatusCode -eq 200 -and $r.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/'+$role.ToLower()+'/dashboard')) "login $role";return $session}
$student=Login 'STUDENT';$teacher=Login 'TEACHER';$admin=Login 'ADMIN'
$page=ReadPage $admin '/admin/dashboard'
Check ($page.StatusCode -eq 200 -and $page.Content.Contains('Tổng quan hệ thống')) 'system dashboard'
foreach($path in @('courses','quiz','lesson','flashcards','reports','ai-tutor','teachers')){Check ((ReadPage $admin ('/admin/'+$path)).StatusCode -eq 403) ('admin excludes '+$path)}
$settings=ReadPage $admin '/admin/ai-settings'
Check ($settings.Content.Contains('name="model"') -and $settings.Content.Contains('name="provider"')) 'provider and model controls'
$policy=Fields (Form $settings.Content 'name="provider"');if($settings.Content -match 'name="enabled"\s+checked'){$policy['enabled']='on'}
$model=$policy['model'];$policy['model']='invalid/model'
Check ((Post $admin '/admin/ai-settings' $policy).StatusCode -eq 400) 'invalid model rejected'
$policy['model']=$model
Check ((Post $admin '/admin/ai-settings' $policy).StatusCode -eq 200) 'existing model and provider persisted'
$page=ReadPage $student '/student/dashboard'
Check ($page.Content -match '(?s)/quiz".*?/essays".*?/flashcards"') 'quiz and essays adjacent'
$page=ReadPage $student '/student/essays?id=newton';$csrf=Token $page.Content
$token=[regex]::Match($page.Content,'name="token"\s+value="([^"]+)"').Groups[1].Value
$result=Post $student '/student/essays' @{csrf=$csrf;action='submit';id='newton';token=$token;answer='Theo định luật II Newton, a = F nhân m. Với F = 10 N và m = 2 kg, gia tốc là 20 m/s². Đây là phép tính em sử dụng để tìm gia tốc.'}
$id=QueryId $result 'submission'
Check ($id.Length -eq 36 -and $result.Content.Contains('form.requestSubmit()')) 'submission triggers automatic AI feedback'
$graded=Post $student '/student/essays' @{csrf=$csrf;action='student-feedback';submission=$id}
Check ($graded.StatusCode -eq 200 -and $graded.Content.Contains('Điểm AI đề xuất:')) 'real AI grades student submission'
Check ($graded.Content -match 'chia|F/m|F / m') 'real AI explains incorrect calculation'
$teacherPage=ReadPage $teacher ('/teacher/essays?submission='+$id)
Check ($teacherPage.Content.Contains('Đề xuất AI:') -and $teacherPage.Content.Contains('name="score_0"')) 'teacher can inspect and edit AI grading'
Check (!$graded.Content.Contains('Kết quả giáo viên đã công bố')) 'AI feedback is separate from final grade'
$review=Fields (Form $teacherPage.Content 'name="score_0"')
$review['score_0']='1';$review['score_1']='1';$review['score_2']='1';$review['feedback']='Giáo viên đã kiểm tra bài mẫu: cần dùng a = F/m, không nhân F với m. Điểm được điều chỉnh sau khi xem xét nhận xét AI.';$review['status']='draft'
$saved=Post $teacher '/teacher/essays' $review
Check ($saved.StatusCode -eq 200) 'teacher edits AI grade as private draft'
$studentResult=ReadPage $student ('/student/essays?submission='+$id)
Check (!$studentResult.Content.Contains('Kết quả giáo viên đã công bố')) 'teacher draft not published accidentally'
$review=Fields (Form $saved.Content 'name="score_0"');$review['status']='published'
$published=Post $teacher '/teacher/essays' $review
Check ($published.StatusCode -eq 200) 'teacher publishes adjusted grade'
$studentResult=ReadPage $student ('/student/essays?submission='+$id)
Check ($studentResult.Content.Contains('Kết quả giáo viên đã công bố') -and $studentResult.Content.Contains('Giáo viên đã kiểm tra bài mẫu')) 'student receives teacher-corrected final feedback'
Write-Output "PASS $script:checks role/model/instant-feedback HTTP checks; test submission $id"