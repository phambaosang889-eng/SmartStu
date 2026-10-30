document.querySelectorAll('[data-video-upload]').forEach(form=>{
 const file=form.querySelector('input[type=file]'),status=form.querySelector('[data-upload-status]');
 file.addEventListener('change',()=>{file.setCustomValidity('');if(file.files[0]?.size>200*1024*1024)file.setCustomValidity('Video vượt 200 MB. Hãy nén video hoặc dùng liên kết YouTube.');file.reportValidity();});
 form.addEventListener('submit',()=>{const button=form.querySelector('button');button.disabled=true;button.textContent='Đang tải video…';status.textContent='Đang gửi video lên máy chủ. Vui lòng giữ trang mở cho đến khi hoàn tất.';});
 window.addEventListener('pageshow',()=>{const button=form.querySelector('button');button.disabled=false;button.textContent='Tải video lên';});
});
