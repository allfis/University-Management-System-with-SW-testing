'use strict';


const RESOURCES = {
  admins:   { label: 'Admins',   singular: 'Admin',   extra: [],
              columns: ['id', 'username', 'fullName', 'email'] },
  teachers: { label: 'Teachers', singular: 'Teacher', extra: ['department', 'designation'],
              columns: ['id', 'username', 'fullName', 'email', 'department', 'designation'] },
  students: { label: 'Students', singular: 'Student', extra: ['studentIdNo', 'department', 'semester'],
              columns: ['id', 'username', 'fullName', 'email', 'studentIdNo', 'department', 'semester'] },
};

const ROLE_ACCESS = {
  ADMIN:   ['admins', 'teachers', 'students'],
  TEACHER: ['teachers'],
  STUDENT: ['students'],
};

const FIELDS = {
  username:    { label: 'Username',    required: true, minlength: 3, maxlength: 50 },
  password:    { label: 'Password',    type: 'password', minlength: 6, maxlength: 100 },
  fullName:    { label: 'Full name',   required: true, maxlength: 100 },
  email:       { label: 'Email',       type: 'email', required: true, maxlength: 100 },
  department:  { label: 'Department',  maxlength: 100 },
  designation: { label: 'Designation', maxlength: 100 },
  studentIdNo: { label: 'Student ID',  maxlength: 30 },
  semester:    { label: 'Semester',    type: 'number', min: 1, max: 12 },
};

/* ------------------------------------------------------------------ *
 * State + tiny helpers
 * ------------------------------------------------------------------ */
const $ = (id) => document.getElementById(id);

let me = null;          // logged-in user (from /api/auth/me)
let current = null;     // key of the open tab: 'admins' | 'teachers' | 'students'
let rows = [];          // records shown in the table
let filteredRows = [];  // records matching the search query
let editingRow = null;  // record being edited; null while creating
let toastTimer = null;

/** Build a DOM element. Text goes in via append(), so nothing is parsed as HTML. */
function h(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === 'class') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else if (value !== false && value != null) node.setAttribute(key, value === true ? '' : value);
  }
  node.append(...children.filter((c) => c != null));
  return node;
}

function columnLabel(name) {
  return name === 'id' ? 'ID' : FIELDS[name].label;
}

function isMe(row) {
  return me && row.username === me.username;
}

function toast(message, bad = false) {
  const box = $('toast');
  box.textContent = message;
  box.className = bad ? 'toast bad' : 'toast';
  box.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { box.hidden = true; }, 3500);
}

/** fetch wrapper: JSON in/out, throws Error with .status and .fieldErrors on failure. */
async function api(path, { method = 'GET', body } = {}) {
  const res = await fetch(path, {
    method,
    credentials: 'same-origin',
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;

  let data = null;
  try { data = await res.json(); } catch (_) { /* empty body */ }

  if (!res.ok) {
    if (res.status === 401 && me) showLogin('Session expired. Please log in again.');
    const err = new Error((data && data.message) || `Request failed (${res.status})`);
    err.status = res.status;
    err.fieldErrors = (data && data.fieldErrors) || {};
    throw err;
  }
  return data;
}

/* ------------------------------------------------------------------ *
 * Login / logout
 * ------------------------------------------------------------------ */
function showLogin(message) {
  me = null;
  current = null;
  if ($('form-dialog').open) $('form-dialog').close();
  $('app-view').hidden = true;
  $('login-view').hidden = false;
  $('login-form').elements.password.value = '';
  setLoginError(message);
  $('login-form').elements.username.focus();
}

function setLoginError(message) {
  const box = $('login-error');
  box.textContent = message || '';
  box.hidden = !message;
}

function startApp(user) {
  me = user;
  $('login-view').hidden = true;
  $('app-view').hidden = false;
  $('user-name').textContent = user.fullName;
  $('user-role').textContent = user.role;
  $('user-avatar').textContent = (user.fullName || user.username || 'U').trim().charAt(0).toUpperCase();
  $('welcome-name').textContent = (user.fullName || user.username || 'there').split(' ')[0];
  $('today-date').textContent = new Intl.DateTimeFormat('en', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }).format(new Date());
  $('stat-role').textContent = user.role;
  renderTabs();
  selectResource(ROLE_ACCESS[user.role][0]);
}

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  setLoginError('');
  try {
    const user = await api('/api/auth/login', {
      method: 'POST',
      body: { username: form.elements.username.value.trim(), password: form.elements.password.value },
    });
    form.reset();
    startApp(user);
  } catch (err) {
    setLoginError(err.message);
  }
});

