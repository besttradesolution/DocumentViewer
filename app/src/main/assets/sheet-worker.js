importScripts('vendor/xlsx.full.min.js');
self.onmessage = ({data}) => {
  try {
    const workbook = XLSX.read(data, {type:'array',cellStyles:true,cellDates:true,cellNF:true,sheetStubs:true,sheetRows:200000});
    self.postMessage({names:workbook.SheetNames, sheets:workbook.SheetNames.map(name=>workbook.Sheets[name])});
  } catch(e) { self.postMessage({error:'This spreadsheet could not be read. It may be encrypted or corrupted.'}); }
};
