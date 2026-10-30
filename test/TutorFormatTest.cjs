// Development-only checks. SmartStu still runs on Java/Tomcat, no Node runtime.
const assert=require('node:assert/strict');
const {normalize,render}=require('../web/assets/js/tutor-format.js');
const katex=require('../web/assets/vendor/katex/katex.min.js');
let checks=0,formulas=[];
function check(condition){assert.ok(condition);checks++;}
class Element{
  constructor(tag){this.tagName=tag.toUpperCase();this.children=[];this.value='';this.classes=new Set();this.classList={add:value=>this.classes.add(value)};}
  append(...nodes){this.children.push(...nodes);}
  set textContent(value){this.value=value;this.children=[];}
  get textContent(){return this.value+this.children.map(node=>node.textContent).join('');}
  replaceChildren(...nodes){this.value='';this.children=nodes;}
}
global.document={createElement:tag=>new Element(tag),createTextNode:value=>({textContent:value}),createDocumentFragment:()=>new Element('fragment')};
global.window={katex:{render:(formula,node,options)=>{
  katex.renderToString(formula,options); // Actual parser, including security limits.
  formulas.push(formula);node.textContent='[formula]';
}}};
const walk=node=>[node,...(node.children??[]).flatMap(walk)];
function display(source){formulas=[];const root=new Element('div');root.textContent=source;render(root);return root;}
check(normalize(String.raw`Xin chào\n\n1. **Dữ kiện**\n - Quãng đường`)=== 'Xin chào\n\n1. **Dữ kiện**\n - Quãng đường');
check(normalize(String.raw`Dòng 1\r\nDòng 2`)==='Dòng 1\nDòng 2');
check(normalize(String.raw`$\nu + \nabla x \neq 0$` )===String.raw`$\nu + \nabla x \neq 0$`);
check(normalize(String.raw`Giữ \nu và \nabla và \neq`)===String.raw`Giữ \nu và \nabla và \neq`);
check(normalize(String.raw`Dòng trước\nVậy kết quả`)==='Dòng trước\nVậy kết quả');
check(normalize('Mã: `\\n`').includes(String.raw`\n`));
const screenshot=String.raw`Chào em, chúng ta thực hiện các bước sau:\n\n1. **Tóm tắt dữ kiện:**\n - Quãng đường $s = 1\text{ km} = 1000\text{ m}$\n - Thời gian $t = 10\text{ phút} = 10\times 60 = 600\text{ s}$\n\n2. **Áp dụng công thức:**\n$$v = \frac{s}{t}$$\n\n3. **Thay số:**\n$$v = \frac{1000}{600} \approx 1,67\text{ m/s}$\n\nVậy kết quả là $1,67\text{ m/s}$.`;
let root=display(screenshot),nodes=walk(root);
check(!root.textContent.includes(String.raw`\n`));
check(!root.textContent.includes('$'));
check(formulas.length===5);
check(nodes.filter(node=>node.tagName==='STRONG').length===3);
check(nodes.filter(node=>node.tagName==='LI').length===5);
check(!nodes.some(node=>node.classes?.has('tutor-math-fallback')));
root=display(String.raw`**Gia tốc $a=\frac{F}{m}$**`);
check(formulas.length===1&&walk(root).some(node=>node.tagName==='STRONG'));
root=display('<img src=x onerror=alert(1)>\n<script>alert(1)</script>');
check(root.textContent.includes('<script>'));
check(!walk(root).some(node=>['IMG','SCRIPT'].includes(node.tagName)));
root=display(String.raw`Sai công thức $\frac{$`);
check(walk(root).some(node=>node.classes?.has('tutor-math-fallback')));
root=display('Đoạn một\n\nĐoạn hai');
check(walk(root).filter(node=>node.tagName==='P').length===2);
root=display(String.raw`$\href{javascript:alert(1)}{x}$`);
check(formulas.length===1||walk(root).some(node=>node.classes?.has('tutor-math-fallback')));
console.log(`PASS ${checks} tutor formatting checks (escaped newlines, mixed delimiters, real KaTeX parsing, safe text).`);
