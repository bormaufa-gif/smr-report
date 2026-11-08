(function(){
if(window.ptsDictation)return;
let pending=null;
const allowed=s=>/^(проблемы\s*[/]\s*замечания|прочие работы|план на завтра)(?:$|[\s(:])/i.test(s.trim().replace(/^[^а-яё]+/i,''));
function caption(e){if(e.labels)for(const l of e.labels)if(allowed(l.textContent))return l.textContent;let p=e.previousElementSibling;for(let i=0;p&&i<4;i++,p=p.previousElementSibling){if(p.matches('textarea,input'))break;if(p.matches('label,h3,h4')&&allowed(p.textContent))return p.textContent;}return ''; }
function scan(){document.querySelectorAll('textarea').forEach(e=>{if(e.disabled||e.readOnly||!caption(e))return;if(e.nextElementSibling&&e.nextElementSibling.dataset.ptsVoice==='1')return;const b=document.createElement('button');b.type='button';b.dataset.ptsVoice='1';b.textContent='🎤 Надиктовать';b.style.cssText='display:block;margin:6px 0 12px;padding:10px 14px;border:0;border-radius:10px;background:#7861aa;color:white;font:inherit;';b.onclick=event=>{if(!event.isTrusted||e.disabled||e.readOnly||!e.isConnected)return;pending={e:e,v:e.value};location.href='ptsvoice://start';};e.insertAdjacentElement('afterend',b);});}
window.ptsDictation={ready:()=>!!(pending&&pending.e.isConnected&&!pending.e.disabled&&!pending.e.readOnly),cancel:()=>{pending=null;},insert:text=>{const s=pending;if(!s||!s.e.isConnected||s.e.disabled||s.e.readOnly||s.e.value!==s.v)return false;const value=s.v+(s.v?'\n':'')+text;if(s.e.maxLength>=0&&value.length>s.e.maxLength)return false;pending=null;Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set.call(s.e,value);s.e.dispatchEvent(new Event('input',{bubbles:true}));s.e.dispatchEvent(new Event('change',{bubbles:true}));return true;}};
new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});scan();
})();
