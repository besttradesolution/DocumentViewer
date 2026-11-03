let started=false;
function renderFailure(){document.getElementById('slides').replaceChildren();document.getElementById('status').textContent='Unable to render this presentation. It may use unsupported features or be corrupted.';}
window.addEventListener('error',renderFailure);
window.addEventListener('unhandledrejection',renderFailure);
window.addEventListener('message',async event=>{
 if(started||event.source!==parent||!/^https:\/\/appassets\.androidplatform\.net\/document\/[a-zA-Z0-9-]+$/.test(event.data?.documentUrl||''))return;started=true;
 const status=document.getElementById('status');
 try {
  const bytes=await (await fetch(event.data.documentUrl)).arrayBuffer();
  window.JSZipUtils={getBinaryContent:(_url,callback)=>callback(null,bytes)};
  $.globalEval=()=>{throw Error('Document scripts are disabled');};
  $('#slides').pptxToHtml({pptxFileUrl:'selected.pptx',slideMode:false,keyBoardShortCut:false,mediaProcess:false,jsZipV2:false,themeProcess:true});
  const deadline=Date.now()+30000;
  const timer=setInterval(()=>{const slides=[...document.querySelectorAll('#slides .slide')];if(!slides.length){if(Date.now()>deadline){clearInterval(timer);status.textContent='The presentation could not be rendered.';}return;}clearInterval(timer);
   let index=0;function show(){slides.forEach((s,i)=>s.style.display=i===index?'block':'none');document.getElementById('position').textContent=(index+1)+' / '+slides.length;fit();}
   function fit(){const slide=slides[index];slide.style.transformOrigin='top left';slide.style.transform='scale('+Math.min(1,innerWidth/slide.offsetWidth)+')';}
   document.getElementById('prev').onclick=()=>{index=Math.max(0,index-1);show();};document.getElementById('next').onclick=()=>{index=Math.min(slides.length-1,index+1);show();};document.getElementById('fit').onclick=fit;status.textContent='';show();
  },100);
 }catch(e){renderFailure();}
});
