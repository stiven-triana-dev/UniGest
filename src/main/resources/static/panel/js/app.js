
(() => {
  'use strict';

  const $ = id => document.getElementById(id);
  const $$ = q => document.querySelectorAll(q);
  const API = '/api/v1';

  const s = {
    ok: false, csrf: null, view: 'inicio',
    programs: [], subjects: [], teachers: [], groups: [], periods: [],
    students: null, total: null, enrollments: [],
    enrollmentGroup: '', enrollmentStatus: '',
    page: 0, size: 10, seq: 0, enrollmentSeq: 0,
    search: '', program: '', teacherSearch: '',
    groupSearch: '', groupStatus: '', subjectSearch: '',
    editing: null, teacherEditing: null, groupEditing: null,
    gradeGroup: '', gradeEvaluation: '', evaluations: [], grades: [], finals: [],
    evalEditing: null, gradeMatricula: null, gradeSeq: 0, evalSeq: 0,
    reportGroup: '', reportPeriod: '', reportStudent: '',
    groupReport: null, studentReport: null,
    reportSeq: 0, reportPeriodSeq: 0, reportStudentSeq: 0
  };

  const views = {
    inicio: ['VISTA GENERAL', 'Bienvenido a UniGest',
      'Gestiona la información académica de manera sencilla y organizada.', 'Inicio'],
    estudiantes: ['GESTIÓN DE ESTUDIANTES', 'Estudiantes',
      'Administra los registros académicos.', 'Estudiantes'],
    docentes: ['GESTIÓN DE DOCENTES', 'Docentes',
      'Consulta y administra el personal docente.', 'Docentes'],
    grupos: ['GESTIÓN DE GRUPOS', 'Grupos académicos',
      'Administra docentes, cupos y períodos académicos.', 'Grupos Académicos'],
    matriculas: ['GESTIÓN DE MATRÍCULAS', 'Matrículas académicas',
      'Consulta inscripciones, estados y disponibilidad de cupos.', 'Matrículas Académicas'],
    programas: ['OFERTA ACADÉMICA', 'Programas universitarios',
      'Explora la oferta académica disponible.', 'Programas'],
    asignaturas: ['CATÁLOGO DE MATERIAS', 'Asignaturas',
      'Consulta materias, créditos y oferta académica.', 'Asignaturas'],
    evaluaciones: ['GESTIÓN DE CALIFICACIONES', 'Evaluaciones y notas',
      'Diseña evaluaciones, registra notas y consulta definitivas.', 'Evaluaciones y Notas'],
    reportes: ['INFORMES ACADÉMICOS', 'Reportes y boletines',
      'Consulta resultados por grupo y estudiante, exporta e imprime.', 'Reportes Académicos']
  };

  // UTILIDADES Y SEGURIDAD
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c =>
    ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[c]);

  const date = v => {
    const x = String(v ?? '').slice(0, 10);
    return /^\d{4}-\d{2}-\d{2}$/.test(x)
      ? `${x.slice(8,10)}/${x.slice(5,7)}/${x.slice(0,4)}` : '—';
  };

  const today = () => {
    const d = new Date();
    return [d.getFullYear(), String(d.getMonth()+1).padStart(2,'0'),
      String(d.getDate()).padStart(2,'0')].join('-');
  };

  const initials = x =>
    `${x.nombres?.trim()[0] || ''}${x.apellidos?.trim()[0] || ''}`.toUpperCase();

  const field = (form, name) => $(form).elements.namedItem(name);
  const empty = (cols, message) =>
    `<tr><td colspan="${cols}" class="empty-table">${esc(message)}</td></tr>`;
  const match = (term, ...values) => values.some(v =>
    String(v ?? '').toLocaleLowerCase('es').includes(term.toLocaleLowerCase('es')));
  const set = (id, value) => { if ($(id)) $(id).textContent = value; };

  function flash(message = '') {
    set('flash', message);
    $('flash').hidden = !message;
  }

  let toastTimer;
  function toast(message) {
    set('toast', message);
    $('toast').hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => $('toast').hidden = true, 4500);
  }

  function errorText(e, fallback = 'No se pudo completar la solicitud') {
    const messages = {
      400: 'Revisa los datos ingresados.',
      401: 'La sesión no es válida o ha expirado.',
      403: 'Acceso denegado o token CSRF no válido.',
      404: 'No se encontró el registro solicitado.',
      409: 'La operación no está permitida: registro duplicado, grupo cerrado o sin cupos.',
      422: 'Los datos seleccionados no están disponibles.'
    };
    return messages[e?.status] || (e instanceof TypeError
      ? 'No hay conexión con UniGest.'
      : `${fallback} (HTTP ${e?.status || 'desconocido'}).`);
  }

  function formError(id, message = '') {
    set(id, message);
    $(id).hidden = !message;
  }

  const editingKeys = {
    student: 'editing', teacher: 'teacherEditing', group: 'groupEditing'
  };

  function closeModal(type) {
    $(`${type}-modal`).hidden = true;
    if (editingKeys[type]) s[editingKeys[type]] = null;
    document.body.style.overflow = '';
  }

  function showModal(type, focusName = 'nombres') {
    $(`${type}-modal`).hidden = false;
    document.body.style.overflow = 'hidden';
    field(`${type}-form`, focusName)?.focus();
  }

  async function request(url, options = {}) {
    const response = await fetch(url, {
      credentials: 'same-origin', cache: 'no-store', ...options
    });
    if (!response.ok) {
      const error = new Error(`HTTP ${response.status}`);
      error.status = response.status;
      throw error;
    }
    if (response.status === 204) return null;
    const body = await response.text();
    if (!body) return null;
    try { return JSON.parse(body); } catch { return body; }
  }

  async function csrf() {
    const data = await request(`${API}/auth/csrf`);
    if (!data?.headerName || !data?.token)
      throw new Error('No se recibió un token CSRF válido.');
    s.csrf = { header: data.headerName, token: data.token };
    return s.csrf;
  }

  async function api(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = {
      Accept: 'application/json',
      ...(options.body ? {'Content-Type':'application/json'} : {}),
      ...options.headers
    };
    if (!['GET','HEAD','OPTIONS','TRACE'].includes(method)) {
      const token = s.csrf || await csrf();
      headers[token.header] = token.token;
    }
    return request(`${API}${path}`, { ...options, method, headers });
  }

  // AUTENTICACIÓN
  function signedIn(user) {
    s.ok = true;
    set('account-name', user.correo || 'Administrador');
    $('login-overlay').hidden = true;
    $('logout-btn').disabled = false;
    flash();
  }

  function reset() {
    Object.assign(s, {
      ok:false, csrf:null, programs:[], subjects:[], teachers:[],
      groups:[], periods:[], students:null, total:null, enrollments:[],
      page:0, search:'', program:'', teacherSearch:'', groupSearch:'',
      groupStatus:'', subjectSearch:'', enrollmentGroup:'',
      enrollmentStatus:'', editing:null, teacherEditing:null, groupEditing:null,
      gradeGroup:'', gradeEvaluation:'', evaluations:[], grades:[], finals:[],
      evalEditing:null, gradeMatricula:null,
      reportGroup:'', reportPeriod:'', reportStudent:'',
      groupReport:null, studentReport:null
    });
    s.seq++;
    s.enrollmentSeq++;
    s.gradeSeq++;
    s.evalSeq++;
    s.reportSeq++; s.reportPeriodSeq++; s.reportStudentSeq++;

    sessionStorage.removeItem('unigest-grade-group');

    set('account-name', 'Sin sesión');
    $('logout-btn').disabled = true;
    $('login-overlay').hidden = false;
    $('login-password').value = '';
    formError('login-error');

    ['student-search','teacher-search','group-search',
      'group-status-filter','student-program-filter',
      'enrollment-group-filter','enrollment-status-filter',
      'subject-search'].forEach(id => $(id).value = '');

    ['student','teacher','group','enrollment'].forEach(closeModal);
    ['teacher-profile-modal','eval-modal','grade-modal']
      .forEach(id => $(id).hidden = true);
    closeCredentialModal();

    document.body.style.overflow = '';
    navigate('inicio');
    renderStudents();
    renderTeachers();
    renderGroups();
    renderEnrollments();
    renderPrograms();
    renderSubjects();
    renderEvaluations();
    renderGrades();
    renderFinals();
    renderGroupReport();
    renderStudentReport();
    stats();
  }

  async function endSession() {
    const token = await csrf();
    await request(`${API}/auth/logout`, {
      method: 'POST', headers: {[token.header]: token.token}
    });
    s.csrf = null;
  }

  async function dashboard() {
    await catalogs();
    navigate('inicio');
    await Promise.all([students(), total(), teachers(), groups()]);
  }

  async function login(e) {
    e.preventDefault();
    const btn = $('login-submit');
    const username = $('login-user').value.trim();
    const password = $('login-password').value;
    if (!username || !password) return;

    btn.disabled = true;
    btn.textContent = 'Verificando acceso...';
    formError('login-error');

    try {
      const token = await csrf();
      await request(`${API}/auth/login`, {
        method:'POST',
        headers:{
          'Content-Type':'application/x-www-form-urlencoded',
          [token.header]:token.token
        },
        body:new URLSearchParams({username,password}).toString()
      });

      await csrf();
      const user = await api('/auth/me');

      if (!user.roles?.includes('ROLE_ADMIN')) {
        if (user.roles?.some(r => ['ROLE_DOCENTE','ROLE_ESTUDIANTE'].includes(r))) {
          window.location.replace('/portal/index.html');
          return;
        }
        await endSession();
        const error = new Error('Sin permisos ADMIN');
        error.status = 403;
        throw error;
      }

      signedIn(user);
      await dashboard();
      if (s.ok) toast('Sesión iniciada correctamente.');
    } catch (e) {
      reset();
      formError('login-error', errorText(e, 'No se pudo iniciar sesión'));
    } finally {
      $('login-password').value = '';
      btn.disabled = false;
      btn.textContent = 'Entrar al panel →';
    }
  }

  async function logout() {
    if (!s.ok) return;
    $('logout-btn').disabled = true;
    try {
      await endSession();
      reset();
      $('login-user').focus();
      toast('Sesión cerrada correctamente.');
      await csrf().catch(() => {});
    } catch {
      $('logout-btn').disabled = false;
      toast('No fue posible cerrar la sesión en el servidor.');
    }
  }

  async function restore() {
    try {
      await csrf();
      const user = await api('/auth/me');
      if (!user.roles?.includes('ROLE_ADMIN')) {
        if (user.roles?.some(r => ['ROLE_DOCENTE','ROLE_ESTUDIANTE'].includes(r))) {
          window.location.replace('/portal/index.html');
          return;
        }
        await endSession();
        return;
      }
      signedIn(user);
      await dashboard();
    } catch {
      if (!s.ok) $('login-user').focus();
    }
  }

  // NAVEGACIÓN
  function navigate(view) {
    if (!views[view]) return;
    s.view = view;

    Object.keys(views).forEach(name =>
      $(`view-${name}`).classList.toggle('active', name === view));

    $$('.nav-link').forEach(b => {
      const active = b.dataset.view === view;
      b.classList.toggle('active', active);
      if (active) b.setAttribute('aria-current','page');
      else b.removeAttribute('aria-current');
    });

    ['eyebrow','page-title','page-subtitle','breadcrumb']
      .forEach((id,i) => set(id,views[view][i]));

    $('sidebar').classList.remove('open');
    $('menu-toggle').setAttribute('aria-expanded','false');
    flash();

    if (view === 'grupos' && s.ok) groups();
    if (view === 'matriculas' && s.ok) loadEnrollmentGroups();
    if (view === 'asignaturas') renderSubjects();
    if (view === 'evaluaciones' && s.ok) loadEvaluationGroups();
    if (view === 'reportes' && s.ok) loadReportOptions();
  }

  function stats() {
    set('stat-students', s.total ?? '—');
    set('stat-programs', s.programs.length);
    set('stat-subjects', s.subjects.length);
    set('student-programs', s.programs.length);
  }

  // PROGRAMAS Y ASIGNATURAS
  async function catalogs() {
    try {
      const [programs,subjects] = await Promise.all([
        api('/programas'), api('/asignaturas')
      ]);
      if (!s.ok) return;
      s.programs = Array.isArray(programs) ? programs : [];
      s.subjects = Array.isArray(subjects) ? subjects : [];
      renderPrograms();
      renderSubjects();
      programOptions();
      stats();
    } catch(e) {
      flash(errorText(e,'No se pudieron cargar los catálogos'));
      if (e.status === 401) reset();
    }
  }

  function renderPrograms() {
    set('program-total', s.programs.length);
    set('program-total-intro', s.programs.length);
    set('program-subjects', s.subjects.length);

    $('programs-list').innerHTML = s.programs.length
      ? s.programs.map(x => `
        <article class="catalog-card">
          <div class="catalog-icon">▤</div>
          <h3>${esc(x.nombre)}</h3>
          <p>Código del programa: <strong>${esc(x.codigo)}</strong></p>
        </article>`).join('')
      : '<div class="empty-catalog">No hay programas disponibles.</div>';
  }

  function renderSubjects() {
    const list = s.subjects;
    const term = s.subjectSearch.trim();
    const rows = list.filter(x => match(term,x.nombre,x.codigo));
    const credits = list.map(x => Number(x.creditos))
      .filter(n => Number.isFinite(n) && n >= 0);
    const average = credits.length
      ? credits.reduce((sum,n) => sum+n,0)/credits.length : null;

    set('subject-total',list.length);
    set('subject-average',average === null ? '—'
      : average.toLocaleString('es-CO',{maximumFractionDigits:1}));
    set('subject-active',list.length);
    set('subjects-count',`${rows.length} de ${list.length} asignaturas`);

    $('subjects-body').innerHTML = rows.length ? rows.map(x => `
      <tr>
        <td><strong>${esc(x.nombre)}</strong></td>
        <td><span class="code-tag">${esc(x.codigo)}</span></td>
        <td><span class="subject-credit">${esc(x.creditos)} créditos</span></td>
        <td><span class="tag-active">● Disponible</span></td>
      </tr>`).join('')
      : empty(4,term
        ? 'No hay asignaturas que coincidan con la búsqueda.'
        : 'No hay asignaturas disponibles.');
  }

  function programOptions() {
    const select = $('student-form-program');
    const filter = $('student-program-filter');
    const previous = [select.value,filter.value];
    const options = s.programs.map(x =>
      `<option value="${Number(x.id)}">${esc(x.nombre)}</option>`).join('');

    select.innerHTML =
      '<option value="">Selecciona un programa</option>'+options;
    filter.innerHTML =
      '<option value="">Todos los programas</option>'+options;
    select.value = previous[0];
    filter.value = previous[1];
  }

  // ESTUDIANTES
  async function students() {
    if (!s.ok) return;
    const seq = ++s.seq;
    const params = new URLSearchParams({
      pagina:s.page, tamano:s.size, buscar:s.search
    });
    if (s.program) params.set('programaId',s.program);
    $('students-body').innerHTML = empty(5,'Consultando estudiantes...');

    try {
      const data = await api(`/estudiantes?${params}`);
      if (seq !== s.seq || !s.ok) return;

      s.students = data;
      renderStudents();

      set('updated-at',`Actualizado ${new Date().toLocaleTimeString('es-CO',{
        hour:'2-digit',minute:'2-digit'
      })}`);
    } catch(e) {
      if (seq !== s.seq) return;
      s.students = null;
      renderStudents();
      flash(errorText(e,'No se pudieron consultar estudiantes'));
      if (e.status === 401) reset();
    }
  }

  async function total() {
    if (!s.ok) return;
    try {
      s.total = (await api('/estudiantes?pagina=0&tamano=1')).totalElements;
    } catch {
      s.total = null;
    }
    stats();
    set('student-total',s.total ?? '—');
  }

  function renderStudents() {
    const p = s.students;
    const rows = p?.content || [];

    set('students-count',`${p?.totalElements ?? 0} estudiantes activos`);
    set('student-total',p?.totalElements ?? s.total ?? '—');
    set('student-on-page',rows.length);
    set('page-indicator',
      `Página ${(p?.number ?? s.page)+1} de ${Math.max(1,p?.totalPages || 1)}`);

    $('prev-page').disabled = !p || p.first;
    $('next-page').disabled = !p || p.last;

    $('students-body').innerHTML = rows.length ? rows.map(x => `
      <tr>
        <td><div class="name-cell">
          <span class="student-avatar">${esc(initials(x))}</span>
          <span><strong>${esc(x.nombres)} ${esc(x.apellidos)}</strong>
          <small>${esc(x.correo)}</small></span>
        </div></td>
        <td><span class="code-tag">${esc(x.codigo)}</span></td>
        <td>${esc(x.programa)}</td>
        <td>${date(x.fechaIngreso)}</td>
        <td><div class="table-actions">
          <button class="text-action" data-edit="${Number(x.id)}">Editar</button>
          <button class="text-action" data-credential-student="${Number(x.id)}">Acceso</button>
          <button class="text-action danger" data-disable="${Number(x.id)}">Desactivar</button>
        </div></td>
      </tr>`).join('')
      : empty(5,'No hay estudiantes activos.');
  }

  function openStudentModal(student = null) {
    if (!s.ok) return;

    $('student-form').reset();
    s.editing = student?.id ?? null;
    set('modal-title',student ? 'Editar estudiante' : 'Nuevo estudiante');
    set('form-submit',student ? 'Guardar cambios' : 'Guardar estudiante');
    formError('student-form-error');

    if (student) {
      ['nombres','apellidos','correo','documento','codigo','fechaIngreso']
        .forEach(k => field('student-form',k).value = student[k] || '');
      field('student-form','programaId').value = student.programaId ?? '';
    } else {
      field('student-form','fechaIngreso').value = today();
    }

    field('student-form','fechaIngreso').readOnly = !!student;
    set('date-help',student
      ? 'La fecha de ingreso no se modifica al editar.'
      : 'Utiliza únicamente información ficticia.');

    showModal('student');
  }

  async function editStudent(id) {
    try {
      openStudentModal(await api(`/estudiantes/${id}`));
    } catch(e) {
      toast(errorText(e,'No se pudo abrir el estudiante'));
    }
  }

  async function saveStudent(e) {
    e.preventDefault();
    const btn = $('form-submit');
    const data = Object.fromEntries(new FormData($('student-form')));
    data.programaId = Number(data.programaId);

    if (data.fechaIngreso > today()) {
      formError('student-form-error',
        'La fecha de ingreso no puede estar en el futuro.');
      return;
    }

    btn.disabled = true;
    formError('student-form-error');

    try {
      const id = s.editing;
      await api(id ? `/estudiantes/${id}` : '/estudiantes',{
        method:id ? 'PUT':'POST', body:JSON.stringify(data)
      });

      closeModal('student');
      s.page = 0;
      s.search = '';
      s.program = '';
      $('student-search').value = '';
      $('student-program-filter').value = '';

      await Promise.all([students(),total()]);
      toast(id ? 'Estudiante actualizado.' : 'Estudiante registrado.');
    } catch(e) {
      formError('student-form-error',errorText(e,'No se pudo guardar'));
    } finally {
      btn.disabled = false;
    }
  }

  async function disableStudent(id) {
    const x = s.students?.content?.find(v => Number(v.id) === id);
    if (!x || !confirm(
      `¿Desactivar a ${x.nombres} ${x.apellidos}?\nSu historial se conservará.`
    )) return;

    try {
      await api(`/estudiantes/${id}`,{method:'DELETE'});
      if (s.students?.content?.length === 1 && s.page > 0) s.page--;
      await Promise.all([students(),total()]);
      toast('Estudiante desactivado. Su historial permanece en PostgreSQL.');
    } catch(e) {
      toast(errorText(e,'No se pudo desactivar'));
    }
  }

  // DOCENTES Y ESTADÍSTICAS
  async function teachers() {
    if (!s.ok) return;
    $('teachers-body').innerHTML = empty(5,'Consultando docentes...');

    try {
      const data = await api('/docentes');
      if (!s.ok) return;
      s.teachers = Array.isArray(data) ? data : [];
      renderTeachers();
    } catch(e) {
      s.teachers = [];
      renderTeachers();
      flash(errorText(e,'No se pudieron consultar docentes'));
      if (e.status === 401) reset();
    }
  }

  function teacherStats() {
    set('teacher-total',s.teachers.length);
    set('teacher-dated',s.teachers.filter(x => !!x.fechaVinculacion).length);
    set('teacher-groups',s.groups.filter(x => !!x.docenteNombre).length);
  }

  function renderTeachers() {
    const rows = s.teachers.filter(x => match(s.teacherSearch,
      x.nombres,x.apellidos,x.correo,x.codigo,x.documento));

    teacherStats();
    set('teachers-count',
      `${rows.length} de ${s.teachers.length} docentes activos`);

    $('teachers-body').innerHTML = rows.length ? rows.map(x => `
      <tr>
        <td><div class="name-cell">
          <span class="student-avatar">${esc(initials(x))}</span>
          <span><button type="button" class="teacher-link"
            data-profile-teacher="${Number(x.id)}">
            ${esc(x.nombres)} ${esc(x.apellidos)}
          </button><small>${esc(x.correo)}</small></span>
        </div></td>
        <td><span class="code-tag">${esc(x.codigo)}</span></td>
        <td>${esc(x.documento)}</td>
        <td>${date(x.fechaVinculacion)}</td>
        <td><div class="table-actions">
          <button class="text-action"
            data-teacher-edit="${Number(x.id)}">Editar</button>
          <button class="text-action" data-credential-teacher="${Number(x.id)}">Acceso</button>
          <button class="text-action danger"
            data-teacher-disable="${Number(x.id)}">Desactivar</button>
        </div></td>
      </tr>`).join('')
      : empty(5,'No hay docentes activos.');
  }

  function openTeacherModal(teacher = null) {
    if (!s.ok) return;

    $('teacher-form').reset();
    s.teacherEditing = teacher?.id ?? null;
    set('teacher-modal-title',teacher ? 'Editar docente' : 'Nuevo docente');
    set('teacher-form-submit',teacher ? 'Guardar cambios' : 'Guardar docente');
    formError('teacher-form-error');

    if (teacher) {
      ['nombres','apellidos','correo','documento','codigo','fechaVinculacion']
        .forEach(k => field('teacher-form',k).value = teacher[k] || '');
    } else {
      field('teacher-form','fechaVinculacion').value = today();
    }

    field('teacher-form','fechaVinculacion').readOnly = !!teacher;
    set('teacher-date-help',teacher
      ? 'La fecha de vinculación no se modifica al editar.'
      : 'Utiliza únicamente información ficticia.');

    showModal('teacher');
  }

  async function editTeacher(id) {
    try {
      openTeacherModal(await api(`/docentes/${id}`));
    } catch(e) {
      toast(errorText(e,'No se pudo consultar el docente'));
    }
  }

  async function saveTeacher(e) {
    e.preventDefault();
    const btn = $('teacher-form-submit');
    const data = Object.fromEntries(new FormData($('teacher-form')));

    if (data.fechaVinculacion > today()) {
      formError('teacher-form-error',
        'La vinculación no puede tener fecha futura.');
      return;
    }

    btn.disabled = true;
    formError('teacher-form-error');

    try {
      const id = s.teacherEditing;
      await api(id ? `/docentes/${id}` : '/docentes',{
        method:id ? 'PUT':'POST',body:JSON.stringify(data)
      });

      closeModal('teacher');
      await teachers();
      toast(id ? 'Docente actualizado.' : 'Docente registrado.');
    } catch(e) {
      formError('teacher-form-error',errorText(e,'No se pudo guardar'));
    } finally {
      btn.disabled = false;
    }
  }

  async function disableTeacher(id) {
    const x = s.teachers.find(v => Number(v.id) === id);
    if (!x || !confirm(
      `¿Desactivar a ${x.nombres} ${x.apellidos}?\nSu historial se conservará.`
    )) return;

    try {
      await api(`/docentes/${id}`,{method:'DELETE'});
      await teachers();
      toast('Docente desactivado. Su historial se conserva.');
    } catch(e) {
      toast(errorText(e,'No se pudo desactivar'));
    }
  }

  // GRUPOS ACADÉMICOS Y FICHAS DE DOCENTES
  async function groups() {
    if (!s.ok) return;
    $('groups-body').innerHTML = empty(7,'Consultando grupos...');

    try {
      const data = await api('/grupos');
      if (!s.ok) return;
      s.groups = Array.isArray(data) ? data : data.content || [];
      renderGroups();
      if (s.view === 'matriculas') updateEnrollmentGroupOptions();
    } catch(e) {
      s.groups = [];
      renderGroups();
      flash(errorText(e,'No se pudieron consultar los grupos'));
      if (e.status === 401) reset();
    }
  }

  function groupStats() {
    set('group-total',s.groups.length);
    set('group-open',s.groups.filter(g => g.estado === 'ABIERTO').length);
    set('group-capacity',s.groups.reduce(
      (sum,g) => sum + (Number(g.cupo) || 0),0));
    teacherStats();
  }

  function teacherProfile(group) {
    const name = group.docenteNombre || 'Sin asignar';
    return group.docenteId
      ? `<button class="teacher-link" type="button"
          data-profile-teacher="${Number(group.docenteId)}">
          ${esc(name)} ↗</button>`
      : esc(name);
  }

  function showTeacherProfile(id) {
    const t = s.teachers.find(x => Number(x.id) === id);
    if (!t) {
      toast('No se encontró la ficha del docente activo.');
      return;
    }

    const fullName = `${t.nombres} ${t.apellidos}`;
    const assigned = s.groups.filter(g => Number(g.docenteId) === id);

    set('profile-name',fullName);
    $('profile-details').innerHTML = `
      <div class="profile-identity">
        <span class="student-avatar">${esc(initials(t))}</span>
        <div><strong>${esc(fullName)}</strong>
          <small>Docente institucional</small></div>
      </div>
      <div class="profile-grid">
        <div><small>Código</small><strong>${esc(t.codigo)}</strong></div>
        <div><small>Documento</small><strong>${esc(t.documento)}</strong></div>
        <div><small>Vinculación</small>
          <strong>${date(t.fechaVinculacion)}</strong></div>
        <div><small>Grupos asignados</small>
          <strong>${assigned.length}</strong></div>
      </div>
      <p class="profile-email">${esc(t.correo)}</p>
      ${assigned.length ? `
        <div class="profile-groups"><b>Grupos asignados</b>
          ${assigned.map(g => `<span>${esc(g.codigo)} ·
            ${esc(g.asignaturaNombre)}</span>`).join('')}
        </div>` : ''}`;

    $('teacher-profile-modal').hidden = false;
    document.body.style.overflow = 'hidden';
  }

  function renderGroups() {
    const rows = s.groups.filter(x =>
      (!s.groupStatus || x.estado === s.groupStatus) &&
      match(s.groupSearch,x.codigo,x.asignaturaNombre,
        x.docenteNombre,x.periodoNombre));

    groupStats();
    set('groups-count',
      `${rows.length} de ${s.groups.length} grupos académicos`);

    $('groups-body').innerHTML = rows.length ? rows.map(x => {
      const id = Number(x.id);
      const status = x.estado || 'BORRADOR';
      const actions = status === 'CERRADO' ? '—' : `
        <button class="text-action" data-group-edit="${id}">Editar</button>
        ${status === 'BORRADOR' ? `
          <button class="text-action" data-group-state="${id}"
            data-next-state="ABIERTO">Abrir</button>` : ''}
        ${status === 'ABIERTO' ? `
          <button class="text-action danger" data-group-state="${id}"
            data-next-state="CERRADO">Cerrar</button>` : ''}`;

      return `<tr>
        <td><span class="code-tag">${esc(x.codigo)}</span></td>
        <td>${esc(x.asignaturaNombre)}</td>
        <td>${teacherProfile(x)}</td>
        <td>${esc(x.periodoNombre)}</td>
        <td>${esc(x.cupo)}</td>
        <td><span class="group-status ${esc(status.toLowerCase())}">
          ${esc(status)}</span></td>
        <td><div class="table-actions">${actions}</div></td>
      </tr>`;
    }).join('') : empty(7,'No se encontraron grupos.');
  }

  function selectOptions(id, rows, placeholder) {
    $(id).innerHTML = `<option value="">${esc(placeholder)}</option>` +
      rows.map(x => `<option value="${Number(x.id)}">
        ${esc(x.label)}</option>`).join('');
  }

  async function openGroupModal(group = null) {
    if (!s.ok) return;

    $('group-form').reset();
    s.groupEditing = group?.id ?? null;
    set('group-modal-title',group
      ? 'Editar grupo académico' : 'Nuevo grupo académico');
    set('group-form-submit',group ? 'Guardar cambios' : 'Guardar grupo');
    formError('group-form-error');

    try {
      const [subjects,docentes,periods] = await Promise.all([
        api('/asignaturas'),api('/docentes'),api('/periodos')
      ]);
      if (!s.ok) return;

      s.subjects = subjects;
      s.teachers = docentes;
      s.periods = periods;

      selectOptions('group-subject',subjects.map(x => ({
        id:x.id,label:`${x.codigo} — ${x.nombre}`
      })),'Selecciona una asignatura');

      selectOptions('group-teacher',docentes.map(x => ({
        id:x.id,label:`${x.nombres} ${x.apellidos} (${x.codigo})`
      })),'Selecciona un docente');

      selectOptions('group-period',periods.map(x => ({
        id:x.id,label:`${x.codigo} — ${x.nombre}`
      })),'Selecciona un período');

      if (group) {
        ['asignaturaId','docenteId','periodoId','codigo','cupo']
          .forEach(k => field('group-form',k).value = group[k] ?? '');
      }

      set('group-form-help',group
        ? 'El estado se administra desde el directorio.'
        : 'El nuevo grupo se registrará en estado BORRADOR.');

      renderTeachers();
      renderGroups();
      renderSubjects();
      showModal('group','asignaturaId');
    } catch(e) {
      toast(errorText(e,'No se pudieron cargar las opciones del grupo'));
    }
  }

  async function editGroup(id) {
    try {
      const group = await api(`/grupos/${id}`);

      if (group.estado === 'CERRADO') {
        toast('No se puede editar un grupo cerrado.');
        return;
      }

      await openGroupModal(group);
    } catch(e) {
      toast(errorText(e,'No se pudo consultar el grupo'));
    }
  }

  async function saveGroup(e) {
    e.preventDefault();
    const btn = $('group-form-submit');
    const data = Object.fromEntries(new FormData($('group-form')));

    ['asignaturaId','docenteId','periodoId','cupo']
      .forEach(k => data[k] = Number(data[k]));

    const valid = ['asignaturaId','docenteId','periodoId','cupo']
      .every(k => Number.isInteger(data[k]) && data[k] > 0);

    if (!valid || data.cupo > 1000 || !data.codigo.trim()) {
      formError('group-form-error',
        'Completa los campos. El cupo debe estar entre 1 y 1000.');
      return;
    }

    btn.disabled = true;
    formError('group-form-error');

    try {
      const id = s.groupEditing;
      await api(id ? `/grupos/${id}` : '/grupos',{
        method:id ? 'PUT':'POST', body:JSON.stringify(data)
      });

      closeModal('group');
      await groups();
      toast(id ? 'Grupo actualizado.' : 'Grupo registrado.');
    } catch(e) {
      formError('group-form-error',errorText(e,'No se pudo guardar'));
    } finally {
      btn.disabled = false;
    }
  }

  async function changeGroupState(id, next) {
    const group = s.groups.find(x => Number(x.id) === id);
    if (!group || !['ABIERTO','CERRADO'].includes(next)) return;

    if (!confirm(`¿Cambiar el grupo ${group.codigo} a ${next}?`)) return;

    try {
      await api(`/grupos/${id}/estado`,{
        method:'PATCH',body:JSON.stringify({estado:next})
      });
      await groups();
      toast(`Grupo actualizado a ${next}.`);
    } catch(e) {
      toast(errorText(e,'No se pudo cambiar el estado'));
    }
  }

  // MATRÍCULAS
  function updateEnrollmentGroupOptions() {
    const filter = $('enrollment-group-filter');
    const current = s.enrollmentGroup;

    selectOptions('enrollment-group-filter',s.groups.map(g => ({
      id:g.id,label:`${g.codigo} — ${g.asignaturaNombre} (${g.estado})`
    })),'Selecciona un grupo');

    if (current && s.groups.some(g => String(g.id) === current))
      filter.value = current;
    else {
      s.enrollmentGroup = '';
      filter.value = '';
    }
  }

  async function loadEnrollmentGroups() {
    if (!s.ok) return;

    try {
      const data = await api('/grupos');
      if (!s.ok) return;

      s.groups = Array.isArray(data) ? data : data.content || [];
      renderGroups();
      updateEnrollmentGroupOptions();
      await loadEnrollments();
    } catch(e) {
      toast(errorText(e,'No se pudieron cargar los grupos'));
    }
  }

  async function loadEnrollments() {
    if (!s.ok) return;
    const seq = ++s.enrollmentSeq;
    const groupId = Number(s.enrollmentGroup);

    if (!groupId) {
      s.enrollments = [];
      renderEnrollments();
      return;
    }

    $('enrollments-body').innerHTML =
      empty(7,'Consultando matrículas...');

    try {
      const data = await api(`/matriculas/grupo/${groupId}`);
      if (seq !== s.enrollmentSeq || !s.ok) return;

      s.enrollments = Array.isArray(data) ? data : [];
      renderEnrollments();
    } catch(e) {
      if (seq !== s.enrollmentSeq) return;

      s.enrollments = [];
      renderEnrollments();
      flash(errorText(e,'No se pudieron consultar las matrículas'));
      if (e.status === 401) reset();
    }
  }

  function renderEnrollments() {
    const groupId = Number(s.enrollmentGroup);
    const group = s.groups.find(g => Number(g.id) === groupId);
    const rows = s.enrollments.filter(m =>
      !s.enrollmentStatus || m.estado === s.enrollmentStatus);

    set('enrollments-count',groupId
      ? `${rows.length} de ${s.enrollments.length} matrículas`
      : '0 matrículas');

    const activeCount =
      s.enrollments.filter(m => m.estado === 'ACTIVA').length;
    const capacity = Number(group?.cupo || 0);
    const available = Math.max(0,capacity-activeCount);

    set('enrollment-capacity',group
      ? `${activeCount} ocupados · ${available} disponibles · Cupo ${capacity}`
      : 'Selecciona un grupo para ver los cupos');

    set('enroll-active',group ? activeCount : '—');
    set('enroll-free',group ? available : '—');
    set('enroll-percent',group && capacity
      ? Math.round(100*activeCount/capacity)+'%' : '—');

    if (!groupId) {
      $('enrollments-body').innerHTML =
        empty(7,'Selecciona un grupo para consultar sus matrículas.');
      return;
    }

    $('enrollments-body').innerHTML = rows.length ? rows.map(m => {
      const state = ['ACTIVA','RETIRADA','FINALIZADA'].includes(m.estado)
        ? m.estado : 'DESCONOCIDO';

      const action = state === 'ACTIVA'
        ? `<button class="text-action danger"
            data-enrollment-withdraw="${Number(m.id)}">Retirar</button>`
        : '—';

      return `<tr>
        <td><span class="code-tag">#${Number(m.id)}</span></td>
        <td><strong>${esc(m.estudianteNombre)}</strong></td>
        <td>${esc(m.estudianteCodigo)}</td>
        <td>${esc(m.grupoCodigo)}</td>
        <td>${date(m.fecha)}</td>
        <td><span class="enrollment-status ${state.toLowerCase()}">
          ${state}</span></td>
        <td><div class="table-actions">${action}</div></td>
      </tr>`;
    }).join('') : empty(7,'No hay matrículas con los filtros seleccionados.');
  }

  async function activeStudents() {
    const result = [];
    let page = 0;
    let totalPages = 1;

    do {
      const data = await api(`/estudiantes?pagina=${page}&tamano=50`);
      if (!Array.isArray(data.content))
        throw new Error('Respuesta de estudiantes no válida.');

      result.push(...data.content);
      totalPages = Number(data.totalPages ?? 1);
      page++;

      if (page > 200)
        throw new Error('Demasiadas páginas de estudiantes.');
    } while (page < totalPages);

    return result;
  }

  async function openEnrollmentModal() {
    if (!s.ok) return;
    const btn = $('new-enrollment-btn');
    btn.disabled = true;
    formError('enrollment-form-error');

    try {
      const [estudiantes,groupsData] = await Promise.all([
        activeStudents(),api('/grupos')
      ]);
      if (!s.ok) return;

      s.groups = Array.isArray(groupsData)
        ? groupsData : groupsData.content || [];
      updateEnrollmentGroupOptions();

      const openGroups = s.groups.filter(g => g.estado === 'ABIERTO');

      selectOptions('enrollment-student',estudiantes.map(x => ({
        id:x.id,label:`${x.codigo} — ${x.nombres} ${x.apellidos}`
      })),'Selecciona un estudiante');

      selectOptions('enrollment-group',openGroups.map(x => ({
        id:x.id,label:`${x.codigo} — ${x.asignaturaNombre}`
      })),'Selecciona un grupo abierto');

      $('enrollment-form').reset();

      if (s.enrollmentGroup && openGroups.some(
        g => String(g.id) === s.enrollmentGroup))
        $('enrollment-group').value = s.enrollmentGroup;

      set('enrollment-form-help',
        'UniGest verificará cupos, duplicados y estado del grupo antes de registrar.');

      showModal('enrollment','estudianteId');
    } catch(e) {
      toast(errorText(e,'No se pudo preparar el formulario'));
    } finally {
      btn.disabled = false;
    }
  }

  async function saveEnrollment(e) {
    e.preventDefault();
    const btn = $('enrollment-form-submit');
    const data = Object.fromEntries(new FormData($('enrollment-form')));
    data.estudianteId = Number(data.estudianteId);
    data.grupoId = Number(data.grupoId);

    if (![data.estudianteId,data.grupoId].every(
      n => Number.isInteger(n) && n > 0)) {
      formError('enrollment-form-error',
        'Selecciona un estudiante y un grupo válidos.');
      return;
    }

    btn.disabled = true;
    formError('enrollment-form-error');

    try {
      const result = await api('/matriculas',{
        method:'POST',body:JSON.stringify(data)
      });
      closeModal('enrollment');
      s.enrollmentGroup = String(data.grupoId);
      s.enrollmentStatus = '';
      $('enrollment-status-filter').value = '';

      await loadEnrollmentGroups();
      toast(`Matrícula #${result.id} registrada o reactivada correctamente.`);
    } catch(e) {
      formError('enrollment-form-error',
        errorText(e,'No fue posible registrar la matrícula'));
    } finally {
      btn.disabled = false;
    }
  }

  async function withdrawEnrollment(id) {
    const enrollment = s.enrollments.find(m => Number(m.id) === id);
    if (!enrollment || enrollment.estado !== 'ACTIVA') return;

    if (!confirm(
      `¿Retirar a ${enrollment.estudianteNombre} del grupo ${enrollment.grupoCodigo}?\n\nEl historial de matrícula se conservará.`
    )) return;

    try {
      await api(`/matriculas/${id}/retirar`,{method:'PATCH'});
      await loadEnrollments();
      toast('Matrícula retirada correctamente.');
    } catch(e) {
      toast(errorText(e,'No se pudo retirar la matrícula'));
    }
  }

  // ETAPA 8 — EVALUACIONES, NOTAS Y DEFINITIVAS

  // CORREGIDO: CARGA Y CONSERVACIÓN DEL GRUPO
  async function loadEvaluationGroups() {
    try {
      const data = await api('/grupos');
      if (!s.ok) return;

      s.groups = Array.isArray(data) ? data : data.content || [];
      renderGroups();

      const previous = s.gradeGroup ||
        sessionStorage.getItem('unigest-grade-group') || '';

      selectOptions('grade-group', s.groups.map(g => ({
        id: g.id,
        label: `${g.codigo} — ${g.asignaturaNombre}`
      })), 'Selecciona un grupo');

      if (s.groups.some(g => String(g.id) === previous)) {
        s.gradeGroup = previous;
        $('grade-group').value = previous;
      } else {
        s.gradeGroup = '';
        sessionStorage.removeItem('unigest-grade-group');
      }

      await loadEvaluations();
    } catch(e) {
      toast(errorText(e,'No fue posible cargar los grupos'));
    }
  }

  async function loadEvaluations() {
    const seq = ++s.evalSeq;

    s.evaluations = [];
    s.grades = [];
    s.finals = [];
    s.gradeEvaluation = '';

    renderEvaluations();
    renderGrades();
    renderFinals();

    if (!s.gradeGroup) return;

    $('evaluations-body').innerHTML =
      empty(5,'Consultando evaluaciones...');

    try {
      const [evaluations,finals] = await Promise.all([
        api(`/evaluaciones/grupo/${Number(s.gradeGroup)}`),
        api(`/calificaciones/definitivas/grupo/${Number(s.gradeGroup)}`)
      ]);

      if (seq !== s.evalSeq || !s.ok) return;

      s.evaluations = Array.isArray(evaluations) ? evaluations : [];
      s.finals = Array.isArray(finals) ? finals : [];

      renderEvaluations();
      renderFinals();
    } catch(e) {
      if (seq !== s.evalSeq) return;
      renderEvaluations();
      flash(errorText(e,'No se pudieron cargar evaluaciones'));
      if (e.status === 401) reset();
    }
  }

  function renderEvaluations() {
    const all = s.evaluations;
    const weight = all.reduce(
      (sum,x) => sum + Number(x.ponderacion || 0),0);
    const published = all.filter(x => x.publicada).length;

    set('grade-total',s.gradeGroup ? all.length : '—');
    set('grade-weight',s.gradeGroup
      ? `${weight.toLocaleString('es-CO')}%` : '—');
    set('grade-published',s.gradeGroup ? published : '—');
    set('evaluations-count',`${all.length} evaluaciones`);

    $('new-eval-btn').disabled = !s.gradeGroup;

    const prev = $('grade-eval').value;
    selectOptions('grade-eval',all.map(x => ({
      id:x.id,
      label:`${x.titulo} (${x.ponderacion}%)`
    })), 'Selecciona una evaluación');

    $('grade-eval').value =
      all.some(x => String(x.id) === prev) ? prev : '';

    $('evaluations-body').innerHTML = !s.gradeGroup
      ? empty(5,'Selecciona un grupo para ver sus evaluaciones.')
      : all.length ? all.map(x => `<tr>
        <td><strong>${esc(x.titulo)}</strong></td>
        <td><span class="code-tag">${esc(x.ponderacion)}%</span></td>
        <td>${date(x.fecha)}</td>
        <td><span class="${x.publicada ? 'tag-active' : 'read-only-label'}">
          ${x.publicada ? 'Publicada' : 'Borrador'}</span></td>
        <td><div class="table-actions">${x.publicada ? '—' : `
          <button class="text-action"
            data-eval-edit="${Number(x.id)}">Editar</button>
          <button class="text-action"
            data-eval-publish="${Number(x.id)}"
            ${weight !== 100
              ? 'disabled title="Las ponderaciones deben sumar 100%"'
              : ''}>Publicar</button>`}</div></td>
      </tr>`).join('')
      : empty(5,'Todavía no hay evaluaciones para este grupo.');
  }

  async function loadGrades() {
    const seq = ++s.gradeSeq;
    s.gradeEvaluation = $('grade-eval').value;
    s.grades = [];
    renderGrades();

    if (!s.gradeEvaluation) return;

    $('grades-body').innerHTML =
      empty(4,'Consultando calificaciones...');

    try {
      const rows = await api(
        `/calificaciones/evaluacion/${Number(s.gradeEvaluation)}`);

      if (seq !== s.gradeSeq || !s.ok) return;

      s.grades = Array.isArray(rows) ? rows : [];
      renderGrades();
    } catch(e) {
      if (seq === s.gradeSeq)
        toast(errorText(e,'No se pudieron consultar las notas'));
    }
  }

  function renderGrades() {
    $('grades-body').innerHTML = !s.gradeEvaluation
      ? empty(4,'Selecciona una evaluación para consultar las notas.')
      : s.grades.length ? s.grades.map(m => `<tr>
        <td><div class="grade-person">
          <strong>${esc(m.estudianteNombre)}</strong>
          <small>${esc(m.estudianteCodigo)}</small>
        </div></td>
        <td><span class="enrollment-status ${esc(m.estado.toLowerCase())}">
          ${esc(m.estado)}</span></td>
        <td><strong>${m.nota == null
          ? 'Pendiente' : Number(m.nota).toFixed(2)}</strong></td>
        <td>${m.estado === 'ACTIVA'
          ? `<button class="text-action"
              data-grade-save="${Number(m.matriculaId)}">
              ${m.nota == null ? 'Calificar' : 'Corregir'}</button>`
          : '—'}</td>
      </tr>`).join('')
      : empty(4,'Este grupo aún no tiene matrículas.');
  }

  function renderFinals() {
    $('finals-list').innerHTML = !s.gradeGroup
      ? '<p class="empty-catalog">Selecciona un grupo para consultar definitivas.</p>'
      : s.finals.length ? s.finals.map(x => `
        <div class="final-row">
          <div><strong>${esc(x.estudianteNombre)}</strong>
            <small>${esc(x.estudianteCodigo)}</small></div>
          <span class="final-grade ${x.aprobado === false ? 'low' : ''}">
            ${x.definitiva == null
              ? 'Pendiente' : Number(x.definitiva).toFixed(2)}
          </span>
        </div>`).join('')
      : '<p class="empty-catalog">No hay matrículas activas en este grupo.</p>';
  }

  function openEvalModal(id = null) {
    if (!s.gradeGroup) {
      toast('Primero selecciona un grupo.');
      return;
    }

    const evalData = s.evaluations.find(e => Number(e.id) === id);
    s.evalEditing = evalData?.id ?? null;

    $('eval-form').reset();
    formError('eval-error');

    set('eval-modal-title',
      evalData ? 'Editar evaluación' : 'Nueva evaluación');
    set('eval-submit',
      evalData ? 'Guardar cambios' : 'Guardar evaluación');

    if (evalData) {
      ['titulo','ponderacion','fecha'].forEach(k =>
        field('eval-form',k).value = evalData[k] ?? '');
    }

    $('eval-modal').hidden = false;
    document.body.style.overflow = 'hidden';
    field('eval-form','titulo').focus();
  }

  async function saveEvaluation(e) {
    e.preventDefault();
    if (!s.gradeGroup) return;

    const btn = $('eval-submit');
    const data = Object.fromEntries(new FormData($('eval-form')));

    data.grupoId = Number(s.gradeGroup);
    data.ponderacion = Number(data.ponderacion);
    data.fecha ||= null;

    if (!Number.isFinite(data.ponderacion) ||
        data.ponderacion <= 0 || data.ponderacion > 100) {
      formError('eval-error',
        'La ponderación debe estar entre 0,01% y 100%.');
      return;
    }

    btn.disabled = true;
    formError('eval-error');

    try {
      const id = s.evalEditing;

      await api(id ? `/evaluaciones/${id}` : '/evaluaciones',{
        method:id ? 'PUT' : 'POST',
        body:JSON.stringify(data)
      });

      hideExtraModal('eval-modal');
      await loadEvaluations();

      toast(id ? 'Evaluación actualizada.' : 'Evaluación creada.');
    } catch(err) {
      formError('eval-error',
        errorText(err,'No se pudo guardar la evaluación'));
    } finally {
      btn.disabled = false;
    }
  }

  async function publishEvaluation(id) {
    if (!confirm(
      '¿Publicar la evaluación? La actividad quedará bloqueada para edición.'
    )) return;

    try {
      await api(`/evaluaciones/${id}/publicar`,{method:'PATCH'});
      await loadEvaluations();
      toast('Evaluación publicada.');
    } catch(err) {
      toast(errorText(err,'No se pudo publicar'));
    }
  }

  function openGradeModal(id) {
    const row = s.grades.find(x => Number(x.matriculaId) === id);
    if (!row || row.estado !== 'ACTIVA' || !s.gradeEvaluation) return;

    s.gradeMatricula = id;
    $('grade-form').reset();
    formError('grade-error');

    set('grade-modal-title',`Nota de ${row.estudianteNombre}`);
    field('grade-form','nota').value = row.nota ?? '';
    field('grade-form','motivo').required = row.nota != null;
    field('grade-form','motivo').placeholder = row.nota == null
      ? 'Opcional para primera nota'
      : 'Obligatorio si cambias la nota';

    $('grade-modal').hidden = false;
    document.body.style.overflow = 'hidden';
    field('grade-form','nota').focus();
  }

  async function saveGrade(e) {
    e.preventDefault();

    const btn = $('grade-submit');
    const data = Object.fromEntries(new FormData($('grade-form')));

    data.matriculaId = s.gradeMatricula;
    data.evaluacionId = Number(s.gradeEvaluation);
    data.nota = Number(data.nota);

    if (!Number.isFinite(data.nota) ||
        data.nota < 0 || data.nota > 5) {
      formError('grade-error',
        'La nota debe estar entre 0,00 y 5,00.');
      return;
    }

    btn.disabled = true;
    formError('grade-error');

    try {
      await api('/calificaciones',{
        method:'POST',
        body:JSON.stringify(data)
      });

      hideExtraModal('grade-modal');
      await loadGrades();

      const dataFinal = await api(
        `/calificaciones/definitivas/grupo/${Number(s.gradeGroup)}`);

      s.finals = Array.isArray(dataFinal) ? dataFinal : [];
      renderFinals();
      toast('Calificación registrada correctamente.');
    } catch(err) {
      formError('grade-error',
        errorText(err,'No se pudo guardar la nota'));
    } finally {
      btn.disabled = false;
    }
  }

  function hideExtraModal(id) {
    $(id).hidden = true;
    document.body.style.overflow = '';

    if (id === 'eval-modal') s.evalEditing = null;
    if (id === 'grade-modal') s.gradeMatricula = null;
  }

  // ETAPA 9 — REPORTES ACADÉMICOS Y BOLETINES
  const reportNota = n => n == null ? 'Pendiente' : Number(n).toFixed(2);
  const reportEstado = n => n == null ? 'Pendiente' : Number(n) >= 3 ? 'Aprobada' : 'Reprobada';

  function reportGradeBadge(n) {
    if (n == null) return '<span class="report-pending">Pendiente</span>';
    return `<span class="${Number(n) >= 3 ? 'report-pass' : 'report-fail'}">${reportNota(n)}</span>`;
  }

  async function loadReportOptions() {
    if (!s.ok) return;
    try {
      const [grupos, periodos] = await Promise.all([api('/grupos'), api('/periodos')]);
      if (!s.ok || s.view !== 'reportes') return;

      const gruposList = Array.isArray(grupos) ? grupos : grupos.content || [];
      const periodosList = Array.isArray(periodos) ? periodos : periodos.content || [];
      selectOptions('report-group', gruposList.map(g => ({
        id:g.id, label:`${g.codigo} — ${g.asignaturaNombre}`
      })), 'Selecciona un grupo');
      selectOptions('report-period', periodosList.map(p => ({
        id:p.id, label:`${p.codigo} — ${p.nombre}`
      })), 'Selecciona un período');

      s.reportGroup = gruposList.some(g => String(g.id) === s.reportGroup)
        ? s.reportGroup : '';
      s.reportPeriod = periodosList.some(p => String(p.id) === s.reportPeriod)
        ? s.reportPeriod : '';
      $('report-group').value = s.reportGroup;
      $('report-period').value = s.reportPeriod;

      await Promise.all([loadGroupReport(), loadPeriodStudents()]);
    } catch(e) {
      toast(errorText(e,'No fue posible cargar los filtros de reportes'));
      if (e.status === 401) reset();
    }
  }

  async function loadGroupReport() {
    const seq = ++s.reportSeq;
    s.groupReport = null;
    renderGroupReport();
    if (!s.reportGroup || !s.ok) return;
    $('report-group-body').innerHTML = empty(4,'Consultando consolidado...');
    try {
      const report = await api(`/reportes/grupos/${Number(s.reportGroup)}`);
      if (seq !== s.reportSeq || !s.ok) return;
      s.groupReport = report;
      renderGroupReport();
    } catch(e) {
      if (seq !== s.reportSeq) return;
      $('report-group-body').innerHTML = empty(4,'No fue posible cargar el reporte.');
      toast(errorText(e,'No se pudo obtener el consolidado'));
      if (e.status === 401) reset();
    }
  }

  function renderGroupReport() {
    const report = s.groupReport;
    const resumen = report?.resumen;
    $('report-group-csv').disabled = !report;
    $('report-group-print').disabled = !report;
    set('report-total', resumen?.total ?? '—');
    set('report-complete', resumen?.completas ?? '—');
    set('report-average', reportNota(resumen?.promedio).replace('Pendiente','—'));
    set('report-group-caption', report
      ? `${report.periodoNombre} · ${report.asignaturaNombre}`
      : 'Selecciona un grupo para consultar.');
    set('report-group-footer', report
      ? `${resumen.completas} definitivas · ${resumen.aprobadas} aprobadas · ${resumen.reprobadas} reprobadas · ${resumen.pendientes} pendientes`
      : 'Sin reporte seleccionado');
    const columns = report?.evaluaciones || [];
    $('report-group-head').innerHTML = '<tr><th>ESTUDIANTE</th>' +
      columns.map(e => `<th>${esc(e.titulo)}<br><small>${esc(e.ponderacion)}%</small></th>`).join('') +
      '<th>DEFINITIVA</th><th>RESULTADO</th></tr>';
    const rows = report?.estudiantes || [];
    $('report-group-body').innerHTML = !report
      ? empty(4,'Selecciona un grupo académico.')
      : rows.length ? rows.map(m => `<tr>
        <td><span class="report-student">${esc(m.estudianteNombre)}</span>
          <small class="report-code">${esc(m.estudianteCodigo)}</small></td>
        ${columns.map(e => `<td>${esc(reportNota(m.notas.find(n => n.evaluacionId === e.id)?.nota))}</td>`).join('')}
        <td>${reportGradeBadge(m.definitiva)}</td>
        <td>${esc(reportEstado(m.definitiva))}</td>
      </tr>`).join('')
      : empty(columns.length + 3, 'Este grupo no tiene matrículas activas.');
  }

  async function loadPeriodStudents() {
    const seq = ++s.reportPeriodSeq;
    s.studentReport = null;
    renderStudentReport();
    $('report-student').disabled = true;
    selectOptions('report-student', [], 'Selecciona un estudiante');
    if (!s.reportPeriod || !s.ok) {
      s.reportStudent = '';
      return;
    }
    try {
      const estudiantes = await api(`/reportes/periodos/${Number(s.reportPeriod)}/estudiantes`);
      if (seq !== s.reportPeriodSeq || !s.ok) return;
      const rows = Array.isArray(estudiantes) ? estudiantes : [];
      selectOptions('report-student', rows.map(e => ({
        id:e.id, label:`${e.codigo} — ${e.nombre}`
      })), rows.length ? 'Selecciona un estudiante' : 'No hay estudiantes matriculados');
      s.reportStudent = rows.some(e => String(e.id) === s.reportStudent)
        ? s.reportStudent : '';
      $('report-student').value = s.reportStudent;
      $('report-student').disabled = !rows.length;
      if (s.reportStudent) await loadStudentReport();
    } catch(e) {
      if (seq !== s.reportPeriodSeq) return;
      toast(errorText(e,'No fue posible cargar estudiantes del período'));
      if (e.status === 401) reset();
    }
  }

  async function loadStudentReport() {
    const seq = ++s.reportStudentSeq;
    s.studentReport = null;
    renderStudentReport();
    if (!s.reportPeriod || !s.reportStudent || !s.ok) return;
    $('report-student-body').innerHTML = empty(5,'Consultando boletín...');
    try {
      const boletin = await api(`/reportes/boletines/estudiantes/${Number(s.reportStudent)}?periodoId=${Number(s.reportPeriod)}`);
      if (seq !== s.reportStudentSeq || !s.ok) return;
      s.studentReport = boletin;
      renderStudentReport();
    } catch(e) {
      if (seq !== s.reportStudentSeq) return;
      $('report-student-body').innerHTML = empty(5,'No fue posible cargar el boletín.');
      toast(errorText(e,'No se pudo obtener el boletín'));
      if (e.status === 401) reset();
    }
  }

  function renderStudentReport() {
    const boletin = s.studentReport;
    $('report-student-csv').disabled = !boletin;
    $('report-student-print').disabled = !boletin;
    $('report-student-info').hidden = !boletin;
    if (!boletin) {
      $('report-student-info').innerHTML = '';
      $('report-student-body').innerHTML = empty(5,
        'Selecciona un período y un estudiante para generar su boletín.');
      set('report-student-footer','Boletín de solo lectura');
      return;
    }
    const r = boletin.resumen;
    $('report-student-info').innerHTML = `
      <div><strong>${esc(boletin.estudianteNombre)}</strong>
        <small>${esc(boletin.estudianteCodigo)} · ${esc(boletin.programa)}</small>
        <small>Período: ${esc(boletin.periodoNombre)}</small></div>
      <div><small>Promedio de materias con nota definitiva</small>
        <span class="report-average">${esc(r.promedio == null ? '—' : reportNota(r.promedio))}</span></div>`;
    set('report-student-footer',
      `${r.total} materias · ${r.completas} completas · ${r.aprobadas} aprobadas · ${r.pendientes} pendientes`);
    $('report-student-body').innerHTML = boletin.materias.length
      ? boletin.materias.map(m => `<tr>
        <td><span class="report-student">${esc(m.nombre)}</span>
          <small class="report-code">${esc(m.codigo)} · Matrícula ${esc(m.estadoMatricula)}</small>
          <div class="report-detail-notes">${m.notas.map(n => `<span>${esc(n.titulo)} (${esc(n.ponderacion)}%): ${esc(reportNota(n.nota))}</span>`).join('')}</div></td>
        <td>${esc(m.grupoCodigo)}</td><td>${esc(m.creditos)}</td>
        <td>${reportGradeBadge(m.definitiva)}</td><td>${esc(reportEstado(m.definitiva))}</td>
      </tr>`).join('')
      : empty(5,'No hay matrículas activas o finalizadas en este período.');
  }

  // Se protege el CSV contra fórmulas inyectadas por celdas que comienzan con =, +, - o @.
  function csvCell(value) {
    let v = String(value ?? '');
    if (/^[\s]*[=+\-@]/.test(v)) v = "'" + v;
    return '"' + v.replace(/"/g,'""') + '"';
  }

  function downloadReportCsv(kind) {
    const grupo = kind === 'grupo' ? s.groupReport : null;
    const boletin = kind === 'boletin' ? s.studentReport : null;
    if (!grupo && !boletin) return;
    let rows, filename;
    if (grupo) {
      rows = [[ 'Código estudiante','Estudiante','Grupo','Período','Asignatura',
        ...grupo.evaluaciones.map(e => `${e.titulo} (${e.ponderacion}%)`),
        'Definitiva','Resultado' ],
        ...grupo.estudiantes.map(m => [m.estudianteCodigo,m.estudianteNombre,
          grupo.grupoCodigo,grupo.periodoNombre,grupo.asignaturaNombre,
          ...grupo.evaluaciones.map(e => m.notas.find(n => n.evaluacionId === e.id)?.nota ?? ''),
          m.definitiva ?? '', reportEstado(m.definitiva)])];
      filename = `UniGest_Reporte_${grupo.grupoCodigo}.csv`;
    } else {
      rows = [['Estudiante','Código','Período','Asignatura','Código asignatura',
        'Grupo','Créditos','Matrícula','Definitiva','Resultado','Detalle evaluaciones'],
        ...boletin.materias.map(m => [boletin.estudianteNombre,
          boletin.estudianteCodigo,boletin.periodoNombre,m.nombre,m.codigo,
          m.grupoCodigo,m.creditos,m.estadoMatricula,m.definitiva ?? '',
          reportEstado(m.definitiva),
          m.notas.map(n => `${n.titulo} (${n.ponderacion}%): ${reportNota(n.nota)}`).join('; ')])];
      filename = `UniGest_Boletin_${boletin.estudianteCodigo}.csv`;
    }
    const contenido = '\ufeff' + rows.map(row => row.map(csvCell).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([contenido],{type:'text/csv;charset=utf-8'}));
    const a = document.createElement('a');
    a.href = url;
    a.download = filename.replace(/[^\w.\-]/g,'_');
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url),1500);
  }

  function printAcademicReport(kind) {
    const report = kind === 'grupo' ? s.groupReport : s.studentReport;
    if (!report) return;
    const hoja = $('report-print-sheet');
    const logo = '<img src="/panel/img/logo-unigest.png" alt="Logo UniGest">';
    const encabezado = `<div class="print-header">${logo}<div>
      <small>UNIGEST · GESTIÓN UNIVERSITARIA</small><h1>${kind === 'grupo'
        ? 'Reporte de calificaciones' : 'Boletín académico'}</h1></div></div>`;
    let meta, head, rows, pie;
    if (kind === 'grupo') {
      meta = `<strong>Grupo:</strong> ${esc(report.grupoCodigo)} ·
        <strong>Asignatura:</strong> ${esc(report.asignaturaNombre)} ·
        <strong>Período:</strong> ${esc(report.periodoNombre)} ·
        <strong>Docente:</strong> ${esc(report.docenteNombre)}`;
      head = `<tr><th>ESTUDIANTE</th>${report.evaluaciones.map(e =>
        `<th>${esc(e.titulo)} (${esc(e.ponderacion)}%)</th>`).join('')}
        <th>DEFINITIVA</th><th>RESULTADO</th></tr>`;
      rows = report.estudiantes.map(m => `<tr>
        <td>${esc(m.estudianteNombre)} (${esc(m.estudianteCodigo)})</td>
        ${report.evaluaciones.map(e => `<td>${esc(reportNota(m.notas.find(n => n.evaluacionId === e.id)?.nota))}</td>`).join('')}
        <td>${esc(reportNota(m.definitiva))}</td><td>${esc(reportEstado(m.definitiva))}</td></tr>`).join('');
      pie = `${report.resumen.total} estudiantes · ${report.resumen.completas} definitivas · Promedio ${esc(reportNota(report.resumen.promedio))}`;
    } else {
      meta = `<strong>Estudiante:</strong> ${esc(report.estudianteNombre)} (${esc(report.estudianteCodigo)}) ·
        <strong>Programa:</strong> ${esc(report.programa)} ·
        <strong>Período:</strong> ${esc(report.periodoNombre)}`;
      head = '<tr><th>ASIGNATURA</th><th>GRUPO</th><th>CRÉDITOS</th><th>NOTAS PARCIALES</th><th>DEFINITIVA</th><th>RESULTADO</th></tr>';
      rows = report.materias.map(m => `<tr><td>${esc(m.nombre)} (${esc(m.codigo)})</td>
        <td>${esc(m.grupoCodigo)}</td><td>${esc(m.creditos)}</td>
        <td>${m.notas.map(n => `${esc(n.titulo)} (${esc(n.ponderacion)}%): ${esc(reportNota(n.nota))}`).join(' · ')}</td>
        <td>${esc(reportNota(m.definitiva))}</td><td>${esc(reportEstado(m.definitiva))}</td></tr>`).join('');
      pie = `${report.resumen.total} materias · ${report.resumen.completas} completas · Promedio ${esc(reportNota(report.resumen.promedio))}`;
    }
    hoja.innerHTML = encabezado + `<div class="print-meta">${meta}</div>
      <table><thead>${head}</thead><tbody>${rows || '<tr><td>No hay registros para mostrar.</td></tr>'}</tbody></table>
      <p><strong>${pie}</strong></p><div class="print-footer">Generado el ${esc(new Date().toLocaleDateString('es-CO'))}.
      Reporte informativo; no sustituye una certificación oficial. Las notas pendientes no se estiman.</div>`;
    hoja.hidden = false;
    document.body.classList.add('print-report-active');
    window.addEventListener('afterprint',() => {
      document.body.classList.remove('print-report-active');
      hoja.hidden = true;
      hoja.innerHTML = '';
    },{once:true});
    window.print();
  }

  // ETAPA 10: EMISIÓN ADMINISTRATIVA DE CREDENCIALES
  function closeCredentialModal() {
    $('credential-modal').hidden = true;
    $('credential-password').textContent = '';
    $('credential-user').textContent = '';
    document.body.style.overflow = '';
  }

  async function issueCredential(id, role) {
    if (!s.ok) return;
    if (!confirm(`¿Generar una contraseña temporal nueva para este ${role}? La anterior dejará de funcionar.`)) return;
    try {
      const data = await api(`/admin/accesos/${id}/credencial`, { method:'POST' });
      $('credential-user').textContent = data.correo;
      $('credential-password').textContent = data.claveTemporal;
      $('credential-modal').hidden = false;
      document.body.style.overflow = 'hidden';
      toast('Credencial generada. Entrégala de forma privada y exige cambiarla al entrar.');
    } catch(e) {
      toast(errorText(e, 'No se pudo emitir la credencial'));
    }
  }

  // ACTUALIZACIÓN GENERAL
  async function refresh() {
    if (!s.ok) return;

    flash();
    await catalogs();
    await Promise.all([students(),total(),teachers(),groups()]);

    if (s.view === 'matriculas') {
      updateEnrollmentGroupOptions();
      await loadEnrollments();
    }

    if (s.view === 'evaluaciones') await loadEvaluationGroups();
    if (s.view === 'reportes') await loadReportOptions();
    if (s.ok) toast('Información actualizada.');
  }

  // EVENTOS
  function bind() {
    const on = (id,event,fn) => $(id).addEventListener(event,fn);

    on('login-form','submit',login);
    on('logout-btn','click',logout);
    on('refresh-btn','click',refresh);
    on('credential-close','click',closeCredentialModal);
    on('credential-cancel','click',closeCredentialModal);
    on('credential-modal','click',e => {
      if (e.target === $('credential-modal')) closeCredentialModal();
    });
    on('credential-copy','click',async () => {
      const value = $('credential-password').textContent;
      try {
        await navigator.clipboard.writeText(value);
        toast('Contraseña copiada al portapapeles.');
      } catch {
        toast('Copia manualmente la contraseña temporal.');
      }
    });

    on('report-group','change',() => {
      s.reportGroup = $('report-group').value;
      loadGroupReport();
    });
    on('report-period','change',() => {
      s.reportPeriod = $('report-period').value;
      s.reportStudent = '';
      loadPeriodStudents();
    });
    on('report-student','change',() => {
      s.reportStudent = $('report-student').value;
      loadStudentReport();
    });
    on('report-group-csv','click',() => downloadReportCsv('grupo'));
    on('report-student-csv','click',() => downloadReportCsv('boletin'));
    on('report-group-print','click',() => printAcademicReport('grupo'));
    on('report-student-print','click',() => printAcademicReport('boletin'));

    // Guardar selección del grupo en esta pestaña
    on('grade-group','change',() => {
      s.gradeGroup = $('grade-group').value;

      if (s.gradeGroup) {
        sessionStorage.setItem('unigest-grade-group', s.gradeGroup);
      } else {
        sessionStorage.removeItem('unigest-grade-group');
      }

      loadEvaluations();
    });

    on('grade-eval','change',loadGrades);
    on('new-eval-btn','click',() => openEvalModal());
    on('eval-form','submit',saveEvaluation);
    on('grade-form','submit',saveGrade);

    ['eval','grade'].forEach(type => {
      on(`${type}-close`,'click',
        () => hideExtraModal(`${type}-modal`));
      on(`${type}-cancel`,'click',
        () => hideExtraModal(`${type}-modal`));
      on(`${type}-modal`,'click',e => {
        if (e.target === $(`${type}-modal`))
          hideExtraModal(`${type}-modal`);
      });
    });

    on('profile-close','click',
      () => hideExtraModal('teacher-profile-modal'));

    on('teacher-profile-modal','click',e => {
      if (e.target === $('teacher-profile-modal'))
        hideExtraModal('teacher-profile-modal');
    });

    on('menu-toggle','click',() => {
      const open = $('sidebar').classList.toggle('open');
      $('menu-toggle').setAttribute('aria-expanded',String(open));
    });

    $$('[data-view]').forEach(b =>
      b.addEventListener('click',() => navigate(b.dataset.view)));

    $$('[data-go]').forEach(b =>
      b.addEventListener('click',() => navigate(b.dataset.go)));

    on('welcome-student-btn','click',
      () => navigate('estudiantes'));

    on('quick-new','click',() => {
      navigate('estudiantes');
      openStudentModal();
    });

    [
      ['student','new-student-btn','modal-close','form-cancel',
        openStudentModal,saveStudent],
      ['teacher','new-teacher-btn','teacher-modal-close',
        'teacher-form-cancel',openTeacherModal,saveTeacher],
      ['group','new-group-btn','group-modal-close',
        'group-form-cancel',openGroupModal,saveGroup],
      ['enrollment','new-enrollment-btn','enrollment-modal-close',
        'enrollment-form-cancel',openEnrollmentModal,saveEnrollment]
    ].forEach(([type,newId,closeId,cancelId,open,save]) => {
      on(newId,'click',() => open());
      on(closeId,'click',() => closeModal(type));
      on(cancelId,'click',() => closeModal(type));
      on(`${type}-form`,'submit',save);

      on(`${type}-modal`,'click',e => {
        if (e.target === $(`${type}-modal`)) closeModal(type);
      });
    });

    // Los formularios solo se cierran con Escape
    window.addEventListener('keydown',e => {
      if (e.key === 'Escape') {
        ['student','teacher','group','enrollment'].forEach(type => {
          if (!$(`${type}-modal`).hidden) closeModal(type);
        });

        ['teacher-profile-modal','eval-modal','grade-modal'].forEach(id => {
          if (!$(id).hidden) hideExtraModal(id);
        });
        if (!$('credential-modal').hidden) closeCredentialModal();
      }
    });

    let timer;

    on('student-search','input',() => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        s.search = $('student-search').value.trim();
        s.page = 0;
        students();
      },300);
    });

    on('student-program-filter','change',() => {
      s.program = $('student-program-filter').value;
      s.page = 0;
      students();
    });

    on('prev-page','click',() => {
      if (s.page > 0) {
        s.page--;
        students();
      }
    });

    on('next-page','click',() => {
      if (s.students && !s.students.last) {
        s.page++;
        students();
      }
    });

    on('teacher-search','input',() => {
      s.teacherSearch = $('teacher-search').value.trim();
      renderTeachers();
    });

    on('group-search','input',() => {
      s.groupSearch = $('group-search').value.trim();
      renderGroups();
    });

    on('group-status-filter','change',() => {
      s.groupStatus = $('group-status-filter').value;
      renderGroups();
    });

    on('enrollment-group-filter','change',() => {
      s.enrollmentGroup = $('enrollment-group-filter').value;
      loadEnrollments();
    });

    on('enrollment-status-filter','change',() => {
      s.enrollmentStatus = $('enrollment-status-filter').value;
      renderEnrollments();
    });

    on('subject-search','input',() => {
      s.subjectSearch = $('subject-search').value;
      renderSubjects();
    });

    function delegate(container,handlers) {
      on(container,'click',e => {
        const b = e.target.closest('button');
        if (!b) return;

        for (const [attribute,action] of Object.entries(handlers)) {
          if (b.dataset[attribute]) {
            action(Number(b.dataset[attribute]),b);
            return;
          }
        }
      });
    }

    delegate('students-body',{
      edit:editStudent,
      credentialStudent:id => issueCredential(id, 'estudiante'),
      disable:disableStudent
    });

    delegate('teachers-body',{
      teacherEdit:editTeacher,
      credentialTeacher:id => issueCredential(id, 'docente'),
      teacherDisable:disableTeacher,
      profileTeacher:showTeacherProfile
    });

    delegate('groups-body',{
      groupEdit:editGroup,
      profileTeacher:showTeacherProfile,
      groupState:(id,b) =>
        changeGroupState(id,b.dataset.nextState)
    });

    delegate('enrollments-body',{
      enrollmentWithdraw:withdrawEnrollment
    });

    delegate('evaluations-body',{
      evalEdit:openEvalModal,
      evalPublish:publishEvaluation
    });

    delegate('grades-body',{
      gradeSave:openGradeModal
    });
  }

  bind();
  restore();
})();
