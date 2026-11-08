const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),assert=require('assert');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'app/src/main/assets');
const kotlin=fs.readFileSync(path.join(root,'app/src/main/java/com/local/documentviewer/AssetMimeTypes.kt'),'utf8');
assert(/"mjs", "js"\s*->\s*"application\/javascript"/.test(kotlin),'Native WebView must explicitly serve PDF modules as JavaScript');
assert(/"wasm"\s*->\s*"application\/wasm"/.test(kotlin),'Native WebView must explicitly serve WebAssembly as binary');
const mime={'.js':'application/javascript','.mjs':'application/javascript','.wasm':'application/wasm','.css':'text/css','.html':'text/html'};
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
 const results=[];
 try {
  for(const mode of ['module-mime','worker-mime','correct-mime']){
   const brokenMime=mode!=='correct-mime';
   const page=await browser.newPage({viewport:{width:360,height:780},deviceScaleFactor:2,isMobile:true,hasTouch:true});
   const errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.route('**/*',async route=>{
    const u=new URL(route.request().url());if(u.host!=='appassets.androidplatform.net')return route.abort();
    const file=u.pathname==='/document/test-session'?path.join(__dirname,'fixtures/sample.pdf'):u.pathname.startsWith('/assets/')?path.join(assets,decodeURIComponent(u.pathname.slice(8))):null;
    if(!file||!fs.existsSync(file))return route.fulfill({status:404,body:''});
    const ext=path.extname(file),rejectModule=mode==='module-mime'&&ext==='.mjs'||mode==='worker-mime'&&file.endsWith('pdf.worker.mjs');
    const contentType=rejectModule?'text/plain':mime[ext]||'application/octet-stream';
    return route.fulfill({body:fs.readFileSync(file),contentType,headers:{'X-Content-Type-Options':'nosniff'}});
   });
   await page.goto('https://appassets.androidplatform.net/assets/viewer.html?type=pdf&doc=/document/test-session');
   if(brokenMime){
    await page.waitForFunction(()=>/Unable to display|reader could not start/.test(document.getElementById('status').textContent));
    assert(await page.locator('#status').isVisible(),'Module failures must show an error, not an empty page');
    results.push({case:'Android '+mode+' response',result:'PASS',detail:'Rejected modules produce a visible error'});
   }else{
    await page.waitForFunction(()=>document.documentElement.dataset.pdfRendered==='true');
    const pixels=await page.locator('.page canvas').first().evaluate(canvas=>{
     const {data}=canvas.getContext('2d').getImageData(0,0,canvas.width,canvas.height);let ink=0;
     for(let i=0;i<data.length;i+=4)if(data[i+3]>0&&Math.min(data[i],data[i+1],data[i+2])<220)ink++;
     return ink;
    });assert(pixels>50,'PDF must contain rendered content, not merely an empty canvas');
    const box=await page.locator('.page canvas').first().boundingBox();assert(box&&box.height>300,'The displayed canvas must not collapse to a horizontal strip');
    await page.evaluate(()=>window.documentViewerFind('Paperwren'));
    await page.waitForFunction(()=>document.querySelectorAll('.textLayer .highlight').length>0);
    await page.setViewportSize({width:780,height:360});
    await page.waitForTimeout(200);
    assert(await page.locator('.page canvas').first().isVisible());assert.equal(errors.length,0,errors.join('\n'));
    await page.setViewportSize({width:360,height:780});await page.waitForTimeout(200);
    await page.screenshot({path:path.join(__dirname,'pdf-mobile-fixed.png')});
    await page.evaluate(()=>document.documentElement.dataset.theme='dark');
    assert.equal(await page.evaluate(()=>getComputedStyle(document.documentElement).backgroundColor),'rgb(24, 24, 33)');
    await page.screenshot({path:path.join(__dirname,'pdf-mobile-dark.png')});
    results.push({case:'Android explicit module response',result:'PASS',paintedPixels:pixels,detail:'PDF paints visible pixels, search highlights, viewport resize succeeds'});
   }
   await page.close();
  }
 }finally{await browser.close();}
 fs.writeFileSync(path.join(__dirname,'pdf-android-results.json'),JSON.stringify(results,null,2));console.log(JSON.stringify(results,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
