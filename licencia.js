/* Portero de licencia SEYEAL — bloquea la app hasta introducir una clave válida
   de ESTE teléfono. Misma lógica y secreto que LicenseManager.kt (repo luis971227/Licencia). */
(function(){
'use strict';
var APP_CODE='COPACD';
var SECRET='SEYEAL-0758pf1q5zq42eeablb3oss1-2026';
var LS_KEY='seyeal_lic_'+APP_CODE;
var B32='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
var DEVICE_ID=null;

var K=[0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2];

function sha256Bytes(bytes){
 var H=[0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19];
 var l=bytes.length,w1=l+1,k=(56-(w1%64)+64)%64,total=w1+k+8;
 var m=new Array(total),i;
 for(i=0;i<l;i++)m[i]=bytes[i];
 m[l]=0x80;
 for(i=l+1;i<total-8;i++)m[i]=0;
 var hi=Math.floor(l/536870912),lo=(l*8)%4294967296;
 m[total-8]=(hi>>>24)&255;m[total-7]=(hi>>>16)&255;m[total-6]=(hi>>>8)&255;m[total-5]=hi&255;
 m[total-4]=(lo>>>24)&255;m[total-3]=(lo>>>16)&255;m[total-2]=(lo>>>8)&255;m[total-1]=lo&255;
 var w=new Array(64);
 for(var off=0;off<total;off+=64){
  for(i=0;i<16;i++)w[i]=((m[off+4*i]<<24)|(m[off+4*i+1]<<16)|(m[off+4*i+2]<<8)|m[off+4*i+3])|0;
  for(i=16;i<64;i++){
   var s0=((w[i-15]>>>7)|(w[i-15]<<25))^((w[i-15]>>>18)|(w[i-15]<<14))^(w[i-15]>>>3);
   var s1=((w[i-2]>>>17)|(w[i-2]<<15))^((w[i-2]>>>19)|(w[i-2]<<13))^(w[i-2]>>>10);
   w[i]=(w[i-16]+s0+w[i-7]+s1)|0;
  }
  var a=H[0],b=H[1],c=H[2],d=H[3],e=H[4],f=H[5],g=H[6],h=H[7];
  for(i=0;i<64;i++){
   var S1=((e>>>6)|(e<<26))^((e>>>11)|(e<<21))^((e>>>25)|(e<<7));
   var ch=(e&f)^(~e&g);
   var t1=(h+S1+ch+K[i]+w[i])|0;
   var S0=((a>>>2)|(a<<30))^((a>>>13)|(a<<19))^((a>>>22)|(a<<10));
   var mj=(a&b)^(a&c)^(b&c);
   var t2=(S0+mj)|0;
   h=g;g=f;f=e;e=(d+t1)|0;d=c;c=b;b=a;a=(t1+t2)|0;
  }
  H[0]=(H[0]+a)|0;H[1]=(H[1]+b)|0;H[2]=(H[2]+c)|0;H[3]=(H[3]+d)|0;
  H[4]=(H[4]+e)|0;H[5]=(H[5]+f)|0;H[6]=(H[6]+g)|0;H[7]=(H[7]+h)|0;
 }
 var out=new Array(32);
 for(i=0;i<8;i++){out[4*i]=(H[i]>>>24)&255;out[4*i+1]=(H[i]>>>16)&255;out[4*i+2]=(H[i]>>>8)&255;out[4*i+3]=H[i]&255;}
 return out;
}
function utf8Bytes(str){
 var s=unescape(encodeURIComponent(str)),out=new Array(s.length);
 for(var i=0;i<s.length;i++)out[i]=s.charCodeAt(i)&255;
 return out;
}
function hmacSha256(keyBytes,msgBytes){
 var key=keyBytes.length>64?sha256Bytes(keyBytes):keyBytes,i;
 var ipad=new Array(64),opad=new Array(64);
 for(i=0;i<64;i++){var b=i<key.length?key[i]:0;ipad[i]=b^0x36;opad[i]=b^0x5c;}
 var inner=ipad.concat(msgBytes);
 var ih=sha256Bytes(inner);
 return sha256Bytes(opad.concat(ih));
}
function firmaLicencia(appCode,deviceCode,expiry){
 var mac=hmacSha256(utf8Bytes(SECRET),utf8Bytes(appCode.toUpperCase()+deviceCode+expiry));
 var bits=0,i;
 for(i=0;i<5;i++)bits=bits*256+mac[i];
 var s='';
 for(i=0;i<8;i++){s+=B32[Math.floor(bits/Math.pow(2,35-i*5))%32];}
 return s;
}
function validaClave(clave,appCode,deviceCode){
 var p=String(clave||'').trim().toUpperCase().split('-');
 if(p.length!==5||p[0]!=='SEYEAL')return{ok:false,motivo:'Formato de clave incorrecto'};
 if(p[1]!==appCode.toUpperCase())return{ok:false,motivo:'La clave no corresponde a esta app ('+appCode+')'};
 if(!deviceCode)return{ok:false,motivo:'Preparando el teléfono, espera unos segundos…'};
 if(p[2]!==deviceCode)return{ok:false,motivo:'Esta licencia es para OTRO teléfono'};
 if(!/^\d{8}$/.test(p[3]))return{ok:false,motivo:'Fecha de caducidad inválida'};
 if(p[4]!==firmaLicencia(p[1],p[2],p[3]))return{ok:false,motivo:'Firma inválida: clave falsificada o mal copiada'};
 var y=+p[3].slice(0,4),mo=+p[3].slice(4,6),d=+p[3].slice(6,8);
 var exp=Date.UTC(y,mo-1,d)+24*3600*1000;
 if(Date.now()>exp)return{ok:false,motivo:'Licencia caducada el '+p[3]};
 return{ok:true};
}
if(typeof module!=='undefined'&&module.exports){
 module.exports={sha256Bytes:sha256Bytes,hmacSha256:hmacSha256,firmaLicencia:firmaLicencia,validaClave:validaClave};
}
if(typeof document==='undefined')return;

var ov=null,guardada=null;
try{guardada=localStorage.getItem(LS_KEY);}catch(e){}

function desbloquear(){
 if(ov&&ov.parentNode)ov.parentNode.removeChild(ov);
 ov=null;guardada=null;
}
function bloquear(motivoPrevio){
 if(ov)return;
 var el=document.createElement('div');
 el.id='gate-licencia';
 el.style.cssText='position:fixed;inset:0;z-index:999999;background:radial-gradient(circle at 50% 18%,#571015 0%,#2A0709 45%,#120304 100%);display:flex;align-items:center;justify-content:center;padding:20px;font-family:Arial,Helvetica,sans-serif';
 var tarjeta=document.createElement('div');
 tarjeta.style.cssText='background:#131318;border:1px solid #2A2A31;border-radius:20px;padding:26px 24px;max-width:350px;width:100%;box-shadow:0 24px 80px rgba(0,0,0,.7),0 0 0 1px rgba(229,57,53,.08);text-align:center;font-family:Arial,Helvetica,sans-serif';
 tarjeta.innerHTML='<div style="width:64px;height:64px;margin:0 auto 12px;border-radius:50%;background:linear-gradient(135deg,#E53935,#7A0C14);display:flex;align-items:center;justify-content:center;font-size:30px;box-shadow:0 8px 24px rgba(229,57,53,.35)">🔐</div>'
  +'<h2 style="margin:0 0 6px;font-size:18px;color:#FFFFFF;letter-spacing:2px;font-weight:900">LICENCIA REQUERIDA</h2>'
  +'<p style="margin:0 0 16px;font-size:12px;color:#8E8E96;line-height:1.5">Esta licencia es para un solo teléfono.<br>Copia los códigos y pídele la clave al administrador.</p>'
  +'<div style="background:#0E0E12;border:1px solid #33333B;border-radius:12px;padding:12px 14px;margin:0 0 12px">'
  +'<div style="display:flex;justify-content:space-between;align-items:center;padding:4px 0">'
  +'<span style="font-size:10px;color:#8E8E96;letter-spacing:1.5px;font-weight:800">APP</span>'
  +'<b style="font-size:14px;color:#F2B8B5;letter-spacing:1.5px">'+APP_CODE+'</b></div>'
  +'<div style="height:1px;background:#26262B;margin:6px 0"></div>'
  +'<div style="display:flex;justify-content:space-between;align-items:center;padding:4px 0">'
  +'<span style="font-size:10px;color:#8E8E96;letter-spacing:1.5px;font-weight:800">TELÉFONO</span>'
  +'<b id="gate-dev" style="font-size:14px;color:#F2B8B5;letter-spacing:1.5px">···</b></div>'
  +'</div>'
  +'<button id="gate-copiar-cod" type="button" style="width:100%;border:1px solid #5A1620;background:#1B1114;color:#F2B8B5;border-radius:10px;font-size:11.5px;font-weight:800;letter-spacing:1px;padding:10px;cursor:pointer;margin:0 0 14px">⧉ COPIAR CÓDIGOS</button>'
  +'<input id="gate-clave" type="text" autocapitalize="characters" autocomplete="off" spellcheck="false" placeholder="SEYEAL-…-20260101-XXXXXXXX" style="width:100%;box-sizing:border-box;padding:12px;background:#0E0E12;border:1px solid #33333B;border-radius:10px;color:#FFFFFF;font-size:14px;text-align:center;text-transform:uppercase;margin-bottom:10px;outline:none">'
  +'<button id="gate-btn" type="button" style="width:100%;padding:13px;border:none;border-radius:10px;background:linear-gradient(135deg,#E53935,#7A0C14);color:#fff;font-size:14px;font-weight:800;letter-spacing:1.5px;cursor:pointer;box-shadow:0 6px 20px rgba(229,57,53,.30)">ACTIVAR LICENCIA</button>'
  +'<p id="gate-msg" style="margin:12px 0 0;font-size:12px;min-height:15px;font-weight:700;line-height:1.4"></p>';
 el.appendChild(tarjeta);
 document.body.appendChild(el);
 ov=el;
 var msg=tarjeta.querySelector('#gate-msg');
 var inp=tarjeta.querySelector('#gate-clave');
 if(motivoPrevio){msg.style.color='#C81E1E';msg.textContent=motivoPrevio;}
 tarjeta.querySelector('#gate-copiar-cod').addEventListener('click',function(){
  var cod=APP_CODE+(DEVICE_ID?'-'+DEVICE_ID:'');
  try{
   var ta=document.createElement('textarea');
   ta.value=cod;ta.style.cssText='position:fixed;opacity:0';
   document.body.appendChild(ta);ta.select();document.execCommand('copy');
   document.body.removeChild(ta);
   msg.style.color='#1E7B4F';msg.textContent='Códigos copiados: '+cod;
  }catch(e){msg.style.color='#C81E1E';msg.textContent=cod;}
 });
 function intentar(){
  var r=validaClave(inp.value,APP_CODE,DEVICE_ID);
  if(r.ok){
   try{localStorage.setItem(LS_KEY,inp.value.trim().toUpperCase());}catch(e){}
   desbloquear();
  }else{
   msg.style.color='#C81E1E';
   msg.textContent=r.motivo;
  }
 }
 tarjeta.querySelector('#gate-btn').addEventListener('click',intentar);
 inp.addEventListener('keydown',function(ev){if(ev.key==='Enter')intentar();});
 setTimeout(function(){inp.focus();},300);
}
function decidirConDevice(){
 var dev=document.getElementById('gate-dev');
 if(dev&&DEVICE_ID)dev.textContent=DEVICE_ID;
 if(!guardada)return;
 var r=validaClave(guardada,APP_CODE,DEVICE_ID);
 if(r.ok){desbloquear();}
 else if(ov){
  var m=ov.querySelector('#gate-msg');
  if(m){m.style.color='#C81E1E';m.textContent=r.motivo;}
 }
}
function usarId(id){
 if(!id)return;
 DEVICE_ID=String(id).toUpperCase();
 decidirConDevice();
}
function idFallback(){
 if(DEVICE_ID)return;
 try{DEVICE_ID=localStorage.getItem('seyeal_dev_id');}catch(e){}
 if(!DEVICE_ID){
  var s='',i;
  for(i=0;i<16;i++)s+='0123456789ABCDEF'[Math.floor(Math.random()*16)];
  DEVICE_ID=s;
  try{localStorage.setItem('seyeal_dev_id',DEVICE_ID);}catch(e){}
 }
 decidirConDevice();
}
function pedirIdNativo(){
 if(typeof cdpCompartir!=='undefined'&&cdpCompartir&&cdpCompartir.idTelefono){
  try{cdpCompartir.idTelefono(usarId,idFallback);return true;}catch(e){}
 }
 return false;
}
if(!pedirIdNativo()){
 document.addEventListener('deviceready',function(){
  if(!DEVICE_ID&&!pedirIdNativo())idFallback();
 },false);
 setTimeout(function(){if(!DEVICE_ID)idFallback();},3000);
}
bloquear(null);
})();
