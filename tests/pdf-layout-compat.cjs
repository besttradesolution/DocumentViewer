const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),assert=require('assert');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'app/src/main/assets');
const mime={'.js':'application/javascript','.mjs':'application/javascript','.wasm':'application/wasm','.css':'text/css','.html':'text/html'};
const expectCollapsed=process.argv.includes('--expect-collapsed');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});const results=[];
 try {
  for(const {legacyRound,fixture} of (expectCollapsed?[{legacyRound:true,fixture:'sample.pdf'}]:[false,true].flatMap(legacyRound=>['sample.pdf','layout-rotations.pdf'].map(fixture=>({legacyRound,fixture}))))){
   const page=await browser.newPage({viewport:{width:360,height:780},deviceScaleFactor:2,isMobile:true,hasTouch:true});
   if(legacyRound)await page.addInitScript(()=>{
    // Report the same feature support as an older WebView.
    const supports=CSS.supports.bind(CSS);
    CSS.supports=(...args)=>args.some(value=>String(value).includes('round('))?false:supports(...args);
   });
   await page.route('**/*',async route=>{
    const u=new URL(route.request().url());if(u.host!=='appassets.androidplatform.net')return route.abort();
    const file=u.pathname==='/document/test-session'?path.join(__dirname,'fixtures',fixture):u.pathname.startsWith('/assets/')?path.join(assets,decodeURIComponent(u.pathname.slice(8))):null;
    if(!file||!fs.existsSync(file))return route.fulfill({status:404,body:''});
    let body=fs.readFileSync(file);
    // Current Chromium supports round(); invalidate just the library's round()
    // dimension expressions to reproduce the computed-value failure on old engines.
    if(legacyRound&&file.endsWith(path.join('pdf','pdf.mjs')))body=Buffer.from(body.toString().replace(/`round\(down,[^`]+`/g,'`var(--unavailable-css-round)`'));
    return route.fulfill({body,contentType:mime[path.extname(file)]||'application/octet-stream'});
   });
   await page.goto('https://appassets.androidplatform.net/assets/viewer.html?type=pdf&doc=/document/test-session&theme=dark');
   if(expectCollapsed){await page.waitForFunction(()=>document.getElementById('count').textContent.includes('3'));await page.waitForTimeout(200);}
   else await page.waitForFunction(()=>document.documentElement.dataset.pdfRendered==='true');
   async function geometry(){return page.locator('.page').first().evaluate(el=>{
    const p=el.getBoundingClientRect(),canvas=el.querySelector('canvas'),c=canvas?.getBoundingClientRect();
    return {pageWidth:p.width,pageHeight:p.height,canvasWidth:c?.width,canvasHeight:c?.height,inlineHeight:el.style.height};
   });}
   const before=await geometry();
   if(expectCollapsed){
    assert(before.pageHeight<30,JSON.stringify(before));await page.screenshot({path:path.join(__dirname,'pdf-collapsed-before.png')});
    results.push({mode:'legacy CSS round unavailable',result:'REPRODUCED',...before});
   }else{
    assert(before.pageHeight>300,'Visible PDF page must not collapse: '+JSON.stringify(before));
    assert(before.canvasHeight>300,'Canvas display must have page height, not only a painted bitmap');
    assert(Math.abs(before.pageHeight/before.pageWidth-792/612)<.03,'Page aspect ratio must match the actual PDF');
    await page.locator('#zoomin').click();await page.waitForTimeout(200);const zoomed=await geometry();assert(zoomed.pageHeight>before.pageHeight*1.1,'Zoom must resize the displayed page');
    await page.locator('#fit').click();await page.setViewportSize({width:780,height:360});await page.waitForTimeout(200);const landscape=await geometry();assert(landscape.pageWidth>before.pageWidth*1.7);
    await page.setViewportSize({width:360,height:780});await page.waitForTimeout(200);
    await page.locator('#next').click();await page.waitForFunction(()=>document.getElementById('page').value==='2');
    const second=await page.locator('.page[data-page-number="2"]').boundingBox();assert(second.height>300);
    if(fixture==='layout-rotations.pdf'){
     const ratio=second.width/second.height;assert(Math.abs(ratio-792/612)<.03,'Rotated/UserUnit page must preserve landscape geometry');
     for(const number of [3,4]){await page.locator('#page').fill(String(number));await page.locator('#page').dispatchEvent('change');await page.waitForTimeout(150);const box=await page.locator('.page[data-page-number="'+number+'"]').boundingBox();assert(box.height>100&&box.width>100);}
    }
    await page.locator('#page').fill('1');await page.locator('#page').dispatchEvent('change');await page.evaluate(query=>window.documentViewerFind(query),fixture==='sample.pdf'?'Paperwren':'Layout test');await page.waitForFunction(()=>document.querySelectorAll('.textLayer .highlight').length>0);
    await page.screenshot({path:path.join(__dirname,(legacyRound?'pdf-legacy-':'pdf-modern-')+fixture+'.png')});
    results.push({mode:legacyRound?'legacy CSS round unavailable':'modern CSS',fixture,result:'PASS',...before});
   }
   await page.close();
  }
 }finally{await browser.close();}
 console.log(JSON.stringify(results,null,2));fs.writeFileSync(path.join(__dirname,expectCollapsed?'pdf-layout-before.json':'pdf-layout-results.json'),JSON.stringify(results,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
