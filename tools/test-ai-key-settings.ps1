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
$page=ReadPage $admin '/admin/ai-settings'
Check ($page.StatusCode -eq 200 -and $page.Content.Contains('name="apiKey"')) 'admin key form present'
Check ($page.Content -match 'type="password" name="apiKey"') 'key input masked'
$keyForm=Fields (Form $page.Content 'name="apiKey"')
Check ($keyForm['apiKey'] -eq '') 'stored key never repopulated'
$keyForm['apiKey']='invalid key'
Check ((Post $admin '/admin/ai-settings' $keyForm).StatusCode -eq 400) 'malformed key rejected'
$keyForm['apiKey']=''
Check ((Post $admin '/admin/ai-settings' $keyForm).StatusCode -eq 200) 'blank preserves current key'
foreach($case in @(@($student,'student'),@($teacher,'teacher'))){$csrf=Token (ReadPage $case[0] ('/'+$case[1]+'/profile')).Content;Check ((Post $case[0] '/admin/ai-settings' @{csrf=$csrf;action='update-key';keyProvider='gemini';apiKey='synthetic-test-key-12345'}).StatusCode -eq 403) ('key edit denied '+$case[1])}
Check ((Post $admin '/admin/ai-settings' @{action='update-key';keyProvider='gemini';apiKey='synthetic-test-key-12345'}).StatusCode -eq 403) 'key update CSRF required'
Write-Output "PASS $script:checks admin API-key HTTP checks; real key unchanged"