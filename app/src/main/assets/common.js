/* shared helpers: storage, XHR json, location, prayer-time maths, alarms */
var $=function(i){return document.getElementById(i)};
function ls(k,d){try{var v=localStorage.getItem(k);return v==null?d:JSON.parse(v)}catch(e){return d}}
function ss(k,v){try{localStorage.setItem(k,JSON.stringify(v))}catch(e){}}
function getJSON(u,ok,bad){var x=new XMLHttpRequest();x.open('GET',u,true);
  x.onload=function(){try{ok(JSON.parse(x.responseText))}catch(e){bad&&bad(e)}};
  x.onerror=function(){bad&&bad(new Error('xhr'))};x.send()}
function toast(t){var e=$('toast');if(!e)return;e.textContent=t;e.style.display='block';clearTimeout(toast._t);toast._t=setTimeout(function(){e.style.display='none'},2600)}
var AR='٠١٢٣٤٥٦٧٨٩';function ar(n){return String(n).replace(/\d/g,function(d){return AR[d]})}

/* ---------- location ---------- */
function askLocation(cb){window.onLoc=cb;
  if(window.Android&&Android.requestLocation){Android.requestLocation()}
  else if(navigator.geolocation){navigator.geolocation.getCurrentPosition(function(p){window.__locResult({lat:p.coords.latitude,lng:p.coords.longitude,gov:'',dist:'',city:'',country:''},null)},function(){window.__locResult(null,'denied')},{timeout:20000})}
  else window.__locResult(null,'none')}
window.__locResult=function(o,err){
  if(o){var p=ls('pray',{});p.loc=o;ss('pray',p)}
  if(window.onLoc)window.onLoc(o,err)};
function placeName(l){if(!l)return '';var a=[];
  [l.dist||l.city,l.gov,l.country].forEach(function(x){if(x&&a.indexOf(x)<0)a.push(x)});
  return a.length?a.join('، '):('خط العرض '+l.lat.toFixed(2)+'، خط الطول '+l.lng.toFixed(2))}

/* ---------- prayer times (offline, PrayTimes-style astronomical algorithm) ---------- */
var METHODS={
 mwl:{n:'رابطة العالم الإسلامي',f:18,i:17},
 egypt:{n:'الهيئة المصرية العامة للمساحة',f:19.5,i:17.5},
 karachi:{n:'جامعة العلوم الإسلامية بكراتشي',f:18,i:18},
 makkah:{n:'أم القرى (مكة المكرمة)',f:18.5,im:90},
 isna:{n:'أمريكا الشمالية (ISNA)',f:15,i:15},
 jafari:{n:'الجعفري (معهد الجيوفيزياء – طهران)',f:17.7,i:14,m:4.5},
 leva:{n:'الجعفري (ليفا – قم)',f:16,i:14,m:4}
};
var NAMES=[['fajr','الفجر'],['sunrise','الشروق'],['dhuhr','الظهر'],['asr','العصر'],['maghrib','المغرب'],['isha','العشاء']];
var D2R=Math.PI/180,R2D=180/Math.PI;
function sn(x){return Math.sin(x*D2R)}function cs(x){return Math.cos(x*D2R)}function tn(x){return Math.tan(x*D2R)}
function fix(a,b){a=a-b*Math.floor(a/b);return a<0?a+b:a}
function jd(y,m,d){if(m<=2){y-=1;m+=12}var A=Math.floor(y/100),B=2-A+Math.floor(A/4);
  return Math.floor(365.25*(y+4716))+Math.floor(30.6001*(m+1))+d+B-1524.5}
function sunPos(j){var D=j-2451545,g=fix(357.529+.98560028*D,360),q=fix(280.459+.98564736*D,360),
  L=fix(q+1.915*sn(g)+.02*sn(2*g),360),e=23.439-3.6e-7*D,
  RA=fix(Math.atan2(cs(e)*sn(L),cs(L))*R2D/15,24);
  return{decl:Math.asin(sn(e)*sn(L))*R2D,eq:q/15-RA}}
