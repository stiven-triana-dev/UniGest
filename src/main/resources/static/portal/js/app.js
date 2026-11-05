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
  const roles={ROLE_DOCENTE:'docente',ROLE_ESTUDIANTE:'estudiante'};

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
    $('teacher-group').value=state.group && state.groups.some(g=>String(g.id)===state.group)?state.group:'';
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
      cell(tr,score(row.nota));
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
    const sum=node('div',null,'sum');
    sum.append(node('span',`Estudiantes: ${report.resumen.total}`),node('span',`Completas: ${report.resumen.completas}`),node('span',`Promedio: ${score(report.resumen.promedio)}`));
    $('teacher-report').append(sum);
    const wrap=node('div',null,'table-wrap');const table=node('table');
    const head=node('thead');head.innerHTML='<tr><th>Estudiante</th><th>Definitiva</th><th>Estado</th></tr>';table.append(head);
    const body=node('tbody');
    for(const e of report.estudiantes){const tr=node('tr');cell(tr,e.estudianteNombre);cell(tr,score(e.definitiva));cell(tr,e.aprobado==null?'Pendiente':e.aprobado?'Aprobado':'No aprobado');body.append(tr);}
    table.append(body);wrap.append(table);$('teacher-report').append(wrap);
  }

  async function loadPeriods(){
    try{
      const list=await request('/mi-espacio/estudiante/periodos');
      clear('student-period');option($('student-period'),'','Selecciona un período');
      list.forEach(p=>option($('student-period'),p.id,`${p.codigo} — ${p.nombre}`));
      clear('student-report');clear('student-summary');$('student-print').hidden=true;
      if(!list.length)notice('Todavía no tienes matrículas activas o finalizadas.');
    }catch(e){notice(e.message);}
  }
  async function loadStudentReport(){
    const id=$('student-period').value;
    clear('student-report');clear('student-summary');$('student-print').hidden=true;
    if(!id)return;
    try{
      const b=await request(`/mi-espacio/estudiante/boletin?periodoId=${Number(id)}`);
      $('student-summary').append(node('p',`${b.estudianteNombre} · ${b.estudianteCodigo} · ${b.programa}`));
      const sum=node('div',null,'sum');sum.append(node('span',`Materias: ${b.resumen.total}`),
        node('span',`Completas: ${b.resumen.completas}`),node('span',`Promedio: ${score(b.resumen.promedio)}`));
      $('student-summary').append(sum);
      for(const m of b.materias){
        const section=node('div',null,'card-block');section.append(node('h3',`${m.nombre} · ${m.grupoCodigo}`));
        const tag=node('span',m.definitiva==null?'Definitiva pendiente':`Definitiva: ${score(m.definitiva)}`,m.definitiva==null?'pill pending':'pill');section.append(tag);
        for(const n of m.notas){section.append(node('div',`${n.titulo} (${n.ponderacion}%) — ${score(n.nota)}`,'note-line'));}
        if(!m.notas.length)section.append(node('p','No hay notas publicadas para esta materia.'));
        $('student-report').append(section);
      }
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
    $('student-period').addEventListener('change',loadStudentReport);
    $('student-print').addEventListener('click',()=>window.print());
    document.querySelectorAll('[data-close-dialog]').forEach(b=>b.addEventListener('click',()=>b.closest('dialog').close()));
  }
  bind();
  logoutUI();
  fetchCsrf().then(()=>afterLogin()).catch(()=>logoutUI());
})();
