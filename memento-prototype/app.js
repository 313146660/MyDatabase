(() => {
  'use strict';

  const LS_KEY = 'memento_proto_v2';
  const BK_KEY = 'memento_backup_v1';
  const BK_FILE = 'memento-backup.json';
  const DAY = 86400000;
  const $ = (s, r = document) => r.querySelector(s);
  const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const uid = (p) => p + Math.random().toString(36).slice(2, 9);
  const now = () => Date.now();
  const round2 = (n) => Math.round(n * 100) / 100;

  /* ---------------- icons ---------------- */
  const ICONS = {
    home: '<path d="M3 10.6 12 3l9 7.6"/><path d="M5.5 9.3V20a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1V9.3"/><path d="M10 21v-6h4v6"/>',
    link: '<path d="M10.5 13.5a4.5 4.5 0 0 0 6.4 0l2.1-2.1a4.5 4.5 0 0 0-6.4-6.4L11.4 6"/><path d="M13.5 10.5a4.5 4.5 0 0 0-6.4 0l-2.1 2.1a4.5 4.5 0 0 0 6.4 6.4l1.2-1.2"/>',
    settings: '<circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 1.55V21a2 2 0 1 1-4 0v-.09A1.7 1.7 0 0 0 9 19.4a1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.55-1H3a2 2 0 1 1 0-4h.09A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.55V3a2 2 0 1 1 4 0v.09A1.7 1.7 0 0 0 15 4.6a1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.4 9c.2.6.8 1 1.55 1H21a2 2 0 1 1 0 4h-.09c-.71 0-1.35.4-1.51 1Z"/>',
    plus: '<path d="M12 5v14"/><path d="M5 12h14"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2"/>',
    arrowLeft: '<path d="M15 19l-7-7 7-7"/>',
    chevron: '<path d="M9 18l6-6-6-6"/>',
    more: '<circle cx="5" cy="12" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="19" cy="12" r="1.5"/>',
    bell: '<path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10.3 21a2 2 0 0 0 3.4 0"/>',
    x: '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
    star: '<path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2l1.1-6.2L3 9.6l6.2-.9L12 3Z"/>',
    edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
    trash: '<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v5M14 11v5"/>',
    users: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-5 6-5s6 1.7 6 5"/><path d="M16 5.5a3 3 0 0 1 0 5.8"/><path d="M18 15.2c2 .6 3 2 3 4.8"/>',
    box: '<path d="M21 8.5 12 3 3 8.5v7L12 21l9-5.5v-7Z"/><path d="M3.3 8.7 12 13l8.7-4.3"/><path d="M12 13v8"/>',
    doc: '<path d="M14 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8l-5-5Z"/><path d="M14 3v5h5"/><path d="M8 13h8M8 17h5"/>',
    calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 9h18M8 3v4M16 3v4"/>',
    heart: '<path d="M20.8 5.6a5.2 5.2 0 0 0-7.4 0L12 7l-1.4-1.4a5.2 5.2 0 1 0-7.4 7.4L12 21l8.8-8a5.2 5.2 0 0 0 0-7.4Z"/>',
    download: '<path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M5 21h14"/>',
    upload: '<path d="M12 21V9"/><path d="m7 14 5-5 5 5"/><path d="M5 3h14"/>',
    refresh: '<path d="M20 11a8 8 0 0 0-14-5L4 8"/><path d="M4 4v4h4"/><path d="M4 13a8 8 0 0 0 14 5l2-2"/><path d="M20 20v-4h-4"/>',
    help: '<circle cx="12" cy="12" r="9"/><path d="M9.6 9.2a2.5 2.5 0 1 1 3.4 2.3c-.7.3-1 .8-1 1.5v.5"/><circle cx="12" cy="17" r=".6" fill="currentColor" stroke="none"/>',
    check: '<path d="m5 12.5 4.5 4.5L19 6.5"/>',
    spark: '<path d="M12 2v4M12 18v4M4.9 4.9l2.8 2.8M16.3 16.3l2.8 2.8M2 12h4M18 12h4M4.9 19.1l2.8-2.8M16.3 7.7l2.8-2.8"/>',
    phone: '<path d="M6.6 3h3l1.5 4-2 1.4a13 13 0 0 0 5.5 5.5L16 12l4 1.5v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.6 5.2 2 2 0 0 1 6.6 3Z"/>',
    pin: '<path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11Z"/><circle cx="12" cy="10" r="2.6"/>',
    layers: '<path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="m3 13 9 5 9-5"/><path d="m3 17 9 5 9-5"/>',
    sliders: '<path d="M4 7h9"/><circle cx="16" cy="7" r="2.2"/><path d="M21 7h-2.8"/><path d="M4 12h4"/><circle cx="11" cy="12" r="2.2"/><path d="M21 12h-6.8"/><path d="M4 17h11"/><circle cx="18" cy="17" r="2.2"/><path d="M21 17h-.2"/>',
    database: '<ellipse cx="12" cy="5.5" rx="8" ry="3"/><path d="M4 5.5v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6"/><path d="M4 11.5v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6"/>',
    arrowRight: '<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>',
    arrowUp: '<path d="M12 19V5"/><path d="m6 11 6-6 6 6"/>',
    arrowDown: '<path d="M12 5v14"/><path d="m6 13 6 6 6-6"/>',
    list: '<path d="M8 6h13M8 12h13M8 18h13"/><circle cx="4" cy="6" r="1.3" fill="currentColor" stroke="none"/><circle cx="4" cy="12" r="1.3" fill="currentColor" stroke="none"/><circle cx="4" cy="18" r="1.3" fill="currentColor" stroke="none"/>',
    grid: '<rect x="3" y="3" width="7" height="7" rx="1.6"/><rect x="14" y="3" width="7" height="7" rx="1.6"/><rect x="3" y="14" width="7" height="7" rx="1.6"/><rect x="14" y="14" width="7" height="7" rx="1.6"/>',
    cloud: '<path d="M7 18h10a4 4 0 0 0 .6-7.96A6 6 0 0 0 6.2 9.3 3.9 3.9 0 0 0 7 18Z"/>'
  };

  function icon(name, size = 20, stroke = 1.8) {
    return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${stroke}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`;
  }

  /* ---------------- palettes ---------------- */
  const COLORS = [
    { value: '#1976D2', name: '经典蓝' },
    { value: '#0F9D8A', name: '青碧' },
    { value: '#7C4DFF', name: '灵感紫' },
    { value: '#E05252', name: '珊瑚红' },
    { value: '#F29B38', name: '暖橙' },
    { value: '#546E7A', name: '石墨' }
  ];
  const AVATARS = {
    '#1976D2': ['#5b9ee8', '#1565bd'],
    '#0F9D8A': ['#3bc0ad', '#0a7a6b'],
    '#7C4DFF': ['#a98bff', '#6338d6'],
    '#E05252': ['#f08a8a', '#c93b3b'],
    '#F29B38': ['#ffc06b', '#dd7f1c'],
    '#546E7A': ['#8aa4b1', '#405863']
  };
  const ICON_CHOICES = [
    { value: 'users', name: '联系人' },
    { value: 'box', name: '物品' },
    { value: 'doc', name: '文档' },
    { value: 'heart', name: '收藏' },
    { value: 'calendar', name: '日程' },
    { value: 'database', name: '数据' }
  ];
  const FIELD_TYPES = [
    { value: 'text', label: '文本' },
    { value: 'longtext', label: '长文本' },
    { value: 'number', label: '数字' },
    { value: 'tel', label: '电话' },
    { value: 'email', label: '邮箱' },
    { value: 'url', label: '网址' },
    { value: 'date', label: '日期' },
    { value: 'select', label: '单选' },
    { value: 'multiselect', label: '多选' },
    { value: 'pick', label: '关联选择（从其它库选）' },
    { value: 'checkbox', label: '是 / 否' },
    { value: 'rating', label: '评分' },
    { value: 'autonumber', label: '自动编号' },
    { value: 'createdat', label: '创建时间' },
    { value: 'updatedat', label: '更新时间' },
    { value: 'formula', label: '公式' }
  ];
  const MULTI_SEP = '、';

  /* ---------------- seed ---------------- */
  function seed() {
    const t = now();
    return {
      libraries: [
        {
          id: 'lib_customer', name: '客户', icon: 'users', color: '#1976D2',
          fields: [
            { id: 'c_name', name: '姓名', type: 'text' },
            { id: 'c_phone', name: '电话', type: 'tel' },
            { id: 'c_use', name: '用途', type: 'text' },
            { id: 'c_od', name: 'OD', type: 'text' },
            { id: 'c_od_axis', name: 'OD轴向', type: 'text' },
            { id: 'c_os', name: 'OS', type: 'text' },
            { id: 'c_os_axis', name: 'OS轴向', type: 'text' },
            { id: 'c_pd', name: '瞳距', type: 'text' },
            { id: 'c_frame', name: '镜框', type: 'text' },
            { id: 'c_lens', name: '镜片', type: 'text' },
            { id: 'c_total', name: '合计', type: 'number', unit: '元' },
            { id: 'c_date', name: '时间', type: 'text' },
            { id: 'c_no', name: 'NO.', type: 'text' },
            { id: 'c_add', name: 'ADD', type: 'text' },
            { id: 'c_note', name: '备注', type: 'longtext' },
            { id: 'c_customer', name: '客户', type: 'text' }
          ],
          entries: [
            {
              id: 'e_fu', favorite: 0, createdAt: t - 2 * DAY, updatedAt: t - 2 * 3600000,
              values: {
                c_name: '陈晓明', c_phone: '13800000001', c_use: '远用', c_od: '-2.75ds', c_od_axis: '', c_os: '-2.25ds', c_os_axis: '',
                c_pd: '63', c_frame: '眉毛架', c_lens: '慧睦防蓝光非球面1.56', c_total: '380.00', c_date: '2026/11/15', c_no: '1548', c_add: '', c_note: 'DEMO-01', c_customer: ''
              }
            },
            {
              id: 'e_liang', favorite: 0, createdAt: t - 5 * DAY, updatedAt: t - 5 * DAY,
              values: {
                c_name: '李伟', c_phone: '13800000002', c_use: '远用', c_od: '-5.00ds/-0.75dc', c_od_axis: '180', c_os: '-4.75ds/-0.75dc', c_os_axis: '160',
                c_pd: '66', c_frame: '眉毛架, 半框', c_lens: '阿曼斯特非球面1.60', c_total: '350.00', c_date: '2026/11/5', c_no: '1546', c_add: '', c_note: 'DEMO-02', c_customer: ''
              }
            },
            {
              id: 'e_zhou', favorite: 1, createdAt: t - 8 * DAY, updatedAt: t - 3 * DAY,
              values: {
                c_name: '王芳', c_phone: '13800000003', c_use: '远用', c_od: '-2.50ds', c_od_axis: '', c_os: '-0.75ds', c_os_axis: '',
                c_pd: '65', c_frame: '自备', c_lens: '慧睦防蓝光非球面1.56', c_total: '340.00', c_date: '2026/10/31', c_no: '1545', c_add: '', c_note: '', c_customer: ''
              }
            },
            {
              id: 'e_xiao1', favorite: 0, createdAt: t - 12 * DAY, updatedAt: t - 12 * DAY,
              values: {
                c_name: '张小雨', c_phone: '13800000004', c_use: '远用', c_od: '-4.00ds/-1.00dc', c_od_axis: '70', c_os: '-4.25ds/-1.25dc', c_os_axis: '120',
                c_pd: '30\n29', c_frame: 'NK记忆金属', c_lens: '安泰非球面1.56', c_total: '200.00', c_date: '2018/8/31', c_no: '217', c_add: '',
                c_note: '演示备注：视物距离约 1 米', c_customer: ''
              }
            },
            {
              id: 'e_xiao2', favorite: 0, createdAt: t - 30 * DAY, updatedAt: t - 30 * DAY,
              values: {
                c_name: '张小雨', c_phone: '13800000004', c_use: '远用', c_od: '-3.25ds/-0.50dc', c_od_axis: '85', c_os: '-3.75ds/-0.75dc', c_os_axis: '122',
                c_pd: '58', c_frame: 'TR90', c_lens: '安泰非球面1.56', c_total: '350.00', c_date: '2017/8/31', c_no: '189', c_add: '',
                c_note: '演示备注：旧镜数据仅供参考', c_customer: ''
              }
            }
          ]
        },
        {
          id: 'lib_lens', name: '镜片库', icon: 'box', color: '#0F9D8A',
          view: 'matrix',
          matrix: { group: 'lf_model', row: 'lf_sph', col: 'lf_cyl', value: 'lf_stock' },
          alerts: { field: 'lf_stock', defaultThreshold: 0, rules: [] },
          listFields: ['lf_model', 'lf_sph', 'lf_cyl', 'lf_stock'],
          fields: [
            { id: 'lf_model', name: '型号', type: 'text' },
            { id: 'lf_sph', name: '球镜', type: 'text' },
            { id: 'lf_cyl', name: '柱镜', type: 'text' },
            { id: 'lf_stock', name: '库存', type: 'number', unit: '片' },
            { id: 'lf_price', name: '单价', type: 'number', unit: '元' },
            { id: 'lf_tier', name: '适配客单价', type: 'number', unit: '元' }
          ],
          entries: [
            { id: 'le_1', favorite: 1, createdAt: t - 8 * DAY, updatedAt: t - 4 * 3600000, values: { lf_model: '1.56 非球绿膜', lf_sph: '-1.00', lf_cyl: '0', lf_stock: '4' } },
            { id: 'le_2', favorite: 0, createdAt: t - 8 * DAY, updatedAt: t - 2 * DAY, values: { lf_model: '1.56 非球绿膜', lf_sph: '-1.00', lf_cyl: '-0.50', lf_stock: '2' } },
            { id: 'le_3', favorite: 0, createdAt: t - 8 * DAY, updatedAt: t - 3 * DAY, values: { lf_model: '1.56 非球绿膜', lf_sph: '-1.50', lf_cyl: '0', lf_stock: '3' } },
            { id: 'le_4', favorite: 0, createdAt: t - 8 * DAY, updatedAt: t - 3 * DAY, values: { lf_model: '1.56 非球绿膜', lf_sph: '-2.00', lf_cyl: '-0.75', lf_stock: '1' } }
          ]
        }
      ],
      reminders: [
        {
          id: 'r1', read: false, createdAt: t - 40 * 60000,
          message: '凯米 U6 1.74 库存为 0，建议补货'
        }
      ]
    };
  }

  /* ---------------- storage ---------------- */
  function load() {
    try {
      const raw = localStorage.getItem(LS_KEY);
      if (raw) {
        const data = JSON.parse(raw);
        if (data && Array.isArray(data.libraries)) return data;
      }
    } catch (e) { /* ignore */ }
    return seed();
  }
  let state = load();
  let bk = loadBk();
  ensureLibMeta();
  ensurePickFields();

  function ensureLibMeta() {
    state.libraries.forEach((l) => {
      if (!l.color || !AVATARS[l.color]) l.color = COLORS[0].value;
      if (!l.icon) l.icon = 'database';
    });
  }

  function ensurePickFields() {
    const lib = state.libraries.find((l) => l.id === 'lib_customer');
    const lens = state.libraries.find((l) => l.id === 'lib_lens');
    if (!lib || !lens) return;
    let changed = false;
    const set = (name, cfg) => {
      const f = lib.fields.find((x) => x.name.trim() === name);
      if (!f || f.pick) return;
      f.type = 'pick';
      f.pick = cfg;
      changed = true;
    };
    set('镜片', { libId: lens.id, kind: 'value', fieldId: 'lf_model', filterFieldId: '', deduct: false, qty: 1 });
    set('OD', { libId: lens.id, kind: 'sku', fieldId: '', filterFieldId: 'c_lens', deduct: true, qty: 1 });
    set('OS', { libId: lens.id, kind: 'sku', fieldId: '', filterFieldId: 'c_lens', deduct: true, qty: 1 });
    if (changed) save();
  }

  function save() {
    try { localStorage.setItem(LS_KEY, JSON.stringify(state)); } catch (e) { /* ignore */ }
    scheduleBk();
  }

  /* ---------------- 备份（本地 / WebDAV） ---------------- */

  function loadBk() {
    const def = { url: '', port: '', user: '', pass: '', path: 'Memento', auto: false, lastLocalAt: 0, lastCloudAt: 0 };
    try {
      const raw = localStorage.getItem(BK_KEY);
      if (raw) return Object.assign(def, JSON.parse(raw));
    } catch (e) { /* ignore */ }
    return def;
  }
  function saveBk() {
    try { localStorage.setItem(BK_KEY, JSON.stringify(bk)); } catch (e) { /* ignore */ }
  }
  function bkBase() {
    let base = String(bk.url || '').trim().replace(/\/+$/, '');
    if (!base) return '';
    if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(base)) base = 'https://' + base;
    const port = String(bk.port || '').trim().replace(/[^0-9]/g, '');
    if (!port) return base;
    try {
      const u = new URL(base);
      u.port = port;
      return u.toString().replace(/\/+$/, '');
    } catch (e) {
      return base;
    }
  }
  function bkUrl() {
    const base = bkBase();
    if (!base) return '';
    const dir = String(bk.path || '').trim().replace(/^\/+|\/+$/g, '');
    const parts = dir ? dir.split('/').filter(Boolean).map(encodeURIComponent) : [];
    return base + (parts.length ? '/' + parts.join('/') : '') + '/' + BK_FILE;
  }
  function bkDirUrl() {
    const u = bkUrl();
    return u ? u.replace(/\/[^/]+$/, '') : '';
  }
  function bkAuth() {
    if (!bk.user) return {};
    const raw = `${bk.user}:${bk.pass || ''}`;
    const bytes = new TextEncoder().encode(raw);
    let bin = '';
    bytes.forEach((b) => { bin += String.fromCharCode(b); });
    return { Authorization: 'Basic ' + btoa(bin) };
  }
  function bkPayload() {
    return { app: 'Memento', kind: 'backup', version: 1, exportedAt: new Date().toISOString(), data: state };
  }
  function bkText() {
    return JSON.stringify(bkPayload(), null, 2);
  }
  function bkStamp(ts) {
    if (!ts) return '还没有备份过';
    const d = new Date(ts);
    return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
  function bkSizeText() {
    const total = state.libraries.reduce((n, l) => n + l.entries.length, 0);
    return `${state.libraries.length} 个库 · ${total} 条记录`;
  }

  let bkTimer = null;
  function scheduleBk() {
    if (!bk.auto || !bk.url) return;
    clearTimeout(bkTimer);
    bkTimer = setTimeout(() => { bkPut(true); }, 4000);
  }

  async function bkPut(silent) {
    const url = bkUrl();
    if (!url) {
      if (!silent) toast('请先填写 WebDAV 地址');
      return false;
    }
    try {
      // 目录不存在时补建，失败也继续尝试直接上传
      await fetch(bkDirUrl(), { method: 'MKCOL', headers: bkAuth() });
    } catch (e) { /* ignore */ }
    try {
      const res = await fetch(url, {
        method: 'PUT',
        headers: { ...bkAuth(), 'Content-Type': 'application/json' },
        body: bkText()
      });
      if (!res.ok && res.status !== 201 && res.status !== 204 && res.status !== 200) {
        throw new Error('HTTP ' + res.status);
      }
      bk.lastCloudAt = now();
      saveBk();
      if (!silent) { toast('已备份到 WebDAV'); render(); }
      return true;
    } catch (e) {
      if (!silent) toast('云端备份失败：' + (e && e.message ? e.message : '网络错误'));
      return false;
    }
  }

  async function bkGet() {
    const url = bkUrl();
    if (!url) { toast('请先填写 WebDAV 地址'); return null; }
    try {
      const res = await fetch(url, { method: 'GET', headers: bkAuth() });
      if (res.status === 404) { toast('云端还没有备份文件'); return null; }
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const text = await res.text();
      return JSON.parse(text);
    } catch (e) {
      toast('云端恢复失败：' + (e && e.message ? e.message : '网络错误'));
      return null;
    }
  }

  function applyBackup(payload) {
    const data = payload && payload.data && Array.isArray(payload.data.libraries) ? payload.data : payload;
    if (!data || !Array.isArray(data.libraries)) throw new Error('invalid');
    takeSnap();
    state = { libraries: data.libraries, links: data.links || [], reminders: data.reminders || [] };
    ensureLibMeta();
    ensurePickFields();
    save();
    ui.stack = [];
    ui.route = { name: 'home' };
    ui.tab = 'home';
    render();
  }

  /* ---------------- 搜索历史（按库保存） ---------------- */
  const HIST_KEY = 'memento_searchhist';
  let searchHist = (() => {
    try {
      const raw = localStorage.getItem(HIST_KEY);
      const obj = raw ? JSON.parse(raw) : {};
      return obj && typeof obj === 'object' ? obj : {};
    } catch (e) { return {}; }
  })();

  const histOf = (libId) => (Array.isArray(searchHist[libId]) ? searchHist[libId] : []);

  function saveHist() {
    try { localStorage.setItem(HIST_KEY, JSON.stringify(searchHist)); } catch (e) { /* ignore */ }
  }

  function pushSearch(libId, raw) {
    const q = String(raw || '').trim();
    if (!q) return;
    const list = [q, ...histOf(libId).filter((x) => x !== q)].slice(0, 8);
    if (JSON.stringify(histOf(libId)) === JSON.stringify(list)) return;
    searchHist[libId] = list;
    saveHist();
  }

  function clearSearch(libId) {
    if (!searchHist[libId]) return;
    delete searchHist[libId];
    saveHist();
    render();
    toast('已清空搜索历史');
  }

  /* ---------------- 恢复前快照 ---------------- */
  const SNAP_KEY = 'memento_snapshot';

  function readSnap() {
    try {
      const raw = localStorage.getItem(SNAP_KEY);
      if (!raw) return null;
      const obj = JSON.parse(raw);
      return obj && obj.data && Array.isArray(obj.data.libraries) ? obj : null;
    } catch (e) { return null; }
  }

  const hasSnap = () => !!readSnap();

  function snapTime() {
    const s = readSnap();
    if (!s || !s.at) return '';
    const d = new Date(s.at);
    const p = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
  }

  function takeSnap() {
    try {
      localStorage.setItem(SNAP_KEY, JSON.stringify({ at: Date.now(), data: state }));
    } catch (e) { /* ignore */ }
  }

  function dropSnap() {
    try { localStorage.removeItem(SNAP_KEY); } catch (e) { /* ignore */ }
    render();
    toast('已丢弃快照');
  }

  function undoRestore() {
    const snap = readSnap();
    if (!snap) { toast('快照不可用'); return; }
    state = snap.data;
    try { localStorage.removeItem(SNAP_KEY); } catch (e) { /* ignore */ }
    ensureLibMeta();
    ensurePickFields();
    save();
    ui.stack = [];
    ui.route = { name: 'settings' };
    ui.tab = 'settings';
    render();
    toast('已撤销上次恢复');
  }

  /* ---------------- ui state ---------------- */
  const ui = {
    tab: 'home',
    route: { name: 'home' },
    stack: [],
    homeSearch: '',
    libSearch: '',
    filter: 'all',
    limit: 60,
    matrixModel: ''
  };

  const libById = (id) => state.libraries.find((l) => l.id === id);
  const fieldById = (lib, id) => lib && lib.fields.find((f) => f.id === id);
  const allEntries = () => state.libraries.flatMap((lib) => lib.entries.map((entry) => ({ lib, entry })));

  function primaryField(lib) {
    return lib.fields.find((f) => f.type === 'text') || lib.fields[0];
  }
  function titleOf(lib, entry) {
    const f = primaryField(lib);
    return (f && entry.values[f.id]) || '未命名条目';
  }
  function oneLine(v) {
    return String(v ?? '').replace(/\s*\r?\n\s*/g, ' / ');
  }

  function subtitleOf(lib, entry) {
    const first = primaryField(lib);
    return lib.fields
      .filter((f) => f !== first && entry.values[f.id])
      .slice(0, 2)
      .map((f) => {
        if (f.type === 'autonumber') return `${f.name} ${(f.prefix || '') + oneLine(entry.values[f.id])}`;
        if (f.type === 'createdat' || f.type === 'updatedat') return `${f.name} ${oneLine(entry.values[f.id])}`;
        return `${f.name} ${oneLine(entry.values[f.id])}${f.unit || ''}`;
      })
      .join(' · ');
  }
  /* ---------------- 列表显示字段 ---------------- */
  function listFieldsOf(lib) {
    const ids = Array.isArray(lib.listFields) ? lib.listFields : [];
    const picked = ids.map((id) => fieldById(lib, id)).filter(Boolean);
    if (picked.length) return picked;
    const pf = primaryField(lib);
    const rest = lib.fields.filter((f) =>
      f !== pf && !['autonumber', 'createdat', 'updatedat', 'formula'].includes(f.type));
    return [pf, ...rest.slice(0, 2)].filter(Boolean);
  }

  function cellTextOf(lib, entry, f) {
    if (!f) return '';
    const raw = f.type === 'formula' ? formulaValue(lib, entry, f) : entry.values[f.id];
    const s = oneLine(raw);
    if (!s) return '';
    if (f.type === 'autonumber') return (f.prefix || '') + s;
    return s + (f.unit || '');
  }

  function listSubHTML(lib, entry, fields) {
    if (!fields.length) return '<span class="dim">暂无更多字段</span>';
    const showNames = lib.listShowNames !== false;
    const cells = fields.map((f) => {
      const v = cellTextOf(lib, entry, f);
      return `<span class="list-cell${showNames ? '' : ' plain'}">${showNames ? `<i>${esc(f.name)}</i>` : ''}${v ? esc(v) : '<em>—</em>'}</span>`;
    }).join('');
    return `<span class="list-line">${cells}</span>`;
  }

  function initialOf(name) {
    const s = String(name || '?').trim();
    return s ? s.slice(0, 1).toUpperCase() : '?';
  }
  function avatarHTML(lib, name, extraClass = '', iconName = '') {
    const p = AVATARS[lib.color] || AVATARS['#1976D2'];
    return `<span class="avatar ${extraClass}" style="background:linear-gradient(145deg,${p[0]},${p[1]})">${iconName ? icon(iconName, 22) : esc(initialOf(name))}</span>`;
  }
  function soft(hex) { return hex + '1f'; }

  function relTime(ts) {
    const diff = now() - ts;
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前';
    if (diff < DAY) return Math.floor(diff / 3600000) + ' 小时前';
    if (diff < 2 * DAY) return '昨天';
    if (diff < 30 * DAY) return Math.floor(diff / DAY) + ' 天前';
    return new Date(ts).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' });
  }
  function dateStr() {
    return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date());
  }

  function entryMatches(lib, entry, q) {
    if (!q) return true;
    const hay = [titleOf(lib, entry), ...Object.values(entry.values), lib.name].join(' ').toLowerCase();
    return hay.includes(q);
  }

  function nameFieldOf(lib) {
    return lib.fields.find((f) => f.name.includes('姓名')) || primaryField(lib);
  }
  function phoneFieldOf(lib) {
    return lib.fields.find((f) => f.name.includes('电话') || f.type === 'tel');
  }
  function normPhone(v) {
    return String(v ?? '').replace(/\D/g, '');
  }
  function nameOf(lib, entry) {
    const f = nameFieldOf(lib);
    return (f && String(entry.values[f.id] ?? '').trim()) || '';
  }

  // 同一个人的其它记录：同名（优先），没有姓名字段时退回同电话
  function samePersonEntries(lib, entry) {
    const nf = nameFieldOf(lib);
    if (!nf) return [];
    const name = nameOf(lib, entry);
    if (!name) return [];
    return lib.entries.filter((e) => e.id !== entry.id && nameOf(lib, e) === name);
  }

  // 家庭组：同电话、不同名
  function familyGroup(lib, entry) {
    const pf = phoneFieldOf(lib);
    const nf = nameFieldOf(lib);
    if (!pf || !nf) return null;
    const phone = normPhone(entry.values[pf.id]);
    if (!phone) return null;
    const myName = nameOf(lib, entry);
    const byName = new Map();
    let total = 0;
    let visits = 0;
    lib.entries.forEach((e) => {
      if (normPhone(e.values[pf.id]) !== phone) return;
      visits += 1;
      const tf = lib.fields.find((f) => /合计|金额|价格/.test(f.name));
      const money = tf ? parseFloat(String(e.values[tf.id] ?? '').replace(/[^\d.\-]/g, '')) : NaN;
      if (isFinite(money)) total += money;
      const n = nameOf(lib, e);
      if (!n || n === myName) return;
      const ts = entryDate(lib, e);
      const prev = byName.get(n);
      if (!prev || ts > prev.ts) byName.set(n, { name: n, entry: e, ts });
    });
    const members = [...byName.values()].sort((a, b) => b.ts - a.ts);
    if (!members.length) return null;
    return { phone, display: oneLine(entry.values[pf.id]), members, total: round2(total), visits, people: members.length + 1 };
  }

  function relatedFor(lib, entry) {
    const nameField = nameFieldOf(lib);
    const phoneField = phoneFieldOf(lib);
    const name = nameField && entry.values[nameField.id];
    const phone = phoneField && normPhone(entry.values[phoneField.id]);
    if (!name && !phone) return [];
    return allEntries().filter(({ lib: l, entry: e }) => {
      if (l.id === lib.id && e.id === entry.id) return false;
      const nf = nameFieldOf(l);
      const pf = phoneFieldOf(l);
      const n = nf && e.values[nf.id];
      const p = pf && normPhone(e.values[pf.id]);
      if (name && n && name === n) return true;
      if (phone && p && phone === p) {
        // 同电话但不同名，交给家庭组处理，避免混进验光历史
        const n2 = String(n ?? '').trim();
        const n1 = String(name ?? '').trim();
        return !n2 || !n1 || n2 === n1;
      }
      return false;
    });
  }

  /* ---------------- 验光历史 ---------------- */
  function dateFieldOf(lib) {
    return lib.fields.find((f) => /^(时间|日期|验光日期|配镜日期)$/.test(f.name.trim()))
      || lib.fields.find((f) => /时间|日期/.test(f.name));
  }

  function hasTimeline(lib) {
    // 只有带日期字段的库才有「一次一次」的时间线；像镜片库存这种规格库不需要
    return !!dateFieldOf(lib);
  }

  function parseDateVal(v) {
    const s = String(v ?? '').trim();
    if (!s) return null;
    const m = s.match(/^(\d{4})\s*[-\/年.]\s*(\d{1,2})\s*[-\/月.]\s*(\d{1,2})/);
    if (m) {
      const d = new Date(+m[1], +m[2] - 1, +m[3]);
      return isNaN(d.getTime()) ? null : d.getTime();
    }
    const t = Date.parse(s.replace(/\//g, '-'));
    return isFinite(t) ? t : null;
  }

  function entryDate(lib, entry) {
    return visitDate(lib, entry) || entry.updatedAt;
  }

  function visitDate(lib, entry) {
    const f = dateFieldOf(lib);
    return (f && parseDateVal(entry.values[f.id])) || null;
  }

  function entryStamp(lib, entry) {
    const ts = visitDate(lib, entry);
    if (!ts) return relTime(entry.updatedAt);
    const d = new Date(ts);
    return d.getFullYear() === new Date().getFullYear()
      ? `${d.getMonth() + 1}/${d.getDate()}`
      : fmtDate(ts);
  }

  function latestStamp(lib) {
    if (!lib.entries.length) return '暂无记录';
    if (dateFieldOf(lib)) {
      const ts = Math.max(...lib.entries.map((e) => visitDate(lib, e) || 0));
      if (ts) return fmtDate(ts);
    }
    return relTime(Math.max(...lib.entries.map((e) => e.updatedAt)));
  }

  function fmtDate(ts) {
    const d = new Date(ts);
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`;
  }

  function yearOf(ts) {
    return new Date(ts).getFullYear();
  }

  function historySignature(lib, entry) {
    const parts = [/^OD$/i, /^OS$/i, /合计|金额|价格/, /^NO\.?$/i].map((re) => {
      const f = lib.fields.find((x) => re.test(x.name));
      return f ? oneLine(entry.values[f.id] || '').toLowerCase() : '';
    });
    return [entryDate(lib, entry), ...parts].join('\u0001');
  }

  function dedupeHistory(lib, items) {
    const map = new Map();
    const out = [];
    items.forEach((item) => {
      const sig = historySignature(lib, item.entry);
      const hit = map.get(sig);
      if (hit) {
        hit.dup += 1;
        if (item.current) hit.current = true;
        return;
      }
      const copy = { entry: item.entry, current: !!item.current, dup: 1 };
      map.set(sig, copy);
      out.push(copy);
    });
    return out;
  }

  function sphereOf(v) {
    const m = String(v || '').match(/[+-]?\d+(?:\.\d+)?/);
    return m ? parseFloat(m[0]) : null;
  }

  function deltaBadges(lib, cur, prev) {
    const odF = lib.fields.find((f) => /^OD$/i.test(f.name));
    const osF = lib.fields.find((f) => /^OS$/i.test(f.name));
    const out = [];
    [['OD', odF], ['OS', osF]].forEach(([label, f]) => {
      if (!f) return;
      const a = sphereOf(prev.values[f.id]);
      const b = sphereOf(cur.values[f.id]);
      if (a === null || b === null) return;
      const diff = round2(b - a);
      if (diff === 0) return;
      // 负值 = 更近视（度数加深）
      const deeper = diff < 0;
      out.push(`<span class="tl-delta ${deeper ? 'deeper' : 'better'}">${label} ${deeper ? '加深' : '回退'} ${Math.abs(diff).toFixed(2)}D</span>`);
    });
    return out.join('');
  }

  function historyHTML(lib, items) {
    return `
      <div class="timeline">
        ${items.map(({ entry: e, current, dup }, i) => {
          const dateField = dateFieldOf(lib);
          const raw = dateField && e.values[dateField.id];
          const ts = entryDate(lib, e);
          const use = lib.fields.find((f) => /用途/.test(f.name));
          const od = lib.fields.find((f) => /^OD$/i.test(f.name));
          const os = lib.fields.find((f) => /^OS$/i.test(f.name));
          const total = lib.fields.find((f) => /合计|金额|价格/.test(f.name));
          const no = lib.fields.find((f) => /^NO\.?$/i.test(f.name));
          const lens = lib.fields.find((f) => /镜片/.test(f.name));
          const frame = lib.fields.find((f) => /镜框|镜架/.test(f.name));
          const val = (f) => (f && e.values[f.id] ? oneLine(e.values[f.id]) : '');
          const tag = current ? 'div' : 'button';
          const attrs = current
            ? 'class="tl-item current"'
            : `class="tl-item" data-act="open-entry" data-lib="${lib.id}" data-id="${e.id}"`;
          const prev = items[i + 1];
          const badges = prev ? deltaBadges(lib, e, prev.entry) : '';
          return `
            <${tag} ${attrs}>
              <div class="tl-top">
                <span class="tl-date">${esc(raw ? oneLine(raw) : fmtDate(ts))}</span>
                ${current ? '<span class="tl-now">当前</span>' : ''}
                ${dup > 1 ? `<span class="tl-dup">同记录 ×${dup}</span>` : ''}
                ${val(use) ? `<span class="tl-use">${esc(val(use))}</span>` : ''}
                ${!current ? `<span class="tl-more">${icon('chevron', 15)}</span>` : ''}
              </div>
              <div class="tl-main">
                ${val(od) ? `<span><i>OD</i><b>${esc(val(od))}</b></span>` : ''}
                ${val(os) ? `<span><i>OS</i><b>${esc(val(os))}</b></span>` : ''}
                ${!val(od) && !val(os) ? `<span>${esc(subtitleOf(lib, e) || '无度数记录')}</span>` : ''}
              </div>
              <div class="tl-meta">
                ${val(total) ? `<span>合计 <b class="money">¥${esc(val(total))}</b></span>` : ''}
                ${val(lens) ? `<span>${esc(val(lens))}</span>` : ''}
                ${val(frame) ? `<span>${esc(val(frame))}</span>` : ''}
                ${val(no) ? `<span>NO. ${esc(val(no))}</span>` : ''}
              </div>
              ${badges ? `<div class="tl-deltas">${badges}${prev ? `<span class="tl-vs">相比上一次</span>` : ''}</div>` : ''}
            </${tag}>
          `;
        }).join('')}
      </div>
    `;
  }

  /* ---------------- rendering ---------------- */
  function render() {
    const view = $('#view');
    const active = document.activeElement;
    const focusId = active && active.id ? active.id : null;
    const selStart = active && 'selectionStart' in active ? active.selectionStart : null;
    const selEnd = active && 'selectionEnd' in active ? active.selectionEnd : null;
    const scroll = view.scrollTop;

    let html = '';
    switch (ui.route.name) {
      case 'lib': html = libView(); break;
      case 'restock': html = restockView(); break;
      case 'entry': html = entryView(); break;
      case 'settings': html = settingsView(); break;
      default: html = homeView();
    }
    view.innerHTML = html;
    view.scrollTop = scroll;
    updateChrome();

    if (focusId) {
      const el = document.getElementById(focusId);
      if (el && typeof el.focus === 'function') {
        el.focus();
        try { if (selStart !== null) el.setSelectionRange(selStart, selEnd); } catch (e) { /* ignore */ }
      }
    }
  }

  function updateChrome() {
    const tabs = $('#tabbar');
    const isTab = ui.route.name === 'home' || ui.route.name === 'settings';
    tabs.style.display = isTab ? 'flex' : 'none';
    tabs.innerHTML = `
      <button class="tab ${ui.tab === 'home' ? 'active' : ''}" data-act="tab" data-tab="home">${icon('home', 22)}<span>首页</span></button>
      <button class="tab ${ui.tab === 'settings' ? 'active' : ''}" data-act="tab" data-tab="settings">${icon('settings', 22)}<span>我的</span></button>
    `;

    const fab = $('#fab');
    let act = '';
    let label = '';
    if (ui.route.name === 'home') { act = 'add-lib'; label = '新建库'; }
    else if (ui.route.name === 'lib') {
      const lb = libById(ui.route.id);
      if (!(lb && lb.view === 'matrix')) { act = 'add-entry'; label = '新增条目'; }
    }
    fab.classList.toggle('hidden', !act);
    fab.dataset.act = act;
    fab.innerHTML = act ? `${icon('plus', 24, 2.2)}<span class="fab-label">${label}</span>` : '';
  }

  /* ---------------- home ---------------- */
  function homeView() {
    const q = ui.homeSearch.trim().toLowerCase();
    const libs = q
      ? state.libraries.filter((l) => l.name.toLowerCase().includes(q) || l.fields.some((f) => f.name.toLowerCase().includes(q)))
      : state.libraries;
    const recent = allEntries()
      .filter((x) => entryMatches(x.lib, x.entry, q))
      .sort((a, b) => b.entry.updatedAt - a.entry.updatedAt)
      .slice(0, 5);
    const total = state.libraries.reduce((n, l) => n + l.entries.length, 0);

    return `
      <div class="screen">
        <section class="home-hero">
          <div class="hero-row">
            <div>
              <div class="hero-kicker">MYDATABASE</div>
              <div class="hero-title">我的数据库</div>
              <div class="hero-date">${dateStr()}</div>
            </div>
            <div class="hero-avatar">M</div>
          </div>
          <div class="search">
            ${icon('search', 17)}
            <input id="homeSearch" placeholder="搜索库、条目或字段" value="${esc(ui.homeSearch)}" autocomplete="off" />
          </div>
        </section>

        <div class="home-body">
          ${lowStockHTML()}

          ${!q ? `
          <div class="stats">
            <div class="stat"><b>${state.libraries.length}</b><span>数据库</span></div>
            <div class="stat"><b>${total}</b><span>条记录</span></div>
            <div class="stat"><b>${state.libraries.reduce((n, l) => n + l.fields.length, 0)}</b><span>字段</span></div>
          </div>` : ''}

          <div class="section-head">
            <h2>${q ? '匹配的数据库' : '我的数据库'}</h2>
            <button data-act="add-lib">+ 新建</button>
          </div>
          <div class="lib-grid">
            ${libs.map(libCardHTML).join('')}
            ${!q ? addLibCardHTML() : ''}
          </div>

          <div class="section-head">
            <h2>${q ? '匹配的条目' : '最近更新'}</h2>
            <span style="font-size:11px;color:var(--muted-2)">${recent.length} 条</span>
          </div>
          <div class="recent">
            ${recent.length ? recent.map(recentRowHTML).join('') : emptyHTML('search', '没有找到结果', '换个关键词试试，或者新建一个数据库')}
          </div>
        </div>
      </div>
    `;
  }

  function libCardHTML(lib) {
    const last = lib.entries.length ? Math.max(...lib.entries.map((e) => e.updatedAt)) : null;
    const p = AVATARS[lib.color] || AVATARS['#1976D2'];
    return `
      <div class="lib-card" role="button" tabindex="0" data-act="open-lib" data-id="${lib.id}">
        <span class="tile" style="background:${soft(lib.color)};color:${lib.color}">${icon(lib.icon, 20)}</span>
        <span class="count">${lib.entries.length}</span>
        <h3>${esc(lib.name)}</h3>
        <p>${lib.fields.length} 个字段${last ? ' · ' + relTime(last) : ''}</p>
      </div>
    `;
  }

  function addLibCardHTML() {
    return `
      <div class="lib-card add" role="button" tabindex="0" data-act="add-lib">
        <span class="plus-circle">${icon('plus', 18)}</span>
        <span>新建数据库</span>
      </div>
    `;
  }

  function recentRowHTML({ lib, entry }) {
    const title = titleOf(lib, entry);
    return `
      <button class="recent-row" data-act="open-entry" data-lib="${lib.id}" data-id="${entry.id}">
        ${avatarHTML(lib, title)}
        <span class="recent-main">
          <b>${esc(title)}</b>
          <span>${esc(lib.name)}${subtitleOf(lib, entry) ? ' · ' + esc(subtitleOf(lib, entry)) : ''}</span>
        </span>
        <span class="recent-time">${relTime(entry.updatedAt)}</span>
      </button>
    `;
  }

  function emptyHTML(ico, title, desc) {
    return `
      <div class="empty">
        <div class="empty-ico">${icon(ico, 26)}</div>
        <b>${esc(title)}</b>
        <p>${esc(desc)}</p>
      </div>
    `;
  }

  /* ---------------- library ---------------- */
  function libView() {
    const lib = libById(ui.route.id);
    if (!lib) return emptyHTML('database', '数据库不存在', '它可能已经被删除');
    const q = ui.libSearch.trim().toLowerCase();

    // 电话 -> 该号码下的不同姓名，用来标记 / 筛选家庭组
    const pf = phoneFieldOf(lib);
    const nf = nameFieldOf(lib);
    const phoneNames = new Map();
    if (pf && nf) {
      lib.entries.forEach((e) => {
        const pn = normPhone(e.values[pf.id]);
        if (!pn) return;
        const set = phoneNames.get(pn) || new Set();
        const n = nameOf(lib, e);
        if (n) set.add(n);
        phoneNames.set(pn, set);
      });
    }
    const isFamily = (e) => {
      if (!pf || !nf) return false;
      const set = phoneNames.get(normPhone(e.values[pf.id]));
      return !!set && set.size > 1;
    };
    const familyCount = lib.entries.filter(isFamily).length;

    let entries = lib.entries.filter((e) => entryMatches(lib, e, q));
    if (ui.filter === 'fav') entries = entries.filter((e) => e.favorite);
    if (ui.filter === 'family') entries = entries.filter(isFamily);
    const dated = !!dateFieldOf(lib);
    entries = entries.slice().sort((a, b) => {
      if (dated) {
        const d = (visitDate(lib, b) || 0) - (visitDate(lib, a) || 0);
        if (d !== 0) return d;
      }
      return b.updatedAt - a.updatedAt;
    });
    const mx = matrixConfig(lib);
    if (mx) {
      const order = new Map();
      lib.entries.forEach((e) => {
        const n = String(e.values[mx.group.id] || '').trim();
        if (n && !order.has(n)) order.set(n, order.size);
      });
      entries = entries.slice().sort((a, b) => {
        const ma = String(a.values[mx.group.id] || '').trim();
        const mb = String(b.values[mx.group.id] || '').trim();
        if (ma !== mb) return (order.get(ma) ?? 999) - (order.get(mb) ?? 999);
        const sa = toPower(a.values[mx.row.id]);
        const sb = toPower(b.values[mx.row.id]);
        if (sa !== sb) return Math.abs(sa) - Math.abs(sb) || sa - sb;
        const ca = toPower(a.values[mx.col.id]);
        const cb = toPower(b.values[mx.col.id]);
        return Math.abs(ca) - Math.abs(cb) || ca - cb;
      });
    }
    const total = entries.length;
    const visible = entries.slice(0, ui.limit);
    const p = AVATARS[lib.color] || AVATARS['#1976D2'];
    const mode = mx && lib.view === 'matrix' ? 'matrix' : 'list';
    const segHTML = mx ? `
        <div class="seg-wrap">
          <div class="seg">
            <button class="seg-btn ${mode === 'list' ? 'active' : ''}" data-act="lib-view" data-view="list">${icon('list', 13)} 列表</button>
            <button class="seg-btn ${mode === 'matrix' ? 'active' : ''}" data-act="lib-view" data-view="matrix">${icon('grid', 13)} 表格</button>
          </div>
        </div>` : '';
    const toolsHTML = `
        <div class="chips">
          ${mode === 'list' ? `
          <button class="chip ${ui.filter === 'all' ? 'active' : ''}" data-act="filter" data-filter="all">全部 ${lib.entries.length}</button>
          <button class="chip ${ui.filter === 'fav' ? 'active' : ''}" data-act="filter" data-filter="fav">收藏 ${lib.entries.filter((e) => e.favorite).length}</button>
          ${familyCount ? `<button class="chip ${ui.filter === 'family' ? 'active' : ''}" data-act="filter" data-filter="family">${icon('users', 11)} 家庭 ${familyCount}</button>` : ''}
          ` : ''}
          ${mode === 'matrix' ? `<button class="chip" data-act="add-entry">${icon('plus', 11)} 新增规格</button>` : ''}
          ${mode === 'matrix' ? `<button class="chip" data-act="alerts">${icon('bell', 11)} 提醒${lowStockOf(lib).length ? ' ' + lowStockOf(lib).length : ''}</button>` : ''}
          ${mx ? `<button class="chip" data-act="restock">${icon('box', 11)} 补货清单</button>` : ''}
          ${mx ? `<button class="chip" data-act="batch">${icon('sliders', 11)} 批量</button>` : ''}
          ${batchUndo && batchUndo.libId === lib.id ? `<button class="chip undo" data-act="batch-undo">${icon('refresh', 11)} 撤销</button>` : ''}
          <button class="chip" data-act="fields">${icon('layers', 11)} 字段 ${lib.fields.length}</button>
          <button class="chip" data-act="list-fields">${icon('sliders', 11)} 显示 ${listFieldsOf(lib).length}</button>
          <button class="chip ghost" data-act="library-data">${icon('database', 12)} CSV 导入/导出</button>
        </div>`;

    return `
      <div class="screen">
        <div class="navbar">
          <button class="nav-btn" data-act="back" aria-label="返回">${icon('arrowLeft', 20)}</button>
          <div class="nav-title"><b>${esc(lib.name)}</b><span>${lib.entries.length} 条记录 · ${lib.fields.length} 个字段</span></div>
          <div class="nav-actions">
            <button class="nav-btn" data-act="edit-lib" aria-label="设置">${icon('more', 20)}</button>
          </div>
        </div>
        ${segHTML}

        ${mode === 'matrix' ? `
        ${toolsHTML}
        ${matrixView(lib)}
        ` : `
        <div class="lib-hero" style="background:linear-gradient(145deg,${p[1]},${p[0]})">
          <div class="lib-hero-top">
            <span class="tile">${icon(lib.icon, 22)}</span>
            <div>
              <h2>${esc(lib.name)}</h2>
              <p>${lib.entries.length} 条记录，最近一次 ${latestStamp(lib)}</p>
            </div>
          </div>
          <div class="hero-meta">
            <span class="pill">${icon('layers', 13)} ${lib.fields.length} 个字段</span>
            <span class="pill">${icon('star', 13)} ${lib.entries.filter((e) => e.favorite).length} 个收藏</span>
          </div>
        </div>

        <div class="list-toolbar">
          <div class="search light">
            ${icon('search', 17)}
            <input id="libSearch" placeholder="在「${esc(lib.name)}」中搜索" value="${esc(ui.libSearch)}" autocomplete="off" />
          </div>
        </div>
        ${!ui.libSearch && histOf(lib.id).length ? `
        <div class="search-history">
          <span>搜索历史</span>
          <button data-act="search-clear" data-id="${lib.id}">清空</button>
        </div>
        <div class="chips sh-chips">
          ${histOf(lib.id).map((s) => `<button class="chip" data-act="search-use" data-id="${lib.id}" data-q="${esc(s)}">${esc(s)}</button>`).join('')}
        </div>` : ''}
        ${toolsHTML}

        <div class="entry-list" style="--list-fs:${listFontPx(lib)}px">
          ${visible.length ? visible.map((e) => entryRowHTML(lib, e, isFamily(e))).join('') : emptyHTML('database', '还没有记录', '点右下角的按钮添加第一条记录')}
        </div>
        ${total > visible.length ? `<button class="load-more" data-act="load-more">显示更多（还有 ${total - visible.length} 条）</button>` : ''}
        ${total ? `<div style="text-align:center;color:var(--muted-2);font-size:10.5px;padding:14px 0 4px">共 ${total} 条记录</div>` : ''}
        `}
      </div>
    `;
  }

  function entryRowHTML(lib, entry, family = false, fields = null) {
    const lf = fields || listFieldsOf(lib);
    const head = lf[0] || primaryField(lib);
    const mcfg = matrixConfig(lib);
    let title = (head && cellTextOf(lib, entry, head)) || titleOf(lib, entry);
    if (mcfg && head === mcfg.group) {
      const sph = toPower(entry.values[mcfg.row.id]);
      const cyl = toPower(entry.values[mcfg.col.id]);
      title += ` ${fmtPower(sph)}${cyl ? ' / ' + fmtPower(cyl) : ''}`;
    }
    const sub = listSubHTML(lib, entry, lf.slice(1));
    return `
      <button class="entry-row" data-act="open-entry" data-lib="${lib.id}" data-id="${entry.id}">
        <span class="entry-main">
          <b>${esc(title)}${family ? '<span class="fam-badge">家庭</span>' : ''}${entry.favorite ? `<span class="star">${icon('star', 13)}</span>` : ''}</b>
          ${sub}
        </span>
        <span class="recent-time">${entryStamp(lib, entry)}</span>
        ${icon('chevron', 17)}
      </button>
    `;
  }

  /* ---------------- 矩阵（表格）视图 ---------------- */
  function toPower(v) {
    const n = parseFloat(String(v ?? '').replace(/[^0-9.+-]/g, ''));
    return Number.isFinite(n) ? Math.round(n * 100) / 100 : null;
  }
  function fmtPower(n) {
    if (n === null || n === undefined || !Number.isFinite(n)) return '—';
    if (n === 0) return '0';
    return (n > 0 ? '+' : '') + n.toFixed(2);
  }
  // 验光书写习惯：球镜加 ds，柱镜加 dc
  function powerText(sph, cyl) {
    if (sph === null || sph === undefined || !Number.isFinite(sph)) return '';
    return fmtPower(sph) + 'ds' + (cyl ? '/' + fmtPower(cyl) + 'dc' : '');
  }
  // 比较时忽略 ds/dc 后缀、空格和全角符号
  function normPower(v) {
    return String(v ?? '')
      .replace(/[−–—]/g, '-')
      .replace(/\s+/g, '')
      .replace(/[^0-9+\-./]/g, '');
  }
  function powerSubtitle(entry, cfg) {
    const sph = toPower(entry.values[cfg.row.id]);
    const cyl = cylValueOf(cfg, entry);
    const parts = [];
    if (sph !== null) parts.push('球镜 ' + fmtPower(sph));
    parts.push(cyl ? '柱镜 ' + fmtPower(cyl) : '单光');
    return parts.join(' · ');
  }
  function powerAxis(values, isCyl = false) {
    const arr = values.slice().sort((a, b) => Math.abs(a) - Math.abs(b) || a - b);
    if (!arr.length) return [];
    const step = 0.25;
    const out = [];
    if (isCyl && arr.includes(0)) {
      out.push(0);
      const rest = arr.filter((v) => v !== 0);
      if (!rest.length) return out;
      const maxMag = Math.max(...rest.map((v) => Math.abs(v)));
      for (let m = 0.5; m <= maxMag + 1e-9; m += step) out.push(Math.round(-m * 100) / 100);
      return out;
    }
    const min = Math.min(...arr);
    const max = Math.max(...arr);
    const start = arr[0];
    const dir = start <= 0 ? -1 : 1;
    for (let v = start; dir < 0 ? v >= min - 1e-9 : v <= max + 1e-9; v += dir * step) {
      out.push(Math.round(v * 100) / 100);
    }
    return out;
  }
  function matrixConfig(lib) {
    const m = lib && lib.matrix;
    if (!m) return null;
    const group = fieldById(lib, m.group);
    const row = fieldById(lib, m.row);
    const col = fieldById(lib, m.col);
    const value = fieldById(lib, m.value);
    if (!group || !row || !col || !value) return null;
    return { group, row, col, value };
  }
  function cylValueOf(cfg, entry) {
    const raw = entry.values[cfg.col.id];
    if (raw === undefined || raw === null || String(raw).trim() === '') return 0;
    return toPower(raw);
  }
  function matrixData(lib) {
    const cfg = matrixConfig(lib);
    if (!cfg) return null;
    const models = new Map();
    lib.entries.forEach((e) => {
      const model = String(e.values[cfg.group.id] || '').trim();
      const sph = toPower(e.values[cfg.row.id]);
      const cyl = cylValueOf(cfg, e);
      if (!model || sph === null || cyl === null) return;
      let g = models.get(model);
      if (!g) { g = { sphs: new Set(), cyls: new Set(), cells: new Map(), filled: new Set() }; models.set(model, g); }
      g.sphs.add(sph);
      g.cyls.add(cyl);
      const k = sph + '|' + cyl;
      const rawStock = e.values[cfg.value.id];
      const hasStock = rawStock !== undefined && rawStock !== null && String(rawStock).trim() !== '';
      g.cells.set(k, (g.cells.get(k) || 0) + (Number(rawStock) || 0));
      if (hasStock) g.filled.add(k);
    });
    return { cfg, models };
  }
  function findSku(lib, model, sph, cyl) {
    const cfg = matrixConfig(lib);
    if (!cfg) return null;
    return lib.entries.find((e) =>
      String(e.values[cfg.group.id] || '').trim() === model &&
      toPower(e.values[cfg.row.id]) === sph &&
      cylValueOf(cfg, e) === cyl) || null;
  }
  function mxSave(lib, model, sph, cyl, qty) {
    const cfg = matrixConfig(lib);
    if (!cfg) return;
    const raw = String(qty ?? '').trim();
    const blank = raw === '';
    let hit = findSku(lib, model, sph, cyl);
    if (!hit && blank) {
      toast('留空 = 没有这个镜片，不写入、不提醒');
      return;
    }
    if (!hit) {
      hit = { id: uid('e_'), favorite: 0, createdAt: now(), updatedAt: now(), values: {} };
      hit.values[cfg.group.id] = model;
      hit.values[cfg.row.id] = fmtPower(sph);
      hit.values[cfg.col.id] = fmtPower(cyl);
      lib.entries.push(hit);
    }
    hit.values[cfg.value.id] = blank ? '' : String(Math.max(0, Math.round(Number(raw) || 0)));
    hit.updatedAt = now();
    save();
    render();
    const pos = `${model} · ${fmtPower(sph)}${cyl ? ' / ' + fmtPower(cyl) : ''}`;
    toast(blank ? `${pos} → 未入库（不提醒）` : `${pos} → ${hit.values[cfg.value.id]} ${cfg.value.unit || '副'}`);
  }
  /* ---------------- 库存提醒 ---------------- */
  function alertsOf(lib) {
    const cfg = matrixConfig(lib);
    if (!cfg || !lib.alerts) return null;
    const field = fieldById(lib, lib.alerts.field) || cfg.value;
    const def = Math.max(0, Math.round(Number(lib.alerts.defaultThreshold) || 0));
    return { cfg, field, def, rules: Array.isArray(lib.alerts.rules) ? lib.alerts.rules : [] };
  }
  function ruleMatches(rule, model, sph, cyl) {
    if (rule.model && rule.model !== model) return false;
    const lo = toPower(rule.sphMin);
    const hi = toPower(rule.sphMax);
    if (lo !== null && hi !== null) {
      const a = Math.min(lo, hi);
      const b = Math.max(lo, hi);
      if (sph < a - 1e-9 || sph > b + 1e-9) return false;
    } else if (lo !== null && sph < lo - 1e-9) {
      return false;
    } else if (hi !== null && sph > hi + 1e-9) {
      return false;
    }
    if (rule.cyl === 'none' && cyl !== 0) return false;
    if (rule.cyl === 'has' && cyl === 0) return false;
    return true;
  }
  function thresholdFor(al, model, sph, cyl) {
    if (!al) return null;
    const rule = al.rules.find((r) => ruleMatches(r, model, sph, cyl));
    return rule ? Math.max(0, Math.round(Number(rule.threshold) || 0)) : al.def;
  }
  function lowStockOf(lib) {
    const al = alertsOf(lib);
    if (!al) return [];
    const out = [];
    lib.entries.forEach((e) => {
      const model = String(e.values[al.cfg.group.id] || '').trim();
      const sph = toPower(e.values[al.cfg.row.id]);
      const cyl = cylValueOf(al.cfg, e);
      if (!model || sph === null || cyl === null) return;
      const raw = e.values[al.field.id];
      if (raw === undefined || raw === null || String(raw).trim() === '') return;
      const qty = Number(raw) || 0;
      const th = thresholdFor(al, model, sph, cyl);
      if (qty <= th) out.push({ entry: e, model, sph, cyl, qty, th });
    });
    return out.sort((a, b) =>
      a.qty - b.qty ||
      a.model.localeCompare(b.model, 'zh-Hans-CN') ||
      Math.abs(a.sph) - Math.abs(b.sph));
  }
  function lowStockHTML() {
    const groups = state.libraries
      .map((lib) => ({ lib, items: lowStockOf(lib) }))
      .filter((g) => g.items.length);
    if (!groups.length) return '';
    const total = groups.reduce((n, g) => n + g.items.length, 0);
    const zero = groups.reduce((n, g) => n + g.items.filter((i) => i.qty <= 0).length, 0);
    const first = groups[0];
    const shown = first.items.slice(0, 3)
      .map((i) => `${i.model} ${fmtPower(i.sph)}${i.cyl ? '/' + fmtPower(i.cyl) : ''}`)
      .join('、');
    return `
      <button class="alert-card" data-act="open-lib" data-id="${first.lib.id}" data-model="${esc(first.items[0].model)}">
        <span class="alert-ico">${icon('bell', 17)}</span>
        <span class="alert-copy">
          <b>${total} 个规格库存偏低${zero ? ` · ${zero} 个已缺货` : ''}</b>
          <p>${esc(shown)}${first.items.length > 3 ? ' 等' : ''}</p>
        </span>
        ${icon('chevron', 16)}
      </button>
    `;
  }
  /* ---------------- 补货清单 ---------------- */
  const rsQty = { plain: 4, cyl: 2 };

  // 按型号分组，单光用 plainQty，带散光用 cylQty
  function restockText(lib, plainQty, cylQty) {
    const groups = new Map();
    lowStockOf(lib).forEach((i) => {
      if (!groups.has(i.model)) groups.set(i.model, []);
      groups.get(i.model).push(i);
    });
    const lines = [];
    groups.forEach((list, model) => {
      if (lines.length) lines.push('');
      lines.push(model);
      list.slice()
        .sort((a, b) => a.sph - b.sph || a.cyl - b.cyl)
        .forEach((i) => {
          let s = `${fmtPower(i.sph)}ds`;
          if (i.cyl) s += ` / ${fmtPower(i.cyl)}dc`;
          s += `，${i.cyl ? cylQty : plainQty}`;
          lines.push(s);
        });
    });
    return lines.join('\r\n');
  }

  function restockUnit(lib) {
    const al = alertsOf(lib);
    return (al && al.field && al.field.unit) || '片';
  }

  function restockView() {
    const lib = libById(ui.route.id);
    if (!lib) return emptyHTML('database', '数据库不存在', '它可能已经被删除');
    const items = lowStockOf(lib);
    const unit = restockUnit(lib);
    const groups = new Map();
    items.forEach((i) => {
      if (!groups.has(i.model)) groups.set(i.model, []);
      groups.get(i.model).push(i);
    });

    const body = items.length ? `
      <div class="rs-hint">点一行看这条规格的详情；下方可以生成发给供货商的文本。</div>
      ${[...groups.entries()].map(([model, list]) => {
        const zero = list.filter((i) => i.qty <= 0).length;
        return `
          <div class="rs-group">
            <b>${esc(model)}</b>
            <span class="${zero ? 'danger' : ''}">${list.length} 个${zero ? ` · ${zero} 缺货` : ''}</span>
          </div>
          ${list.map((i) => `
            <button class="rs-row" data-act="open-entry" data-lib="${lib.id}" data-id="${i.entry.id}">
              <span class="rs-sph">${fmtPower(i.sph)}</span>
              <span class="rs-cyl">${i.cyl ? fmtPower(i.cyl) : '单光'}</span>
              <span class="rs-qty ${i.qty <= 0 ? 'zero' : ''}">${Math.round(i.qty)} ${esc(unit)}</span>
              <span class="rs-th">阈值 ${i.th}</span>
              ${icon('chevron', 15)}
            </button>`).join('')}
        `;
      }).join('')}
      <div class="rs-foot">
        <div class="rs-note">补货数量：单光 ${rsQty.plain} ${esc(unit)} · 带散光 ${rsQty.cyl} ${esc(unit)}</div>
        <button class="btn primary" data-act="restock-text">生成补货文本（可复制）</button>
      </div>
    ` : emptyHTML('box', '库存都够用', '没有低于提醒阈值的规格');

    return `
      <div class="screen">
        <div class="navbar">
          <button class="nav-btn" data-act="back" aria-label="返回">${icon('arrowLeft', 20)}</button>
          <div class="nav-title"><b>补货清单</b><span>${esc(lib.name)} · ${items.length} 个规格低于阈值</span></div>
          <div class="nav-actions"></div>
        </div>
        ${body}
      </div>`;
  }

  function restockTextSheet(lib) {
    const items = lowStockOf(lib);
    const unit = restockUnit(lib);
    const root = $('#sheetRoot');
    root.innerHTML = `
      <div class="sheet-backdrop" data-act="close-sheet"></div>
      <div class="sheet" role="dialog" aria-modal="true">
        <div class="sheet-grip"></div>
        <div class="sheet-head">
          <h2>补货文本</h2>
          <p>按型号分组，单光按 ${rsQty.plain} ${esc(unit)}、带散光按 ${rsQty.cyl} ${esc(unit)} 生成，复制后直接发给供货商。</p>
        </div>
        <div class="sheet-body">
          <div class="rs-qty-row">
            <label>单光数量<input id="rsPlain" type="number" min="0" max="999" value="${rsQty.plain}" /></label>
            <label>带散光数量<input id="rsCyl" type="number" min="0" max="999" value="${rsQty.cyl}" /></label>
          </div>
          <pre class="rs-text" id="rsText">${esc(restockText(lib, rsQty.plain, rsQty.cyl))}</pre>
        </div>
        <div class="sheet-actions">
          <button type="button" class="btn ghost" data-act="restock-save">保存为 txt</button>
          <button type="button" class="btn primary" data-act="restock-copy">复制全部（${items.length} 个规格）</button>
        </div>
      </div>`;
    root.classList.add('open');
    sheetOpen = true;
    const refresh = () => {
      rsQty.plain = Math.max(0, Math.min(999, parseInt($('#rsPlain').value, 10) || 0));
      rsQty.cyl = Math.max(0, Math.min(999, parseInt($('#rsCyl').value, 10) || 0));
      $('#rsText').textContent = restockText(lib, rsQty.plain, rsQty.cyl);
    };
    $('#rsPlain').addEventListener('input', refresh);
    $('#rsCyl').addEventListener('input', refresh);
  }

  function saveRestockTxt(lib) {
    const d = new Date();
    const stamp = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const name = `${lib.name.replace(/[\\/:*?"<>|]/g, '_')}-补货-${stamp}.txt`;
    const blob = new Blob(['\uFEFF' + restockText(lib, rsQty.plain, rsQty.cyl)], { type: 'text/plain;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 3000);
    toast('已保存补货文本');
  }

  /* ---------------- 批量改库存 ---------------- */
  let batchUndo = null;
  const batchState = { all: false, model: '', sphMin: '', sphMax: '', cyl: 'all', qty: '' };

  function batchFilterOf(lib) {
    const cfg = matrixConfig(lib);
    if (!cfg) return null;
    const lo = toPower(batchState.sphMin);
    const hi = toPower(batchState.sphMax);
    const both = lo !== null && hi !== null;
    return {
      cfg,
      model: batchState.all ? null : batchState.model,
      min: both ? Math.min(lo, hi) : lo,
      max: both ? Math.max(lo, hi) : hi,
      cyl: batchState.cyl
    };
  }

  function batchEntries(lib, f) {
    if (!f) return [];
    return lib.entries.filter((e) => {
      const model = String(e.values[f.cfg.group.id] || '').trim();
      if (!model) return false;
      if (f.model && model !== f.model) return false;
      const sph = toPower(e.values[f.cfg.row.id]);
      const cyl = cylValueOf(f.cfg, e);
      if (sph === null || cyl === null) return false;
      if (f.min !== null && sph < f.min - 1e-9) return false;
      if (f.max !== null && sph > f.max + 1e-9) return false;
      if (f.cyl === 'none' && cyl !== 0) return false;
      if (f.cyl === 'has' && cyl === 0) return false;
      return true;
    });
  }

  function batchSheet(lib) {
    const cfg = matrixConfig(lib);
    if (!cfg) return;
    const data = matrixData(lib);
    const names = data ? [...data.models.keys()] : [];
    const model = names.includes(ui.matrixModel) ? ui.matrixModel : (names[0] || '');
    const unit = cfg.value.unit || '副';
    batchState.all = false;
    batchState.model = model;
    batchState.sphMin = '';
    batchState.sphMax = '';
    batchState.cyl = 'all';
    batchState.qty = '';

    const root = $('#sheetRoot');
    root.innerHTML = `
      <div class="sheet-backdrop" data-act="close-sheet"></div>
      <div class="sheet" role="dialog" aria-modal="true">
        <div class="sheet-grip"></div>
        <div class="sheet-head">
          <h2>批量改库存</h2>
          <p>按型号、球镜范围、散光筛选，把库存一次改成同一个值。</p>
        </div>
        <div class="sheet-body">
          <div class="form-field">
            <label>范围</label>
            <select id="btScope">
              <option value="one" selected>当前型号（${esc(model)}）</option>
              <option value="all">全部型号</option>
            </select>
          </div>
          <div class="bt-grid">
            <div class="form-field"><label>球镜下限</label><input id="btMin" placeholder="-4.00" autocomplete="off" /></div>
            <div class="form-field"><label>球镜上限</label><input id="btMax" placeholder="-1.00" autocomplete="off" /></div>
          </div>
          <div class="form-field">
            <label>散光</label>
            <select id="btCyl">
              <option value="all">不限散光</option>
              <option value="has">有散光</option>
              <option value="none">无散光</option>
            </select>
          </div>
          <div class="form-field">
            <label>库存改为（${esc(unit)}）</label>
            <input id="btQty" type="number" min="0" max="9999" placeholder="0" autocomplete="off" />
          </div>
          <div class="bt-count">将影响 <b id="btCount">0 个规格</b></div>
        </div>
        <div class="sheet-actions">
          <button type="button" class="btn ghost" data-act="close-sheet">取消</button>
          <button type="button" class="btn primary" data-act="batch-apply">应用</button>
        </div>
      </div>`;
    root.classList.add('open');
    sheetOpen = true;

    const sync = () => {
      batchState.all = $('#btScope').value === 'all';
      batchState.sphMin = $('#btMin').value;
      batchState.sphMax = $('#btMax').value;
      batchState.cyl = $('#btCyl').value;
      batchState.qty = $('#btQty').value;
      const n = batchEntries(lib, batchFilterOf(lib)).length;
      const el = $('#btCount');
      el.textContent = `${n} 个规格`;
      el.classList.toggle('zero', n === 0);
    };
    ['btScope', 'btMin', 'btMax', 'btCyl', 'btQty'].forEach((id) => {
      $('#' + id).addEventListener('input', sync);
      $('#' + id).addEventListener('change', sync);
    });
    sync();
  }

  function applyBatch(lib) {
    const f = batchFilterOf(lib);
    if (!f) return;
    const list = batchEntries(lib, f);
    if (!list.length) { toast('没有匹配的规格'); return; }
    const qty = Math.max(0, Math.round(Number(batchState.qty) || 0));
    const prev = list.map((e) => ({ id: e.id, v: e.values[f.cfg.value.id] }));
    list.forEach((e) => {
      e.values[f.cfg.value.id] = String(qty);
      e.updatedAt = now();
    });
    batchUndo = { libId: lib.id, fieldId: f.cfg.value.id, prev };
    save();
    closeSheet();
    render();
    toast(`已把 ${list.length} 个规格改成 ${qty} ${f.cfg.value.unit || '副'}，可点「撤销」`);
  }

  function undoBatch() {
    if (!batchUndo) return;
    const lib = libById(batchUndo.libId);
    const undo = batchUndo;
    batchUndo = null;
    if (!lib) { render(); return; }
    let n = 0;
    undo.prev.forEach((p) => {
      const e = lib.entries.find((x) => x.id === p.id);
      if (e) { e.values[undo.fieldId] = p.v == null ? '' : p.v; n++; }
    });
    save();
    render();
    toast(`已撤销 ${n} 个规格的库存修改`);
  }

  function alertRuleText(rule) {
    const lo = toPower(rule.sphMin);
    const hi = toPower(rule.sphMax);
    const range = lo === null && hi === null
      ? '全部球镜'
      : `${lo !== null ? fmtPower(lo) : '—'} ~ ${hi !== null ? fmtPower(hi) : '—'}`;
    const cyl = rule.cyl === 'none' ? '无散光' : rule.cyl === 'has' ? '有散光' : '不限散光';
    return `${range} · ${cyl}`;
  }
  function alertsSheet(lib) {
    const al = alertsOf(lib);
    if (!al) return;
    const items = lowStockOf(lib);
    const rows = al.rules.map((r) => `
      <button type="button" class="ar-row" data-act="alert-edit" data-rid="${r.id}">
        <span class="ar-main">
          <b>${esc(r.model || '全部型号')}</b>
          <span>${esc(alertRuleText(r))}</span>
        </span>
        <span class="ar-th">≤ ${r.threshold}</span>
        ${icon('chevron', 15)}
      </button>`).join('');
    openSheet({
      title: '库存提醒',
      desc: '库存低于或等于阈值时，在首页提醒。库存留空的规格不参与提醒。',
      fields: [{
        name: 'def', label: '统一阈值（没有单独规则的规格）', type: 'number',
        value: al.def, min: 0, step: 1, hint: '0 表示只在缺货时提醒'
      }],
      extraHTML: `
        <div class="ar-sum">${items.length
          ? `当前有 <b>${items.length}</b> 个规格低于阈值` + (items.filter((i) => i.qty <= 0).length ? `，其中 ${items.filter((i) => i.qty <= 0).length} 个已缺货` : '')
          : '当前没有低于阈值的规格'}</div>
        <div class="ar-list">
          ${rows || '<div class="ar-empty">还没有单独规则，所有规格都使用上面的统一阈值</div>'}
        </div>
        <button type="button" class="btn ghost full" data-act="alert-add">+ 添加规则</button>
        <div class="lf-hint">规则从上到下匹配，先匹配到的生效。「球镜从 / 到」按数值比较，留空表示不限。</div>
      `,
      submitText: '保存',
      onSubmit(d) {
        lib.alerts.defaultThreshold = Math.max(0, Math.round(Number(d.def) || 0));
        save();
        render();
        toast('提醒设置已保存');
      }
    });
  }
  function alertRuleSheet(lib, rid) {
    const al = alertsOf(lib);
    if (!al) return;
    const editing = rid ? al.rules.find((r) => r.id === rid) : null;
    const models = [...new Set(lib.entries
      .map((e) => String(e.values[al.cfg.group.id] || '').trim())
      .filter(Boolean))];
    openSheet({
      title: editing ? '编辑提醒规则' : '添加提醒规则',
      desc: '可以按型号、球镜范围和是否有散光分别设置阈值。',
      fields: [
        {
          name: 'model', label: '型号', type: 'select', value: editing ? editing.model : '',
          options: [{ value: '', label: '全部型号' }, ...models.map((m) => ({ value: m, label: m }))]
        },
        { name: 'sphMin', label: '球镜从', value: editing ? editing.sphMin : '', placeholder: '例如 -4.00，留空不限' },
        { name: 'sphMax', label: '球镜到', value: editing ? editing.sphMax : '', placeholder: '例如 -1.00，留空不限' },
        {
          name: 'cyl', label: '柱镜', type: 'select', value: editing ? editing.cyl : 'all',
          options: [{ value: 'all', label: '不限' }, { value: 'none', label: '无散光（柱镜 0）' }, { value: 'has', label: '有散光' }]
        },
        { name: 'threshold', label: '库存 ≤', type: 'number', value: editing ? editing.threshold : 1, min: 0, step: 1, hint: '低于或等于这个数量时提醒' }
      ],
      submitText: '保存',
      extraHTML: editing
        ? `<button type="button" class="settings-row danger" style="margin-top:4px;background:var(--danger-soft);border:1px solid #f5d2d2;border-radius:14px" data-act="alert-del" data-rid="${editing.id}"><span class="s-ico">${icon('trash', 17)}</span><span class="s-main"><b>删除这条规则</b><span>删除后使用统一阈值</span></span></button>`
        : '',
      onSubmit(d) {
        const rule = {
          id: editing ? editing.id : uid('ar_'),
          model: String(d.model || '').trim(),
          sphMin: String(d.sphMin || '').trim(),
          sphMax: String(d.sphMax || '').trim(),
          cyl: d.cyl || 'all',
          threshold: Math.max(0, Math.round(Number(d.threshold) || 0))
        };
        if (editing) Object.assign(editing, rule);
        else al.rules.push(rule);
        lib.alerts.rules = al.rules;
        save();
        render();
        setTimeout(() => alertsSheet(lib), 420);
      }
    });
  }

  function matrixView(lib) {
    const data = matrixData(lib);
    if (!data) return '';
    const names = [...data.models.keys()];
    if (!names.length) return emptyHTML('box', '还没有库存数据', '用 CSV 导入，或点右下角新增一条记录');
    const model = names.includes(ui.matrixModel) ? ui.matrixModel : names[0];
    const g = data.models.get(model);
    const range = ((lib.matrix && lib.matrix.ranges) || {})[model];
    const sphs = range && range.sph ? powerAxis(range.sph.map(Number)) : powerAxis([...g.sphs]);
    const cyls = range && range.cyl ? powerAxis(range.cyl.map(Number), true) : powerAxis([...g.cyls], true);
    const at = (s, c) => g.cells.get(s + '|' + c) || 0;
    const level = (n) => (n <= 0 ? 0 : n >= 4 ? 4 : n);
    const rowTotal = (s) => cyls.reduce((n, c) => n + at(s, c), 0);
    const colTotal = (c) => sphs.reduce((n, s) => n + at(s, c), 0);
    const grand = sphs.reduce((n, s) => n + rowTotal(s), 0);
    const totals = new Map(names.map((n) => {
      const mg = data.models.get(n);
      let sum = 0;
      mg.cells.forEach((v) => { sum += v; });
      return [n, sum];
    }));
    const al = alertsOf(lib);
    const lowSet = new Set();
    if (al) {
      sphs.forEach((s) => cyls.forEach((c) => {
        const k = s + '|' + c;
        if (!g.filled.has(k)) return;
        if (at(s, c) <= thresholdFor(al, model, s, c)) lowSet.add(k);
      }));
    }
    const modelLows = new Map(names.map((n) => {
      const mg = data.models.get(n);
      let cnt = 0;
      if (al) {
        mg.filled.forEach((k) => {
          const parts = k.split('|');
          const s = Number(parts[0]);
          const c = Number(parts[1]);
          if ((mg.cells.get(k) || 0) <= thresholdFor(al, n, s, c)) cnt++;
        });
      }
      return [n, cnt];
    }));

    return `
      <div class="mx">
        <div class="mx-models">
          ${names.map((n) => `
            <button class="mx-model ${n === model ? 'active' : ''}" data-act="mx-model" data-model="${esc(n)}">
              <b>${esc(n)}</b><span>${totals.get(n)} ${esc(data.cfg.value.unit || '副')}</span>
              ${modelLows.get(n) ? `<span class="mx-model-low">${modelLows.get(n)} 提醒</span>` : ''}
            </button>`).join('')}
        </div>
        <div class="mx-hint">点格子改库存 · 左右滑动看更多柱镜</div>
        <div class="mx-scroll">
          <table class="mx-table">
            <thead>
              <tr>
                <th class="mx-corner">球镜 \\ 柱镜</th>
                ${cyls.map((c) => `<th>${c === 0 ? '0' : fmtPower(c)}</th>`).join('')}
                <th class="mx-sum">合计</th>
              </tr>
            </thead>
            <tbody>
              ${sphs.map((s) => `
                <tr>
                  <th class="mx-rh">${fmtPower(s)}</th>
                  ${cyls.map((c) => {
                    const q = at(s, c);
                    const k = s + '|' + c;
                    const low = lowSet.has(k) ? ' low' : '';
                    return `<td class="mx-cell lv${level(q)}${q ? ' has' : ''}${low}" data-act="mx-cell" data-model="${esc(model)}" data-sph="${s}" data-cyl="${c}">${q || '·'}</td>`;
                  }).join('')}
                  <td class="mx-sum">${rowTotal(s) || '·'}</td>
                </tr>`).join('')}
            </tbody>
            <tfoot>
              <tr>
                <th class="mx-rh">合计</th>
                ${cyls.map((c) => `<td class="mx-sum">${colTotal(c) || '·'}</td>`).join('')}
                <td class="mx-sum mx-grand">${grand}</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div class="mx-legend">
          <span>库存</span>
          ${[1, 2, 3, 4].map((n) => `<i class="lv${n}"></i>`).join('')}
          <span class="mx-legend-note">颜色越深，库存越多（4 ${esc(data.cfg.value.unit || '副')}以上同一档）</span>
        </div>
        ${al ? `<div class="mx-legend"><i class="mx-low-swatch"></i><span>红框 = 低于该型号的提醒阈值</span></div>` : ''}
      </div>
    `;
  }
  function mxSheet(lib, model, sph, cyl) {
    const cfg = matrixConfig(lib);
    if (!cfg) return;
    const hit = findSku(lib, model, sph, cyl);
    const rawCur = hit ? String(hit.values[cfg.value.id] ?? '').trim() : '';
    const cur = rawCur === '' ? '' : Number(rawCur) || 0;
    openSheet({
      title: model,
      desc: `球镜 ${fmtPower(sph)}${cyl ? ' / 柱镜 ' + fmtPower(cyl) : ' · 单光'}`,
      fields: [{ name: 'qty', label: `库存（${cfg.value.unit || '副'}）`, type: 'number', value: cur, min: 0, step: 1, hint: '留空表示没有这个镜片，不计库存、不提醒；填 0 表示缺货，会提醒' }],
      extraHTML: `
        <div class="mx-quick">
          <div class="mx-quick-label">快速设置</div>
          <div class="mx-quick-row">
            ${[0, 1, 2, 3, 4].map((n) => `<button type="button" class="mx-quick-btn" data-act="mx-set" data-qty="${n}">${n}</button>`).join('')}
            <button type="button" class="mx-quick-btn" data-act="mx-set" data-qty="">留空</button>
          </div>
        </div>`,
      submitText: '保存',
      onSubmit(d) {
        const raw = String(d.qty ?? '').trim();
        mxSave(lib, model, sph, cyl, raw === '' ? '' : Math.max(0, Math.round(Number(raw) || 0)));
      }
    });
  }

  /* ---------------- 关联选择（从其它库取值，扣库存但只读快照） ---------------- */
  function pickCfg(lib, field) {
    const p = field && field.pick;
    if (!p || field.type !== 'pick') return null;
    const src = libById(p.libId);
    if (!src) return null;
    return { p, src };
  }
  function pickDesc(cfg, field) {
    if (!cfg) return '';
    const { p, src } = cfg;
    if (p.kind === 'sku') {
      const mx = matrixConfig(src);
      const parts = mx ? [`${mx.row.name} / ${mx.col.name}`] : [];
      return `从「${src.name}」选择${parts.length ? ' ' + parts[0] : '规格'}，只会写入文字，之后改库也不影响这条记录`;
    }
    const f = fieldById(src, p.fieldId);
    return `从「${src.name}」的「${f ? f.name : '字段'}」里选择，只会写入文字，之后改库也不影响这条记录`;
  }
  function pickOptions(lib, field, entry, filterOverride) {
    const c = pickCfg(lib, field);
    if (!c) return [];
    const { p, src } = c;
    if (p.kind === 'sku') {
      const mx = matrixConfig(src);
      if (!mx) return [];
      let filter = filterOverride !== undefined
        ? String(filterOverride ?? '').trim()
        : (p.filterFieldId ? String(entry && entry.values ? entry.values[p.filterFieldId] : '').trim() : '');
      const build = (f) => {
        const rows = [];
        src.entries.forEach((e) => {
          const model = String(e.values[mx.group.id] || '').trim();
          if (!model) return;
          if (f && model !== f) return;
          const sph = toPower(e.values[mx.row.id]);
          const cyl = cylValueOf(mx, e);
          if (sph === null) return;
          const plain = fmtPower(sph) + (cyl ? '/' + fmtPower(cyl) : '');
          const text = powerText(sph, cyl);
          rows.push({
            value: text,
            plain,
            label: text,
            sub: model,
            stock: Number(e.values[mx.value.id]) || 0,
            unit: mx.value.unit || '',
            meta: { libId: src.id, entryId: e.id }
          });
        });
        return rows;
      };
      let out = build(filter);
      // 本库里填的是手写型号（跟来源库对不上）时，退回显示全部规格
      if (!out.length && filter) { filter = ''; out = build(''); }
      return out.sort((a, b) => {
        const sa = toPower(a.plain.split('/')[0]);
        const sb = toPower(b.plain.split('/')[0]);
        if (sa !== sb) return Math.abs(sa) - Math.abs(sb) || sa - sb;
        const ca = toPower(a.plain.split('/')[1] || '0');
        const cb = toPower(b.plain.split('/')[1] || '0');
        return Math.abs(ca) - Math.abs(cb) || ca - cb;
      });
    }
    const sf = fieldById(src, p.fieldId);
    if (!sf) return [];
    const mx = matrixConfig(src);
    const seen = new Map();
    src.entries.forEach((e) => {
      const v = String(e.values[sf.id] ?? '').trim();
      if (!v || seen.has(v)) return;
      let sub = '';
      if (mx && mx.group.id === sf.id) {
        let n = 0;
        src.entries.forEach((x) => {
          if (String(x.values[sf.id] ?? '').trim() === v) n += Number(x.values[mx.value.id]) || 0;
        });
        sub = `库存 ${n}${mx.value.unit || ''}`;
      }
      seen.set(v, { value: v, label: v, sub, meta: null });
    });
    return [...seen.values()];
  }
  function adjustStock(meta, delta) {
    const src = libById(meta.libId);
    if (!src) return null;
    const e = src.entries.find((x) => x.id === meta.entryId);
    if (!e) return null;
    const mx = matrixConfig(src);
    if (!mx) return null;
    const cur = Number(e.values[mx.value.id]) || 0;
    const next = Math.max(0, cur + delta);
    e.values[mx.value.id] = String(next);
    e.updatedAt = now();
    const sph = toPower(e.values[mx.row.id]);
    const cyl = cylValueOf(mx, e);
    return {
      libName: src.name,
      label: `${String(e.values[mx.group.id] || '')} ${fmtPower(sph)}${cyl ? '/' + fmtPower(cyl) : ''}`.trim(),
      from: cur,
      to: next,
      unit: mx.value.unit || ''
    };
  }
  function skuTextOf(src, e) {
    const mx = matrixConfig(src);
    if (!mx) return '';
    const sph = toPower(e.values[mx.row.id]);
    const cyl = cylValueOf(mx, e);
    return powerText(sph, cyl);
  }
  function applyPickStock(lib, entry, values, picked) {
    const changes = [];
    if (!entry.picks) entry.picks = {};
    lib.fields.forEach((f) => {
      if (f.type !== 'pick') return;
      const c = pickCfg(lib, f);
      if (!c || !c.p.deduct) return;
      const opts = pickOptions(lib, f, { ...entry, values });
      const val = String(values[f.id] ?? '').trim();
      const norm = c.p.kind === 'sku' ? normPower : (v) => String(v ?? '').trim();
      const valKey = norm(val);
      const prev = entry.picks[f.id];
      const prevSrc = prev ? libById(prev.libId) : null;
      const prevSku = prev && prevSrc ? prevSrc.entries.find((x) => x.id === prev.entryId) : null;
      const sameAsPrev = !!(prev && prevSku && norm(skuTextOf(prevSrc, prevSku)) === valKey);
      const wasPicked = !picked || picked[f.id];
      // 没从面板里挑、值也只是被顺手改了：只把之前扣的还回去，绝不凭文字再扣一次
      if (!wasPicked) {
        if (prev && !sameAsPrev) {
          const back = adjustStock(prev, Number(prev.qty) || 1);
          if (back) changes.push({ ...back, dir: 1 });
          delete entry.picks[f.id];
        }
        return;
      }
      const modelHint = c.p.filterFieldId ? String(values[c.p.filterFieldId] ?? '').trim() : '';
      const hit = (prev && opts.find((o) => o.meta && o.meta.entryId === prev.entryId && norm(o.value) === valKey))
        || opts.find((o) => norm(o.value) === valKey && o.meta && modelHint && o.sub === modelHint)
        || opts.find((o) => norm(o.value) === valKey && o.meta);
      const same = prev && hit && prev.entryId === hit.meta.entryId;
      if (prev && !same) {
        const back = adjustStock(prev, Number(prev.qty) || 1);
        if (back) changes.push({ ...back, dir: 1 });
        delete entry.picks[f.id];
      }
      if (hit && !same) {
        const qty = Math.max(1, Number(c.p.qty) || 1);
        const used = adjustStock(hit.meta, -qty);
        if (used) changes.push({ ...used, dir: -1 });
        entry.picks[f.id] = { libId: hit.meta.libId, entryId: hit.meta.entryId, qty };
      }
    });
    return changes;
  }
  function stockToast(changes) {
    if (!changes || !changes.length) return;
    const c = changes[changes.length - 1];
    toast(`${c.libName} · ${c.label} 库存 ${c.from} → ${c.to}${c.unit}`);
  }

  /* ---------------- 选择面板 ---------------- */
  function closePicker() {
    const el = document.querySelector('.pick-layer');
    if (el) el.remove();
  }
  function openPicker({ title, desc, options, selected, onPick }) {
    closePicker();
    if (!options || !options.length) { toast('没有可选的选项'); return; }
    const layer = document.createElement('div');
    layer.className = 'pick-layer';
    layer.innerHTML = `
      <div class="sheet-backdrop"></div>
      <div class="sheet pick-sheet">
        <div class="sheet-grip"></div>
        <div class="sheet-head"><h2>${esc(title)}</h2>${desc ? `<p>${esc(desc)}</p>` : ''}</div>
        ${options.length > 6 ? '<div class="pick-search-wrap"><input class="pick-search" type="search" placeholder="搜索型号或度数" autocomplete="off" /></div>' : ''}
        <div class="pick-list"></div>
      </div>`;
    $('#sheetRoot').appendChild(layer);
    const list = layer.querySelector('.pick-list');
    const selectedText = String(selected ?? '').trim();
    const paint = (q) => {
      const k = String(q || '').trim().toLowerCase();
      const rows = options.filter((o) => !k || `${o.label} ${o.plain || ''} ${o.sub || ''}`.toLowerCase().includes(k));
      list.innerHTML = rows.length
        ? rows.map((o, i) => `
          <button type="button" class="pick-row ${o.value === selectedText || (o.plain && normPower(o.plain) === normPower(selectedText)) ? 'sel' : ''}" data-i="${i}">
            <span class="pk-main"><b>${esc(o.label)}</b>${o.sub ? `<span>${esc(o.sub)}</span>` : ''}</span>
            ${o.stock !== undefined ? `<span class="pk-stock${o.stock > 0 ? '' : ' zero'}">${o.stock}${esc(o.unit || '')}</span>` : ''}
          </button>`).join('')
        : '<div class="pick-empty">没有匹配的选项</div>';
      list._rows = rows;
    };
    paint('');
    list.addEventListener('click', (ev) => {
      const btn = ev.target.closest('.pick-row');
      if (!btn) return;
      const opt = list._rows[Number(btn.dataset.i)];
      if (!opt) return;
      closePicker();
      onPick(opt);
    });
    layer.querySelector('.sheet-backdrop').addEventListener('click', closePicker);
    const search = layer.querySelector('.pick-search');
    if (search) search.addEventListener('input', () => paint(search.value));
  }

  /* ---------------- entry detail ---------------- */
  function entryView() {
    const lib = libById(ui.route.libId);
    const entry = lib && lib.entries.find((e) => e.id === ui.route.id);
    if (!lib || !entry) return emptyHTML('database', '记录不存在', '它可能已经被删除');
    const title = titleOf(lib, entry);
    const related = relatedFor(lib, entry);
    const otherLib = related.filter((x) => x.lib.id !== lib.id);
    const family = familyGroup(lib, entry);
    const mx = matrixConfig(lib);
    const subtitle = mx ? powerSubtitle(entry, mx) : subtitleOf(lib, entry);
    const hist = hasTimeline(lib)
      ? dedupeHistory(lib, [{ entry, current: true }, ...samePersonEntries(lib, entry).map((e) => ({ entry: e, current: false }))]
        .sort((a, b) => entryDate(lib, b.entry) - entryDate(lib, a.entry)))
      : [];

    return `
      <div class="screen">
        <div class="navbar">
          <button class="nav-btn" data-act="back" aria-label="返回">${icon('arrowLeft', 20)}</button>
          <div class="nav-title"><b>${esc(title)}</b><span>${esc(lib.name)}</span></div>
          <div class="nav-actions">
            <button class="nav-btn" data-act="toggle-fav" aria-label="收藏" style="${entry.favorite ? 'color:#e69a12' : ''}">${icon('star', 19)}</button>
            <button class="nav-btn" data-act="entry-menu" aria-label="更多">${icon('more', 19)}</button>
          </div>
        </div>

        <div class="profile">
          ${avatarHTML(lib, title, '', mx ? 'grid' : '')}
          <div>
            <h2>${esc(title)}</h2>
            <p>${subtitle ? esc(subtitle) : '点击字段即可编辑'}</p>
            <div class="profile-tags">
              <span class="tag">${esc(lib.name)}</span>
              ${entry.favorite ? '<span class="tag">已收藏</span>' : ''}
              ${family ? `<span class="tag">家庭 · ${family.people} 人</span>` : ''}
              <span class="tag gray">${visitDate(lib, entry) ? '验光 ' + fmtDate(visitDate(lib, entry)) : '更新于 ' + relTime(entry.updatedAt)}</span>
            </div>
          </div>
        </div>

        <div class="field-card">
          ${lib.fields.map((f) => fieldRowHTML(lib, entry, f)).join('')}
        </div>

        ${hist.length > 1 ? `
        <div class="block-title">
          <h3>验光历史</h3>
          <span>同名 · 共 ${hist.length} 次</span>
        </div>
        ${historyHTML(lib, hist)}` : ''}

        ${family ? `
        <div class="block-title">
          <h3>家庭组</h3>
          <span>同电话 · 共 ${family.people} 人</span>
        </div>
        <div class="family-card">
          <div class="family-head">
            <span class="family-ico">${icon('users', 17)}</span>
            <div class="family-info">
              <b>${esc(family.display)}</b>
              <span>${family.people} 人共用此号码 · ${family.visits} 次验光 · 合计 ¥${family.total}</span>
            </div>
            <button type="button" class="family-merge" data-act="merge-person">合并</button>
          </div>
          ${family.members.map((m) => {
            const totalF = lib.fields.find((f) => /合计|金额|价格/.test(f.name));
            const money = totalF ? m.entry.values[totalF.id] : '';
            return `
            <button class="family-row" data-act="open-entry" data-lib="${lib.id}" data-id="${m.entry.id}">
              ${avatarHTML(lib, m.name, 'sm')}
              <span class="family-main">
                <b>${esc(m.name)}</b>
                <span>最近验光 ${fmtDate(m.ts)}${money ? ` · ¥${esc(oneLine(money))}` : ''}</span>
              </span>
              ${icon('chevron', 15)}
            </button>`;
          }).join('')}
        </div>` : ''}

        ${otherLib.length ? `
        <div class="block-title">
          <h3>关联记录</h3>
          <span>其他数据库</span>
        </div>
        ${otherLib.map(({ lib: l, entry: e }) => `
          <button class="related-row" data-act="open-entry" data-lib="${l.id}" data-id="${e.id}">
            ${avatarHTML(l, titleOf(l, e))}
            <span>
              <b>${esc(titleOf(l, e))}</b>
              <span>${esc(l.name)} · ${subtitleOf(l, e) ? esc(subtitleOf(l, e)) : '最近更新 ' + relTime(e.updatedAt)}</span>
            </span>
            <span class="go">${icon('chevron', 16)}</span>
          </button>
        `).join('')}` : ''}

        ${hasTimeline(lib) && hist.length <= 1 && !otherLib.length && !family ? `
        <div class="block-title">
          <h3>关联记录</h3>
          <span>同名 / 同电话</span>
        </div>
        <div style="margin:0 16px;padding:14px;border-radius:14px;background:var(--surface);border:1px dashed var(--line);color:var(--muted);font-size:11.5px;text-align:center">暂未发现同名或同电话的记录</div>` : ''}

        <div style="height:18px"></div>
      </div>
    `;
  }

  function fieldIcon(f) {
    if (f.type === 'autonumber') return 'layers';
    if (f.type === 'createdat' || f.type === 'updatedat') return 'calendar';
    if (f.type === 'date') return 'calendar';
    if (f.type === 'checkbox') return 'check';
    if (f.type === 'rating') return 'star';
    if (f.type === 'url') return 'link';
    if (f.type === 'pick') return 'link';
    if (f.type === 'email') return 'doc';
    if (f.type === 'select' || f.type === 'multiselect') return 'layers';
    if (f.type === 'tel' || f.name.includes('电话')) return 'phone';
    if (f.name.includes('城市') || f.name.includes('地址')) return 'pin';
    if (f.type === 'number') return 'layers';
    return 'doc';
  }

  function fieldValueHTML(f, value, lib, entry) {
    const raw = String(value ?? '');
    if (f.type === 'formula') {
      const out = lib && entry ? formulaValue(lib, entry, f) : '';
      if (!out) return '<span class="f-value empty-value">公式无结果</span>';
      return `<span class="f-value"><span class="fx-out">${esc(out)}${f.unit ? esc(f.unit) : ''}</span></span>`;
    }
    if (f.type === 'autonumber') {
      if (!raw) return '<span class="f-value empty-value">保存后自动生成</span>';
      return `<span class="f-value"><span class="auto-num">${esc((f.prefix || '') + raw)}</span></span>`;
    }
    if (f.type === 'createdat' || f.type === 'updatedat') {
      if (!raw) return '<span class="f-value empty-value">保存后自动生成</span>';
      return `<span class="f-value auto-time">${esc(raw)}</span>`;
    }
    if (!raw) return `<span class="f-value empty-value">${f.type === 'pick' ? '点击选择' : '点击填写'}</span>`;
    if (f.type === 'multiselect') {
      const parts = raw.split(MULTI_SEP).filter(Boolean);
      return `<span class="f-chips">${parts.map((p) => `<span class="mini-chip">${esc(p)}</span>`).join('')}</span>`;
    }
    if (f.type === 'checkbox') {
      return `<span class="f-value">${isTrueVal(raw) ? '是' : '否'}</span>`;
    }
    if (f.type === 'rating') {
      const n = Math.max(0, Math.min(5, parseInt(raw, 10) || 0));
      return `<span class="f-stars">${'★'.repeat(n)}<i>${'★'.repeat(5 - n)}</i></span>`;
    }
    if (f.type === 'date') {
      const ts = parseDateVal(raw);
      return `<span class="f-value">${esc(ts ? fmtDate(ts) : oneLine(raw))}</span>`;
    }
    return `<span class="f-value">${esc(oneLine(raw) + (f.unit || ''))}</span>`;
  }

  function fieldRowHTML(lib, entry, f) {
    const value = entry.values[f.id];
    const locked = isSysField(f) || f.type === 'formula';
    const inner = `
        <span class="f-ico">${icon(fieldIcon(f), 16)}</span>
        <span class="f-main">
          <span class="f-label">${esc(f.name)}${locked ? `<span class="lock-tag">${f.type === 'formula' ? '公式' : '自动'}</span>` : ''}</span>
          ${fieldValueHTML(f, value, lib, entry)}
        </span>
        ${locked ? '' : `<span class="f-edit">${icon('edit', 16)}</span>`}
    `;
    return locked
      ? `<div class="field-row locked">${inner}</div>`
      : `<button class="field-row" data-act="edit-field" data-fid="${f.id}">${inner}</button>`;
  }

  /* ---------------- settings ---------------- */
  function settingsView() {
    const total = state.libraries.reduce((n, l) => n + l.entries.length, 0);
    return `
      <div class="screen">
        <div class="settings-head">
          <h1>我的</h1>
          <p>数据保存在本机浏览器，可以导出到本地或备份到 WebDAV</p>
        </div>

        <div class="stats" style="margin:4px 16px 0;box-shadow:var(--shadow-sm)">
          <div class="stat"><b>${state.libraries.length}</b><span>数据库</span></div>
          <div class="stat"><b>${total}</b><span>条记录</span></div>
          <div class="stat"><b>${state.libraries.reduce((n, l) => n + l.fields.length, 0)}</b><span>字段</span></div>
        </div>

        <div class="block-title"><h3>数据管理</h3><span>本机 + 云端</span></div>
        <div class="settings-card">
          <button class="settings-row" data-act="backup">
            <span class="s-ico">${icon('cloud', 17)}</span>
            <span class="s-main"><b>备份与恢复</b><span>本地文件 / WebDAV 云端 · 上次 ${esc(bkStamp(bk.lastLocalAt > bk.lastCloudAt ? bk.lastLocalAt : bk.lastCloudAt))}</span></span>
            ${icon('chevron', 16)}
          </button>
          <button class="settings-row" data-act="export">
            <span class="s-ico">${icon('download', 17)}</span>
            <span class="s-main"><b>导出到文件</b><span>把所有库和记录保存为 JSON</span></span>
            ${icon('chevron', 16)}
          </button>
          <button class="settings-row" data-act="import">
            <span class="s-ico">${icon('upload', 17)}</span>
            <span class="s-main"><b>从文件恢复</b><span>用之前导出的 JSON 覆盖当前数据</span></span>
            ${icon('chevron', 16)}
          </button>
          <button class="settings-row" data-act="reset">
            <span class="s-ico">${icon('refresh', 17)}</span>
            <span class="s-main"><b>重置演示数据</b><span>清空当前数据，回到初始示例</span></span>
            ${icon('chevron', 16)}
          </button>
        </div>

        ${hasSnap() ? `
        <div class="block-title"><h3>恢复前快照</h3><span>${esc(snapTime())}</span></div>
        <div class="settings-card">
          <button class="settings-row" data-act="snap-undo">
            <span class="s-ico">${icon('refresh', 17)}</span>
            <span class="s-main"><b>撤销上次恢复</b><span>回到恢复数据之前的状态</span></span>
            ${icon('chevron', 16)}
          </button>
          <button class="settings-row" data-act="snap-drop">
            <span class="s-ico">${icon('trash', 17)}</span>
            <span class="s-main"><b>丢弃快照</b><span>删除这份临时备份</span></span>
            ${icon('chevron', 16)}
          </button>
        </div>` : ''}

        <div class="block-title"><h3>关于</h3><span>原型 v0.1</span></div>
        <div class="settings-card">
          <button class="settings-row" data-act="about">
            <span class="s-ico">${icon('help', 17)}</span>
            <span class="s-main"><b>这个原型怎么用</b><span>功能说明与后续开发计划</span></span>
            ${icon('chevron', 16)}
          </button>
        </div>

        <div class="about">
          <b>MyDB 原型</b><br />
          用网页快速验证交互，确认后 1:1 移植到 Android Compose
        </div>
      </div>
    `;
  }

  /* ---------------- navigation ---------------- */
  function go(route) {
    ui.stack.push(ui.route);
    ui.route = route;
    render();
  }
  function back() {
    ui.route = ui.stack.pop() || { name: 'home' };
    if (ui.route.name === 'home') ui.tab = 'home';
    render();
  }
  function openTab(tab) {
    ui.tab = tab;
    ui.route = { name: tab };
    ui.stack = [];
    render();
  }

  /* ---------------- sheet ---------------- */
  let sheetOpen = false;
  function openSheet({ title, desc, fields = [], submitText = '保存', danger = false, extraHTML = '', onSubmit, onMount }) {
    const root = $('#sheetRoot');
    const body = fields.map(fieldHTML).join('');
    root.innerHTML = `
      <div class="sheet-backdrop" data-act="close-sheet"></div>
      <div class="sheet" role="dialog" aria-modal="true">
        <div class="sheet-grip"></div>
        <div class="sheet-head">
          <h2>${esc(title)}</h2>
          ${desc ? `<p>${esc(desc)}</p>` : ''}
        </div>
        <form id="sheetForm" novalidate>
          <div class="sheet-body">
            ${body}
            ${extraHTML}
          </div>
          <div class="sheet-actions">
            <button type="button" class="btn ghost" data-act="close-sheet">取消</button>
            <button type="submit" class="btn ${danger ? 'danger' : 'primary'}">${esc(submitText)}</button>
          </div>
        </form>
      </div>
    `;
    root.classList.add('open');
    sheetOpen = true;
    const form = $('#sheetForm');
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      if (!form.reportValidity()) return;
      const data = {};
      new FormData(form).forEach((v, k) => {
        if (k in data) data[k] = [].concat(data[k], v);
        else data[k] = v;
      });
      const result = onSubmit(data);
      if (result !== false) closeSheet();
    });
    if (onMount) onMount(form);
    const first = form.querySelector('input, select, textarea');
    if (first) setTimeout(() => first.focus(), 260);
  }

  function fieldHTML(f) {
    const v = f.value === undefined || f.value === null ? '' : f.value;
    let control = '';
    if (f.type === 'select') {
      control = `<select name="${f.name}" ${f.required ? 'required' : ''}>${(f.options || []).map((o) => {
        const val = typeof o === 'string' ? o : o.value;
        const label = typeof o === 'string' ? o : (o.label || o.name || o.value);
        return `<option value="${esc(val)}" ${String(val) === String(v) ? 'selected' : ''}>${esc(label)}</option>`;
      }).join('')}</select>`;
    } else if (f.type === 'checkboxgroup') {
      const picked = Array.isArray(v) ? v.map(String) : String(v || '').split(MULTI_SEP).filter(Boolean);
      control = `<div class="chk-grid">${(f.options || []).map((o) => {
        const val = typeof o === 'string' ? o : o.value;
        const label = typeof o === 'string' ? o : o.label;
        return `<label class="chk"><input type="checkbox" name="${f.name}" value="${esc(val)}" ${picked.includes(String(val)) ? 'checked' : ''} /><span>${esc(label)}</span></label>`;
      }).join('')}</div>`;
    } else if (f.type === 'rating') {
      const cur = parseInt(v, 10) || 0;
      control = `<div class="star-pick">${[1, 2, 3, 4, 5].map((n) => `
        <label title="${n} 分"><input type="radio" name="${f.name}" value="${n}" ${cur === n ? 'checked' : ''} /><span>★</span></label>
      `).join('')}<label class="clr" title="清除评分"><input type="radio" name="${f.name}" value="" ${cur ? '' : 'checked'} /><span>✕</span></label></div>`;
    } else if (f.type === 'textarea') {
      control = `<textarea name="${f.name}" placeholder="${esc(f.placeholder || '')}" ${f.required ? 'required' : ''}>${esc(v)}</textarea>`;
    } else if (f.type === 'readonly') {
      control = `<input name="${f.name}" type="text" value="${esc(v)}" placeholder="${esc(f.placeholder || '自动生成')}" readonly />`;
    } else {
      const type = f.type === 'number' ? 'number' : f.type === 'tel' ? 'tel' : f.type === 'date' ? 'date' : f.type === 'password' ? 'password' : (f.type === 'url' || f.type === 'email') ? f.type : 'text';
      control = `<input name="${f.name}" type="${type}" value="${esc(v)}" placeholder="${esc(f.placeholder || '')}" ${f.step ? `step="${f.step}"` : ''} ${f.min !== undefined ? `min="${f.min}"` : ''} ${f.max !== undefined ? `max="${f.max}"` : ''} ${f.required ? 'required' : ''} autocomplete="off" />`;
    }
    return `
      <div class="form-field">
        <label>${esc(f.label)}${f.required ? ' *' : ''}</label>
        ${control}
        ${f.hint ? `<div class="hint">${esc(f.hint)}</div>` : ''}
      </div>
    `;
  }

  function closeSheet() {
    const root = $('#sheetRoot');
    root.classList.remove('open');
    sheetOpen = false;
    setTimeout(() => { if (!sheetOpen) root.innerHTML = ''; }, 280);
  }

  function confirmSheet({ title, desc, okText = '确认删除', onOk }) {
    openSheet({
      title, desc, fields: [], submitText: okText, danger: true,
      onSubmit() { onOk(); }
    });
  }

  /* ---------------- forms ---------------- */
  function addLibrarySheet() {
    openSheet({
      title: '新建数据库',
      desc: '先起个名字，字段之后还可以继续添加。',
      fields: [
        { name: 'name', label: '名称', placeholder: '例如：客户、书籍、药品', required: true },
        { name: 'icon', label: '图标', type: 'select', value: 'database', options: ICON_CHOICES.map((o) => ({ value: o.value, label: o.name })) },
        { name: 'color', label: '主题色', type: 'select', value: COLORS[0].value, options: COLORS.map((o) => ({ value: o.value, label: o.name })) },
        { name: 'fields', label: '初始字段', value: '名称,备注', hint: '用逗号分隔，第一个字段会作为条目标题' }
      ],
      onSubmit(d) {
        const name = String(d.name || '').trim();
        if (!name) return false;
        const names = String(d.fields || '').split(/[,，]/).map((s) => s.trim()).filter(Boolean);
        const lib = {
          id: uid('lib_'), name, icon: d.icon, color: d.color,
          fields: (names.length ? names : ['名称']).map((fn) => ({ id: uid('f_'), name: fn, type: 'text' })),
          entries: []
        };
        state.libraries.push(lib);
        save();
        toast('已创建「' + name + '」');
        render();
      }
    });
  }

  function editLibrarySheet() {
    const lib = libById(ui.route.id);
    if (!lib) return;
    openSheet({
      title: '数据库设置',
      desc: lib.name,
      fields: [
        { name: 'name', label: '名称', value: lib.name, required: true },
        { name: 'icon', label: '图标', type: 'select', value: lib.icon, options: ICON_CHOICES.map((o) => ({ value: o.value, label: o.name })) },
        { name: 'color', label: '主题色', type: 'select', value: lib.color, options: COLORS.map((o) => ({ value: o.value, label: o.name })) },
        { name: 'fontSize', label: '列表字号（px）', type: 'number', value: listFontPx(lib), min: 10, max: 26, step: 0.5, hint: '标题字号，字段值约为它的 86%；建议 12–18，只影响列表' }
      ],
      extraHTML: `<button type="button" class="settings-row danger" style="margin-top:4px;background:var(--danger-soft);border:1px solid #f5d2d2;border-radius:14px" data-act="delete-lib"><span class="s-ico">${icon('trash', 17)}</span><span class="s-main"><b>删除这个数据库</b><span>包含其中所有记录，无法恢复</span></span></button>`,
      onSubmit(d) {
        const name = String(d.name || '').trim();
        if (!name) return false;
        lib.name = name;
        lib.icon = d.icon;
        lib.color = d.color;
        lib.listFontSize = Math.min(26, Math.max(10, Number(d.fontSize) || 14));
        delete lib.listFont;
        save();
        toast('已更新');
        render();
      }
    });
  }

  function typeLabel(f) {
    const hit = FIELD_TYPES.find((t) => t.value === f.type);
    return hit ? hit.label : '文本';
  }

  function fieldUsage(lib, fid) {
    return lib.entries.filter((e) => String(e.values[fid] ?? '').trim() !== '').length;
  }

  function autoNumOf(v) {
    const m = String(v ?? '').match(/(\d+)\s*$/);
    return m ? parseInt(m[1], 10) : 0;
  }

  function nextAutoNumber(lib, field) {
    let max = 0;
    lib.entries.forEach((e) => {
      const n = autoNumOf(e.values[field.id]);
      if (n > max) max = n;
    });
    const start = parseInt(field.start, 10) || 1;
    return Math.max(max + 1, start);
  }

  function fillAutoNumbers(lib, field) {
    const list = lib.entries.slice().sort((a, b) =>
      (visitDate(lib, a) || a.createdAt || 0) - (visitDate(lib, b) || b.createdAt || 0));
    let n = parseInt(field.start, 10) || 1;
    list.forEach((e) => { e.values[field.id] = String(n++); });
    return list.length;
  }

  function fmtStamp(ts) {
    const d = new Date(ts || Date.now());
    const p = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${p(d.getHours())}:${p(d.getMinutes())}`;
  }

  function fmtDay(ts) {
    const d = new Date(ts || Date.now());
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`;
  }

  function isSysField(f) {
    return f.type === 'autonumber' || f.type === 'createdat' || f.type === 'updatedat';
  }

  /* ---------------- 公式 ---------------- */
  function firstNumberIn(v) {
    const m = String(v ?? '').replace(/,/g, '').match(/-?\d+(?:\.\d+)?/);
    return m ? parseFloat(m[0]) : null;
  }

  // 球镜：-2.75ds → -2.75；只有柱镜时球镜按 0 计
  function sphOf(v) {
    const s = String(v ?? '');
    const sph = s.match(/(-?\d+(?:\.\d+)?)\s*ds/i);
    if (sph) return parseFloat(sph[1]);
    if (/(-?\d+(?:\.\d+)?)\s*dc/i.test(s)) return 0;
    return firstNumberIn(s);
  }

  // 柱镜：-2.75ds/-0.75dc → -0.75；没有 dc 记 0
  function cylOf(v) {
    const s = String(v ?? '');
    const cyl = s.match(/(-?\d+(?:\.\d+)?)\s*dc/i);
    return cyl ? parseFloat(cyl[1]) : 0;
  }

  function valueOfNamedField(lib, entry, name) {
    const key = String(name ?? '').trim();
    const f = lib.fields.find((x) => x.name.trim() === key);
    if (!f) return '';
    if (f.type === 'formula') return formulaValue(lib, entry, f);
    return entry.values[f.id] ?? '';
  }

  function fmtD(x, d = 2) {
    if (x === null || x === undefined || x === '' || !isFinite(x)) return '—';
    const n = Number(x);
    return (n > 0 ? '+' : '') + n.toFixed(d);
  }

  function formulaValue(lib, entry, f) {
    const expr = String(f.expr || '').trim();
    if (!expr) return '';
    try {
      const scope = {
        num: (n) => firstNumberIn(valueOfNamedField(lib, entry, n)),
        sph: (n) => sphOf(valueOfNamedField(lib, entry, n)),
        cyl: (n) => cylOf(valueOfNamedField(lib, entry, n)),
        se: (n) => {
          const s = sphOf(valueOfNamedField(lib, entry, n));
          if (s === null) return null;
          return s + cylOf(valueOfNamedField(lib, entry, n)) / 2;
        },
        text: (n) => String(valueOfNamedField(lib, entry, n) ?? ''),
        fmt: (x, d) => fmtD(x, d === undefined ? 2 : d),
        round: (x, d) => (x === null || x === undefined || x === '' ? null : Math.round(Number(x) * Math.pow(10, d === undefined ? 2 : d)) / Math.pow(10, d === undefined ? 2 : d)),
        cond: (c, a, b) => (c ? a : b),
        abs: (x) => (x === null ? null : Math.abs(Number(x)))
      };
      const fn = new Function(...Object.keys(scope), `"use strict"; return (${expr});`);
      const out = fn(...Object.values(scope));
      if (out === null || out === undefined || out === '') return '';
      if (typeof out === 'number') {
        if (!isFinite(out)) return '';
        return (out > 0 ? '+' : '') + out.toFixed(2);
      }
      return String(out);
    } catch (err) {
      return '';
    }
  }

  // 写入自动编号 / 创建时间 / 更新时间
  function stampAutoFields(lib, entry, isNew) {
    const t = now();
    lib.fields.forEach((f) => {
      const cur = String(entry.values[f.id] ?? '').trim();
      if (f.type === 'autonumber') {
        if (isNew || !cur) entry.values[f.id] = String(nextAutoNumber(lib, f));
      } else if (f.type === 'createdat') {
        if (isNew || !cur) {
          const df = dateFieldOf(lib);
          const fromData = df && !isNew ? parseDateVal(entry.values[df.id]) : null;
          entry.values[f.id] = fmtDay(fromData || entry.createdAt || t);
        }
      } else if (f.type === 'updatedat') {
        entry.values[f.id] = fmtStamp(t);
      }
    });
  }

  function fieldsSheet(lib) {
    openSheet({
      title: '字段管理',
      desc: `${lib.name} · ${lib.fields.length} 个字段，删除字段会同时清除所有记录里的对应值`,
      fields: [],
      submitText: '完成',
      extraHTML: `
        <div class="sheet-list">
          ${lib.fields.map((f, i) => `
            <div class="fmg-row">
              <span class="fmg-ico">${icon(fieldIcon(f), 15)}</span>
              <span class="fmg-main">
                <b>${esc(f.name)}${i === 0 ? '<span class="fmg-first">标题</span>' : ''}</b>
                <span>${typeLabel(f)}${f.unit ? ' · ' + esc(f.unit) : ''}${(f.type === 'select' || f.type === 'multiselect') && f.options && f.options.length ? ` · ${f.options.length} 个选项` : ''}${f.type === 'formula' ? ` · <code>${esc(String(f.expr || '').slice(0, 22))}</code>` : ''} · ${f.type === 'formula' ? '实时计算' : fieldUsage(lib, f.id) + ' 条有值'}</span>
              </span>
              <button type="button" class="icon-btn" data-act="move-field" data-fid="${f.id}" data-dir="-1" ${i === 0 ? 'disabled' : ''} aria-label="上移">${icon('arrowUp', 15)}</button>
              <button type="button" class="icon-btn" data-act="move-field" data-fid="${f.id}" data-dir="1" ${i === lib.fields.length - 1 ? 'disabled' : ''} aria-label="下移">${icon('arrowDown', 15)}</button>
              <button type="button" class="icon-btn" data-act="edit-field-def" data-fid="${f.id}" aria-label="编辑字段">${icon('edit', 15)}</button>
              ${f.type === 'autonumber' ? `<button type="button" class="icon-btn" data-act="renumber-field" data-fid="${f.id}" aria-label="重新编号" title="重新编号">${icon('refresh', 15)}</button>` : ''}
              <button type="button" class="icon-btn danger" data-act="delete-field-def" data-fid="${f.id}" aria-label="删除字段">${icon('trash', 15)}</button>
            </div>
          `).join('')}
        </div>
        <button type="button" class="btn ghost full" data-act="add-field-def" style="margin-top:8px">+ 添加字段</button>
        <div class="hint" style="margin-top:8px">字段顺序就是详情页的显示顺序；第一个字段会作为条目标题。</div>
      `,
      onSubmit() {}
    });
  }

  /* ---------------- 列表显示字段 ---------------- */
  // 列表字号：数字（px），旧备份里的 s/m/l/xl 自动换算
  function listFontPx(lib) {
    const n = Number(lib && lib.listFontSize);
    if (Number.isFinite(n) && n > 0) return Math.min(30, Math.max(8, n));
    const legacy = { s: 12.5, m: 14, l: 15.5, xl: 17 }[(lib && lib.listFont) || 'm'];
    return legacy || 14;
  }
  let lfDraft = null;
  let lfLibId = null;
  let lfShowNames = true;

  function listFieldsSheet(lib) {
    lfLibId = lib.id;
    lfDraft = listFieldsOf(lib).map((f) => f.id);
    lfShowNames = lib.listShowNames !== false;
    openSheet({
      title: '列表显示字段',
      desc: `${lib.name} · 勾选要出现在列表里的字段，第一个作为标题`,
      fields: [],
      submitText: '保存',
      extraHTML: `
        <button type="button" class="lf-toggle" data-act="lf-names">
          <span class="lf-toggle-main"><b>显示字段名称</b><em>关掉后只显示值，一行能放更多内容</em></span>
          <span class="lf-switch${lfShowNames ? ' on' : ''}"><i></i></span>
        </button>
        <div class="lf-preview" id="lfPreview"></div>
        <div class="lf-box" id="lfBox"></div>
      `,
      onMount() { paintListDraft(); },
      onSubmit() {
        lib.listFields = lfDraft.slice();
        lib.listShowNames = lfShowNames;
        save();
        render();
        toast('已更新显示字段');
      }
    });
  }

  function lfRowHTML(f, on, i, total) {
    return `
      <div class="lf-row${on ? ' on' : ''}">
        <button type="button" class="lf-tap" data-act="lf-toggle" data-fid="${f.id}">
          <span class="lf-check">${on ? icon('check', 11) : ''}</span>
          <span class="lf-name">${esc(f.name)}${on && i === 0 ? '<b class="lf-badge">标题</b>' : ''}</span>
          <span class="lf-type">${typeLabel(f)}${f.type === 'formula' ? ' · 实时计算' : ''}</span>
        </button>
        ${on ? `
          <button type="button" class="icon-btn" data-act="lf-move" data-fid="${f.id}" data-dir="-1" ${i === 0 ? 'disabled' : ''} aria-label="上移">${icon('arrowUp', 14)}</button>
          <button type="button" class="icon-btn" data-act="lf-move" data-fid="${f.id}" data-dir="1" ${i === total - 1 ? 'disabled' : ''} aria-label="下移">${icon('arrowDown', 14)}</button>
        ` : ''}
      </div>
    `;
  }

  function paintListDraft() {
    const lib = libById(lfLibId);
    if (!lib || !lfDraft) return;
    const on = lfDraft.map((id) => fieldById(lib, id)).filter(Boolean);
    const off = lib.fields.filter((f) => !lfDraft.includes(f.id));
    const sample = lib.entries.slice().sort((a, b) => entryDate(lib, b) - entryDate(lib, a))[0];
    const prev = $('#lfPreview');
    if (prev) {
      prev.innerHTML = sample ? `
        <div class="lf-prev-label">预览 · ${listFontPx(lib)}px</div>
        <div class="lf-prev-card" style="--list-fs:${listFontPx(lib)}px">${entryRowHTML({ ...lib, listShowNames: lfShowNames }, sample, false, on)}</div>
      ` : '';
    }
    const box = $('#lfBox');
    if (!box) return;
    box.innerHTML = `
      <div class="lf-sec"><span>已显示 ${on.length}</span>${on.length > 1 ? '<button type="button" class="lf-mini" data-act="lf-none">只留标题</button>' : ''}</div>
      ${on.map((f, i) => lfRowHTML(f, true, i, on.length)).join('')}
      <div class="lf-sec"><span>未显示 ${off.length}</span>${off.length ? '<button type="button" class="lf-mini" data-act="lf-all">全选</button>' : ''}</div>
      ${off.length ? off.map((f) => lfRowHTML(f, false, -1, 0)).join('') : '<div class="lf-hint">所有字段都已经显示</div>'}
      <div class="lf-hint">第一条为标题，其余按可用宽度自动换行，行高随内容变化。</div>
    `;
  }

  function fieldDefSheet(lib, field) {
    const editing = !!field;
    const wasSys = editing && isSysField(field);
    const isChoice = (t) => t === 'select' || t === 'multiselect';
    const isAuto = (t) => t === 'autonumber';
    const isSys = (t) => t === 'autonumber' || t === 'createdat' || t === 'updatedat';
    const otherLibs = state.libraries.filter((l) => l.id !== lib.id);
    const pk = editing ? field.pick : null;
    const pkLibId = (pk && pk.libId) || (otherLibs[0] ? otherLibs[0].id : '');
    const pkSrcLib = libById(pkLibId);
    const pkKind = (pk && pk.kind) || 'value';
    const pkFields = pkSrcLib ? pkSrcLib.fields : [];
    const pkFieldId = (pk && pk.fieldId) || (pkFields[0] ? pkFields[0].id : '');
    const pkFilterId = (pk && pk.filterFieldId) || '';
    const pkDeduct = !!(pk && pk.deduct);
    const pickHTML = otherLibs.length ? `
        <div class="pk-box" id="pkBox">
          <div class="pk-head">关联设置</div>
          <div class="pf-row">
            <label>关联库</label>
            <select name="pkLib">${otherLibs.map((l) => `<option value="${l.id}" ${l.id === pkLibId ? 'selected' : ''}>${esc(l.name)}</option>`).join('')}</select>
          </div>
          <div class="pf-row">
            <label>选择什么</label>
            <select name="pkKind">
              <option value="value" ${pkKind === 'value' ? 'selected' : ''}>某个字段的值（如型号）</option>
              <option value="sku" ${pkKind === 'sku' ? 'selected' : ''}>规格（球镜 + 柱镜）</option>
            </select>
          </div>
          <div class="pf-row" id="pkFieldRow">
            <label>来源字段</label>
            <select name="pkField">${pkFields.map((f) => `<option value="${f.id}" ${f.id === pkFieldId ? 'selected' : ''}>${esc(f.name)}</option>`).join('')}</select>
          </div>
          <div class="pf-row">
            <label>按本库字段过滤</label>
            <select name="pkFilter">
              <option value="">不过滤</option>
              ${lib.fields.filter((f) => !editing || f.id !== field.id).map((f) => `<option value="${f.id}" ${f.id === pkFilterId ? 'selected' : ''}>${esc(f.name)}</option>`).join('')}
            </select>
          </div>
          <div class="pf-row">
            <label>选中后</label>
            <select name="pkDeduct">
              <option value="" ${!pkDeduct ? 'selected' : ''}>只写入文字</option>
              <option value="1" ${pkDeduct ? 'selected' : ''}>扣减来源库库存 1</option>
            </select>
          </div>
          <div class="lf-hint">选中的值只作为文字写进记录，之后修改或删除来源库都不会改动这些数据。</div>
        </div>` : '';
    openSheet({
      title: editing ? '编辑字段' : '添加字段',
      desc: editing
        ? (field.type === 'formula'
          ? `${lib.name} · ${field.expr || '（空公式）'}`
          : `${lib.name} · ${fieldUsage(lib, field.id)} 条记录有值`)
        : '字段会出现在这个库的每一条记录里。',
      fields: [
        { name: 'name', label: '字段名', value: editing ? field.name : '', placeholder: '例如：生日、邮箱、度数', required: true },
        { name: 'type', label: '类型', type: 'select', value: editing ? field.type : 'text', options: FIELD_TYPES },
        { name: 'unit', label: '单位（可选）', value: editing ? (field.unit || '') : '', placeholder: '例如：D、元、片' },
        {
          name: 'options', label: '选项（单选 / 多选使用）', type: 'textarea',
          value: editing && field.options ? field.options.join('\n') : '',
          placeholder: '每行一个选项，例如：\n远用\n近用\n渐进',
          hint: '只有类型是「单选 / 多选」时才生效'
        },
        {
          name: 'prefix', label: '编号前缀（可选）',
          value: editing ? (field.prefix || '') : '',
          placeholder: '例如：NO- 或 A',
          hint: '只影响显示，不写进数据'
        },
        {
          name: 'start', label: '起始编号', type: 'number', step: '1', min: '1',
          value: editing && field.start ? field.start : '1',
          hint: '新记录从这个数字开始往后排'
        },
        {
          name: 'expr', label: '公式', type: 'textarea',
          value: editing ? (field.expr || '') : '',
          placeholder: "例如：se('OD')",
          hint: '可用：num、sph、cyl、se、text、fmt、round、cond，字段名用引号括起来'
        }
      ],
      extraHTML: `
        <div class="fx-box" id="fxBox">
          <div class="fx-presets">
            <button type="button" class="fx-chip" data-act="fx-preset" data-name="等效球镜" data-expr="se('OD')">单眼等效球镜</button>
            <button type="button" class="fx-chip" data-act="fx-preset" data-name="等效球镜" data-expr="'OD ' + fmt(se('OD')) + ' / OS ' + fmt(se('OS'))">双眼等效球镜</button>
            <button type="button" class="fx-chip" data-act="fx-preset" data-name="球镜合计" data-expr="sph('OD') + sph('OS')">双眼球镜相加</button>
            <button type="button" class="fx-chip" data-act="fx-preset" data-name="备注摘要" data-expr="text('姓名') + ' · ' + text('用途')">文本拼接</button>
          </div>
          <div class="fx-preview" id="fxPreview"></div>
        </div>
        ${pickHTML}
      `,
      submitText: editing ? '保存' : '添加',
      onSubmit(d) {
        const name = String(d.name || '').trim();
        if (!name) return false;
        const dup = lib.fields.some((f) => f.name.trim() === name && (!editing || f.id !== field.id));
        if (dup) { toast('已经有同名字段了'); return false; }
        const unit = String(d.unit || '').trim() || undefined;
        const options = isChoice(d.type)
          ? String(d.options || '').split('\n').map((s) => s.trim()).filter(Boolean)
          : undefined;
        if (isChoice(d.type) && (!options || !options.length)) { toast('请至少填一个选项'); return false; }
        const prefix = isAuto(d.type) ? (String(d.prefix || '').trim() || undefined) : undefined;
        const start = isAuto(d.type) ? (parseInt(d.start, 10) || 1) : undefined;
        const expr = d.type === 'formula' ? String(d.expr || '').trim() : undefined;
        if (d.type === 'formula' && !expr) { toast('请填写公式'); return false; }
        const pick = d.type === 'pick' ? {
          libId: String(d.pkLib || ''),
          kind: d.pkKind === 'sku' ? 'sku' : 'value',
          fieldId: d.pkKind === 'sku' ? '' : String(d.pkField || ''),
          filterFieldId: String(d.pkFilter || ''),
          deduct: d.pkDeduct === '1',
          qty: 1
        } : undefined;
        if (pick) {
          const src = libById(pick.libId);
          if (!src) { toast('请选择关联来源库'); return false; }
          if (pick.kind === 'value' && !fieldById(src, pick.fieldId)) { toast('请选择来源字段'); return false; }
          if (pick.kind === 'sku' && !matrixConfig(src)) { toast('来源库要先有库存表格（矩阵视图）'); return false; }
        }
        let target = field;
        if (editing) {
          field.name = name;
          field.type = d.type;
          field.unit = unit;
          field.options = options;
          field.prefix = prefix;
          field.start = start;
          field.expr = expr;
          field.pick = pick;
          toast(`已更新字段「${name}」`);
        } else {
          target = { id: uid('f_'), name, type: d.type, unit, options, prefix, start, expr, pick };
          lib.fields.push(target);
          toast(`已添加字段「${name}」`);
        }
        save();
        render();
        if (lib.entries.length && !wasSys) {
          const n = lib.entries.length;
          if (isAuto(d.type)) {
            setTimeout(() => {
              confirmSheet({
                title: `给现有 ${n} 条记录补编号？`,
                desc: '按日期从早到晚依次编号，该字段现有的内容会被覆盖。也可以稍后再补。',
                okText: '补编号',
                onOk() {
                  fillAutoNumbers(lib, target);
                  save();
                  render();
                  toast(`已编号 ${n} 条记录`);
                  setTimeout(() => fieldsSheet(lib), 300);
                }
              });
            }, 320);
            return;
          }
          if (d.type === 'createdat' || d.type === 'updatedat') {
            const df = d.type === 'createdat' ? dateFieldOf(lib) : null;
            lib.entries.forEach((e) => {
              if (d.type === 'createdat') {
                const fromData = df ? parseDateVal(e.values[df.id]) : null;
                e.values[target.id] = fmtDay(fromData || e.createdAt || e.updatedAt);
              } else {
                e.values[target.id] = fmtStamp(e.updatedAt);
              }
            });
            save();
            toast(df ? `已按「${df.name}」回填 ${n} 条` : `已按记录时间回填 ${n} 条`);
          }
        }
        setTimeout(() => fieldsSheet(lib), 320);
      },
      onMount(form) {
        const typeSel = form.querySelector('select[name=type]');
        const optBox = form.querySelector('textarea[name=options]').closest('.form-field');
        const preBox = form.querySelector('input[name=prefix]').closest('.form-field');
        const startBox = form.querySelector('input[name=start]').closest('.form-field');
        const unitBox = form.querySelector('input[name=unit]').closest('.form-field');
        const exprBox = form.querySelector('textarea[name=expr]').closest('.form-field');
        const fxBox = form.querySelector('#fxBox');
        const preview = form.querySelector('#fxPreview');
        const pkBox = form.querySelector('#pkBox');
        const pkLibSel = form.querySelector('select[name=pkLib]');
        const pkKindSel = form.querySelector('select[name=pkKind]');
        const pkFieldSel = form.querySelector('select[name=pkField]');
        const pkFieldRow = form.querySelector('#pkFieldRow');
        const fillPkFields = () => {
          const src = pkLibSel ? libById(pkLibSel.value) : null;
          if (pkFieldRow) pkFieldRow.style.display = pkKindSel.value === 'sku' ? 'none' : '';
          if (!src || !pkFieldSel) return;
          const keep = pkFieldSel.value;
          pkFieldSel.innerHTML = src.fields.map((f) => `<option value="${f.id}">${esc(f.name)}</option>`).join('');
          if (src.fields.some((f) => f.id === keep)) pkFieldSel.value = keep;
        };
        const sample = lib.entries.find((e) => {
          const od = lib.fields.find((x) => x.name.trim() === 'OD');
          return od && String(e.values[od.id] || '').trim();
        }) || lib.entries[0];
        const refresh = () => {
          const on = typeSel.value === 'formula';
          exprBox.style.display = on ? '' : 'none';
          fxBox.style.display = on ? '' : 'none';
          if (!on) return;
          if (!sample) {
            preview.innerHTML = '<span class="fx-none">库里还没有记录，保存后才能预览</span>';
            return;
          }
          const out = formulaValue(lib, sample, {
            expr: form.querySelector('textarea[name=expr]').value, unit: ''
          });
          preview.innerHTML = `<span class="fx-sample">示例 · ${esc(titleOf(lib, sample))}</span><b class="${out ? '' : 'fx-none'}">${esc(out || '无结果')}</b>`;
        };
        const sync = () => {
          const t = typeSel.value;
          optBox.style.display = isChoice(t) ? '' : 'none';
          preBox.style.display = isAuto(t) ? '' : 'none';
          startBox.style.display = isAuto(t) ? '' : 'none';
          unitBox.style.display = isSys(t) ? 'none' : '';
          if (pkBox) pkBox.style.display = t === 'pick' ? '' : 'none';
          refresh();
        };
        sync();
        typeSel.addEventListener('change', sync);
        form.querySelector('textarea[name=expr]').addEventListener('input', refresh);

        if (pkLibSel) {
          pkLibSel.addEventListener('change', fillPkFields);
          pkKindSel.addEventListener('change', fillPkFields);
          fillPkFields();
        }
      }
    });
  }

  function deleteFieldDef(lib, field) {
    const used = fieldUsage(lib, field.id);
    closeSheet();
    setTimeout(() => {
      confirmSheet({
        title: `删除字段「${field.name}」？`,
        desc: used
          ? `这个字段在 ${used} 条记录里有值，删除后这些内容会一起消失，且无法恢复。`
          : '这个字段还没有任何内容，删除后不可恢复。',
        onOk() {
          lib.fields = lib.fields.filter((f) => f.id !== field.id);
          lib.entries.forEach((e) => { delete e.values[field.id]; });
          save();
          toast('已删除字段');
          render();
          setTimeout(() => fieldsSheet(lib), 320);
        }
      });
    }, 300);
  }

  function fieldSpecFor(f, rawValue, required = false) {
    const raw = String(rawValue ?? '');
    const hint = f.unit ? '单位：' + f.unit : '';
    let spec;
    if (f.type === 'autonumber') {
      return {
        label: f.name, required: false, type: 'readonly',
        value: raw ? (f.prefix || '') + raw : '',
        placeholder: '保存后自动生成',
        hint: '由系统自动编号，不能手动修改'
      };
    }
    if (f.type === 'createdat' || f.type === 'updatedat') {
      return {
        label: f.name, required: false, type: 'readonly',
        value: raw,
        placeholder: '保存后自动生成',
        hint: f.type === 'createdat' ? '自动记录创建时间' : '每次保存都会刷新'
      };
    }
    if (f.type === 'formula') {
      return {
        label: f.name, required: false, type: 'readonly',
        value: '',
        placeholder: '由公式计算',
        hint: '公式结果，不能手动修改'
      };
    }
    if (f.type === 'pick') {
      spec = {
        type: 'text', value: raw, placeholder: '点击选择或手动填写',
        hint: f.pick && f.pick.deduct ? '可以从来源库挑，也可以手动填；挑选后会扣减库存' : '可以从来源库挑，也可以手动填'
      };
    } else if (f.type === 'select') {
      spec = { type: 'select', value: raw, options: [{ value: '', label: '（未填）' }, ...(f.options || [])], hint };
    } else if (f.type === 'multiselect') {
      spec = { type: 'checkboxgroup', value: raw, options: f.options || [], hint: hint || '可以多选' };
    } else if (f.type === 'checkbox') {
      spec = {
        type: 'select', value: raw === '' ? '' : (isTrueVal(raw) ? '1' : '0'), hint,
        options: [{ value: '', label: '（未填）' }, { value: '1', label: '是' }, { value: '0', label: '否' }]
      };
    } else if (f.type === 'rating') {
      spec = { type: 'rating', value: raw, hint: hint || '点击星标评分' };
    } else if (f.type === 'date') {
      spec = { type: 'date', value: toISODate(raw), hint };
    } else if (f.type === 'number') {
      spec = { type: 'number', value: raw, step: 'any', hint };
    } else if (f.type === 'tel') {
      spec = { type: 'tel', value: raw, hint };
    } else if (f.type === 'longtext') {
      spec = { type: 'textarea', value: raw, hint };
    } else if (f.type === 'url' || f.type === 'email') {
      spec = { type: f.type, value: raw, hint };
    } else {
      spec = { type: 'text', value: raw, hint };
    }
    return { label: f.name, required, ...spec };
  }

  function normalizeFieldValue(f, v) {
    if (Array.isArray(v)) v = v.join(MULTI_SEP);
    v = String(v ?? '').trim();
    if (f.type === 'date' && v) v = v.replace(/-/g, '/');
    return v;
  }

  function entrySheet(entry) {
    const lib = libById(ui.route.name === 'entry' ? ui.route.libId : ui.route.id);
    if (!lib) return;
    const editing = !!entry;
    const fields = lib.fields.map((f, i) => {
      const spec = { ...fieldSpecFor(f, editing ? entry.values[f.id] : '', i === 0), name: 'v_' + f.id };
      if (f.type === 'formula') spec.value = editing ? formulaValue(lib, entry, f) : '';
      return spec;
    });    const pickFields = lib.fields.filter((f) => f.type === 'pick');
    const picked = {};
    openSheet({
      title: editing ? '编辑条目' : '新增条目',
      desc: lib.name,
      fields,
      onMount(form) {
        pickFields.forEach((f) => {
          const input = form.querySelector(`[name="v_${f.id}"]`);
          if (!input) return;
          const c = pickCfg(lib, f);
          if (!c) return;
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'pick-trigger';
          btn.innerHTML = `${icon('link', 15)}<span>从「${esc(c.src.name)}」选择</span>`;
          input.insertAdjacentElement('afterend', btn);
          btn.addEventListener('click', () => {
            const filterEl = c.p.filterFieldId ? form.querySelector(`[name="v_${c.p.filterFieldId}"]`) : null;
            openPicker({
              title: `${f.name} · 选择`,
              desc: pickDesc(c, f),
              options: pickOptions(lib, f, { values: {} }, filterEl ? filterEl.value : ''),
              selected: input.value,
              onPick(opt) { input.value = opt.value; picked[f.id] = true; }
            });
          });
        });
      },
      onSubmit(d) {
        const values = {};
        lib.fields.forEach((f) => {
          if (isSysField(f)) {
            values[f.id] = editing ? String(entry.values[f.id] ?? '') : '';
            return;
          }
          values[f.id] = normalizeFieldValue(f, d['v_' + f.id]);
        });
        let target = entry;
        if (editing) {
          entry.values = values;
          entry.updatedAt = now();
        } else {
          target = { id: uid('e_'), values, favorite: 0, createdAt: now(), updatedAt: now() };
          lib.entries.unshift(target);
        }
        stampAutoFields(lib, target, !editing);
        const stockChanges = applyPickStock(lib, target, target.values, picked);
        save();
        toast('已保存');
        stockToast(stockChanges);
        if (!editing) {
          ui.route = { name: 'entry', libId: lib.id, id: target.id };
          ui.stack = [{ name: 'home' }, { name: 'lib', id: lib.id }];
        }
        render();
      }
    });
  }

  function isTrueVal(v) {
    const s = String(v ?? '').trim().toLowerCase();
    return s === '1' || s === 'true' || s === '是' || s === 'on' || s === 'yes';
  }

  function toISODate(v) {
    const ts = parseDateVal(v);
    if (!ts) return '';
    const d = new Date(ts);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  function editFieldSheet(fid) {
    const lib = libById(ui.route.libId);
    const entry = lib && lib.entries.find((e) => e.id === ui.route.id);
    const field = lib && fieldById(lib, fid);
    if (!lib || !entry || !field) return;
    const c = field.type === 'pick' ? pickCfg(lib, field) : null;
    const picked = {};
    openSheet({
      title: field.name,
      desc: lib.name + ' · ' + titleOf(lib, entry),
      fields: [{ name: 'value', ...fieldSpecFor(field, entry.values[field.id]) }],
      extraHTML: c ? `
        <button type="button" class="pick-trigger" id="pickTrigger">
          ${icon('link', 15)}
          <span>从「${esc(c.src.name)}」选择</span>
        </button>
        <div class="lf-hint">${esc(pickDesc(c, field))}${c.p.deduct ? '，选择后会扣减对应库存' : ''}</div>
      ` : '',
      onMount(form) {
        const btn = form.querySelector('#pickTrigger');
        if (!btn) return;
        btn.addEventListener('click', () => {
          const filterField = c.p.filterFieldId && fieldById(lib, c.p.filterFieldId);
          openPicker({
            title: `${field.name} · 选择`,
            desc: pickDesc(c, field),
            options: pickOptions(lib, field, entry),
            selected: form.querySelector('[name="value"]').value,
            onPick(opt) {
              form.querySelector('[name="value"]').value = opt.value;
              picked[field.id] = true;
              if (filterField) toast(`已选择 ${opt.label}`);
            }
          });
        });
      },
      onSubmit(d) {
        const v = normalizeFieldValue(field, d.value);
        const old = entry.values[field.id];
        entry.values[field.id] = v;
        entry.updatedAt = now();
        stampAutoFields(lib, entry, false);
        const stockChanges = applyPickStock(lib, entry, entry.values, picked);
        save();
        toast('已更新');
        stockToast(stockChanges);
        render();
      }
    });
  }

  function mergePersonSheet(lib, entry) {
    const pf = phoneFieldOf(lib);
    const nf = nameFieldOf(lib);
    if (!pf || !nf) return;
    const phone = normPhone(entry.values[pf.id]);
    const myName = nameOf(lib, entry);
    const counts = new Map();
    lib.entries.forEach((e) => {
      if (normPhone(e.values[pf.id]) !== phone) return;
      const n = nameOf(lib, e);
      if (!n || n === myName) return;
      counts.set(n, (counts.get(n) || 0) + 1);
    });
    const options = [...counts.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([n, c]) => ({ value: n, label: `${n}（${c} 条记录）` }));
    if (!options.length) { toast('这个号码下没有其他姓名'); return; }
    openSheet({
      title: '合并为同一个人',
      desc: `把所选姓名的记录改成「${myName}」，之后会归到同一个人的验光历史里。`,
      fields: [{
        name: 'target', label: '要合并的姓名', type: 'select', options,
        hint: '只处理共用这个电话号码的记录，其它号码不受影响'
      }, {
        name: 'keep', label: '合并后保留哪个名字', type: 'select',
        options: [
          { value: 'current', label: `保留「${myName}」` },
          { value: 'target', label: `保留所选姓名` }
        ]
      }],
      submitText: '合并',
      onSubmit(d) {
        const target = String(d.target || '');
        const keepCurrent = d.keep !== 'target';
        const fromName = keepCurrent ? target : myName;
        const toName = keepCurrent ? myName : target;
        let n = 0;
        lib.entries.forEach((e) => {
          if (normPhone(e.values[pf.id]) !== phone) return;
          if (nameOf(lib, e) !== fromName) return;
          e.values[nf.id] = toName;
          e.updatedAt = now();
          n += 1;
        });
        save();
        toast(`已把 ${n} 条记录合并到「${toName}」`);
        render();
      }
    });
  }

  function aboutSheet() {
    openSheet({
      title: '原型使用说明',
      desc: '这是一个可点击的交互原型，数据只保存在本机浏览器。',
      fields: [],
      submitText: '知道了',
      extraHTML: `
        <div class="sheet-list" style="margin-bottom:6px">
          <div class="option-row"><span>${icon('database', 18)}</span><span><b>库与条目</b><br><span>首页可以新建库，库里可以新增、搜索、收藏条目</span></span></div>
          <div class="option-row"><span>${icon('bell', 18)}</span><span><b>库存提醒</b><br><span>镜片库存低于阈值时，首页会出现提醒</span></span></div>
          <div class="option-row"><span>${icon('link', 18)}</span><span><b>关联选择</b><br><span>客户库的镜片/OD/OS 直接从镜片库挑，选中后扣库存</span></span></div>
          <div class="option-row"><span>${icon('download', 18)}</span><span><b>备份</b><br><span>「我的 · 备份与恢复」可以导出 JSON，也可以上传到 WebDAV</span></span></div>
        </div>
      `,
      onSubmit() {}
    });
  }

  function backupSheet() {
    openSheet({
      title: '备份与恢复',
      desc: `${bkSizeText()} · 备份包含所有库、字段、记录和提醒规则`,
      submitText: '保存 WebDAV 设置',
      fields: [
        { name: 'url', label: 'WebDAV 地址', type: 'url', value: bk.url, placeholder: 'https://dav.example.com/remote.php/dav/files/me', hint: '填到账号根目录即可；不写协议时默认 https' },
        { name: 'port', label: '端口', value: bk.port, placeholder: '默认端口（如 443 / 80）', hint: '自建 NAS 或反向代理才需要，填了会覆盖地址里的端口' },
        { name: 'user', label: '用户名', value: bk.user, placeholder: '留空表示不需要登录' },
        { name: 'pass', label: '密码 / 应用密码', type: 'password', value: bk.pass, placeholder: '建议用应用专用密码' },
        { name: 'path', label: '远端目录', value: bk.path, placeholder: 'MyDB', hint: '备份文件会保存为 目录/memento-backup.json' },
        { name: 'auto', label: '自动备份', type: 'select', value: bk.auto ? '1' : '', options: [{ value: '', label: '关闭' }, { value: '1', label: '每次修改后自动上传（约 4 秒）' }] }
      ],
      extraHTML: `
        <div class="bk-box">
          <div class="bk-head">本地备份</div>
          <div class="bk-row">
            <button type="button" class="bk-btn" data-act="export">导出备份…</button>
            <button type="button" class="bk-btn ghost" data-act="import">从文件恢复</button>
          </div>
          <div class="bk-meta">上次导出：${esc(bkStamp(bk.lastLocalAt))}</div>
          <div class="lf-hint">点「导出备份…」会打开系统保存窗口，可以自己选目录和文件名；浏览器不支持时会退回默认下载目录。</div>
        </div>
        <div class="bk-box">
          <div class="bk-head">云端 WebDAV</div>
          <div class="bk-row">
            <button type="button" class="bk-btn" data-act="wd-backup">立即备份到云端</button>
            <button type="button" class="bk-btn ghost" data-act="wd-restore">从云端恢复</button>
          </div>
          <div class="bk-meta" id="bkCloudMeta">上次上传：${esc(bkStamp(bk.lastCloudAt))}</div>
          <div class="lf-hint">支持 Nextcloud、坚果云、群晖等标准 WebDAV。用网页原型时，服务器需要允许跨域请求（CORS）；移植到 App 后没有这个限制。</div>
        </div>
      `,
      onSubmit(d) {
        bk.url = String(d.url || '').trim();
        bk.port = String(d.port || '').trim().replace(/[^0-9]/g, '');
        bk.user = String(d.user || '').trim();
        bk.pass = String(d.pass || '');
        bk.path = String(d.path || '').trim() || 'Memento';
        bk.auto = d.auto === '1';
        saveBk();
        toast('已保存备份设置');
      }
    });
  }

  async function wdBackupNow() {
    const btn = document.querySelector('[data-act="wd-backup"]');
    if (btn) { btn.disabled = true; btn.textContent = '上传中…'; }
    const ok = await bkPut(false);
    if (btn) { btn.disabled = false; btn.textContent = '立即备份到云端'; }
    const meta = document.querySelector('#bkCloudMeta');
    if (meta) meta.textContent = '上次上传：' + bkStamp(bk.lastCloudAt);
    if (ok) toast('已备份到云端 · ' + bkSizeText());
  }

  async function wdRestoreNow() {
    const payload = await bkGet();
    if (!payload) return;
    const data = payload && payload.data && Array.isArray(payload.data.libraries) ? payload.data : payload;
    if (!data || !Array.isArray(data.libraries)) { toast('云端文件格式不正确'); return; }
    const n = data.libraries.reduce((a, l) => a + (l.entries ? l.entries.length : 0), 0);
    const at = payload.exportedAt ? Date.parse(payload.exportedAt) : 0;
    confirmSheet({
      title: '从云端恢复？',
      desc: `云端备份${at ? '（' + bkStamp(at) + '）' : ''}包含 ${data.libraries.length} 个库、${n} 条记录，会覆盖当前所有数据。`,
      okText: '恢复',
      onOk() {
        try {
          applyBackup(payload);
          toast('已从云端恢复');
        } catch (e) {
          toast('云端文件格式不正确');
        }
      }
    });
  }

  /* ---------------- actions ---------------- */
  function toast(msg) {    const el = $('#toast');
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(toast._t);
    toast._t = setTimeout(() => el.classList.remove('show'), 1900);
  }

  function backupFileName() {
    const d = new Date();
    return `memento-backup-${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}.json`;
  }

  function markLocalExport() {
    bk.lastLocalAt = now();
    saveBk();
    const meta = document.querySelector('.bk-meta');
    if (meta) meta.textContent = '上次导出：' + bkStamp(bk.lastLocalAt);
  }

  async function exportData() {
    const text = bkText();
    const name = backupFileName();
    if (typeof window.showSaveFilePicker === 'function') {
      try {
        const handle = await window.showSaveFilePicker({
          suggestedName: name,
          types: [{ description: 'MyDB 备份', accept: { 'application/json': ['.json'] } }]
        });
        const writable = await handle.createWritable();
        await writable.write(new Blob([text], { type: 'application/json' }));
        await writable.close();
        markLocalExport();
        toast('已保存到你选择的目录');
        return;
      } catch (e) {
        if (e && e.name === 'AbortError') return;
        // 不支持或没有权限时退回浏览器默认下载目录
      }
    }
    const blob = new Blob([text], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 3000);
    markLocalExport();
    toast('浏览器不支持选择目录，已存到默认下载目录');
  }

  function importData() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'application/json,.json';
    input.onchange = () => {
      const file = input.files && input.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        try {
          applyBackup(JSON.parse(String(reader.result)));
          toast('已导入备份');
        } catch (e) {
          toast('文件格式不正确');
        }
      };
      reader.readAsText(file);
    };
    input.click();
  }

  /* ---------------- csv ---------------- */
  function parseCSV(text) {
    if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
    const rows = [];
    let row = [];
    let field = '';
    let inQuotes = false;
    for (let i = 0; i < text.length; i++) {
      const c = text[i];
      if (inQuotes) {
        if (c === '"') {
          if (text[i + 1] === '"') { field += '"'; i++; }
          else inQuotes = false;
        } else if (c === '\r') {
          if (text[i + 1] === '\n') { field += '\n'; i++; }
          else field += '\n';
        } else {
          field += c;
        }
      } else if (c === '"') {
        inQuotes = true;
      } else if (c === ',') {
        row.push(field);
        field = '';
      } else if (c === '\r') {
        /* wait for \n */
      } else if (c === '\n') {
        row.push(field);
        rows.push(row);
        row = [];
        field = '';
      } else {
        field += c;
      }
    }
    if (field !== '' || row.length) {
      row.push(field);
      rows.push(row);
    }
    return rows;
  }

  function csvEscape(v) {
    const s = String(v ?? '');
    return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
  }

  function exportCSV(lib) {
    const header = lib.fields.map((f) => csvEscape(f.name)).join(',');
    const lines = [header];
    lib.entries.forEach((entry) => {
      lines.push(lib.fields.map((f) => csvEscape(
        f.type === 'formula' ? formulaValue(lib, entry, f) : (entry.values[f.id] ?? '')
      )).join(','));
    });
    const csv = '\uFEFF' + lines.join('\r\n') + '\r\n';
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = lib.name.replace(/[\\/:*?"<>|]/g, '_') + '-' + new Date().toISOString().slice(0, 10) + '.csv';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 3000);
    toast('已导出 ' + lib.entries.length + ' 条记录');
  }

  function inferFieldType(header, rows, idx) {
    if (/电话|手机/.test(header)) return 'tel';
    if (/备注|说明|地址/.test(header)) return 'longtext';
    const vals = rows.map((r) => String(r[idx] ?? '').trim()).filter(Boolean);
    if (vals.length && vals.every((v) => /^-?\d+(\.\d+)?$/.test(v.replace(/,/g, '')))) return 'number';
    return 'text';
  }

  function duplicateInfo(lib) {
    const userFields = lib.fields.filter((f) => !['autonumber', 'createdat', 'updatedat', 'formula'].includes(f.type));
    const map = new Map();
    lib.entries.forEach((e) => {
      const k = userFields.map((f) => String(e.values[f.id] ?? '').trim()).join('\u0001');
      if (!map.has(k)) map.set(k, []);
      map.get(k).push(e);
    });
    const groups = [...map.values()].filter((g) => g.length > 1);
    return { groups, extra: groups.reduce((s, g) => s + g.length - 1, 0) };
  }

  function libraryDataSheet(lib) {
    const dup = duplicateInfo(lib);
    openSheet({
      title: 'CSV 导入 / 导出',
      desc: lib.name + ' · 共 ' + lib.entries.length + ' 条记录',
      fields: [],
      submitText: '完成',
      extraHTML: `
        <div class="sheet-list">
          <button type="button" class="option-row" data-act="export-csv">
            <span>${icon('download', 18)}</span>
            <span><b>导出 CSV</b><br><span>按当前字段导出，Excel 可直接打开</span></span>
            <span class="go">${icon('chevron', 16)}</span>
          </button>
          <button type="button" class="option-row" data-act="import-csv">
            <span>${icon('upload', 18)}</span>
            <span><b>导入 CSV</b><br><span>按表头自动匹配字段，缺失的列自动新增</span></span>
            <span class="go">${icon('chevron', 16)}</span>
          </button>
          <button type="button" class="option-row" data-act="dedupe">
            <span>${icon('refresh', 18)}</span>
            <span><b>清理重复记录</b><br><span>${dup.extra ? `发现 ${dup.groups.length} 组、共 ${dup.extra} 条多余` : '没有发现完全重复的记录'}</span></span>
            <span class="go">${icon('chevron', 16)}</span>
          </button>
        </div>
        <div class="hint" style="margin-top:10px;line-height:1.6">当前列：${lib.fields.map((f) => esc(f.name)).join('、')}</div>
      `,
      onSubmit() {}
    });
  }

  function csvImportSheet(lib) {
    let parsed = null;
    openSheet({
      title: '导入 CSV',
      desc: '支持带引号、含逗号或换行的单元格，表头会自动匹配字段。',
      fields: [
        { name: 'mode', label: '导入方式', type: 'select', options: [
          { value: 'append', label: '追加到现有记录' },
          { value: 'skip', label: '追加，跳过重复行（按姓名+电话+时间）' },
          { value: 'replace', label: '清空后替换' }
        ] }
      ],
      extraHTML: `
        <div class="form-field">
          <label>CSV 文件</label>
          <input id="csvFileInput" type="file" accept=".csv,text/csv" style="padding:9px;background:#fff" />
        </div>
        <div class="csv-preview" id="csvPreview">还没有选择文件。建议使用 UTF-8 编码的 CSV。</div>
      `,
      submitText: '开始导入',
      onSubmit(d) {
        if (!parsed) { toast('请先选择 CSV 文件'); return false; }
        const matched = parsed.headers.filter((h) => lib.fields.some((f) => f.name.trim() === h));
        if (!matched.length) { toast('CSV 表头没有匹配到任何字段'); return false; }
        const count = importCSV(lib, parsed, d.mode);
        toast(count.skipped ? `新增 ${count.added} 条，跳过 ${count.skipped} 条重复` : `已导入 ${count.added} 条记录`);
        ui.limit = 60;
        render();
      }
    });

    const input = $('#csvFileInput');
    const preview = $('#csvPreview');
    if (!input) return;
    input.addEventListener('change', () => {
      const file = input.files && input.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        const rows = parseCSV(String(reader.result));
        const headers = (rows.shift() || []).map((h) => h.replace(/^\uFEFF/, '').trim());
        const dataRows = rows.filter((r) => r.some((c) => String(c).trim() !== ''));
        const matched = headers.map((h) => lib.fields.find((f) => f.name.trim() === h));
        const missing = headers.filter((h, i) => h && !matched[i]);
        parsed = { headers, rows: dataRows };
        preview.innerHTML = `
          <div>已读取 <b>${dataRows.length}</b> 行 × <b>${headers.length}</b> 列</div>
          <div style="margin-top:4px">匹配字段 <b>${matched.filter(Boolean).length}</b> 个${missing.length ? `，将新增 <b>${missing.length}</b> 列` : ''}</div>
          <div class="csv-cols">${headers.map((h, i) => h ? `<span class="${matched[i] ? '' : 'new'}">${esc(h)}${matched[i] ? '' : ' +'}</span>` : '').join('')}</div>
          ${missing.length ? '<div style="margin-top:7px;color:var(--amber)">标记 + 的列会自动加成新字段。</div>' : ''}
        `;
      };
      reader.readAsText(file, 'utf-8');
    });
  }

  function identityKey(fields, values) {
    const keys = fields.filter((f) => /姓名|电话|时间|日期|NO\.?/i.test(f.name));
    const use = keys.length ? keys : fields.slice(0, 1);
    return use.map((f) => values[f.id] || '').join('\u0001').toLowerCase();
  }

  function importCSV(lib, parsed, mode) {
    const { headers, rows } = parsed;
    const mapping = headers.map((h) => lib.fields.find((f) => f.name.trim() === h));
    headers.forEach((h, i) => {
      if (h && !mapping[i]) {
        const field = { id: uid('f_'), name: h, type: inferFieldType(h, rows, i) };
        lib.fields.push(field);
        mapping[i] = field;
      }
    });
    if (mode === 'replace') lib.entries = [];
    const seen = new Set();
    if (mode === 'skip') lib.entries.forEach((e) => seen.add(identityKey(lib.fields, e.values)));
    const created = [];
    let skipped = 0;
    rows.forEach((cols) => {
      if (!cols.some((c) => String(c).trim() !== '')) return;
      const values = {};
      mapping.forEach((field, i) => {
        if (!field || field.type === 'formula') return;
        const v = String(cols[i] ?? '').trim();
        if (v !== '') values[field.id] = v;
      });
      if (mode === 'skip') {
        const key = identityKey(lib.fields, values);
        if (seen.has(key)) { skipped++; return; }
        seen.add(key);
      }
      created.push({ id: uid('e_'), values, favorite: 0, createdAt: now(), updatedAt: now() });
    });
    // 自动编号 / 自动时间：导入的记录按行序补号
    if (created.length) {
      const auto = lib.fields.filter(isSysField);
      const df = dateFieldOf(lib);
      const seq = {};
      auto.forEach((f) => { if (f.type === 'autonumber') seq[f.id] = nextAutoNumber(lib, f); });
      created.forEach((e) => {
        auto.forEach((f) => {
          if (f.type === 'autonumber') e.values[f.id] = String(seq[f.id]++);
          else if (f.type === 'createdat') {
            const fromData = df ? parseDateVal(e.values[df.id]) : null;
            e.values[f.id] = e.values[f.id] || fmtDay(fromData || e.createdAt);
          } else if (f.type === 'updatedat') e.values[f.id] = e.values[f.id] || fmtStamp(e.updatedAt);
        });
      });
    }
    lib.entries = created.concat(lib.entries);
    save();
    return { added: created.length, skipped };
  }

  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-act]');
    if (!el) return;
    const act = el.dataset.act;
    switch (act) {
      case 'tab': openTab(el.dataset.tab); break;
      case 'back': back(); break;
      case 'open-lib': {
        ui.limit = 60;
        ui.libSearch = '';
        ui.filter = 'all';
        const target = libById(el.dataset.id);
        if (target && target.view === 'matrix' && el.dataset.model) ui.matrixModel = el.dataset.model;
        go({ name: 'lib', id: el.dataset.id });
        break;
      }
      case 'open-entry':
        if (ui.route.name === 'lib') go({ name: 'entry', libId: ui.route.id, id: el.dataset.id });
        else go({ name: 'entry', libId: el.dataset.lib, id: el.dataset.id });
        break;
      case 'load-more':
        ui.limit += 60;
        render();
        break;
      case 'library-data': {
        const lib = libById(ui.route.id);
        if (lib) libraryDataSheet(lib);
        break;
      }
      case 'export-csv': {
        const lib = libById(ui.route.id);
        closeSheet();
        if (lib) setTimeout(() => exportCSV(lib), 280);
        break;
      }
      case 'import-csv': {
        const lib = libById(ui.route.id);
        closeSheet();
        if (lib) setTimeout(() => csvImportSheet(lib), 300);
        break;
      }
      case 'dedupe': {
        const lib = libById(ui.route.id);
        if (!lib) break;
        const info = duplicateInfo(lib);
        closeSheet();
        if (!info.extra) { toast('没有发现完全重复的记录'); break; }
        setTimeout(() => {
          confirmSheet({
            title: `删除 ${info.extra} 条重复记录？`,
            desc: `共 ${info.groups.length} 组内容完全相同的记录（自动编号、公式不计入比较）。每组保留最早的一条。`,
            okText: '清理',
            onOk() {
              const drop = new Set();
              info.groups.forEach((g) => {
                const keep = g.slice().sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0))[0];
                g.forEach((e) => { if (e !== keep) drop.add(e.id); });
              });
              lib.entries = lib.entries.filter((e) => !drop.has(e.id));
              save();
              render();
              toast(`已清理 ${drop.size} 条重复记录`);
              setTimeout(() => libraryDataSheet(lib), 320);
            }
          });
        }, 300);
        break;
      }
      case 'add-lib': addLibrarySheet(); break;
      case 'edit-lib': editLibrarySheet(); break;
      case 'fields': {
        const lib = libById(ui.route.id);
        if (lib) fieldsSheet(lib);
        break;
      }
      case 'lib-view': {
        const lib = libById(ui.route.id);
        if (!lib) break;
        lib.view = el.dataset.view === 'matrix' ? 'matrix' : 'list';
        save();
        render();
        break;
      }
      case 'mx-model':
        ui.matrixModel = el.dataset.model || '';
        render();
        break;
      case 'mx-cell': {
        const lib = libById(ui.route.id);
        if (lib) mxSheet(lib, el.dataset.model, toPower(el.dataset.sph), toPower(el.dataset.cyl));
        break;
      }
      case 'mx-set': {
        const input = $('#sheetForm input[name="qty"]');
        if (input) { input.value = el.dataset.qty; input.focus(); }
        break;
      }
      case 'alerts': {
        const lib = libById(ui.route.id);
        if (lib) alertsSheet(lib);
        break;
      }
      case 'alert-add': {
        const lib = libById(ui.route.id);
        if (!lib) break;
        closeSheet();
        setTimeout(() => alertRuleSheet(lib, null), 300);
        break;
      }
      case 'alert-edit': {
        const lib = libById(ui.route.id);
        if (!lib) break;
        const rid = el.dataset.rid;
        closeSheet();
        setTimeout(() => alertRuleSheet(lib, rid), 300);
        break;
      }
      case 'alert-del': {
        const lib = libById(ui.route.id);
        const al = lib && alertsOf(lib);
        if (!al) break;
        lib.alerts.rules = al.rules.filter((r) => r.id !== el.dataset.rid);
        save();
        render();
        closeSheet();
        setTimeout(() => alertsSheet(lib), 380);
        break;
      }
      case 'restock': go({ name: 'restock', id: ui.route.id }); break;
      case 'restock-text': { const lib = libById(ui.route.id); if (lib) restockTextSheet(lib); break; }
      case 'restock-copy': {
        const lib = libById(ui.route.id);
        if (lib) {
          const txt = restockText(lib, rsQty.plain, rsQty.cyl);
          navigator.clipboard.writeText(txt).then(
            () => toast('已复制补货文本'),
            () => toast('复制失败，请手动选择文本')
          );
        }
        break;
      }
      case 'restock-save': { const lib = libById(ui.route.id); if (lib) saveRestockTxt(lib); break; }
      case 'batch': { const lib = libById(ui.route.id); if (lib) batchSheet(lib); break; }
      case 'batch-apply': { const lib = libById(ui.route.id); if (lib) applyBatch(lib); break; }
      case 'batch-undo': undoBatch(); break;
      case 'search-use': {
        const lib = libById(el.dataset.id);
        ui.libSearch = el.dataset.q || '';
        ui.limit = 60;
        if (lib) {
          pushSearch(lib.id, ui.libSearch);
          ui.filter = 'all';
        }
        render();
        break;
      }
      case 'search-clear': clearSearch(el.dataset.id); break;
      case 'snap-undo': undoRestore(); break;
      case 'snap-drop': dropSnap(); break;
      case 'list-fields': {
        const lib = libById(ui.route.id);
        if (lib) listFieldsSheet(lib);
        break;
      }
      case 'lf-toggle': {
        const lib = libById(lfLibId);
        if (!lib || !lfDraft) break;
        const i = lfDraft.indexOf(el.dataset.fid);
        if (i >= 0) {
          if (lfDraft.length === 1) { toast('至少要保留一个显示字段'); break; }
          lfDraft.splice(i, 1);
        } else {
          lfDraft.push(el.dataset.fid);
        }
        paintListDraft();
        break;
      }
      case 'lf-names': {
        lfShowNames = !lfShowNames;
        const sw = el.closest('.lf-toggle')?.querySelector('.lf-switch');
        if (sw) sw.classList.toggle('on', lfShowNames);
        paintListDraft();
        break;
      }
      case 'lf-move': {
        if (!lfDraft) break;
        const i = lfDraft.indexOf(el.dataset.fid);
        const j = i + Number(el.dataset.dir);
        if (i < 0 || j < 0 || j >= lfDraft.length) break;
        const [moved] = lfDraft.splice(i, 1);
        lfDraft.splice(j, 0, moved);
        paintListDraft();
        break;
      }
      case 'lf-all': {
        const lib = libById(lfLibId);
        if (!lib || !lfDraft) break;
        lib.fields.forEach((f) => { if (!lfDraft.includes(f.id)) lfDraft.push(f.id); });
        paintListDraft();
        break;
      }
      case 'lf-none': {
        if (!lfDraft) break;
        lfDraft = lfDraft.slice(0, 1);
        paintListDraft();
        break;
      }
      case 'add-field-def': {
        const lib = libById(ui.route.id);
        if (lib) { closeSheet(); setTimeout(() => fieldDefSheet(lib, null), 300); }
        break;
      }
      case 'edit-field-def': {
        const lib = libById(ui.route.id);
        const field = lib && lib.fields.find((f) => f.id === el.dataset.fid);
        if (lib && field) { closeSheet(); setTimeout(() => fieldDefSheet(lib, field), 300); }
        break;
      }
      case 'move-field': {        const lib = libById(ui.route.id);
        if (!lib) break;
        const dir = Number(el.dataset.dir);
        const i = lib.fields.findIndex((f) => f.id === el.dataset.fid);
        const j = i + dir;
        if (i < 0 || j < 0 || j >= lib.fields.length) break;
        const [moved] = lib.fields.splice(i, 1);
        lib.fields.splice(j, 0, moved);
        save();
        render();
        fieldsSheet(lib);
        break;
      }
      case 'delete-field-def': {
        const lib = libById(ui.route.id);
        const field = lib && lib.fields.find((f) => f.id === el.dataset.fid);
        if (lib && field) deleteFieldDef(lib, field);
        break;
      }
      case 'renumber-field': {
        const lib = libById(ui.route.id);
        const field = lib && lib.fields.find((f) => f.id === el.dataset.fid);
        if (!lib || !field) break;
        closeSheet();
        setTimeout(() => {
          confirmSheet({
            title: `重新编号「${field.name}」？`,
            desc: `按日期从早到晚重排 ${lib.entries.length} 条记录，起始值 ${parseInt(field.start, 10) || 1}。`,
            okText: '重新编号',
            onOk() {
              fillAutoNumbers(lib, field);
              save();
              render();
              toast('已重新编号');
              setTimeout(() => fieldsSheet(lib), 300);
            }
          });
        }, 300);
        break;
      }
      case 'add-entry': entrySheet(null); break;
      case 'edit-field': editFieldSheet(el.dataset.fid); break;
      case 'fx-preset': {
        const ta = document.querySelector('#sheetForm textarea[name=expr]');
        if (!ta) break;
        ta.value = el.dataset.expr || '';
        ta.dispatchEvent(new Event('input', { bubbles: true }));
        const nameIn = document.querySelector('#sheetForm input[name=name]');
        if (nameIn && !nameIn.value.trim() && el.dataset.name) nameIn.value = el.dataset.name;
        break;
      }
      case 'filter': ui.filter = el.dataset.filter; ui.limit = 60; render(); break;
      case 'toggle-fav': {
        const lib = libById(ui.route.libId);
        const entry = lib && lib.entries.find((x) => x.id === ui.route.id);
        if (entry) { entry.favorite = entry.favorite ? 0 : 1; save(); render(); toast(entry.favorite ? '已收藏' : '已取消收藏'); }
        break;
      }
      case 'entry-menu': {
        const lib = libById(ui.route.libId);
        const entry = lib && lib.entries.find((x) => x.id === ui.route.id);
        if (!entry) break;
        openSheet({
          title: '条目操作',
          desc: titleOf(lib, entry),
          fields: [],
          submitText: '编辑条目',
          extraHTML: `<button type="button" class="settings-row danger" style="background:var(--danger-soft);border:1px solid #f5d2d2;border-radius:14px" data-act="delete-entry"><span class="s-ico">${icon('trash', 17)}</span><span class="s-main"><b>删除这条记录</b><span>删除后无法恢复</span></span></button>`,
          onSubmit() { closeSheet(); setTimeout(() => entrySheet(entry), 280); }
        });
        break;
      }
      case 'delete-entry': {
        const lib = libById(ui.route.libId);
        if (!lib) break;
        const entry = lib.entries.find((x) => x.id === ui.route.id);
        const name = entry ? titleOf(lib, entry) : '这条记录';
        closeSheet();
        setTimeout(() => confirmSheet({
          title: '删除记录？',
          desc: '「' + name + '」将被永久删除。',
          onOk() {
            lib.entries = lib.entries.filter((x) => x.id !== ui.route.id);
            save();
            toast('已删除');
            ui.route = { name: 'lib', id: lib.id };
            ui.stack = [{ name: 'home' }];
            render();
          }
        }), 280);
        break;
      }
      case 'delete-lib': {
        const lib = libById(ui.route.id);
        if (!lib) break;
        const name = lib.name;
        closeSheet();
        setTimeout(() => confirmSheet({
          title: '删除数据库？',
          desc: '「' + name + '」及其中 ' + lib.entries.length + ' 条记录将被永久删除。',
          onOk() {
            state.libraries = state.libraries.filter((x) => x.id !== lib.id);
            save();
            toast('已删除数据库');
            ui.route = { name: 'home' };
            ui.stack = [];
            ui.tab = 'home';
            render();
          }
        }), 280);
        break;
      }
      case 'merge-person': {
        const lib = libById(ui.route.libId);
        const entry = lib && lib.entries.find((e) => e.id === ui.route.id);
        if (lib && entry) mergePersonSheet(lib, entry);
        break;
      }
      case 'dismiss-reminder': {
        const r = state.reminders.find((x) => x.id === el.dataset.id);
        if (r) { r.read = true; save(); render(); }
        break;
      }
      case 'export': exportData(); break;
      case 'import': importData(); break;
      case 'backup': backupSheet(); break;
      case 'wd-backup': wdBackupNow(); break;
      case 'wd-restore': wdRestoreNow(); break;
      case 'reset':
        confirmSheet({
          title: '重置演示数据？',
          desc: '当前所有数据库、记录和规则都会被清空，并恢复初始示例。',
          okText: '重置',
          onOk() {
            state = seed();
            save();
            ui.stack = [];
            ui.route = { name: 'home' };
            ui.tab = 'home';
            ui.filter = 'all';
            render();
            toast('已重置');
          }
        });
        break;
      case 'about': aboutSheet(); break;
      case 'close-sheet': closeSheet(); break;
      default: break;
    }
  });

  document.addEventListener('input', (e) => {
    if (e.target.id === 'homeSearch') { ui.homeSearch = e.target.value; render(); }
    else if (e.target.id === 'libSearch') { ui.libSearch = e.target.value; ui.limit = 60; render(); }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && sheetOpen) closeSheet();
    if (e.key === 'Enter' && e.target.id === 'libSearch') {
      const lib = libById(ui.route.id);
      if (lib) pushSearch(lib.id, ui.libSearch);
    }
  });

  render();
})();
