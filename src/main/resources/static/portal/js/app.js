(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const API = '/api/v1';
  const state = { csrf:null, user:null, role:null, group:null, groups:[],
    evaluations:[], evaluation:null, grades:[], gradeRow:null, editingEval:null, seq:0 };

  function notice(message, good=false) {
    $('notice').textContent = message;
    $('notice').hidden = !message;
    $('notice').classList.toggle('ok', good);
  }
  async function request(path, options={}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { Accept:'application/json', ...(options.headers||{}) };
    if (options.body) headers['Content-Type'] = 'application/json';
    if (!['GET','HEAD'].includes(method)) {
      const token = state.csrf || await fetchCsrf();
      headers[token.headerName] = token.token;
    }
    const r = await fetch(API+path, { credentials:'same-origin', cache:'no-store', ...options, headers });
    if (!r.ok) {
      let message = `Solicitud rechazada (HTTP ${r.status})`;
      if (r.status===401) message='Tu sesión expiró. Inicia sesión otra vez.';
      if (r.status===403) message='No tienes permiso para esta operación.';
      if (r.status===404) message='El registro no está asignado a tu cuenta.';
      if (r.status===409) message='La operación no está permitida por las reglas académicas.';
      throw new Error(message);
    }
    if (r.status===204) return null;
    const text = await r.text();
    return text ? JSON.parse(text) : null;
  }
  async function fetchCsrf() {
    const r=await fetch(API+'/auth/csrf',{credentials:'same-origin',cache:'no-store'});
    if (!r.ok) throw new Error('No fue posible obtener la seguridad de la sesión.');
    state.csrf=await r.json();
    return state.csrf;
  }
  const node = (tag,text,cls) => { const el=document.createElement(tag); if(text!=null)el.textContent=String(text); if(cls)el.className=cls;return el; };
  const option=(sel,value,label)=>{const x=node('option',label);x.value=String(value);sel.append(x);};
  const clear=(id)=>$(id).replaceChildren();
  const score=x=>x==null?'Pendiente':Number(x).toFixed(2);
  function cell(tr,text){tr.append(node('td',text));}
  function button(label,fn){const b=node('button',label);b.type='button';b.addEventListener('click',fn);return b;}
  const APROBATORIA=3;

  function logoutUI() {
    state.user=null; state.role=null;
    $('identity').textContent='Sin sesión';
    $('logout').hidden=true;
    $('login-card').hidden=false;
    $('password-card').hidden=true;
    $('teacher').hidden=true;
    $('student').hidden=true;
  }
  async function afterLogin() {
    const user=await request('/auth/me');
    state.user=user;
    if(user.roles?.includes('ROLE_ADMIN')) {
      window.location.replace('/panel/index.html'); return;
    }
    state.role=user.roles?.includes('ROLE_DOCENTE')?'docente':
      user.roles?.includes('ROLE_ESTUDIANTE')?'estudiante':null;
    if (!state.role) throw new Error('La cuenta no tiene un perfil académico autorizado.');
    $('identity').textContent=user.correo;
    $('logout').hidden=false;
    $('login-card').hidden=true;
    $('title').textContent=state.role==='docente'?'Mi espacio docente':'Mi espacio estudiantil';
    $('subtitle').textContent=state.role==='docente'
      ?'Consulta y administra únicamente tus grupos asignados.'
      :'Consulta únicamente tus materias y calificaciones publicadas.';
    const status=await request('/auth/estado-clave');
    $('password-card').hidden=!status.requiereCambio;
    $('teacher').hidden=status.requiereCambio || state.role!=='docente';
    $('student').hidden=status.requiereCambio || state.role!=='estudiante';
    if(status.requiereCambio){notice('Debes cambiar la contraseña temporal antes de continuar.');return;}
    notice('');
    if(state.role==='docente') await loadGroups();
    else await loadPeriods();
  }
  async function login(e) {
    e.preventDefault(); const btn=e.submitter;
    if(btn)btn.disabled=true;
    try {
      const f=new FormData(e.target);
      const csrf=await fetchCsrf();
      const body=new URLSearchParams({username:f.get('username'),password:f.get('password')});
      const r=await fetch(API+'/auth/login',{
        method:'POST',credentials:'same-origin',cache:'no-store',
        headers:{'Content-Type':'application/x-www-form-urlencoded',[csrf.headerName]:csrf.token},body
      });
      if(!r.ok)throw new Error('Correo o contraseña incorrectos.');
      e.target.reset();
      await fetchCsrf();
      await afterLogin();
    }catch(ex){notice(ex.message);}finally{if(btn)btn.disabled=false;}
  }
  async function changePassword(e) {
    e.preventDefault();const btn=e.submitter;
    if(btn)btn.disabled=true;
    try {
      const data=Object.fromEntries(new FormData(e.target));
      await request('/auth/cambiar-clave',{method:'POST',body:JSON.stringify(data)});
      e.target.reset();
      await afterLogin();
      notice('Tu contraseña personal quedó configurada.',true);
    }catch(ex){notice(ex.message+' Verifica que la nueva contraseña sea distinta y tenga al menos 12 caracteres.');}
    finally{if(btn)btn.disabled=false;}
  }
  async function logout() {
    try{await request('/auth/logout',{method:'POST'});}catch{}
    state.csrf=null;logoutUI();notice('Sesión cerrada.',true);
  }

  function renderGroups() {
    clear('teacher-group');option($('teacher-group'),'','Selecciona un grupo');
    state.groups.forEach(g=>option($('teacher-group'),g.id,`${g.codigo} — ${g.asignatura} (${g.periodo})`));
    // Sin grupo elegido, abre el primero para que el docente vea su consolidado de una vez.
    $('teacher-group').value=state.group && state.groups.some(g=>String(g.id)===state.group)?state.group
      :state.groups.length?String(state.groups[0].id):'';
    $('new-eval').disabled=!$('teacher-group').value;
  }
  async function loadGroups() {
    try {
      state.groups=await request('/mi-espacio/docente/grupos');
      if(!Array.isArray(state.groups))state.groups=[];
      renderGroups();
      await loadGroup();
      if(!state.groups.length)notice('No tienes grupos académicos asignados.');
    } catch(e){notice(e.message);}
  }
  async function loadGroup() {
    const seq=++state.seq;
    state.group=$('teacher-group').value;
    state.evaluations=[];state.grades=[];state.evaluation=null;
    renderEvaluations();renderGrades();
    $('new-eval').disabled=!state.group;
    clear('teacher-report');
    if(!state.group){$('teacher-summary').textContent='Selecciona un grupo.';return;}
    $('teacher-summary').textContent='Consultando grupo...';
    try{
      const [report,evals]=await Promise.all([
        request(`/mi-espacio/docente/grupos/${Number(state.group)}/reporte`),
        request(`/mi-espacio/docente/grupos/${Number(state.group)}/evaluaciones`)
      ]);
      if(seq!==state.seq)return;
      state.evaluations=evals;
      $('teacher-summary').textContent=`${report.grupoCodigo} · ${report.asignaturaNombre} · ${report.periodoNombre}`;
      renderEvaluations();renderTeacherReport(report);
      // Abre la primera evaluación para que el libro de notas no arranque vacío.
      if(state.evaluations.length){$('teacher-eval').value=String(state.evaluations[0].id);await loadGrades();}
    }catch(e){if(seq===state.seq)notice(e.message);}
  }
  function renderEvaluations() {
    clear('eval-rows');clear('teacher-eval');
    option($('teacher-eval'),'','Selecciona una evaluación');
    let weight=state.evaluations.reduce((a,e)=>a+Number(e.ponderacion||0),0);
    for(const e of state.evaluations){
      option($('teacher-eval'),e.id,`${e.titulo} (${e.ponderacion}%)`);
      const tr=node('tr');cell(tr,e.titulo);cell(tr,`${e.ponderacion}%`);
      cell(tr,e.publicada?'Publicada':'Borrador');
      const td=node('td');
      if(!e.publicada){td.append(button('Editar',()=>openEval(e)));
        const pub=button('Publicar',()=>publish(e.id));pub.disabled=Math.abs(weight-100)>0.0001;
        td.append(pub);
      } else td.textContent='—';
      tr.append(td);$('eval-rows').append(tr);
    }
    if(!state.evaluations.length){const tr=node('tr');const td=node('td','Sin evaluaciones para este grupo.');td.colSpan=4;tr.append(td);$('eval-rows').append(tr);}
  }
  async function publish(id){
    if(!confirm('¿Publicar la evaluación? Ya no podrás editarla.'))return;
    try{await request(`/mi-espacio/docente/evaluaciones/${id}/publicar`,{method:'PATCH'});await loadGroup();notice('Evaluación publicada.',true);}
    catch(e){notice(e.message);}
  }
  function openEval(e=null){
    if(!state.group)return;
    state.editingEval=e?.id||null;
    $('eval-title').textContent=e?'Editar evaluación':'Nueva evaluación';
    const form=$('eval-form');form.reset();
    if(e){form.elements.titulo.value=e.titulo;form.elements.ponderacion.value=e.ponderacion;form.elements.fecha.value=e.fecha||'';}
    $('eval-dialog').showModal();
  }
  async function saveEval(event){
    event.preventDefault();const form=event.target;
    const data=Object.fromEntries(new FormData(form));
    data.grupoId=Number(state.group);data.ponderacion=Number(data.ponderacion);data.fecha=data.fecha||null;
    const id=state.editingEval;
    try{
      await request(id?`/mi-espacio/docente/evaluaciones/${id}`:'/mi-espacio/docente/evaluaciones',
        {method:id?'PUT':'POST',body:JSON.stringify(data)});
      $('eval-dialog').close();await loadGroup();notice('Evaluación guardada.',true);
    }catch(e){notice(e.message);}
  }
  async function loadGrades(){
    state.evaluation=$('teacher-eval').value;state.grades=[];renderGrades();
    if(!state.evaluation)return;
    const selected=state.evaluation;
    try{const rows=await request(`/mi-espacio/docente/evaluaciones/${selected}/notas`);
      if(selected!==state.evaluation)return;state.grades=rows;renderGrades();}
    catch(e){notice(e.message);}
  }
  function renderGrades(){
    clear('grade-rows');
    for(const row of state.grades){
      const tr=node('tr');const td=node('td');td.append(node('strong',row.estudianteNombre),node('small',row.estudianteCodigo));tr.append(td);
      const nt=node('td');nt.append(gradeBar(row.nota));tr.append(nt);
      const action=node('td');
      if(row.estado==='ACTIVA')action.append(button(row.nota==null?'Calificar':'Corregir',()=>openGrade(row)));
      else action.textContent='—';
      tr.append(action);$('grade-rows').append(tr);
    }
    if(!state.grades.length){const tr=node('tr');const td=node('td',state.evaluation?'No hay matrículas.':'Selecciona una evaluación.');td.colSpan=3;tr.append(td);$('grade-rows').append(tr);}
  }
  function openGrade(row){
    state.gradeRow=row;
    $('grade-title').textContent=`Nota de ${row.estudianteNombre}`;
    $('grade-form').reset();
    $('grade-form').elements.nota.value=row.nota??'';
    $('grade-form').elements.motivo.required=row.nota!=null;
    $('grade-dialog').showModal();
  }
  async function saveGrade(event){
    event.preventDefault();
    const data=Object.fromEntries(new FormData(event.target));
    data.nota=Number(data.nota);data.matriculaId=Number(state.gradeRow.matriculaId);data.evaluacionId=Number(state.evaluation);
    try{
      await request('/mi-espacio/docente/calificaciones',{method:'POST',body:JSON.stringify(data)});
      $('grade-dialog').close();await loadGrades();
      const report=await request(`/mi-espacio/docente/grupos/${Number(state.group)}/reporte`);
      renderTeacherReport(report);notice('Calificación guardada. Las correcciones quedan auditadas.',true);
    }catch(e){notice(e.message);}
  }
  function renderTeacherReport(report){
    clear('teacher-report');
    const r=report.resumen;
    const tiles=node('div',null,'tiles');
    tiles.append(tile('Promedio del grupo',r.promedio==null?'—':score(r.promedio),'destacada'),
      tile('Estudiantes',r.total),tile('Con definitiva',`${r.completas} de ${r.total}`));
    $('teacher-report').append(tiles);
    const wrap=node('div',null,'table-wrap');const table=node('table');
    const head=node('thead');head.innerHTML='<tr><th>Estudiante</th><th>Definitiva</th><th>Estado</th></tr>';table.append(head);
    const body=node('tbody');
    for(const e of report.estudiantes){
      const tr=node('tr');cell(tr,e.estudianteNombre);
      const g=node('td');g.append(gradeBar(e.definitiva));tr.append(g);
      const s=node('td');s.append(estadoPill(e.aprobado,'o'));tr.append(s);
      body.append(tr);
    }
    table.append(body);wrap.append(table);$('teacher-report').append(wrap);
  }

  async function loadPeriods(){
    try{
      const list=(await request('/mi-espacio/estudiante/periodos')||[]).sort((x,y)=>String(x.codigo).localeCompare(String(y.codigo)));
      clear('student-periods');clear('student-report');clear('student-summary');$('student-print').hidden=true;
      list.forEach(p=>{
        const b=button(p.codigo,()=>selectPeriod(p.id));
        b.dataset.period=String(p.id);b.title=p.nombre;b.setAttribute('aria-pressed','false');
        $('student-periods').append(b);
      });
      if(!list.length){notice('Todavía no tienes matrículas activas o finalizadas.');return;}
      // El período más reciente abre por defecto: el estudiante ve sus notas sin buscar nada.
      await selectPeriod(list[list.length-1].id);
    }catch(e){notice(e.message);}
  }
  async function selectPeriod(id){
    state.period=String(id);
    $('student-periods').querySelectorAll('button').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.period===state.period)));
    await loadStudentReport();
  }
  // Nota de 0 a 5 con una barra; la marca vertical es el mínimo aprobatorio (3,0).
  function gradeBar(value){
    const wrap=node('span',null,'nota');
    if(value==null){wrap.classList.add('vacia');wrap.append(node('b','—'));return wrap;}
    const v=Number(value);
    wrap.classList.add(v>=APROBATORIA?'ok':'baja');
    const bar=node('i');bar.style.setProperty('--v',`${Math.max(0,Math.min(100,v/5*100))}%`);
    wrap.append(node('b',score(v)),bar);
    return wrap;
  }
  function estadoPill(aprobado,fin='a'){
    if(aprobado==null)return node('span','En curso','pill pending');
    return node('span',aprobado?`Aprobad${fin}`:`No aprobad${fin}`,aprobado?'pill':'pill baja');
  }
  function tile(label,value,extra){
    const t=node('div',null,'tile'+(extra?` ${extra}`:''));
    t.append(node('strong',value),node('span',label));return t;
  }
  function materiaCard(m){
    const card=node('article',null,'materia');
    const head=node('div',null,'materia-head');
    const title=node('div');
    title.append(node('h3',m.nombre),node('p',`${m.codigo} · Grupo ${m.grupoCodigo} · ${m.creditos} créditos`,'muted'));
    const result=node('div',null,'materia-final');
    if(m.definitiva!=null)result.append(node('strong',score(m.definitiva)));
    result.append(estadoPill(m.aprobado));
    head.append(title,result);card.append(head);

    const evaluado=m.notas.reduce((a,n)=>a+Number(n.ponderacion||0),0);
    const acumulado=m.notas.reduce((a,n)=>a+Number(n.nota||0)*Number(n.ponderacion||0)/100,0);
    // Barra de avance: cada tramo es una evaluación publicada, del ancho de su porcentaje.
    const avance=node('div',null,'avance');avance.setAttribute('role','img');
    avance.setAttribute('aria-label',`Evaluado ${Math.round(evaluado)} % del curso`);
    for(const n of m.notas){
      const seg=node('i',null,Number(n.nota)>=APROBATORIA?'ok':'baja');
      seg.style.width=`${Number(n.ponderacion)}%`;seg.title=`${n.titulo}: ${score(n.nota)} (${n.ponderacion} %)`;
      avance.append(seg);
    }
    card.append(avance);
    if(m.definitiva==null && m.notas.length){
      const restante=100-evaluado;
      let consejo='';
      if(restante>0.0001){
        const necesita=(APROBATORIA-acumulado)/(restante/100);
        consejo=necesita<=0?' Ya tienes asegurado el 3.0.'
          :necesita>5?' Con lo que falta ya no alcanza el 3.0.'
          :` Para aprobar necesitas ${score(necesita)} en el ${Math.round(restante)} % que falta.`;
      }
      card.append(node('p',`Evaluado ${Math.round(evaluado)} % · llevas ${score(acumulado)} de 5.00.${consejo}`,'avance-texto'));
    }
    if(!m.notas.length){card.append(node('p','Todavía no hay notas publicadas en esta materia.','avance-texto'));return card;}
    const list=node('ul',null,'evaluaciones');
    for(const n of m.notas){
      const li=node('li');
      const name=node('span',null,'evaluacion');name.append(node('span',n.titulo),node('small',`${n.ponderacion} %`));
      li.append(name,gradeBar(n.nota));list.append(li);
    }
    card.append(list);
    return card;
  }
  // Porcentaje evaluado promedio de las materias, ponderado por créditos.
  function avancePeriodo(materias){
    const cr=materias.reduce((a,m)=>a+Number(m.creditos||0),0)||1;
    return materias.reduce((a,m)=>a+Number(m.creditos||0)*m.notas.reduce((s,n)=>s+Number(n.ponderacion||0),0),0)/cr;
  }
  async function loadStudentReport(){
    const id=state.period;
    clear('student-report');clear('student-summary');$('student-print').hidden=true;
    if(!id)return;
    try{
      const b=await request(`/mi-espacio/estudiante/boletin?periodoId=${Number(id)}`);
      if(id!==state.period)return;
      $('student-summary').append(node('p',`${b.estudianteNombre} · ${b.estudianteCodigo} · ${b.programa} · ${b.periodoNombre}`,'muted'));
      const r=b.resumen;
      const creditos=b.materias.reduce((a,m)=>a+Number(m.creditos||0),0);
      const tiles=node('div',null,'tiles');
      tiles.append(
        r.promedio==null
          ?tile('Evaluado del período',`${Math.round(avancePeriodo(b.materias))} %`,'destacada')
          :tile('Promedio del período',score(r.promedio),'destacada'),
        tile('Materias inscritas',r.total),
        tile('Créditos',creditos),
        tile(r.pendientes?'En curso':'Aprobadas',r.pendientes?r.pendientes:`${r.aprobadas} de ${r.total}`));
      $('student-summary').append(tiles);
      for(const m of b.materias)$('student-report').append(materiaCard(m));
      $('student-print').hidden=!b.materias.length;
    }catch(e){notice(e.message);}
  }
  function bind(){
    $('login-form').addEventListener('submit',login);
    $('password-form').addEventListener('submit',changePassword);
    $('logout').addEventListener('click',logout);
    $('teacher-group').addEventListener('change',loadGroup);
    $('teacher-refresh').addEventListener('click',loadGroups);
    $('new-eval').addEventListener('click',()=>openEval());
    $('teacher-eval').addEventListener('change',loadGrades);
    $('eval-form').addEventListener('submit',saveEval);
    $('grade-form').addEventListener('submit',saveGrade);
    $('student-print').addEventListener('click',()=>window.print());
    document.querySelectorAll('[data-close-dialog]').forEach(b=>b.addEventListener('click',()=>b.closest('dialog').close()));
  }
  bind();
  logoutUI();
  fetchCsrf().then(()=>afterLogin()).catch(()=>logoutUI());
})();
