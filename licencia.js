/* Portero de licencia SEYEAL — bloquea la app hasta introducir una clave válida.
   Misma lógica y secreto que LicenseManager.kt (repo luis971227/Licencia). */
(function(){
'use strict';
var APP_CODE='COPACD';
var SECRET='SEYEAL-0758pf1q5zq42eeablb3oss1-2026';
var LS_KEY='seyeal_lic_'+APP_CODE;
var B32='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

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
function firmaLicencia(appCode,expiry){
 var mac=hmacSha256(utf8Bytes(SECRET),utf8Bytes(appCode.toUpperCase()+expiry));
 var bits=0,i;
 for(i=0;i<5;i++)bits=bits*256+mac[i];
 var s='';
 for(i=0;i<8;i++){s+=B32[Math.floor(bits/Math.pow(2,35-i*5))%32];}
 return s;
}
function validaClave(clave,appCode){
 var p=String(clave||'').trim().toUpperCase().split('-');
 if(p.length!==4||p[0]!=='SEYEAL')return{ok:false,motivo:'Formato de clave incorrecto'};
 if(p[1]!==appCode.toUpperCase())return{ok:false,motivo:'La clave no corresponde a esta app ('+appCode+')'};
 if(!/^\d{8}$/.test(p[2]))return{ok:false,motivo:'Fecha de caducidad inválida'};
 if(p[3]!==firmaLicencia(p[1],p[2]))return{ok:false,motivo:'Firma inválida: clave falsificada o mal copiada'};
 var y=+p[2].slice(0,4),mo=+p[2].slice(4,6),d=+p[2].slice(6,8);
 var exp=Date.UTC(y,mo-1,d)+24*3600*1000;
 if(Date.now()>exp)return{ok:false,motivo:'Licencia caducada el '+p[2]};
 return{ok:true};
}
if(typeof module!=='undefined'&&module.exports){
 module.exports={sha256Bytes:sha256Bytes,hmacSha256:hmacSha256,firmaLicencia:firmaLicencia,validaClave:validaClave};
}
if(typeof document==='undefined')return;

function bloquear(motivoPrevio){
 var ov=document.createElement('div');
 ov.id='gate-licencia';
 ov.style.cssText='position:fixed;inset:0;z-index:999999;background:linear-gradient(160deg,#8C1614,#3d0a09);display:flex;align-items:center;justify-content:center;padding:20px;font-family:Arial,Helvetica,sans-serif';
 var tarjeta=document.createElement('div');
 tarjeta.style.cssText='background:#fff;border-radius:14px;padding:22px;max-width:340px;width:100%;box-shadow:0 10px 40px rgba(0,0,0,.45);text-align:center';
 tarjeta.innerHTML='<div style="font-size:40px">🔐</div>'
  +'<h2 style="margin:8px 0 4px;font-size:20px;color:#151515">Licencia requerida</h2>'
  +'<p id="gate-sub" style="margin:0 0 6px;font-size:12.5px;color:#7F7F7F">Esta aplicación necesita una clave de licencia válida para usarse.</p>'
  +'<p style="margin:0 0 14px;font-size:12px;color:#151515">Código de esta app: <b style="color:#8C1614;letter-spacing:1px">'+APP_CODE+'</b> · cópialo en el generador para crear la clave.</p>'
  +'<input id="gate-clave" type="text" autocapitalize="characters" autocomplete="off" spellcheck="false" placeholder="SEYEAL-'+APP_CODE+'-20260101-XXXXXXXX" style="width:100%;box-sizing:border-box;padding:11px;border:1px solid #E5E5E5;border-radius:9px;font-size:14px;text-align:center;text-transform:uppercase;margin-bottom:9px">'
  +'<button id="gate-btn" type="button" style="width:100%;padding:12px;border:none;border-radius:9px;background:#8C1614;color:#fff;font-size:15px;font-weight:800;cursor:pointer">Activar licencia</button>'
  +'<p id="gate-msg" style="margin:11px 0 0;font-size:12.5px;min-height:16px;font-weight:700"></p>';
 ov.appendChild(tarjeta);
 document.body.appendChild(ov);
 var msg=tarjeta.querySelector('#gate-msg');
 var inp=tarjeta.querySelector('#gate-clave');
 if(motivoPrevio){msg.style.color='#C81E1E';msg.textContent=motivoPrevio;}
 function intentar(){
  var r=validaClave(inp.value,APP_CODE);
  if(r.ok){
   try{localStorage.setItem(LS_KEY,inp.value.trim().toUpperCase());}catch(e){}
   ov.parentNode.removeChild(ov);
  }else{
   msg.style.color='#C81E1E';
   msg.textContent=r.motivo;
  }
 }
 tarjeta.querySelector('#gate-btn').addEventListener('click',intentar);
 inp.addEventListener('keydown',function(ev){if(ev.key==='Enter')intentar();});
 setTimeout(function(){inp.focus();},300);
}

var guardada=null;
try{guardada=localStorage.getItem(LS_KEY);}catch(e){}
if(guardada){
 var r=validaClave(guardada,APP_CODE);
 if(r.ok)return;
 bloquear(r.motivo);
}else{
 bloquear(null);
}
})();
