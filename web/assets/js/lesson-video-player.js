document.querySelectorAll('.lesson-video video').forEach(video=>{
 video.addEventListener('error',()=>{
  const message=video.closest('.lesson-video').querySelector('[data-video-error]');
  if(message)message.hidden=false;
 });
});
