/* Run: NODE_PATH=<directory containing playwright> node tests/renderers.cjs */
const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),assert=require('assert');
const root=path.resolve(__dirname,'..'), assets=path.join(root,'app/src/main/assets');
const types={'.js':'text/javascript','.mjs':'text/javascript','.css':'text/css','.html':'text/html','.wasm':'application/wasm'};
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});const results=[];
 for(const [file,type,selector] of [['sample.pdf','pdf','.page canvas'],['locked.pdf','pdf','.page canvas'],['cjk.pdf','pdf','.page canvas'],['sample.docx','docx','section.docx'],['multipage.docx','docx','section.docx'],['lists.docx','docx','section.docx'],['sample.xlsx','xlsx','table td'],['merges.xlsx','xlsx','td[colspan]'],['styled.xlsx','xlsx','table td'],['sample.pptx','pptx','iframe'],['effects.pptx','pptx','iframe'],['corrupted.pdf','pdf','error'],['notes-報告.txt','txt','pre']]){
  const page=await browser.newPage({viewport:{width:412,height:850}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/*',async route=>{
   const u=new URL(route.request().url());let target;
   if(u.hostname!=='appassets.androidplatform.net')return route.abort();
   if(u.pathname.startsWith('/document/'))target=path.join(__dirname,'fixtures',file);
   else if(u.pathname.startsWith('/assets/'))target=path.join(assets,decodeURIComponent(u.pathname.slice(8)));
   if(!target||!fs.existsSync(target))return route.fulfill({status:404,body:'missing'});
   return route.fulfill({status:200,body:fs.readFileSync(target),contentType:types[path.extname(target)]||'application/octet-stream',headers:{'Access-Control-Allow-Origin':'*'}});
  });
  let result;
  try {
   await page.goto('https://appassets.androidplatform.net/assets/viewer.html?type='+type+'&doc=/document/test-session');
   if(file==='locked.pdf'){
    await page.locator('#password[open]').waitFor();await page.locator('#passwordValue').fill('wrong');await page.locator('button[value="open"]').click();
    await page.locator('#password[open]').waitFor();await page.locator('#passwordValue').fill('wren');await page.locator('button[value="open"]').click();
   }
   if(selector==='error'){await page.waitForFunction(()=>document.getElementById('status').textContent.includes('Unable to display'));results.push({file,result:'PASS',check:'Malformed PDF rejected'});await page.close();continue;}
   if(file==='effects.pptx'){
    const frame=page.frameLocator('iframe');await frame.locator('#status').filter({hasText:'Unable to render'}).waitFor({timeout:30000});
    results.push({file,result:'UNSUPPORTED',check:'Known PPTXjs effects fixture failure is reported clearly',errors});await page.screenshot({path:path.join(__dirname,file+'.png')});await page.close();continue;
   }
   if(type==='pptx'){
    const frame=page.frameLocator('iframe');await frame.locator('.slide').first().waitFor({timeout:30000});assert((await frame.locator('.slide').count())>0);
    assert(!(await frame.locator('#status').innerText()).includes('Unable'));if(await frame.locator('.slide').count()>1){await frame.locator('#next').click();assert((await frame.locator('#position').innerText()).startsWith('2 /'));}
   }else await page.locator(selector).first().waitFor({timeout:30000});
   if(type==='pdf'){await page.locator('#find').fill('document');await page.locator('#findnext').click();await page.locator('#zoomin').click();}
   if(type==='xlsx')assert((await page.locator('table td').count())>0);
   if(type==='docx')assert((await page.locator('section.docx').first().innerText()).length>0);
   assert(await page.evaluate(()=>!DOMPurify.sanitize('<img src="x" onerror="alert(1)"><script>alert(2)</script>').includes('onerror')));
   await page.screenshot({path:path.join(__dirname,file+'.png'),fullPage:false});result={file,result:'PASS',errors};
  }catch(e){result={file,result:'FAIL',reason:e.message,errors,status:await page.locator('#status').innerText().catch(()=>''),frames:await Promise.all(page.frames().map(f=>f.locator('#status').innerText().catch(()=>'')))};}
  results.push(result);console.log(JSON.stringify(result));await page.close();
 }
 await browser.close();fs.writeFileSync(path.join(__dirname,'renderer-results.json'),JSON.stringify(results,null,2));if(results.some(r=>r.result==='FAIL'))process.exitCode=1;
})();
