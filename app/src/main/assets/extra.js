/* istighfar page extras: animated splash, onboarding / what's-new, in-app update sheet, settings rows */
(function(){
var $$=function(i){return document.getElementById(i)};
var wnOpen=false,upOpen=false,upBusy=false,upManual=false,upMode='';
function ver(x){return String(x||'').replace(/^\D+/,'').trim()}

/* ---------- markup ---------- */
var box=document.createElement('div');
box.innerHTML=
'<div id="scrim2"></div>'+
'<div id="upSheet"><div class="grab"></div><div id="upIco" data-ic="download" data-is="30"></div><div id="upT"></div><div id="upS"></div>'+
 '<div id="upVer"><span id="upCur"></span><span data-ic="chevR" data-is="18" style="background:none;border:0;padding:0;color:var(--gold)"></span><span class="n" id="upNew"></span></div>'+
 '<div id="upNotes"><span id="upList"></span></div><div id="upBar"><div id="upFill"></div></div><div id="upTxt"></div>'+
 '<button class="xb" id="upYes"></button><button class="xb ghost" id="upNo"></button></div>'+
'<div id="ov"><div class="pg" id="pg"></div><div class="ovh"><button id="ovBack"></button><button id="ovSkip"></button></div><div class="stage" id="ovStage"></div><div class="ovf" id="ovf"></div></div>';
while(box.firstChild)document.body.appendChild(box.firstChild);

/* ---------- splash (cold start only) ---------- */
var cold=false;try{cold=!sessionStorage.getItem('booted');sessionStorage.setItem('booted','1')}catch(e){}
var firstRun=!ls('ob',false);
function after(){
  if(firstRun)startFlow(obSteps(),function(){ss('ob',true);ss('wn',4);release()});
  else if(ls('wn',0)<4)startFlow(wnSteps(),function(){ss('wn',4);release()});
  else release()}
function endSplash(){var s=$$('splash');if(!s||s.classList.contains('hide'))return;s.classList.add('hide');setTimeout(function(){if(s.parentNode)s.parentNode.removeChild(s)},700);setTimeout(after,350)}
function release(){window.__go=true;window.__splashing=false}
if(cold){
  window.__splashing=true;window.__go=false;
  var sp=document.createElement('div');sp.id='splash';
  sp.innerHTML='<div class="lgw"><div class="halo"></div><div class="rip"></div>'+
   '<svg viewBox="0 0 108 108"><defs><radialGradient id="slg" cx="50%" cy="38%" r="70%"><stop offset="0" stop-color="#24406e"/><stop offset="1" stop-color="#0c1730"/></radialGradient></defs>'+
   '<rect class="tile" width="108" height="108" rx="30" fill="url(#slg)"/>'+
   '<rect class="thr" x="52.6" y="14" width="2.8" height="19" fill="#ffe9a0"/>'+
   '<circle class="gb" cx="54" cy="30" r="7" fill="#e5a43a"/>'+
   '<circle class="wb w1" cx="41" cy="60" r="6.5" fill="#f4ecd8"/><circle class="wb w2" cx="54" cy="55" r="6.5" fill="#f4ecd8"/><circle class="wb w3" cx="67" cy="60" r="6.5" fill="#f4ecd8"/>'+
   '<g class="bowl hit"><path d="M26 62Q30 86 54 86Q78 86 82 62Z" fill="#c58a54"/><path d="M26 62h56" stroke="#e8b27a" stroke-width="2.4" stroke-linecap="round"/></g></svg></div>'+
   '<div class="nm">'+t('app')+'</div><div class="sb">'+t('splash1')+'</div>';
  document.body.appendChild(sp);
  sp.addEventListener('click',endSplash);
  setTimeout(endSplash,3000);
}else if(firstRun||ls('wn',0)<4){window.__go=false;setTimeout(after,0)}

/* ---------- onboarding / what's new ---------- */
var flow=[],fi=0;
function ntfNow(){try{return (window.Android&&Android.notifEnabled)?!!Android.notifEnabled():!!ls('notif',false)}catch(e){return false}}
function setNtf(v){try{if(window.Android&&Android.setNotifications)Android.setNotifications(v);else ss('notif',v)}catch(e){}}
function stepHTML(s){return '<div class="stp"><div class="big '+(s.k||'')+'">'+ico(s.ic,54)+'</div><h2>'+s.t()+'</h2><p>'+s.s()+'</p>'+(s.body?s.body():'')+'</div>'}
function drawFlow(dir){
  var s=flow[fi],st=$$('ovStage');
  $$('pg').innerHTML=flow.map(function(_,i){return '<i'+(i<=fi?' class="d"':'')+'></i>'}).join('');
  $$('ovBack').style.visibility=fi?'visible':'hidden';$$('ovBack').textContent=t('back');
  $$('ovSkip').style.visibility=(fi<flow.length-1)?'visible':'hidden';$$('ovSkip').textContent=t('obSkip');
  var old=st.firstChild;
  function put(){st.innerHTML=stepHTML(s);if(s.after)s.after();
    var f=$$('ovf');f.innerHTML='';
    s.btns().forEach(function(b){var e=document.createElement('button');e.className='xb'+(b.g?' ghost':'');e.textContent=b.l;e.onclick=b.f;f.appendChild(e)})}
  if(old&&dir){old.classList.add('out');setTimeout(put,190)}else put();
}
function nextStep(){if(fi<flow.length-1){fi++;drawFlow(1)}else endFlow()}
function prevStep(){if(fi>0){fi--;drawFlow(1)}}
function endFlow(){var o=$$('ov');o.classList.add('out');setTimeout(function(){o.classList.remove('on','out');wnOpen=false;if(flow.done)flow.done()},340)}
$$('ovBack').addEventListener('click',prevStep);$$('ovSkip').addEventListener('click',endFlow);
function startFlow(steps,done){flow=steps;flow.done=done;fi=0;wnOpen=true;var o=$$('ov');o.classList.remove('out');o.classList.add('on');drawFlow(0)}
function chip(l,ok,txt){return '<div class="'+(ok?'ok':'')+'"><span>'+l+'</span><em>'+txt+'</em></div>'}
function obSteps(){return [
 {ic:'feather',t:function(){return t('ob1T')},s:function(){return t('ob1S')},btns:function(){return [{l:t('obStart'),f:nextStep}]}},
 {ic:'globe',k:'g',t:function(){return t('ob2T')},s:function(){return t('ob2S')},
  body:function(){return '<div class="lo"><button data-l="ar" class="'+(LANG==='ar'?'on':'')+'">العربية</button><button data-l="en" class="'+(LANG==='en'?'on':'')+'">English</button></div>'},
  after:function(){[].forEach.call(document.querySelectorAll('.lo button'),function(b){b.onclick=function(){setLang(b.getAttribute('data-l'));ss('lang',LANG);fillIcons();drawFlow(0)}})},
  btns:function(){return [{l:t('obNext'),f:nextStep}]}},
 {ic:'bell',k:'o',t:function(){return t('ob3T')},s:function(){return t('ob3S')},
  btns:function(){return [{l:t('obAllow'),f:function(){setNtf(true);setTimeout(nextStep,350)}},{l:t('obLater'),g:1,f:nextStep}]}},
 {ic:'check',k:'g',t:function(){return t('ob6T')},s:function(){return t('ob6S')},
  body:function(){var n=ntfNow();return '<div class="chips">'+chip(t('notifRow'),n,n?t('obOn'):t('obOff'))+'</div>'},
  btns:function(){return [{l:t('obStart'),f:endFlow}]}}]}
function wnSteps(){return [
 {ic:'feather',t:function(){return t('w1T')},s:function(){return t('w1S')},btns:function(){return [{l:t('obNext'),f:nextStep}]}},
 {ic:'kaaba',k:'g',t:function(){return t('w2T')},s:function(){return t('w2S')},btns:function(){return [{l:t('obNext'),f:nextStep}]}},
 {ic:'tasbih',k:'o',t:function(){return t('w3T')},s:function(){return t('w3S')},btns:function(){return [{l:t('obNext'),f:nextStep}]}},
 {ic:'moon',t:function(){return t('w4T')},s:function(){return t('w4S')},btns:function(){return [{l:t('obStart'),f:endFlow}]}}]}

/* ---------- update sheet ---------- */
function scrim(){$$('scrim2').classList.toggle('on',upOpen)}
function openUp(on){upOpen=on;$$('upSheet').classList.toggle('on',on);scrim()}
function upState(k){
  upMode=k;var yes=$$('upYes'),no=$$('upNo');
  $$('upIco').classList.toggle('pulse',k==='new'||k==='busy');
  $$('upBar').style.display=(k==='busy')?'block':'none';
  $$('upNotes').style.display=(k==='new'&&$$('upList').childNodes.length)?'block':'none';
  $$('upVer').style.display=(k==='new')?'flex':'none';
  yes.style.display=(k==='busy'||k==='check'||k==='none')?'none':'flex';
  no.style.display=(k==='busy')?'none':'flex';
  $$('upTxt').textContent='';
  if(k==='new'){$$('upT').textContent=t('upTitle');$$('upS').textContent='';yes.textContent=t('upNow');no.textContent=t('later')}
  if(k==='check'){$$('upT').textContent=t('upd');$$('upS').textContent='…';no.textContent=t('back')}
  if(k==='none'){$$('upT').textContent=t('upLatest');$$('upS').textContent=window.__curVer?(t('updS')+': '+ar(ver(window.__curVer))):'';no.textContent=t('obDone')}
  if(k==='err'){$$('upT').textContent=t('upErr');$$('upS').textContent='';yes.textContent=t('upRetry');no.textContent=t('back')}
  if(k==='fail'){$$('upT').textContent=t('upFail');yes.textContent=t('upRetry');no.textContent=t('back')}
  if(k==='busy'){$$('upT').textContent=t('upDl');$$('upS').textContent=''}
}
function checkUpdate(manual){
  upManual=!!manual;
  if(manual){upState('check');openUp(true)}
  try{if(window.Android&&Android.checkUpdate){Android.checkUpdate();return}}catch(e){}
  if(manual)upState('err')
}
window.__updateInfo=function(r){
  if(upBusy)return;
  if(r&&r.current){window.__curVer=r.current;updSub()}
  if(!r||r.error){if(upManual)upState('err');return}
  if(!r.available){if(upManual)upState('none');return}
  $$('upCur').textContent=ar(ver(r.current));$$('upNew').textContent=ar(ver(r.name));
  var L=String(r.notes||'').split(/\r?\n/).map(function(x){return x.replace(/^[\s•\-*·]+/,'').trim()}).filter(Boolean).slice(0,6);
  var b=$$('upList');b.innerHTML='';
  L.forEach(function(x){var d=document.createElement('div');d.textContent=x;b.appendChild(d)});
  upBusy=false;upState('new');openUp(true)
};
window.__updateProgress=function(p){
  if(p>100){upBusy=false;upState('new');$$('upT').textContent=t('obDone');$$('upS').textContent=t('upDone');$$('upYes').style.display='none';$$('upNotes').style.display='none';$$('upVer').style.display='none';return}
  upBusy=true;upState('busy');$$('upFill').style.width=p+'%';$$('upTxt').textContent=ar(p)+'٪'
};
window.__updateFail=function(w){upBusy=false;upState('fail');$$('upS').textContent=(w==='perm')?t('upPerm'):t('upFail')};
$$('upYes').addEventListener('click',function(){
  if(upMode==='err'||upMode==='fail'){checkUpdate(true);return}
  upBusy=true;upState('busy');$$('upFill').style.width='0%';$$('upTxt').textContent=t('upStart');
  try{Android.installUpdate()}catch(e){upBusy=false;upState('fail')}
});
$$('upNo').addEventListener('click',function(){if(!upBusy)openUp(false)});
$$('scrim2').addEventListener('click',function(){if(upOpen&&!upBusy)openUp(false)});
function updSub(){var s=$$('updSub');if(!s)return;var v='';try{v=(window.Android&&Android.appVersion)?Android.appVersion():''}catch(e){}
  window.__curVer=v||window.__curVer||'';s.textContent=window.__curVer?(t('updS')+' '+ar(ver(window.__curVer))):t('upd')}

/* ---------- settings sheet rows ---------- */
function segSync(){
  [].forEach.call(document.querySelectorAll('#thSeg button'),function(b){b.classList.toggle('on',b.getAttribute('data-th')===THEME)});
  [].forEach.call(document.querySelectorAll('#lgSeg button'),function(b){b.classList.toggle('on',b.getAttribute('data-lg')===LANG)});
  updSub()}
var thSeg=$$('thSeg'),lgSeg=$$('lgSeg');
if(thSeg)thSeg.addEventListener('click',function(e){var b=e.target.closest('button');if(!b)return;THEME=b.getAttribute('data-th');ss('theme',THEME);applyTheme();segSync()});
if(lgSeg)lgSeg.addEventListener('click',function(e){var b=e.target.closest('button');if(!b)return;setLang(b.getAttribute('data-lg'));ss('lang',LANG);segSync()});
function go(u){if(window.__app)__app.openSheet(false);setTimeout(function(){nav(u)},120)}
if($$('goTsb'))$$('goTsb').addEventListener('click',function(){go('tasbih.html')});
if($$('goQib'))$$('goQib').addEventListener('click',function(){go('qibla.html')});
if($$('updRow'))$$('updRow').addEventListener('click',function(){if(window.__app)__app.openSheet(false);setTimeout(function(){checkUpdate(true)},200)});
if($$('shareRow'))$$('shareRow').addEventListener('click',function(){var b=$$('shareApp');if(b)b.click()});
var prevRefresh=window.onLangChange;
window.onLangChange=function(){if(prevRefresh)prevRefresh();segSync()};
segSync();applyI18n();fillIcons();

/* ---------- android back ---------- */
var prevBack=window.__back;
window.__back=function(){
  if(wnOpen)return true;
  if(upOpen){if(!upBusy)openUp(false);return true}
  return prevBack?prevBack():false};
setTimeout(function(){checkUpdate(false)},7000);
})();
