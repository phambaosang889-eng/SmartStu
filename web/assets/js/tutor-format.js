/* Deliberately small Markdown subset. Untrusted prose is always a text node;
   HTML, links, images and embedded scripts are never interpreted. */
(()=>{
  'use strict';
  // Some AI responses contain double-escaped JSON newlines. Protect TeX and
  // inline code first: a global replacement would corrupt \\nu, \\nabla, \\neq.
  function normalize(source){
    const protectedParts=[];
    const text=source.replace(/\r\n?/g,'\n').replace(/\${1,2}[^$]+?\${1,2}|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)|`[^`\n]+`/g,value=>{
      protectedParts.push(value);return '\uE002'+(protectedParts.length-1)+'\uE003';
    });
    return text.replace(/\\r\\n|\\n(?![a-z])|\\r(?![a-z])/g,'\n')
      .replace(/\uE002(\d+)\uE003/g,(_,index)=>protectedParts[Number(index)]??'');
  }
  function inline(parent,text){
    const tokens=/(\${1,2}[^$]+?\${1,2}|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)|\*\*[^\n]+?\*\*|`[^\n`]+`)/g;
    let end=0;
    for(const match of text.matchAll(tokens)){
      parent.append(document.createTextNode(text.slice(end,match.index)));
      const token=match[0];let node;
      if(token.startsWith('**')){node=document.createElement('strong');inline(node,token.slice(2,-2));}
      else if(token.startsWith('`')){node=document.createElement('code');node.textContent=token.slice(1,-1);}
      else{
        const display=token.startsWith('$$')||token.startsWith('\\[');
        node=document.createElement('span');node.className=display?'tutor-equation':'tutor-inline-math';
        // Accept a mismatched $ / $$ pair from older stored replies without
        // leaving an extra dollar sign on screen. Do not rewrite database text.
        const formula=token.startsWith('$')?token.replace(/^\${1,2}|\${1,2}$/g,''):token.slice(2,-2);
        try{
          if(!window.katex)throw new Error('Math renderer unavailable');
          window.katex.render(formula,node,{displayMode:display,throwOnError:true,trust:false,strict:'ignore',maxSize:10,maxExpand:300,output:'htmlAndMathml'});
        }catch(error){node.textContent=formula;node.classList.add('tutor-math-fallback');node.title='Công thức hiển thị dạng văn bản';}
      }
      parent.append(node);end=match.index+token.length;
    }
    parent.append(document.createTextNode(text.slice(end)));
  }
  function render(element){
    const source=normalize(element.textContent);
    const fragment=document.createDocumentFragment();let paragraph=[],list=null;
    const flush=()=>{if(paragraph.length){const p=document.createElement('p');inline(p,paragraph.join('\n'));fragment.append(p);paragraph=[];}};
    // Keep multiline display equations intact while splitting prose into blocks.
    const math=[];
    const protectedSource=source.replace(/\${1,2}[^$]+?\${1,2}|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)/g,value=>{math.push(value);return '\uE000'+(math.length-1)+'\uE001';});
    const restore=value=>value.replace(/\uE000(\d+)\uE001/g,(_,index)=>math[Number(index)]??'');
    for(const line of protectedSource.split('\n')){
      if(!line.trim()){flush();continue;}
      const item=line.match(/^\s*(?:(\d+)[.)]|([-*]))\s+(.+)$/);
      const heading=line.match(/^\s*#{1,6}\s+(.+)$/);
      if(item){flush();const type=item[1]?'ol':'ul';if(!list||list.tagName.toLowerCase()!==type){list=document.createElement(type);if(item[1])list.start=Number(item[1]);fragment.append(list);}const li=document.createElement('li');inline(li,restore(item[3]));list.append(li);}
      else if(heading){flush();list=null;const title=document.createElement('h3');inline(title,restore(heading[1]));fragment.append(title);}
      else{list=null;paragraph.push(restore(line));}
    }
    flush();element.replaceChildren(fragment);element.classList.add('tutor-rich-text');
  }
  if(typeof module!=='undefined'&&module.exports)module.exports={normalize,render};
  if(typeof document!=='undefined')document.querySelectorAll('[data-rich-answer]').forEach(render);
})();
