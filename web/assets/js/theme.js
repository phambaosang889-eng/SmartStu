/* SVG paths are ASCII, so the theme icon does not depend on text encoding/fonts. */
(()=>{
  'use strict';
  const key='smartstudy-theme';
  const moon='<path d="M20.9 13A9 9 0 0 1 11 3.1 9 9 0 1 0 20.9 13Z"/>';
  const sun='<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>';
  function apply(theme){
    const dark=theme==='dark';document.body.dataset.theme=dark?'dark':'light';
    document.querySelectorAll('.theme-toggle').forEach(button=>{
      const label=dark?'Chuy\u1ec3n giao di\u1ec7n s\u00e1ng':'Chuy\u1ec3n giao di\u1ec7n t\u1ed1i';
      button.setAttribute('aria-label',label);button.setAttribute('title',label);button.setAttribute('aria-pressed',String(dark));
      if(button.classList.contains('icon-button')||button.classList.contains('icon-btn'))
        button.innerHTML='<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">'+(dark?sun:moon)+'</svg>';
    });
  }
  let saved='light';try{saved=localStorage.getItem(key)||'light';}catch(error){}
  apply(saved);
  document.querySelectorAll('.theme-toggle').forEach(button=>button.addEventListener('click',()=>{
    const next=document.body.dataset.theme==='dark'?'light':'dark';apply(next);try{localStorage.setItem(key,next);}catch(error){}
  }));
})();
