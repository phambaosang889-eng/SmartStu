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
foreach($case in @(@($student,'student'),@($teacher,'teacher'),@($admin,'admin'))){
    $session=$case[0];$role=$case[1];$dashboardPage=ReadPage $session "/$role/dashboard";$nav=[regex]::Match($dashboardPage.Content,'(?s)<nav class="workspace-nav">(.*?)</nav>').Groups[1].Value
    $links=[regex]::Matches($nav,'<a\b[^>]*\bhref="([^"]+)"');Check ($links.Count -gt 5) "$role menu present"
    foreach($link in $links){$path=$link.Groups[1].Value.Replace('/SmartStudy','');$page=ReadPage $session $path;Check ($page.StatusCode -eq 200) "$role menu $path";Check (([regex]::Matches($page.Content,'aria-current="page"')).Count -eq 1) ("$role exactly one active tab $path; count="+([regex]::Matches($page.Content,'aria-current="page"')).Count+"; title="+[regex]::Match($page.Content,'<title>(.*?)</title>').Groups[1].Value);Check ($page.Content -notmatch 'onclick="alert\(') "$role no fake alert $path";Check ($page.Content -notmatch 'Ã¡|á»|�') "$role UTF8 $path"}
}
foreach($path in @('/teacher','/teacher/ai-questions','/teacher/essays','/admin','/admin/users','/%74eacher/ai-questions','/teacher;view=1/ai-questions')){Check ((ReadPage $student $path).StatusCode -eq 403) "student denied $path"}
Check ((ReadPage $teacher '/admin/users').StatusCode -eq 403) 'teacher denied admin'
Check ((ReadPage $admin '/admin/quiz?mode=edit').StatusCode -eq 403) 'admin cannot edit quiz'
Check ((ReadPage $admin '/admin/lesson?mode=edit').StatusCode -eq 403) 'admin cannot edit lesson'
foreach($case in @(@($student,'/student/quiz'),@($teacher,'/teacher/ai-questions'),@($admin,'/admin/users'))){Check ((Post $case[0] $case[1] @{action='invalid'}).StatusCode -eq 403) "missing csrf $($case[1])"}
$page=ReadPage $teacher '/teacher/ai-questions';$csrf=Token $page.Content
Check ((Post $teacher '/teacher/ai-questions' @{csrf=$csrf;action='generate';topic='Định luật Newton';count='99';level='Vận dụng';points='1';mode='demo'}).StatusCode -eq 400) 'AI invalid count returns validation error'
Check ((Post $teacher '/teacher/ai-questions' @{csrf=$csrf;action='generate';count='2';level='Vận dụng';points='1';mode='demo';hardness='Dễ';seconds='60'}).StatusCode -eq 400) 'missing AI topic is validation error, not 500'
$studentCsrf=Token ((ReadPage $student '/student/profile').Content);Check ((Post $student '/student/flashcards' @{csrf=$studentCsrf;action='mark-card';id='card-newton';state='invalid'}).StatusCode -eq 400) 'invalid flashcard state rejected'
$adminCsrf=Token ((ReadPage $admin '/admin/profile').Content);Check ((Post $admin '/admin/users' @{csrf=$adminCsrf;action='save-account';email='invalid@example.vn';name='Invalid';status='active';version='0'}).StatusCode -eq 400) 'missing account role is validation error'
$topicPage=ReadPage $admin '/admin/topics'
$existingTopic=[regex]::Match($topicPage.Content,'<a\s+href="[^"]*/admin/topics\?id=([^"]+)"\s*>\s*Đo lường\s*</a>')
$topicData=@{csrf=$adminCsrf;action='save';id='';version='0';title='Đo lường';body='Sử dụng dụng cụ đo, đơn vị SI và biểu diễn sai số.';topic='Chủ đề';status='published'}
if($existingTopic.Success){$topicEdit=ReadPage $admin ('/admin/topics?mode=edit&id='+$existingTopic.Groups[1].Value);$topicData=Fields (Form $topicEdit.Content 'class="surface editor-form"');$topicData['title']='Đo lường';$topicData['topic']='Chủ đề';$topicData['status']='published'}
$page=Post $admin '/admin/topics' $topicData;Check ($page.StatusCode -eq 200 -and $page.Content -match 'Đo lường') 'admin manages system topic without adding a duplicate on rerun'
Check ((Post $admin '/admin/topics' @{csrf=$adminCsrf;action='save';id='';version='0';title='  ĐO LƯỜNG  ';body='Kiểm tra chống trùng.';topic='Chủ đề';status='published'}).StatusCode -eq 400) 'duplicate topic rejected on server'
$page=ReadPage $teacher '/teacher/courses?mode=edit';Check ($page.Content -match 'Đo lường') 'managed topic offered in course editor'
$page=ReadPage $admin '/admin/quiz';$csrfAdmin=Token ((ReadPage $admin '/admin/profile').Content)
Check ((Post $admin '/admin/quiz' @{csrf=$csrfAdmin;action='copy';id='newton-demo'}).StatusCode -eq 403) 'admin server mutation denied'
$badRange=ReadPage $admin '/admin/reports?from=2026-10-24&to=2026-10-01';Check ($badRange.StatusCode -eq 400 -and $badRange.Content -match 'Khoảng ngày không hợp lệ') 'invalid report range'
$csv=ReadPage $teacher '/teacher/progress?export=csv';Check ($csv.StatusCode -eq 200 -and $csv.Headers.'Content-Type' -like '*text/csv*') 'CSV download'
# Complete real POST workflows against Tomcat; no browser-only mock handlers.
$page=ReadPage $teacher '/teacher/ai-questions';$csrf=Token $page.Content
$page=Post $teacher '/teacher/ai-questions' @{csrf=$csrf;action='generate';mode='demo';topic='Định luật Newton';count='2';level='Vận dụng';hardness='Trung bình';seconds='60';points='1'}
Check ($page.StatusCode -eq 200 -and ([regex]::Matches($page.Content,'name="selected"')).Count -eq 2) 'question generation preview'
$preview=Form $page.Content 'name="selected"';$data=Fields $preview;$qid=[regex]::Match($preview,'name="selected" value="([^"]+)"').Groups[1].Value;$answer=$data['correct_'+$qid];$data['question_'+$qid]+=' (Bài luyện '+[DateTime]::Now.ToString('HH:mm:ss.fff')+').';$data['selected']=$qid;$data['action']='save-selected'
$page=Post $teacher '/teacher/ai-questions' $data;Check ($page.StatusCode -eq 200 -and $page.Content -match 'Đã thêm 1 câu') 'only selected question saved'
$page=ReadPage $teacher '/teacher/quiz?mode=edit';$data=Fields (Form $page.Content 'name="title"');$data['action']='save-quiz';$data['title']='Luyện tập Newton · thang điểm 10';$data['description']='Áp dụng định luật II Newton, xác định hợp lực và đơn vị gia tốc.';$data['course']='course-mechanics';$data['question']=$qid;$data['status']='published';$data['maxScore']='10';$data['maxAttempts']='1';$data['minutes']='10'
$page=Post $teacher '/teacher/quiz' $data;$quizId=QueryId $page 'id';Check ($page.StatusCode -eq 200 -and $quizId) 'quiz created from bank'
$page=ReadPage $student ('/student/quiz?id='+$quizId);$data=Fields (Form $page.Content 'value="start"');$page=Post $student '/student/quiz' $data;$run=QueryId $page 'run';Check ($page.StatusCode -eq 200 -and $run) 'student starts generated quiz'
$answers=Fields (Form $page.Content 'id="quiz-run-form"');$answers['answer_'+$qid]=$answer;$page=Post $student '/student/quiz' $answers;Check ($page.StatusCode -eq 200 -and $page.Content -match '10 / 10 điểm') 'quiz weighted score follows configured scale'
$page=Post $student '/student/quiz' $answers;Check ($page.StatusCode -eq 200 -and $page.Content -match '10 / 10 điểm') 'duplicate quiz POST returns same result'
$page=Post $student '/student/quiz' @{csrf=(Token ((ReadPage $student '/student/profile').Content));action='start';id=$quizId};Check ($page.StatusCode -eq 400 -and $page.Content -match 'hết số lần') 'quiz attempt limit on server'
$page=ReadPage $teacher '/teacher/essays?mode=edit';$data=Fields (Form $page.Content 'value="save-assignment"');$data['title']='Giải thích gia tốc theo định luật Newton';$data['prompt']='Vật 2 kg chịu hợp lực 10 N. Tính gia tốc và giải thích hướng.';$data['instructions']='Nêu công thức, thay số, ghi đơn vị và giải thích kết quả.';$data['reference']='a = F/m = 10/2 = 5 m/s². Gia tốc cùng hướng hợp lực.';$data['rubric']="Kiến thức và công thức | 4`nTính toán, đơn vị và giải thích | 6";$data['status']='published';$data['course']='course-mechanics';$data['action']='save-assignment'
$page=Post $teacher '/teacher/essays' $data;$assignmentId=QueryId $page 'id';Check ($page.StatusCode -eq 200 -and $assignmentId) 'assignment and rubric created'
$page=ReadPage $student ('/student/essays?id='+$assignmentId);$data=Fields (Form $page.Content 'id="essay-form"');$data['answer']='Theo định luật II Newton: a = F/m = 10/2 = 5 m/s². Gia tốc cùng hướng với hợp lực.';$data['action']='draft';$page=Post $student '/student/essays' $data;Check ($page.StatusCode -eq 200 -and $page.Content -match 'Theo định luật II Newton') 'server essay draft saved'
$page=ReadPage $student ('/student/essays?id='+$assignmentId);$submissionData=Fields (Form $page.Content 'id="essay-form"');$submissionData['action']='submit';$page=Post $student '/student/essays' $submissionData;$submissionId=QueryId $page 'submission';Check ($page.StatusCode -eq 200 -and $submissionId) 'essay submitted'
$repeated=Post $student '/student/essays' $submissionData;Check ((QueryId $repeated 'submission') -eq $submissionId) 'essay duplicate POST idempotent'
$page=ReadPage $teacher ('/teacher/essays?submission='+$submissionId);$data=Fields (Form $page.Content 'value="review"');$data['score_0']='4';$data['score_1']='6';$data['feedback']='Em nêu đúng a = F/m = 10/2 = 5 m/s² và giải thích hướng gia tốc cùng hợp lực. Tiếp tục luyện bài có nhiều lực tác dụng.';$data['status']='draft';$page=Post $teacher '/teacher/essays' $data;Check ($page.StatusCode -eq 200) 'teacher rubric grading draft'
$page=ReadPage $student ('/student/essays?submission='+$submissionId);Check ($page.Content -notmatch 'Đáp án tham khảo' -and $page.Content -notmatch '10.0/10.0') 'unpublished score and reference hidden'
$page=ReadPage $teacher ('/teacher/essays?submission='+$submissionId);$data=Fields (Form $page.Content 'value="review"');$data['status']='published';$page=Post $teacher '/teacher/essays' $data;Check ($page.StatusCode -eq 200) 'teacher approves and publishes'
$page=ReadPage $student ('/student/essays?submission='+$submissionId);Check ($page.Content -match '10.0/10.0' -and $page.Content -match 'Đáp án tham khảo') 'student sees only reviewed result'
$csrfStudent=Token ((ReadPage $student '/student/profile').Content);$page=Post $student '/student/lesson' @{csrf=$csrfStudent;action='complete';id='lesson-newton'};Check ($page.Content -match 'Đã ghi nhận hoàn thành') 'lesson completion recorded'
$page=Post $student '/student/flashcards' @{csrf=$csrfStudent;action='mark-card';id='card-newton';state='known'};Check ($page.Content -match '1/3') 'flashcard state recorded'
$page=ReadPage $admin '/admin/users?mode=edit&email=student%40smartstudy.vn';$version=[regex]::Match($page.Content,'name="version" value="(\d+)"').Groups[1].Value;$csrfAdmin=Token $page.Content
$locked=$false
try {
    $r=Post $admin '/admin/users' @{csrf=$csrfAdmin;action='save-account';email='student@smartstudy.vn';name='Minh';role='STUDENT';status='locked';version=$version};Check ($r.StatusCode -eq 200) 'admin locks account';$locked=$true
    $r=ReadPage $student '/student/dashboard';Check ($r.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/login')) 'locked existing session invalidated'
} finally {
    if($locked){$page=ReadPage $admin '/admin/users?mode=edit&email=student%40smartstudy.vn';$version=[regex]::Match($page.Content,'name="version" value="(\d+)"').Groups[1].Value;$r=Post $admin '/admin/users' @{csrf=(Token $page.Content);action='save-account';email='student@smartstudy.vn';name='Minh';role='STUDENT';status='active';version=$version};Check ($r.StatusCode -eq 200) 'student account restored'}
}
$student=Login 'STUDENT'
$page=ReadPage $student '/student/ai-tutor';$tutorCsrf=Token $page.Content
Check ((Post $student '/student/ai-tutor' @{action='new'}).StatusCode -eq 403) 'tutor missing csrf denied'
$page=Post $student '/student/ai-tutor' @{csrf=$tutorCsrf;action='new'};$conversation=QueryId $page 'id'
Check ($page.StatusCode -eq 200 -and $conversation.Length -eq 36) 'tutor conversation persisted'
$composer=Fields (Form $page.Content 'id="tutor-composer"')
$requestId=$composer['requestId']
Check ($requestId -match '^[a-f0-9-]{36}$') 'composer emits a valid request ID'
$historyLink=[regex]::Match($page.Content,'href="([^"]*ai-tutor\?id='+[regex]::Escape($conversation)+'&amp;listPage=\d+)"')
Check ($historyLink.Success) 'history link contains an intact conversation ID'
$historyUrl=[Net.WebUtility]::HtmlDecode($historyLink.Groups[1].Value)
$reopened=Invoke-WebRequest ([uri]::new([uri]$BaseUrl,$historyUrl)) -WebSession $student -SkipHttpErrorCheck
Check ($reopened.StatusCode -eq 200 -and $reopened.Content.Contains('id="tutor-composer"')) 'history link reopens saved conversation'
Check ((Post $student '/student/ai-tutor' @{csrf=$tutorCsrf;action='send';id=$conversation;requestId=$requestId;message=''}).StatusCode -eq 400) 'empty tutor message rejected'
Check ((Post $student '/student/ai-tutor' @{csrf=$tutorCsrf;action='send';id=$conversation;requestId=$requestId;message=('x'*4001)}).StatusCode -eq 400) 'oversized tutor message rejected'
$aiPolicy=ReadPage $admin '/admin/ai-settings'
$aiWasEnabled=$aiPolicy.Content -match 'name="enabled"\s+checked'
$aiLimit=[regex]::Match($aiPolicy.Content,'name="limit" value="(\d+)"').Groups[1].Value
try {
if($aiWasEnabled){$policyResult=Post $admin '/admin/ai-settings' @{csrf=(Token $aiPolicy.Content);limit=$aiLimit};Check ($policyResult.StatusCode -eq 200) 'pause remote AI for deterministic regression without API charges'}
$page=Post $student '/student/ai-tutor' @{csrf=$tutorCsrf;action='send';id=$conversation;requestId=$requestId;message='Giải thích lực và phản lực. <script>alert(1)</script>'}
Check ($page.StatusCode -eq 200) 'tutor send handled without provider configuration'
Check ($page.Content -match '&lt;script&gt;alert\(1\)&lt;/script&gt;') 'tutor user content escaped'
Check ($page.Content -match 'Gửi\s+lại câu hỏi') 'unconfigured tutor shows retry'
Check (([regex]::Matches($page.Content,'class="tutor-message ')).Count -eq 2) 'tutor one message pair'
$retry=Fields (Form $page.Content 'Gửi\s+lại câu hỏi');$page=Post $student '/student/ai-tutor' $retry
Check (([regex]::Matches($page.Content,'class="tutor-message ')).Count -eq 2) 'tutor retry does not duplicate messages'
} finally {
if($aiWasEnabled){$policyResult=Post $admin '/admin/ai-settings' @{csrf=(Token ((ReadPage $admin '/admin/ai-settings').Content));enabled='on';limit=$aiLimit};if($policyResult.StatusCode -ne 200){throw 'Failed to restore original AI enabled policy'};Check ($policyResult.StatusCode -eq 200) 'original AI policy restored'}
}
Check ((ReadPage $teacher ('/teacher/ai-tutor?id='+$conversation)).StatusCode -eq 404) 'teacher cannot read student conversation'
Check ((ReadPage $admin ('/admin/ai-tutor?id='+$conversation)).StatusCode -eq 404) 'admin cannot read private conversation'
$page=Post $student '/student/ai-tutor' @{csrf=$tutorCsrf;action='hide';id=$conversation}
Check ($page.StatusCode -eq 200) 'tutor conversation soft hidden'
Check ((ReadPage $student ('/student/ai-tutor?id='+$conversation)).StatusCode -eq 404) 'hidden conversation unavailable'
Check ((ReadPage $student '/logout').StatusCode -eq 403) 'logout without csrf denied'
$page=ReadPage $student ('/logout?csrf='+$tutorCsrf)
Check ($page.Content -match 'đăng xuất thành công') 'logout invalidates session'
$page=ReadPage $student '/student/dashboard'
Check ($page.BaseResponse.RequestMessage.RequestUri.AbsolutePath.EndsWith('/login')) 'logged-out session cannot access dashboard'
Write-Output "PASS $script:checks HTTP checks: roles, menus, CSRF, quiz/essay, tutor privacy/retry/XSS, logout, account lock and CSV."