$('logout-btn').addEventListener('click', async () => {
  try { await api('/api/auth/logout', { method: 'POST' }); } catch (_) { /* leave anyway */ }
  showLogin();
});

/* ------------------------------------------------------------------ *
 * Tabs + table
 * ------------------------------------------------------------------ */
function renderTabs() {
  const tabs = $('tabs');
  const icons = { admins: '♙', teachers: '▣', students: '▤' };
  tabs.replaceChildren(...ROLE_ACCESS[me.role].map((key) =>
    h('button', { class: 'tab', type: 'button', 'data-key': key, onclick: () => selectResource(key) },
      h('span', { class: 'tab-icon' }, icons[key]), RESOURCES[key].label)));
}

function selectResource(key) {
  current = key;
  document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.key === key));
  $('resource-title').textContent = RESOURCES[key].label;
  $('breadcrumb-current').textContent = RESOURCES[key].label;
  $('table-heading').textContent = `All ${RESOURCES[key].label.toLowerCase()}`;
  $('stat-section').textContent = RESOURCES[key].label;
  $('stat-label').textContent = `Total ${RESOURCES[key].label.toLowerCase()}`;
  $('add-btn').innerHTML = `<span>＋</span> Add ${RESOURCES[key].singular}`;
  $('search-input').value = '';
  loadList();
}

async function loadList() {
  const key = current;
  const wrap = $('table-wrap');
  wrap.replaceChildren(h('p', { class: 'muted pad' }, 'Loading…'));
  try {
    const data = await api(`/api/${key}`);
    if (key !== current) return;           // user switched tabs meanwhile
    rows = data;
    applySearch();
  } catch (err) {
    if (err.status !== 401 && key === current) {
      wrap.replaceChildren(h('p', { class: 'error pad' }, err.message));
    }
  }
}

function applySearch() {
  const query = $('search-input').value.trim().toLowerCase();
  filteredRows = rows.filter((row) => Object.values(row).some((value) =>
    String(value ?? '').toLowerCase().includes(query)));
  $('stat-total').textContent = rows.length.toLocaleString();
  $('record-count').textContent = `${rows.length} ${rows.length === 1 ? 'record' : 'records'}`;
  $('table-footer-text').textContent = `Showing ${filteredRows.length} of ${rows.length} records`;
  renderTable();
}

function renderTable() {
  const def = RESOURCES[current];
  const wrap = $('table-wrap');

  if (filteredRows.length === 0) {
    const message = rows.length === 0
      ? `No ${def.label.toLowerCase()} yet. Use “Add ${def.singular}” to create your first record.`
      : 'No records match your search. Try a different keyword.';
    wrap.replaceChildren(h('div', { class: 'empty-state' }, message));
    return;
  }

  const head = h('tr', {},
    ...def.columns.map((c) => h('th', {}, columnLabel(c))),
    h('th', { class: 'actions' }, 'Actions'));

  const body = filteredRows.map((row) => {
    const cells = def.columns.map((c) => {
      if (c === 'username' && isMe(row)) {
        return h('td', {}, row.username, h('span', { class: 'badge' }, 'you'));
      }
      return h('td', {}, row[c] ?? '—');
    });
    const actions = h('td', { class: 'actions' },
      h('button', { class: 'btn small', type: 'button', onclick: () => openForm(row) }, 'Edit'),
      h('button', {
        class: 'btn small danger', type: 'button',
        disabled: isMe(row),
        title: isMe(row) ? 'You cannot delete the account you are logged in with' : null,
        onclick: () => deleteRow(row),
      }, 'Delete'));
    return h('tr', {}, ...cells, actions);
  });

  wrap.replaceChildren(h('table', {}, h('thead', {}, head), h('tbody', {}, ...body)));
}

