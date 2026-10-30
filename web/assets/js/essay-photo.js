(()=>{
  'use strict';
  const answer=document.getElementById('essay-answer');
  document.querySelectorAll('[data-photo-action]').forEach(form=>form.addEventListener('submit',()=>{
    const hidden=form.querySelector('[name=draftAnswer]');if(hidden&&answer)hidden.value=answer.value;
  }));
  document.querySelectorAll('[data-photo-scan]').forEach(form=>form.addEventListener('submit',()=>{form.querySelector('.photo-progress').hidden=false;}));
  document.querySelector('[data-ocr-insert]')?.addEventListener('click',event=>{
    const text=document.querySelector('.essay-ocr-text')?.value.trim();if(!answer||!text)return;
    if(answer.value.includes(text)){answer.focus();return;}
    const next=answer.value.trim()+(answer.value.trim()?'\n\n':'')+text;
    if(next.length>12000){answer.setCustomValidity('Nội dung vượt 12.000 ký tự. Rút gọn câu trả lời trước khi chèn.');answer.reportValidity();return;}
    answer.setCustomValidity('');answer.value=next;answer.dispatchEvent(new Event('input',{bubbles:true}));answer.focus();event.currentTarget.textContent='Đã chèn · kiểm tra lại trước khi nộp';
  });
  answer?.addEventListener('input',()=>answer.setCustomValidity(''));
  const form=document.querySelector('[data-photo-upload]');if(!form)return;
  const input=form.querySelector('[data-photo-file]'),preview=form.querySelector('[data-photo-preview]'),status=form.querySelector('[data-photo-status]');
  const dialog=document.querySelector('.essay-camera'),video=dialog.querySelector('video');
  const capture=dialog.querySelector('[data-camera-capture]');capture.disabled=true;
  video.addEventListener('loadeddata',()=>{capture.disabled=!video.videoWidth;});
  let stream=null,previewUrl=null,busy=false;
  async function useImage(file,rotate=false){
    if(!file)return;busy=true;status.textContent='Đang chuẩn bị ảnh…';
    try{
      if(file.size>20*1024*1024)throw new Error('Ảnh quá 20 MB. Hãy chọn ảnh nhỏ hơn.');
      const bitmap=await createImageBitmap(file);const scale=Math.min(1,2000/Math.max(bitmap.width,bitmap.height));
      const width=Math.round(bitmap.width*scale),height=Math.round(bitmap.height*scale);const canvas=document.createElement('canvas');canvas.width=rotate?height:width;canvas.height=rotate?width:height;
      const context=canvas.getContext('2d');context.fillStyle='#fff';context.fillRect(0,0,canvas.width,canvas.height);if(rotate){context.translate(canvas.width,0);context.rotate(Math.PI/2);}context.drawImage(bitmap,0,0,width,height);bitmap.close();
      const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',.92));if(!blob)throw new Error('Không xử lý được ảnh. Hãy chọn ảnh JPEG/PNG.');
      const transfer=new DataTransfer();transfer.items.add(new File([blob],'bai-lam.jpg',{type:'image/jpeg'}));input.files=transfer.files;
      if(previewUrl)URL.revokeObjectURL(previewUrl);previewUrl=URL.createObjectURL(blob);preview.src=previewUrl;form.querySelector('.photo-preview').hidden=false;
      status.textContent='Ảnh đã sẵn sàng. Kiểm tra chiều ảnh và chữ trước khi bấm Lưu ảnh vào bài làm.';
    }catch(error){input.value='';form.querySelector('.photo-preview').hidden=true;status.textContent=error.message||'Không đọc được ảnh. Hãy chọn JPEG hoặc PNG.';}finally{busy=false;}
  }
  input.addEventListener('change',()=>useImage(input.files[0]));
  form.querySelector('[data-camera-file]').addEventListener('change',event=>useImage(event.target.files[0]));
  form.querySelector('[data-photo-rotate]').addEventListener('click',()=>{if(!busy)useImage(input.files[0],true);});
  function stop(){stream?.getTracks().forEach(track=>track.stop());stream=null;video.srcObject=null;capture.disabled=true;}
  form.querySelector('[data-camera-open]').addEventListener('click',async()=>{
    if(!navigator.mediaDevices?.getUserMedia){form.querySelector('[data-camera-file]').click();return;}
    dialog.showModal();dialog.querySelector('[data-camera-status]').textContent='Cho phép truy cập camera để chụp bài làm.';
    try{const incoming=await navigator.mediaDevices.getUserMedia({video:{facingMode:{ideal:'environment'},width:{ideal:1920},height:{ideal:1440}},audio:false});if(!dialog.open){incoming.getTracks().forEach(t=>t.stop());return;}stream=incoming;video.srcObject=stream;await video.play();dialog.querySelector('[data-camera-status]').textContent='Giữ máy ổn định rồi bấm Chụp và dùng ảnh.';}
    catch(error){stop();dialog.querySelector('[data-camera-status]').textContent='Không mở được camera. Kiểm tra quyền camera hoặc đóng khung này và chọn ảnh từ thiết bị.';}
  });
  dialog.querySelector('[data-camera-close]').addEventListener('click',()=>dialog.close());dialog.addEventListener('close',stop);
  dialog.querySelector('[data-camera-capture]').addEventListener('click',()=>{
    if(!video.videoWidth)return;const canvas=document.createElement('canvas');canvas.width=video.videoWidth;canvas.height=video.videoHeight;canvas.getContext('2d').drawImage(video,0,0);
    canvas.toBlob(blob=>{if(blob){useImage(new File([blob],'camera.jpg',{type:'image/jpeg'}));dialog.close();}},'image/jpeg',.92);
  });
  form.addEventListener('submit',event=>{if(busy){event.preventDefault();status.textContent='Ảnh đang xử lý, vui lòng chờ rồi bấm lưu lại.';}else if(input.files.length){status.textContent='Đang lưu ảnh…';}});
  window.addEventListener('pagehide',()=>{stop();if(previewUrl)URL.revokeObjectURL(previewUrl);});
  document.addEventListener('visibilitychange',()=>{if(document.hidden&&dialog.open)dialog.close();});
})();
