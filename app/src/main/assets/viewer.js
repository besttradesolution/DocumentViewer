const $=id=>document.getElementById(id), params=new URLSearchParams(location.search), type=params.get('type'), url=params.get('doc');
const status=$('status'), content=$('content');
const cleanOptions={ADD_TAGS:['style'],ADD_ATTR:['style'],FORBID_TAGS:['script','iframe','object','embed','form','input','video','audio','link','meta'],FORBID_ATTR:['srcset'],ALLOW_DATA_ATTR:false};
function fail(e){$('pdfContainer').hidden=true;content.hidden=false;status.textContent='Unable to display this document. '+(e?.message||'It may be corrupted, encrypted or unsupported.');}
function clean(node){
 const fragment=DOMPurify.sanitize(node,{...cleanOptions,RETURN_DOM_FRAGMENT:true});
 fragment.querySelectorAll('[href],[src],[xlink\\:href]').forEach(el=>{for(const attr of ['href','src','xlink:href']) { const v=el.getAttribute(attr); if(v && !/^(data:image\/(png|jpeg|gif|webp);|blob:|#)/i.test(v)) el.removeAttribute(attr); }});
 return fragment;
}
function controls(visible){for(const el of $('controls').children)el.hidden=!visible.includes(el.id);}
async function pdf(){
 controls(['previous','page','count','next','fit','zoomout','zoomin','find','findnext']);
 const lib=await import('./vendor/pdf/pdf.mjs'); globalThis.pdfjsLib=lib;
 lib.GlobalWorkerOptions.workerSrc=new URL('./vendor/pdf/pdf.worker.mjs',location.href).href;
 const {EventBus,PDFLinkService,PDFFindController,PDFViewer}=await import('./vendor/pdf/pdf_viewer.mjs');
 const eventBus=new EventBus(), linkService=new PDFLinkService({eventBus}), findController=new PDFFindController({eventBus,linkService});
 $('pdfContainer').hidden=false; content.hidden=true;
 const viewer=new PDFViewer({container:$('pdfContainer'),viewer:$('pdfViewer'),eventBus,linkService,findController,annotationMode:0,maxCanvasPixels:4096*4096});
 linkService.setViewer(viewer);
 const task=lib.getDocument({url,cMapUrl:new URL('./vendor/pdf/cmaps/',location.href).href,cMapPacked:true,standardFontDataUrl:new URL('./vendor/pdf/standard_fonts/',location.href).href,wasmUrl:new URL('./vendor/pdf/wasm/',location.href).href,isEvalSupported:false,enableXfa:false});
 task.onPassword=(update,reason)=>{
   status.textContent=reason===2?'Incorrect password. Please try again.':'';
   $('passwordValue').value=''; $('password').showModal();
   $('password').onclose=()=>{if($('password').returnValue==='open')update($('passwordValue').value); else {task.destroy();fail(new Error('Password entry cancelled.'));}};
 };
 const doc=await task.promise; viewer.setDocument(doc);linkService.setDocument(doc);findController.setDocument(doc);
 $('count').textContent='/ '+doc.numPages;$('page').max=doc.numPages;
 eventBus.on('pagesinit',()=>{viewer.currentScaleValue='page-width';status.textContent='';});
 eventBus.on('pagechanging',e=>{$('page').value=e.pageNumber;});
 $('previous').onclick=()=>viewer.previousPage(); $('next').onclick=()=>viewer.nextPage();
 $('page').onchange=()=>{viewer.currentPageNumber=Math.max(1,Math.min(doc.numPages,Number($('page').value)||1));};
 $('fit').onclick=()=>viewer.currentScaleValue='page-width';
 $('zoomin').onclick=()=>viewer.currentScale=Math.min(5,viewer.currentScale*1.2);$('zoomout').onclick=()=>viewer.currentScale=Math.max(.2,viewer.currentScale/1.2);
 const find=again=>eventBus.dispatch('find',{source:window,type:again?'again':'',query:$('find').value,caseSensitive:false,entireWord:false,highlightAll:true,findPrevious:false});
 $('find').oninput=()=>find(false);$('findnext').onclick=()=>find(true);
}
async function word(bytes){
 controls(['fit','zoomout','zoomin']); const detached=document.createElement('div');
 await docx.renderAsync(bytes,detached,undefined,{inWrapper:true,ignoreWidth:false,ignoreHeight:false,breakPages:true,renderHeaders:true,renderFooters:true,useBase64URL:true,renderAltChunks:false});
 content.replaceChildren(clean(detached.innerHTML)); status.textContent='';
 let scale=1; const fit=()=>{const section=content.querySelector('section');if(section){scale=Math.min(1,(innerWidth-24)/section.offsetWidth);content.style.zoom=scale;}};
 $('fit').onclick=fit;$('zoomin').onclick=()=>content.style.zoom=scale=Math.min(3,scale*1.2);$('zoomout').onclick=()=>content.style.zoom=scale=Math.max(.2,scale/1.2);fit();
}
async function spreadsheet(bytes){
 controls(['sheets']); const worker=new Worker('sheet-worker.js');
 const data=await new Promise((resolve,reject)=>{worker.onmessage=e=>{worker.terminate();e.data.error?reject(new Error(e.data.error)):resolve(e.data);};worker.onerror=()=>{worker.terminate();reject(new Error('Spreadsheet parser failed.'));};worker.postMessage(bytes,[bytes]);});
 const {names,sheets}=data; names.forEach((name,i)=>$('sheets').add(new Option(name,String(i))));
 let generation=0;
 async function display(index){
  const current=++generation, sheet=sheets[index];content.replaceChildren();status.textContent='Cached formula values are shown; they may be stale. Charts and full Excel styling are not supported.';
  const match=/^([A-Z]+)(\d+)(?::([A-Z]+)(\d+))?$/.exec(sheet['!ref']||'A1');if(!match)throw Error('Invalid worksheet range.');
  const col=s=>[...s].reduce((n,c)=>n*26+c.charCodeAt(0)-64,0)-1;
  const label=n=>{let s='';for(n++;n;n=Math.floor((n-1)/26))s=String.fromCharCode(65+(n-1)%26)+s;return s;};
  const startRow=+match[2]-1,endRow=+(match[4]||match[2])-1,startCol=col(match[1]),endCol=col(match[3]||match[1]);
  if(endCol-startCol>255 || endRow-startRow>199999)throw Error('This sheet exceeds the preview limit of 256 columns or 200,000 rows.');
  const table=document.createElement('table'), group=document.createElement('colgroup');
  for(let c=startCol;c<=endCol;c++){const el=document.createElement('col');el.style.width=Math.min(800,sheet['!cols']?.[c]?.wpx||100)+'px';group.append(el);}table.append(group);content.append(table);
  const merges=sheet['!merges']||[];if(merges.length>10000)throw Error('Too many merged cells.');
  let row=startRow;
  const more=document.createElement('button');more.textContent='Load next 200 rows';content.append(more);
  const chunk=()=>{if(current!==generation)return;const fragment=document.createDocumentFragment();const stop=Math.min(endRow+1,row+200);
   for(;row<stop;row++){const tr=document.createElement('tr');const h=sheet['!rows']?.[row]?.hpx;if(h)tr.style.height=Math.min(800,h)+'px';
    for(let c=startCol;c<=endCol;c++){const merge=merges.find(m=>row>=m.s.r&&row<=m.e.r&&c>=m.s.c&&c<=m.e.c);if(merge&&(row!==merge.s.r||c!==merge.s.c))continue;
     const td=document.createElement('td'),cell=sheet[label(c)+(row+1)];if(merge){td.rowSpan=merge.e.r-merge.s.r+1;td.colSpan=merge.e.c-merge.s.c+1;}
     if(cell){td.textContent=cell.f&&cell.v==null?'[Formula result unavailable]':cell.w??String(cell.v??''); if(cell.f)td.title='Cached formula result; not recalculated'; const fill=cell.s?.fgColor?.rgb;if(/^[0-9A-Fa-f]{6}$/.test(fill||''))td.style.backgroundColor='#'+fill;}
     tr.append(td);
    }fragment.append(tr);
   }table.append(fragment);more.hidden=row>endRow;
  };more.onclick=chunk;chunk();
 }
 $('sheets').onchange=()=>display(+$('sheets').value).catch(fail);await display(0);
}
async function powerpoint(){
 controls([]);status.textContent='Experimental PowerPoint preview. Layout, fonts and charts may differ.';
 const frame=document.createElement('iframe');frame.sandbox='allow-scripts';frame.title='PowerPoint slides';frame.src='powerpoint.html';content.append(frame);
 frame.onload=()=>frame.contentWindow.postMessage({documentUrl:new URL(url,location.origin).href},'*');
}
try {
 if(!url||!/^\/document\/[a-zA-Z0-9-]+$/.test(url))throw Error('Missing document session.');
 if(type==='pdf')await pdf();else if(type==='pptx')await powerpoint();else {
  const response=await fetch(url);if(!response.ok)throw Error('Document access expired.');const bytes=await response.arrayBuffer();
  if(type==='docx')await word(bytes);else if(type==='xlsx'||type==='csv')await spreadsheet(bytes);else if(type==='txt'){controls([]);const pre=document.createElement('pre');pre.textContent=new TextDecoder().decode(bytes);content.append(pre);status.textContent='';}else throw Error('Unsupported format.');
 }
}catch(e){fail(e);}