/* ------------------------------------------------------------------ *
 * Add / edit dialog
 * ------------------------------------------------------------------ */
function formFieldNames() {
  return ['username', 'password', 'fullName', 'email', ...RESOURCES[current].extra];
}

function openForm(row = null) {
  editingRow = row;
  const def = RESOURCES[current];
  $('dialog-title').textContent = `${row ? 'Edit' : 'Add'} ${def.singular}`;
  setFormError('');

  const fields = formFieldNames().map((name) => {
    const f = FIELDS[name];
    const isPassword = name === 'password';
    const required = f.required || (isPassword && !row);
    const input = h('input', {
      name,
      type: f.type || 'text',
      required,
      minlength: f.minlength,
      maxlength: f.maxlength,
      min: f.min,
      max: f.max,
      autocomplete: isPassword ? 'new-password' : 'off',
      placeholder: isPassword && row ? 'Leave blank to keep current password' : null,
      // renaming yourself would break your own session, so it is locked
      readonly: name === 'username' && row && isMe(row),
    });
    if (row && !isPassword && row[name] != null) input.value = row[name];
    return h('label', {}, f.label + (required ? ' *' : ''), input,
      h('span', { class: 'field-error', 'data-for': name }));
  });

  $('form-fields').replaceChildren(...fields);
  $('form-dialog').showModal();
}

function setFormError(message) {
  const box = $('form-error');
  box.textContent = message || '';
  box.hidden = !message;
  document.querySelectorAll('.field-error').forEach((s) => { s.textContent = ''; });
}

$('add-btn').addEventListener('click', () => openForm());
$('cancel-btn').addEventListener('click', () => $('form-dialog').close());
$('cancel-btn-secondary').addEventListener('click', () => $('form-dialog').close());
$('search-input').addEventListener('input', applySearch);

$('record-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  setFormError('');

  const data = new FormData(event.target);
  const body = {};
  for (const name of formFieldNames()) {
    const raw = String(data.get(name) ?? '');
    if (name === 'password') {
      if (raw) body.password = raw;                       // blank = keep old password
    } else if (name === 'semester') {
      body.semester = raw === '' ? null : Number(raw);
    } else {
      body[name] = raw.trim() === '' ? null : raw.trim();
    }
  }

  const def = RESOURCES[current];
  const wasEditing = editingRow !== null;
  const saveBtn = $('save-btn');
  saveBtn.disabled = true;
  try {
    if (wasEditing) await api(`/api/${current}/${editingRow.id}`, { method: 'PUT', body });
    else await api(`/api/${current}`, { method: 'POST', body });
    $('form-dialog').close();
    toast(`${def.singular} ${wasEditing ? 'updated' : 'created'}`);
    await loadList();
  } catch (err) {
    if (err.status === 401) return;
    const entries = Object.entries(err.fieldErrors || {});
    if (entries.length === 0) {
      setFormError(err.message);
    } else {
      setFormError('Please fix the highlighted fields.');   // also clears old field messages
      for (const [name, message] of entries) {
        const span = document.querySelector(`.field-error[data-for="${name}"]`);
        if (span) span.textContent = message;
      }
    }
  } finally {
    saveBtn.disabled = false;
  }
});

/* ------------------------------------------------------------------ *
 * Delete
 * ------------------------------------------------------------------ */
async function deleteRow(row) {
  const def = RESOURCES[current];
  if (!confirm(`Delete ${def.singular.toLowerCase()} "${row.fullName}"? This cannot be undone.`)) return;
  try {
    await api(`/api/${current}/${row.id}`, { method: 'DELETE' });
    toast(`${def.singular} deleted`);
    await loadList();
  } catch (err) {
    if (err.status !== 401) toast(err.message, true);
  }
}

/* ------------------------------------------------------------------ *
 * Start: resume the session if the cookie is still valid
 * ------------------------------------------------------------------ */
(async function init() {
  try {
    startApp(await api('/api/auth/me'));
  } catch (_) {
    showLogin();
  }
})();
