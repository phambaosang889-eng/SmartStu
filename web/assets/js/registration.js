(() => {
  const form=document.getElementById('registration-form');if(!form)return;
  const password=document.getElementById('register-password'),confirmation=document.getElementById('register-confirm');
  const validate=()=>confirmation.setCustomValidity(confirmation.value&&confirmation.value!==password.value?'Mật khẩu xác nhận chưa khớp.':'');
  password.addEventListener('input',validate);confirmation.addEventListener('input',validate);
  document.getElementById('show-registration-password').addEventListener('change',event=>{password.type=confirmation.type=event.target.checked?'text':'password';});
  form.addEventListener('submit',event=>{if(form.dataset.sending){event.preventDefault();return;}form.dataset.sending='true';form.querySelector('button[type=submit]').disabled=true;document.getElementById('registration-status').hidden=false;});
  window.addEventListener('pageshow',()=>{delete form.dataset.sending;form.querySelector('button[type=submit]').disabled=false;document.getElementById('registration-status').hidden=true;});
})();
