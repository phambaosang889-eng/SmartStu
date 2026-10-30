'use strict';
(() => {
  const toggle=document.querySelector('.mobile-toggle'),backdrop=document.querySelector('.nav-backdrop');
  const sidebar=document.getElementById('navigation'),main=document.querySelector('.workspace-main');
  const syncNavigation=()=>{
    const phone=window.innerWidth<=900,open=document.body.classList.contains('nav-open');
    if(sidebar){sidebar.inert=phone&&!open;if(phone&&open){sidebar.setAttribute('role','dialog');sidebar.setAttribute('aria-modal','true')}else{sidebar.removeAttribute('role');sidebar.removeAttribute('aria-modal')}}
    if(main)main.inert=phone&&open;
  };
  const closeMenu=()=>{document.body.classList.remove('nav-open');if(toggle)toggle.setAttribute('aria-expanded','false');if(backdrop)backdrop.hidden=true;syncNavigation()};
  syncNavigation();
  if(toggle)toggle.addEventListener('click',()=>{const open=!document.body.classList.contains('nav-open');document.body.classList.toggle('nav-open',open);toggle.setAttribute('aria-expanded',String(open));backdrop.hidden=!open;syncNavigation();if(open)document.querySelector('.workspace-nav a')?.focus()});
  backdrop?.addEventListener('click',()=>{closeMenu();toggle?.focus()});
  document.querySelector('.mobile-nav-close')?.addEventListener('click',()=>{closeMenu();toggle?.focus()});
  sidebar?.querySelectorAll('a').forEach(link=>link.addEventListener('click',()=>closeMenu()));
  document.addEventListener('keydown',e=>{
    if(!document.body.classList.contains('nav-open'))return;
    if(e.key==='Escape'){closeMenu();toggle?.focus();return}
    if(e.key==='Tab'&&sidebar){const focusable=[...sidebar.querySelectorAll('a[href],button:not([disabled])')].filter(el=>el.getClientRects().length);const first=focusable[0],last=focusable[focusable.length-1];if(e.shiftKey&&document.activeElement===first){e.preventDefault();last?.focus()}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first?.focus()}}
  });
  window.addEventListener('pageshow',()=>{closeMenu();document.querySelectorAll('[data-workspace-label]').forEach(button=>{button.disabled=false;button.removeAttribute('aria-busy');button.textContent=button.dataset.workspaceLabel;delete button.dataset.workspaceLabel})});
  window.matchMedia('(min-width:901px)').addEventListener('change',()=>closeMenu());
  document.querySelectorAll('[data-open]').forEach(b=>b.addEventListener('click',()=>document.getElementById(b.dataset.open)?.showModal()));
  document.querySelectorAll('[data-close]').forEach(b=>b.addEventListener('click',()=>b.closest('dialog').close()));
  document.querySelectorAll('[data-dismiss]').forEach(b=>b.addEventListener('click',()=>b.closest('.notice').remove()));
  document.querySelectorAll('[data-image-fallback]').forEach(img=>{const fallback=()=>{img.hidden=true;const note=img.parentElement.querySelector('.image-error');if(note)note.hidden=false};img.addEventListener('error',fallback);if(img.complete&&img.naturalWidth===0)fallback()});
  const normalize=t=>t.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/\u0111/g,'d');
  const filter=()=>{const query=normalize(document.querySelector('[data-search]')?.value||'');const state=document.querySelector('[data-filter]')?.value||'all';let count=0;document.querySelectorAll('[data-search-item]').forEach(e=>{e.hidden=!(normalize(e.textContent).includes(query)&&(state==='all'||state==='active'&&e.dataset.state!=='archived'||e.dataset.state===state));if(!e.hidden)count++});document.querySelectorAll('[data-result-count]').forEach(e=>e.textContent=count+' n\u1ed9i dung');document.querySelectorAll('[data-empty]').forEach(e=>e.hidden=count>0)};
  document.querySelector('[data-search]')?.addEventListener('input',filter);document.querySelector('[data-filter]')?.addEventListener('change',filter);
  if(document.querySelector('[data-filter]')?.value==='active')filter();
  document.querySelectorAll('[data-count]').forEach(t=>{const out=t.closest('form')?.querySelector('[data-count-output]');const update=()=>{if(out)out.textContent=t.value.length+' / '+t.maxLength+' k\u00fd t\u1ef1'};t.addEventListener('input',update);update()});
  const dialog=document.getElementById('confirm-dialog');let pending=null,submitter=null;
  document.querySelectorAll('form').forEach(f=>f.addEventListener('submit',e=>{if(!f.dataset.confirm||f.dataset.confirmed==='true')return;e.preventDefault();pending=f;submitter=e.submitter;document.getElementById('confirm-message').textContent=f.dataset.confirm;dialog.showModal()}));
  document.querySelectorAll('[data-counter]').forEach(t=>{const out=document.getElementById(t.dataset.counter);const update=()=>{if(out)out.textContent=t.value.length+' k\u00fd t\u1ef1 \u00b7 '+(t.value.trim()?t.value.trim().split(/\s+/).length:0)+' t\u1eeb'};t.addEventListener('input',update);update()});
  document.querySelector('[data-confirm-cancel]')?.addEventListener('click',()=>dialog.close());
  document.querySelector('[data-confirm-ok]')?.addEventListener('click',()=>{dialog.close();if(pending){pending.dataset.confirmed='true';pending.requestSubmit(submitter)}});
  document.querySelectorAll('form').forEach(f=>f.addEventListener('submit',e=>{if(e.defaultPrevented||f.classList.contains('tutor-send'))return;const b=e.submitter;if(b){b.dataset.workspaceLabel=b.textContent;b.setAttribute('aria-busy','true');const label=f.dataset.photoScan!==undefined?'\u0110ang \u0111\u1ecdc \u1ea3nh\u2026':f.method.toLowerCase()==='get'?'\u0110ang t\u1ea3i\u2026':'\u0110ang x\u1eed l\u00fd\u2026';setTimeout(()=>{b.disabled=true;b.textContent=label},0)}}));
})();