function prayerTimes(date,lat,lng,tz,method,asrF){
  var M=METHODS[method]||METHODS.mwl,J=jd(date.getFullYear(),date.getMonth()+1,date.getDate())-lng/360;
  function mid(t){return fix(12-sunPos(J+t).eq,24)}
  function ct(angle,t){var d=sunPos(J+t).decl,x=(-sn(angle)-sn(d)*sn(lat))/(cs(d)*cs(lat));
    if(x>1||x<-1)return NaN;return mid(t)+(angle>90?-1:1)*Math.acos(x)*R2D/15}
  function asr(t){var d=sunPos(J+t).decl,a=-Math.atan(1/(asrF+tn(Math.abs(lat-d))))*R2D;return ct(a,t)}
  function calc(t){return{
    fajr:ct(180-M.f,t.fajr),sunrise:ct(180-.833,t.sunrise),dhuhr:mid(t.dhuhr),asr:asr(t.asr),
    maghrib:ct(M.m||.833,t.maghrib),isha:M.im?NaN:ct(M.i,t.isha)}}
  var t={fajr:5/24,sunrise:6/24,dhuhr:.5,asr:13/24,maghrib:18/24,isha:18/24},r=calc(t);
  for(var k in r){t[k]=isNaN(r[k])?t[k]:r[k]/24}
  r=calc(t);
  if(M.im)r.isha=r.maghrib+M.im/60;
  var adj=tz-lng/15;for(var k in r)r[k]+=adj;
  r.dhuhr+=1/60;if(!M.im)r.maghrib+=0; if(M.im)r.isha+=1/60*0;
  /* high-latitude fallback: seventh of the night */
  var night=fix(r.sunrise-r.maghrib,24);
  if(isNaN(r.fajr))r.fajr=r.sunrise-night/7;if(isNaN(r.isha))r.isha=r.maghrib+night/7;
  return r}
function hmToDate(day,h){var m=Math.round(h*60);return new Date(day.getFullYear(),day.getMonth(),day.getDate(),0,m,0)}
function fmt(d){var h=d.getHours(),m=d.getMinutes(),ap=h>=12?'م':'ص';h=h%12||12;return ar(h)+':'+ar((m<10?'0':'')+m)+' '+ap}
function dayTimes(day){var p=ls('pray',{});if(!p.loc)return null;
  var tz=-new Date(day.getFullYear(),day.getMonth(),day.getDate(),12).getTimezoneOffset()/60;
  var r=prayerTimes(day,p.loc.lat,p.loc.lng,tz,p.method||'mwl',p.asr||1),o={};
  NAMES.forEach(function(n){o[n[0]]=hmToDate(day,r[n[0]])});return o}
function nextPrayer(){var now=new Date(),i,d,t;
  for(i=0;i<2;i++){d=new Date(now.getFullYear(),now.getMonth(),now.getDate()+i);t=dayTimes(d);if(!t)return null;
    for(var k=0;k<NAMES.length;k++){if(NAMES[k][0]=='sunrise')continue;var x=t[NAMES[k][0]];if(x>now)return{key:NAMES[k][0],name:NAMES[k][1],at:x}}}
  return null}
function countdown(ms){var s=Math.max(0,Math.floor(ms/1000)),h=Math.floor(s/3600),m=Math.floor(s%3600/60);
  return (h?ar(h)+' ساعة و ':'')+ar(m)+' دقيقة'}
function qibla(lat,lng){var k1=21.4225,k2=39.8262,dl=(k2-lng)*D2R,a=lat*D2R,b=k1*D2R;
  return fix(Math.atan2(Math.sin(dl),Math.cos(a)*Math.tan(b)-Math.sin(a)*Math.cos(dl))*R2D,360)}
/* hand the next 7 days of enabled prayers to the Android alarm scheduler */
function syncAlarms(){if(!(window.Android&&Android.setPrayerAlarms))return;
  var p=ls('pray',{}),al=p.alarms||{},out=[],now=new Date();
  if(p.loc){for(var i=0;i<7;i++){var t=dayTimes(new Date(now.getFullYear(),now.getMonth(),now.getDate()+i));
    NAMES.forEach(function(n){if(n[0]=='sunrise'||al[n[0]]===false)return;
      if(t[n[0]]>now)out.push({t:t[n[0]].getTime(),n:n[1]})})}}
  try{Android.setPrayerAlarms(JSON.stringify(out))}catch(e){}}
