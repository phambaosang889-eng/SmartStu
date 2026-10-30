document.querySelectorAll('.tutor-send').forEach(form=>form.addEventListener('submit',event=>{
  if(form.dataset.sending==='true'){event.preventDefault();return;}
  form.dataset.sending='true';
  form.querySelectorAll('button[type="submit"]').forEach(button=>{button.dataset.originalLabel=button.textContent;button.disabled=true;button.textContent='\u0110ang x\u1eed l\u00fd\u2026';});
  const status=form.querySelector('.tutor-loading');if(status)status.hidden=false;
}));
window.addEventListener('pageshow',()=>document.querySelectorAll('.tutor-send').forEach(form=>{
  delete form.dataset.sending;
  form.querySelectorAll('button[data-original-label]').forEach(button=>{button.disabled=false;button.textContent=button.dataset.originalLabel;});
  const status=form.querySelector('.tutor-loading');if(status)status.hidden=true;
}));
const tutorInput=document.getElementById('tutor-message');
if(tutorInput)tutorInput.addEventListener('keydown',event=>{
  if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();if(tutorInput.value.trim())tutorInput.form.requestSubmit();}
});
const messageList=document.querySelector('.tutor-messages');if(messageList)messageList.scrollTop=messageList.scrollHeight;

const historyDisclosure=document.querySelector('.tutor-history-disclosure');
if(historyDisclosure){const phoneHistory=window.matchMedia('(max-width:600px)');historyDisclosure.open=!phoneHistory.matches;phoneHistory.addEventListener('change',event=>historyDisclosure.open=!event.matches);}
