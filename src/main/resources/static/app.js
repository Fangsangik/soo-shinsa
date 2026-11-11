/* 無shinsa front — vanilla SPA served by Spring Boot (same origin, no CORS). */
(() => {
    const $ = (sel, el = document) => el.querySelector(sel);
    const app = $('#app'), nav = $('#nav'), modal = $('#modal');
    const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    const mockImg = id => `https://picsum.photos/seed/shinsa-${id}/400/400`;
    const img = (url, id) => esc(url || mockImg(id));
    const won = n => Math.round(Number(n || 0)).toLocaleString('ko-KR') + '원';
    const qs = o => { const p = new URLSearchParams(); Object.entries(o).forEach(([k, v]) => (v !== '' && v != null) && p.set(k, v)); const s = p.toString(); return s ? '?' + s : ''; };
    const state = { user: null };
    /* Spring Page → « 이전 / n / 다음 » 컨트롤. go(page) 로 다시 로드. */
    const pager = (pg, go) => {
        if (!pg || pg.totalPages <= 1) return '';
        const el = document.createElement('div'); el.className = 'pager';
        el.innerHTML = `<button class="btn sm ghost" ${pg.first ? 'disabled' : ''} data-go="${pg.number - 1}">이전</button><span class="muted">${pg.number + 1} / ${pg.totalPages} 페이지 · 총 ${pg.totalElements}개</span><button class="btn sm ghost" ${pg.last ? 'disabled' : ''} data-go="${pg.number + 1}">다음</button>`;
        el.querySelectorAll('[data-go]').forEach(b => b.onclick = () => go(+b.dataset.go));
        return el;
    };
    const mount = (host, pg, go) => { const p = pager(pg, go); if (p) host.append(p); };

    /* ---------- toast / modal ---------- */
    let toastTimer;
    const toast = (msg, err) => {
        const t = $('#toast'); t.textContent = msg; t.className = 'toast' + (err ? ' err' : ''); t.hidden = false;
        clearTimeout(toastTimer); toastTimer = setTimeout(() => t.hidden = true, 2800);
    };
    const openModal = (html, onSubmit) => {
        modal.innerHTML = `<form method="dialog" class="stack">${html}<div class="row"><button class="btn primary" value="ok">확인</button><button class="btn ghost" value="cancel" formnovalidate>취소</button></div></form>`;
        const form = $('form', modal);
        form.onsubmit = async e => {
            if (e.submitter?.value !== 'ok') return;
            e.preventDefault();
            try { await onSubmit(Object.fromEntries(new FormData(form)), form); modal.close(); } catch (err) { toast(err.message, true); }
        };
        modal.showModal();
    };

    /* ---------- auth / api ---------- */
    const tokens = {
        get access() { return localStorage.getItem('accessToken'); },
        get refresh() { return localStorage.getItem('refreshToken'); },
        set(d) { localStorage.setItem('accessToken', d.accessToken); localStorage.setItem('refreshToken', d.refreshToken); localStorage.setItem('email', d.email); },
        clear() { ['accessToken', 'refreshToken', 'email'].forEach(k => localStorage.removeItem(k)); state.user = null; }
    };
    /* 백엔드 오류 응답 {httpStatus, code, message} 를 사용자가 이해할 수 있는 화면으로.
       401: 왜 로그인이 끊겼는지(유휴 만료/로그아웃/위조) 카드로 보여주고 로그인 버튼. 403: 권한 안내. 그 외: 토스트. */
    const ERROR_TITLE = { NO_TOKEN: '로그인이 필요합니다', SESSION_EXPIRED: '세션이 만료되었습니다', LOGGED_OUT: '로그아웃된 상태입니다', INVALID_TOKEN: '로그인 정보가 올바르지 않습니다', TOKEN_EXPIRED: '로그인이 만료되었습니다' };
    const authGate = (code, message) => {
        tokens.clear(); renderNav(); state.gate = { code, message }; // 페이지 진입 시 requireLogin 이 같은 이유를 다시 보여줌
        app.innerHTML = `<div class="gate"><div class="gate-icon">🔒</div><h1>${esc(ERROR_TITLE[code] || '로그인이 필요합니다')}</h1><p class="muted">${esc(message)}</p>
            <div class="row" style="justify-content:center"><a class="btn primary" href="#/login">로그인하기</a><a class="btn ghost" href="#/">홈으로</a></div></div>`;
    };
    const forbidden = (message) => {
        app.innerHTML = `<div class="gate"><div class="gate-icon">⛔</div><h1>접근 권한이 없습니다</h1><p class="muted">${esc(message)}</p>
            <p class="muted">현재 계정: ${esc(state.user?.name || '')} (${esc(state.user?.role || '-')})</p>
            <div class="row" style="justify-content:center"><a class="btn primary" href="#/">홈으로</a></div></div>`;
    };
    class ApiError extends Error { constructor(status, code, message) { super(message); this.status = status; this.code = code; } }
    /* 진행 중인 요청 수에 따라 상단 로딩 바 표시 */
    let inflight = 0;
    const busy = d => { inflight = Math.max(0, inflight + d); $('#loading').hidden = inflight === 0; };
    const api = async (method, path, body, retry = true) => {
        busy(1);
        try { return await request(method, path, body, retry); } finally { busy(-1); }
    };
    const request = async (method, path, body, retry = true) => {
        const headers = {};
        if (tokens.access) headers.Authorization = 'Bearer ' + tokens.access;
        if (body && !(body instanceof FormData)) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(body); }
        const res = await fetch(path, { method, headers, body });
        if (res.status === 204) return null;
        const text = await res.text();
        let json = null; try { json = text ? JSON.parse(text) : null; } catch { /* html error page */ }
        if (res.ok) return json;
        const code = json?.code || '', message = json?.message || json?.error || `요청 실패 (${res.status})`;
        if (res.status === 401) {
            // 액세스 토큰만 만료된 경우: refresh 로 조용히 이어간다
            if (code === 'TOKEN_EXPIRED' && retry && tokens.refresh && await refreshToken()) {
                return request(method, path, body instanceof FormData ? body : (body && JSON.parse(body)), false);
            }
            if (tokens.access || code === 'NO_TOKEN') authGate(code, message);
            throw new ApiError(401, code, message);
        }
        if (res.status === 403) { forbidden(message); throw new ApiError(403, code, message); }
        throw new ApiError(res.status, code, message);
    };
    /* 동시에 여러 요청이 401 을 받아도 refresh 는 한 번만 (single-flight) */
    let refreshing = null;
    const refreshToken = () => refreshing || (refreshing = doRefresh().finally(() => { refreshing = null; }));
    const doRefresh = async () => {
        try {
            const res = await fetch('/users/refresh', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: tokens.refresh, accessToken: tokens.access }) });
            if (!res.ok) throw 0;
            const j = await res.json(); tokens.set(j.data); return true;
        } catch { return false; }
    };
    const loadMe = async () => {
        if (!tokens.access) { state.user = null; return null; }
        try { state.user = (await api('GET', '/users')).data; } catch { state.user = null; }
        return state.user;
    };
    const isStaff = () => ['ADMIN', 'VENDOR'].includes(state.user?.role);
    const requireLogin = () => {
        if (tokens.access) return true;
        const g = state.gate || { code: 'NO_TOKEN', message: '로그인 후 이용할 수 있는 페이지입니다.' }; state.gate = null;
        authGate(g.code, g.message); return false;
    };

    /* ---------- nav ---------- */
    const renderNav = () => {
        const here = location.hash.split('/')[1] || '';
        const link = (h, t) => `<a href="#/${h}" class="${here === h ? 'active' : ''}">${t}</a>`;
        nav.innerHTML = tokens.access
            ? link('', '브랜드') + link('wishes', '찜') + link('cart', '장바구니') + link('orders', '주문') + (isStaff() ? link('admin', state.user?.role === 'ADMIN' ? '관리' : '입점사 관리') : '') + link('me', esc(state.user?.name || '내 정보') + (state.user?.role === 'CUSTOMER' && state.user.point != null ? ` <span class="badge ok">${Math.round(Number(state.user.point)).toLocaleString('ko-KR')}P</span>` : ''))
            : link('', '브랜드') + link('login', '로그인') + link('signup', '회원가입');
    };

    /* ---------- pages ---------- */
    const pages = {};

    pages.login = () => {
        app.innerHTML = `<h1>로그인</h1><form class="stack" id="f">
            <label>이메일<input name="email" type="email" required autocomplete="username"></label>
            <label>비밀번호<input name="password" type="password" required autocomplete="current-password"></label>
            <button class="btn primary">로그인</button>
            <p class="muted">계정이 없으신가요? <a href="#/signup"><u>회원가입</u></a></p></form>`;
        $('#f').onsubmit = async e => {
            e.preventDefault();
            try {
                const j = await api('POST', '/users/login', Object.fromEntries(new FormData(e.target)));
                tokens.set(j.data); await loadMe(); toast(j.message); location.hash = '#/';
            } catch (err) { toast(err.message, true); }
        };
    };

    pages.signup = () => {
        app.innerHTML = `<h1>회원가입</h1><form class="stack" id="f">
            <label>이메일<input name="email" type="email" required></label>
            <label>이름<input name="name" required></label>
            <label>전화번호<input name="phoneNum" placeholder="010-1234-5678" pattern="^\\d{2,3}-\\d{3,4}-\\d{4}$" required></label>
            <label>비밀번호<input name="password" type="password" required minlength="8" placeholder="8자 이상, 대/소문자, 숫자, 특수문자(@$!%*?&#)"></label>
            <label>역할<select name="role"><option value="CUSTOMER">일반 회원</option><option value="VENDOR">입점 사업자</option><option value="ADMIN">관리자</option></select></label>
            <button class="btn primary">가입하기</button></form>`;
        $('#f').onsubmit = async e => {
            e.preventDefault();
            try { const j = await api('POST', '/users/signin', Object.fromEntries(new FormData(e.target))); toast(j.message); location.hash = '#/login'; }
            catch (err) { toast(err.message, true); }
        };
    };

    const productTile = p => `
        <a class="tile" href="#/product/${p.id}">
            <div class="thumb"><img src="${img(p.imageUrl, p.id)}" alt="" loading="lazy"></div>
            <div class="body"><div class="name">${esc(p.name)}</div><div class="price">${won(p.price)}</div>
            <span class="badge ${p.status === 'AVAILABLE' ? 'ok' : 'warn'}">${esc(p.status)}</span></div>
        </a>`;

    pages.home = async () => {
        app.innerHTML = `<form class="row" id="search"><input name="nameKeyword" placeholder="상품 검색" style="flex:1;min-width:200px">
                <input name="minPrice" type="number" min="0" placeholder="최소 가격" style="width:120px"><input name="maxPrice" type="number" min="0" placeholder="최대 가격" style="width:120px">
                <button class="btn sm">검색</button></form>
            <h2>브랜드</h2><div class="grid" id="brands"><div class="muted">불러오는 중…</div></div>
            <h2 id="ptitle">전체 상품</h2><div class="grid" id="g"></div>
            <div class="row" style="justify-content:center;margin-top:16px"><button class="btn ghost sm" id="more" hidden>더 보기</button></div>`;
        let page = 0, filter = {};
        const loadProducts = async (append) => {
            const j = await api('GET', `/products${qs({ ...filter, page, size: 12 })}`);
            const pg = j.data;
            if (!append) $('#g').innerHTML = '';
            $('#g').insertAdjacentHTML('beforeend', pg.content.map(productTile).join('') || '<div class="empty">상품이 없습니다.</div>');
            $('#ptitle').textContent = filter.nameKeyword ? `"${filter.nameKeyword}" 검색 결과 ${pg.totalElements}개` : `전체 상품 ${pg.totalElements}개`;
            $('#more').hidden = pg.last;
        };
        $('#search').onsubmit = e => { e.preventDefault(); filter = Object.fromEntries(new FormData(e.target)); page = 0; loadProducts(false).catch(err => toast(err.message, true)); };
        $('#more').onclick = () => { page++; loadProducts(true).catch(err => toast(err.message, true)); };
        const [bj] = await Promise.all([api('GET', '/brands?page=0&size=100'), loadProducts(false)]);
        const brands = bj.data.content;
        $('#brands').innerHTML = brands.length
            ? brands.map(b => `<a class="tile brand-tile" href="#/brand/${b.id}">${esc(b.name)}</a>`).join('')
            : `<div class="empty">등록된 브랜드가 없습니다.${isStaff() ? ' <a href="#/admin"><u>관리 페이지</u></a>에서 브랜드를 만들어 보세요.' : ''}</div>`;
    };

    pages.brand = async (id) => {
        const brand = (await api('GET', `/brands/${id}`)).data;
        app.innerHTML = `<h1>${esc(brand.name)}</h1><p class="muted">${esc(brand.context || '')}</p>
            <form class="row" id="filter">
                <input name="nameKeyword" placeholder="상품명 검색">
                <input name="minPrice" type="number" min="0" placeholder="최소 가격" style="width:120px">
                <input name="maxPrice" type="number" min="0" placeholder="최대 가격" style="width:120px">
                <button class="btn sm">검색</button>
                <button class="btn sm ghost" type="button" id="report">브랜드 신고</button>
                <button class="btn sm" type="button" id="chat">💬 브랜드 상담</button>
            </form>
            <div class="grid" id="g" style="margin-top:16px"></div><div id="pg"></div>`;
        const load = async (page = 0) => {
            const f = Object.fromEntries(new FormData($('#filter')));
            const j = await api('GET', `/products/brands/${id}${qs({ ...f, page, size: 12 })}`);
            const list = j.data.content;
            $('#g').innerHTML = list.length ? list.map(productTile).join('') : '<div class="empty">상품이 없습니다.</div>';
            $('#pg').innerHTML = ''; mount($('#pg'), j.data, load);
        };
        $('#filter').onsubmit = e => { e.preventDefault(); load(0).catch(err => toast(err.message, true)); };
        $('#report').onclick = () => reportDialog('BRAND', id);
        $('#chat').onclick = () => { if (requireLogin()) openChat(+id, brand.name); };
        await load();
    };

    const reportDialog = (targetType, targetId) => {
        if (!requireLogin()) return;
        openModal(`<h2 style="margin-top:0">신고하기</h2><label>사유<textarea name="content" required></textarea></label>`, async d => {
            const j = await api('POST', '/reports', { targetId, targetType, status: 'OPEN', content: d.content }); toast(j.message);
        });
    };

    pages.product = async (id) => {
        const p = (await api('GET', `/products/${id}`)).data;
        const opts = p.options || [];
        app.innerHTML = `<div class="card">
            <div class="row" style="align-items:flex-start;gap:24px"><img src="${img(p.imageUrl, p.id)}" alt="" style="width:240px;height:240px;object-fit:cover;border-radius:12px;background:var(--soft)"><div style="flex:1;min-width:240px">
            <h1>${esc(p.name)}</h1><div class="price" style="font-size:22px;font-weight:800">${won(p.price)}</div>
            <form class="stack" id="buy" style="margin-top:16px">
                <label>옵션<select name="productOptionId" ${opts.length ? '' : 'disabled'}>${opts.length ? opts.map(o => `<option value="${o.id}">${esc(o.size)} / ${esc(o.color)} (${esc(o.status)})</option>`).join('') : '<option>옵션 없음</option>'}</select></label>
                <label>수량<input name="quantity" type="number" min="1" value="1" required style="width:100px"></label>
                <div class="row">
                    <button class="btn primary" name="act" value="cart" ${opts.length ? '' : 'disabled title="옵션이 없는 상품은 장바구니에 담을 수 없습니다"'}>장바구니</button>
                    <button class="btn" name="act" value="buy">바로 구매</button>
                    <button class="btn ghost sm" type="button" id="wish">♡ 찜</button>
                    <button class="btn ghost sm" type="button" id="report">신고</button>
                </div>
            </form></div></div></div>
            <h2>리뷰</h2>
            <form class="row" id="rf"><select name="minRate"><option value="">최소 별점</option>${[1,2,3,4,5].map(n=>`<option>${n}</option>`).join('')}</select>
                <select name="maxRate"><option value="">최대 별점</option>${[1,2,3,4,5].map(n=>`<option>${n}</option>`).join('')}</select><button class="btn sm">필터</button></form>
            <div id="reviews" style="margin-top:12px"></div><div id="rpg"></div>`;
        $('#buy').onsubmit = async e => {
            e.preventDefault(); if (!requireLogin()) return;
            const d = Object.fromEntries(new FormData(e.target)); const act = e.submitter.value;
            try {
                if (act === 'cart') {
                    const j = await api('POST', '/carts', { productId: p.id, quantity: +d.quantity, productOptionId: +d.productOptionId });
                    toast(j.message);
                } else {
                    const j = await api('POST', '/orders/single', { productId: p.id, quantity: +d.quantity });
                    toast(j.message); location.hash = '#/orders';
                }
            } catch (err) { toast(err.message, true); }
        };
        $('#report').onclick = () => reportDialog('PRODUCT', p.id);
        let wished = false;
        const paintWish = () => { $('#wish').textContent = wished ? '♥ 찜 해제' : '♡ 찜'; $('#wish').classList.toggle('primary', wished); };
        if (tokens.access) api('GET', '/wishes').then(j => { wished = j.data.some(w => w.productId === p.id); paintWish(); }).catch(() => {});
        $('#wish').onclick = async () => {
            if (!requireLogin()) return;
            try { if (wished) { await api('DELETE', `/wishes/${p.id}`); toast('찜을 해제했습니다.'); } else { const r = await api('POST', `/wishes/${p.id}`); toast(r.message); } wished = !wished; paintWish(); }
            catch (err) { toast(err.message, true); }
        };
        const loadReviews = async (page = 0) => {
            const f = Object.fromEntries(new FormData($('#rf')));
            const j = await api('GET', `/reviews/products/${id}${qs({ ...f, page, size: 10 })}`);
            const list = j.data.content;
            const mine = r => state.user && r.userId === state.user.userId;
            $('#reviews').innerHTML = list.length ? list.map(r => `<div class="card" data-rid="${r.id}">
                <div class="row between"><span class="stars">${'★'.repeat(r.rate)}${'☆'.repeat(5 - r.rate)}${mine(r) ? ' <span class="badge">내 리뷰</span>' : ''}</span>
                <span>${mine(r) ? `<button class="btn ghost sm redit">수정</button><button class="btn danger sm rdel">삭제</button>` : `<button class="btn ghost sm" data-report="${r.id}">신고</button>`}</span></div>
                <p style="margin:8px 0 0">${esc(r.content)}</p>${r.imageUrl ? `<img src="${esc(r.imageUrl)}" style="max-width:240px;border-radius:8px;margin-top:8px">` : ''}
            </div>`).join('') : '<div class="empty">아직 리뷰가 없습니다. 구매 후 주문 페이지에서 리뷰를 남길 수 있어요.</div>';
            $('#reviews').querySelectorAll('[data-report]').forEach(b => b.onclick = () => reportDialog('REVIEW', +b.dataset.report));
            $('#reviews').querySelectorAll('[data-rid]').forEach(card => {
                const r = list.find(x => x.id === +card.dataset.rid);
                const edit = $('.redit', card); if (edit) edit.onclick = () => openModal(`<h2 style="margin-top:0">리뷰 수정</h2>
                    <label>별점<select name="rate">${[5,4,3,2,1].map(n=>`<option value="${n}" ${n === r.rate ? 'selected' : ''}>${'★'.repeat(n)}</option>`).join('')}</select></label>
                    <label>내용<textarea name="content" required>${esc(r.content)}</textarea></label><label>사진 교체 (선택)<input type="file" name="imageFile" accept="image/*"></label>`, async (d, form) => {
                    const fd = new FormData(); fd.append('updateDto', new Blob([JSON.stringify({ rate: +d.rate, content: d.content })], { type: 'application/json' }));
                    if (form.imageFile.files[0]) fd.append('imageFile', form.imageFile.files[0]);
                    const res = await api('PATCH', `/reviews/${r.id}`, fd); toast(res.message); loadReviews(page);
                });
                const del = $('.rdel', card); if (del) del.onclick = () => openModal(`<h2 style="margin-top:0">리뷰를 삭제할까요?</h2><p class="muted">삭제 후 되돌릴 수 없습니다.</p>`, async () => {
                    await api('DELETE', `/reviews/${r.id}`); toast('리뷰를 삭제했습니다.'); loadReviews(page);
                });
            });
            $('#rpg').innerHTML = ''; mount($('#rpg'), j.data, loadReviews);
        };
        $('#rf').onsubmit = e => { e.preventDefault(); loadReviews(0).catch(err => toast(err.message, true)); };
        await loadReviews();
    };

    pages.cart = async () => {
        if (!requireLogin()) return;
        app.innerHTML = '<h1>장바구니</h1><div id="c" class="muted">불러오는 중…</div>';
        const j = await api('GET', '/carts/users?page=0&size=100');
        const items = j.data.content;
        if (!items.length) { $('#c').innerHTML = '<div class="empty">장바구니가 비어 있습니다.</div>'; return; }
        const names = {};
        await Promise.all([...new Set(items.map(i => i.productId))].map(async pid => { try { names[pid] = (await api('GET', `/products/${pid}`)).data.name; } catch { names[pid] = `상품 #${pid}`; } }));
        const total = items.reduce((s, i) => s + Number(i.productPrice) * i.quantity, 0);
        $('#c').innerHTML = `<table><thead><tr><th>상품</th><th>옵션</th><th>단가</th><th>수량</th><th>합계</th><th></th></tr></thead><tbody>
            ${items.map(i => `<tr data-id="${i.cartItemId}" data-pid="${i.productId}" data-oid="${i.productOptions?.[0]?.id ?? ''}">
                <td><a href="#/product/${i.productId}"><u>${esc(names[i.productId])}</u></a></td>
                <td class="muted">${(i.productOptions || []).map(o => esc(o.size + '/' + o.color)).join(', ')}</td>
                <td>${won(i.productPrice)}</td>
                <td><input type="number" min="1" value="${i.quantity}" class="qty" style="width:70px"></td>
                <td>${won(Number(i.productPrice) * i.quantity)}</td>
                <td><button class="btn sm ghost coupon">쿠폰</button><button class="btn sm danger del">삭제</button></td></tr>`).join('')}
            </tbody></table>
            <div class="row between" style="margin-top:16px"><strong>총 ${won(total)}</strong>
                <div class="row">${state.user?.role === 'CUSTOMER' ? `<label style="display:flex;align-items:center;gap:6px">포인트 사용 (보유 ${Math.round(Number(state.user.point || 0)).toLocaleString('ko-KR')}P)<input id="usePoint" type="number" min="0" max="${Math.min(Math.floor(Number(state.user.point || 0)), Math.floor(total))}" value="0" style="width:110px"></label>` : ''}<button class="btn primary" id="order">전체 주문하기</button></div></div>`;
        $('#c').querySelectorAll('tr[data-id]').forEach(tr => {
            const id = tr.dataset.id;
            $('.qty', tr).onchange = async e => {
                try { await api('PATCH', `/carts/${id}`, { productId: +tr.dataset.pid, quantity: +e.target.value, productOptionId: +tr.dataset.oid || null }); pages.cart(); }
                catch (err) { toast(err.message, true); }
            };
            $('.del', tr).onclick = async () => { try { await api('DELETE', `/carts/${id}`); toast('삭제되었습니다.'); pages.cart(); } catch (err) { toast(err.message, true); } };
            $('.coupon', tr).onclick = async () => {
                let coupons = [];
                try { coupons = (await api('GET', '/coupons')).data; } catch (err) { toast(err.message, true); return; }
                if (!coupons.length) { toast('사용 가능한 쿠폰이 없습니다. (이미 사용한 쿠폰은 제외됩니다)'); return; }
                openModal(`<h2 style="margin-top:0">쿠폰 적용</h2><label>쿠폰<select name="couponId">${coupons.map(c => `<option value="${c.id}">${esc(c.couponName)} · ${Number(c.discountRate)}% (~${esc(c.expirationDate)})</option>`).join('')}</select></label>`, async d => {
                    const r = await api('POST', `/carts/${id}/apply-coupon`, { couponId: +d.couponId });
                    toast(`${r.message} 할인가 ${won(r.data.discountedPrice)}`); pages.cart();
                });
            };
        });
        $('#order').onclick = async () => {
            try {
                const usePoint = Number($('#usePoint')?.value || 0);
                const r = await api('POST', '/orders/carts', usePoint > 0 ? { usePoint } : null); toast(r.message);
                await loadMe(); // 포인트 잔액 갱신
                location.hash = `#/pay/${r.data.id}`;
            } catch (err) { toast(err.message, true); }
        };
    };

    /* 결제 페이지: 주문은 인증된 /orders/{id}로 조회(소유권 검증), Toss 클라이언트 키만 공개 엔드포인트에서 받는다. */
    pages.pay = async (id) => {
        if (!requireLogin()) return;
        const [o, key] = await Promise.all([api('GET', `/orders/${id}`).then(j => j.data), api('GET', '/api/client-key').then(j => j.clientKey)]);
        if (o.status !== 'BEFOREPAYMENT') { app.innerHTML = `<div class="empty">이미 처리된 주문입니다 (${STATUS_KO[o.status] || esc(o.status)}). <a href="#/orders"><u>주문 목록</u></a></div>`; return; }
        app.innerHTML = `<div class="pay-page" style="margin:24px auto"><section class="card"><h1>주문 결제</h1>
            <dl class="pay-summary"><dt>주문번호</dt><dd>${esc(o.orderId)}</dd><dt>주문자</dt><dd>${esc(state.user?.name || '')}</dd>
            <dt>상품</dt><dd>${(o.orderItems || []).map(it => `${esc(it.productName)} × ${it.quantity}`).join('<br>')}</dd>
            <dt>결제금액</dt><dd><strong>${won(o.totalPrice)}</strong></dd></dl>
            <button id="payment-button" class="btn primary">카드로 결제하기</button> <a class="btn ghost" href="#/orders">주문 목록으로</a></section></div>`;
        if (!window.TossPayments) await new Promise((res, rej) => { const s = document.createElement('script'); s.src = 'https://js.tosspayments.com/v1'; s.onload = res; s.onerror = () => rej(new Error('Toss SDK 로드 실패')); document.head.append(s); });
        $('#payment-button').onclick = async () => {
            try {
                await api('POST', '/api/create', { method: 'CARD', order: o.id }); // Payment(READY) 생성 — 승인 콜백이 orderId로 찾는다
                window.TossPayments(key).requestPayment('카드', {
                    amount: Math.round(Number(o.totalPrice)), orderId: o.orderId, orderName: `無shinsa 주문 ${o.orderId}`.slice(0, 100),
                    customerName: state.user?.name || '고객', successUrl: location.origin + '/api/success', failUrl: location.origin + '/api/fail'
                });
            } catch (err) { toast(err.message, true); }
        };
    };

    const STATUS_KO = { BEFOREPAYMENT: '결제 전', PAYMENTCOMPLETED: '결제 완료', ONDELIVERY: '배송 중', DELIVERYCOMPLETED: '배송 완료', ORDERCOMPLETED: '주문 완료', CANCEL: '취소 신청', RETURN: '취소 완료', REQUESTFORRETURN: '반품 신청', RETURNCOMPLETED: '반품 완료', CANCELRETURN: '반품 취소', APPLYFOREXCHANGE: '교환 신청', COMPLETEDEXCHANGE: '교환 완료', EXCHANGECANCELLATION: '교환 취소' };

    // 고객이 현재 상태에서 누를 수 있는 전이 (백엔드 isCustomerTransitionAllowed 와 동일 규칙)
    const ORDER_ACTIONS = { BEFOREPAYMENT: [['CANCEL', '주문 취소']], PAYMENTCOMPLETED: [['CANCEL', '취소 신청']],
        DELIVERYCOMPLETED: [['REQUESTFORRETURN', '반품 신청'], ['APPLYFOREXCHANGE', '교환 신청']], ORDERCOMPLETED: [['REQUESTFORRETURN', '반품 신청'], ['APPLYFOREXCHANGE', '교환 신청']],
        REQUESTFORRETURN: [['CANCELRETURN', '반품 취소']], APPLYFOREXCHANGE: [['EXCHANGECANCELLATION', '교환 취소']] };

    // 관리자/입점사가 누를 수 있는 전이 (결제 확인은 Toss 테스트키 없이 데모할 때 사용)
    const STAFF_ACTIONS = { BEFOREPAYMENT: [['PAYMENTCOMPLETED', '결제 확인']], PAYMENTCOMPLETED: [['ONDELIVERY', '배송 시작']], ONDELIVERY: [['DELIVERYCOMPLETED', '배송 완료']],
        DELIVERYCOMPLETED: [['ORDERCOMPLETED', '구매 확정']], CANCEL: [['RETURN', '취소 완료']], REQUESTFORRETURN: [['RETURNCOMPLETED', '반품 완료']], APPLYFOREXCHANGE: [['COMPLETEDEXCHANGE', '교환 완료']] };
    const BADGE = s => s === 'PAYMENTCOMPLETED' || s === 'ORDERCOMPLETED' ? 'ok' : s === 'BEFOREPAYMENT' || s.includes('REQUEST') || s === 'APPLYFOREXCHANGE' || s === 'CANCEL' ? 'warn' : '';

    pages.orders = async () => {
        if (!requireLogin()) return;
        app.innerHTML = `<h1>주문 내역</h1>
            <form class="row" id="f"><label>시작일<input type="date" name="startDate"></label><label>종료일<input type="date" name="endDate"></label><button class="btn sm" style="align-self:end">조회</button></form>
            <div id="list" style="margin-top:16px"></div><div id="pg"></div>`;
        const load = async (page = 0) => {
            const f = Object.fromEntries(new FormData($('#f')));
            const j = await api('GET', `/orders/users${qs({ ...f, page, size: 10 })}`);
            const orders = j.data.content;
            $('#list').innerHTML = orders.length ? orders.map(o => `<div class="card" data-id="${o.id}">
                <div class="row between"><div><strong>${esc(o.orderId)}</strong> <span class="badge ${BADGE(o.status)}">${STATUS_KO[o.status] || esc(o.status)}</span></div>
                    <div>${o.status === 'BEFOREPAYMENT' ? `<a class="btn primary sm" href="#/pay/${o.id}">결제하기</a>` : ''}
                    ${(ORDER_ACTIONS[o.status] || []).map(([to, label]) => `<button class="btn sm ${to === 'CANCEL' ? 'danger' : 'ghost'} act" data-to="${to}">${label}</button>`).join('')}</div></div>
                <table style="margin-top:12px"><tbody>${(o.orderItems || []).map(it => `<tr>
                    <td><a href="#/product/${it.productId}"><u>${esc(it.productName)}</u></a> × ${it.quantity}</td>
                    <td>${won(it.discountPrice ?? it.price)}</td>
                    <td style="text-align:right">${o.status !== 'BEFOREPAYMENT' && o.status !== 'CANCEL' && o.status !== 'RETURN' ? `<button class="btn sm ghost review" data-item="${it.orderItemId}" data-pid="${it.productId}">리뷰 작성</button>` : ''}</td></tr>`).join('')}
                </tbody></table>
                <div class="row between" style="margin-top:8px"><span class="muted">총 결제금액${Number(o.usedPoint) > 0 ? ` · 포인트 ${Math.round(Number(o.usedPoint)).toLocaleString('ko-KR')}P 사용` : ''}${Number(o.earnedPoint) > 0 ? ` · ${Math.round(Number(o.earnedPoint)).toLocaleString('ko-KR')}P 적립` : ''}</span><strong>${won(o.totalPrice)}</strong></div>
            </div>`).join('') : '<div class="empty">주문 내역이 없습니다.</div>';
            $('#pg').innerHTML = ''; mount($('#pg'), j.data, load);
            $('#list').querySelectorAll('.card').forEach(card => {
                const id = +card.dataset.id;
                card.querySelectorAll('.act').forEach(b => b.onclick = async () => {
                    try { const r = await api('PATCH', '/orders', { orderId: id, status: b.dataset.to }); toast(r.message); load(page); } catch (err) { toast(err.message, true); }
                });
                card.querySelectorAll('.review').forEach(b => b.onclick = () => openModal(`<h2 style="margin-top:0">리뷰 작성</h2>
                    <label>별점<select name="rate">${[5,4,3,2,1].map(n=>`<option value="${n}">${'★'.repeat(n)}</option>`).join('')}</select></label>
                    <label>내용<textarea name="content" required></textarea></label>
                    <label>사진 (선택)<input type="file" name="imageFile" accept="image/*"></label>`, async (d, form) => {
                    const fd = new FormData();
                    fd.append('requestDto', new Blob([JSON.stringify({ rate: +d.rate, content: d.content, productId: +b.dataset.pid })], { type: 'application/json' }));
                    const file = form.imageFile.files[0]; if (file) fd.append('imageFile', file);
                    const r = await api('POST', `/reviews/order-item/${b.dataset.item}`, fd); toast(r.message);
                }));
            });
        };
        $('#f').onsubmit = e => { e.preventDefault(); load(0).catch(err => toast(err.message, true)); };
        await load();
    };

    pages.me = async () => {
        if (!requireLogin()) return;
        const u = (await api('GET', '/users')).data;
        app.innerHTML = `<h1>내 정보</h1><div class="card">
            <table><tbody><tr><th>이름</th><td>${esc(u.name)}</td></tr><tr><th>이메일</th><td>${esc(localStorage.getItem('email'))}</td></tr>
            <tr><th>전화번호</th><td>${esc(u.phoneNum)}</td></tr><tr><th>역할</th><td>${esc(u.role)}</td></tr>
            ${u.userGrade ? `<tr><th>등급</th><td>${esc(u.userGrade)} (적립률 ${Math.round(Number(u.pointRate) * 100)}%, 승급 기준 누적 ${won(u.requirement)})</td></tr>` : ''}
            ${u.role === 'CUSTOMER' ? `<tr><th>포인트</th><td><strong>${Math.round(Number(u.point || 0)).toLocaleString('ko-KR')}P</strong> <span class="muted">· 누적 구매 ${won(u.totalPurchase)} · 결제 완료 시 적립, 장바구니에서 사용</span></td></tr>` : ''}</tbody></table></div>
            <h2>정보 수정</h2><form class="stack" id="f">
            <label>이름<input name="name" value="${esc(u.name)}"></label><label>전화번호<input name="phoneNum" value="${esc(u.phoneNum)}"></label>
            <label>기존 비밀번호<input name="oldPassword" type="password" required></label><label>새 비밀번호<input name="newPassword" type="password" required></label>
            <button class="btn primary">저장</button></form>
            <h2>내 쿠폰</h2><div id="coupons" class="muted">불러오는 중…</div>
            <h2>내 신고</h2><div id="reports" class="muted">불러오는 중…</div>
            <h2>계정</h2><div class="row"><button class="btn" id="logout">로그아웃</button><button class="btn danger" id="leave">회원 탈퇴</button></div>`;
        const REPORT_KO = { OPEN: '접수됨', IN_PROGRESS: '처리 중', APPROVED: '승인됨', REJECTED: '반려됨', RESOLVED: '처리 완료' };
        const loadReports = (page = 0) => api('GET', `/reports/me?page=${page}&size=5`).then(j => {
            const list = j.data.content;
            $('#reports').innerHTML = list.length ? `<table><thead><tr><th>대상</th><th>내용</th><th>상태</th></tr></thead><tbody>${list.map(r => `<tr><td>${esc({ BRAND: '브랜드', PRODUCT: '상품', REVIEW: '리뷰' }[r.targetType] || r.targetType)} #${r.targetId}</td><td>${esc(r.content)}</td>
                <td><span class="badge ${r.status === 'RESOLVED' || r.status === 'APPROVED' ? 'ok' : r.status === 'REJECTED' ? 'warn' : ''}">${esc(REPORT_KO[r.status] || r.status)}</span>${r.rejectReason ? `<div class="muted">사유: ${esc(r.rejectReason)}</div>` : ''}</td></tr>`).join('')}</tbody></table>` : '<div class="empty">신고한 내역이 없습니다.</div>';
            mount($('#reports'), j.data, loadReports);
        }).catch(err => { $('#reports').textContent = err.message; });
        loadReports();
        api('GET', '/coupons/me').then(j => {
            const list = j.data;
            $('#coupons').innerHTML = list.length ? `<table><thead><tr><th>쿠폰</th><th>할인</th><th>만료일</th><th>상태</th></tr></thead><tbody>${list.map(c => `<tr><td>${esc(c.couponName)} <span class="muted">${esc(c.couponCode)}</span></td><td>${Number(c.discountRate)}%</td><td>${esc(c.expirationDate)}</td><td><span class="badge ${c.used ? '' : 'ok'}">${c.used ? `사용함 (${esc(c.usedAt)})` : '사용 가능'}</span></td></tr>`).join('')}</tbody></table>`
                : '<div class="empty">발급받은 쿠폰이 없습니다. 장바구니에서 쿠폰을 적용하면 여기에 기록됩니다.</div>';
        }).catch(err => { $('#coupons').textContent = err.message; });
        $('#f').onsubmit = async e => { e.preventDefault(); try { const r = await api('PATCH', '/users', Object.fromEntries(new FormData(e.target))); toast('저장되었습니다.'); pages.me(); } catch (err) { toast(err.message, true); } };
        $('#logout').onclick = async () => { try { await api('POST', '/users/logout'); } catch { /* token already invalid */ } tokens.clear(); toast('로그아웃되었습니다.'); location.hash = '#/'; };
        $('#leave').onclick = () => openModal(`<h2 style="margin-top:0">정말 탈퇴하시겠어요?</h2><label>비밀번호 확인<input name="password" type="password" required></label>`, async d => {
            await api('POST', '/users/leave', d); tokens.clear(); toast('탈퇴 처리되었습니다.'); location.hash = '#/';
        });
    };

    pages.wishes = async () => {
        if (!requireLogin()) return;
        app.innerHTML = '<h1>찜한 상품</h1><div id="g" class="grid"><div class="muted">불러오는 중…</div></div>';
        const load = async () => {
            const list = (await api('GET', '/wishes')).data;
            $('#g').innerHTML = list.length ? list.map(w => `<div class="tile"><a href="#/product/${w.productId}"><div class="thumb"><img src="${img(w.imageUrl, w.productId)}" alt="" loading="lazy"></div></a>
                <div class="body"><div class="name">${esc(w.productName)}</div><div class="muted">${esc(w.brandName || '')}</div><div class="row between"><span class="price">${won(w.price)}</span><button class="btn sm ghost" data-un="${w.productId}">해제</button></div></div></div>`).join('')
                : '<div class="empty">찜한 상품이 없습니다. 상품 페이지의 ♡ 버튼으로 담아 보세요.</div>';
            $('#g').querySelectorAll('[data-un]').forEach(b => b.onclick = async () => { try { await api('DELETE', `/wishes/${b.dataset.un}`); toast('찜을 해제했습니다.'); load(); } catch (err) { toast(err.message, true); } });
        };
        await load();
    };

    /* ---------- 상담 채팅 (WebSocket /ws/chat?token&room=brandId, 서버가 Redis pub/sub 으로 전달) ---------- */
    const chat = { ws: null, room: null, name: '' };
    const chatUI = () => $('#chat-panel');
    const openChat = async (brandId, brandName) => {
        chatUI().hidden = false;
        if (brandId) { chat.room = brandId; chat.name = brandName; }
        const rooms = (await api('GET', '/chat/rooms')).data;
        if (!chat.room && rooms.length) { chat.room = rooms[0].brandId; chat.name = rooms[0].brandName; }
        $('#chat-room').innerHTML = rooms.map(r => `<option value="${r.brandId}" ${r.brandId === chat.room ? 'selected' : ''}>${esc(r.brandName)}${r.last ? ` · ${esc(r.last.text.slice(0, 14))}` : ''}</option>`).join('') || '<option value="">상담 가능한 브랜드가 없습니다</option>';
        $('#chat-room').onchange = e => { const o = e.target.selectedOptions[0]; chat.room = +o.value; chat.name = o.textContent.split(' · ')[0]; connectChat(); };
        if (chat.room) connectChat();
    };
    const chatLine = (m, sys) => {
        const box = $('#chat-messages'); const mine = m.senderId === state.user?.userId;
        box.insertAdjacentHTML('beforeend', sys ? `<div class="chat-line sys">${esc(m)}</div>` : `<div class="chat-line ${mine ? 'me' : ''}"><span class="who">${esc(m.sender)}<span class="badge">${{ CUSTOMER: '고객', VENDOR: '입점사', ADMIN: '관리자' }[m.role] || esc(m.role)}</span></span><span class="bubble">${esc(m.text)}</span><span class="muted">${new Date(m.at).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}</span></div>`);
        box.scrollTop = box.scrollHeight;
    };
    const connectChat = () => {
        if (chat.ws) { chat.ws.onclose = null; chat.ws.close(); chat.ws = null; }
        $('#chat-messages').innerHTML = ''; $('#chat-title').textContent = `${chat.name} 상담`;
        const ws = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/chat?token=${encodeURIComponent(tokens.access)}&room=${chat.room}`);
        ws.onopen = () => chatLine('상담이 연결되었습니다. 입점사가 답하면 여기에 표시됩니다.', true);
        ws.onmessage = e => { if (e.data === 'pong') return; try { chatLine(JSON.parse(e.data)); } catch { /* ignore */ } };
        ws.onclose = () => { chatLine('연결이 끊어졌습니다. 방을 다시 선택하면 재연결됩니다.', true); chat.ws = null; };
        ws.onerror = () => toast('상담 연결에 실패했습니다. 로그인 상태와 권한을 확인하세요.', true);
        chat.ws = ws;
    };
    const sendChat = () => { const i = $('#chat-input'); const t = i.value.trim(); if (!t || !chat.ws || chat.ws.readyState !== 1) return; chat.ws.send(t); i.value = ''; };
    $('#chat-fab').onclick = () => { if (!requireLogin()) return; if (chatUI().hidden) openChat().catch(err => toast(err.message, true)); else chatUI().hidden = true; };
    $('#chat-close').onclick = () => { chatUI().hidden = true; };
    $('#chat-send').onclick = sendChat; $('#chat-input').onkeydown = e => { if (e.key === 'Enter') sendChat(); };

    /* ---------- admin / vendor ---------- */
    pages.admin = async () => {
        if (!requireLogin()) return;
        if (!isStaff()) { app.innerHTML = '<div class="empty">관리자 또는 입점 사업자만 접근할 수 있습니다.</div>'; return; }
        const admin = state.user.role === 'ADMIN';
        const tabs = [['orders', '주문 관리'], ['brand', '브랜드/상품'], ['coupon', '쿠폰'], ...(admin ? [['category', '카테고리'], ['report', '신고 관리'], ['stats', '통계']] : [])];
        app.innerHTML = `<h1>관리</h1><div class="tabs">${tabs.map(([k, t], i) => `<button class="btn sm ${i ? '' : 'active'}" data-tab="${k}">${t}</button>`).join('')}</div><div id="panel"></div>`;
        const show = k => { app.querySelectorAll('[data-tab]').forEach(b => b.classList.toggle('active', b.dataset.tab === k)); adminTabs[k]($('#panel'), admin).catch(err => toast(err.message, true)); };
        app.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => show(b.dataset.tab));
        show(tabs[0][0]);
    };

    const formCard = (title, fields, submit) => {
        const el = document.createElement('div'); el.className = 'card';
        el.innerHTML = `<h2 style="margin-top:0">${title}</h2><form class="stack">${fields}<button class="btn primary">생성</button></form><div class="muted result"></div>`;
        $('form', el).onsubmit = async e => {
            e.preventDefault();
            try { const r = await submit(Object.fromEntries(new FormData(e.target)), e.target); $('.result', el).textContent = `${r.message} → ${JSON.stringify(r.data)}`; toast(r.message); }
            catch (err) { toast(err.message, true); }
        };
        return el;
    };
    const adminTabs = {};

    adminTabs.category = async (panel) => {
        panel.innerHTML = '';
        const cats = (await api('GET', '/categories?page=0&size=100')).data.content;
        const catOpts = cats.map(c => `<option value="${c.id}">${esc(c.name)} (#${c.id})</option>`).join('');
        panel.append(formCard('카테고리', `<label>이름<input name="name" required></label>`, d => api('POST', '/categories', d)));
        panel.append(formCard('서브 카테고리', `<label>상위 카테고리<select name="parentId" required>${catOpts}</select></label><label>이름<input name="name" required></label>`,
            d => api('POST', '/sub-categories', { parentId: +d.parentId, name: d.name })));
        const list = document.createElement('div'); list.className = 'card';
        list.innerHTML = `<h2 style="margin-top:0">카테고리 목록</h2>${cats.length ? `<table><tbody>${cats.map(c => `<tr><td>#${c.id}</td><td>${esc(c.name)}</td></tr>`).join('')}</tbody></table>` : '<p class="muted">없음</p>'}
            <p class="muted">서브 카테고리는 브랜드/상품 탭의 브랜드 입점 폼에서 선택할 수 있습니다.</p>`;
        panel.append(list);
    };

    /* 주문 관리: ADMIN 은 전체, VENDOR 는 자기 브랜드 상품이 포함된 주문만 (GET /orders/manage) */
    adminTabs.orders = async (panel) => {
        panel.innerHTML = `<div class="card"><h2 style="margin-top:0">주문 관리</h2>
            <form class="row" id="of"><select name="status"><option value="">전체 상태</option>${Object.entries(STATUS_KO).map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}</select>
            <label>시작일<input type="date" name="startDate"></label><label>종료일<input type="date" name="endDate"></label><button class="btn sm" style="align-self:end">조회</button></form>
            <div id="olist" style="margin-top:12px"></div><div id="opg"></div></div>`;
        const load = async (page = 0) => {
            const f = Object.fromEntries(new FormData($('#of')));
            const j = await api('GET', `/orders/manage${qs({ ...f, page, size: 10 })}`);
            const orders = j.data.content;
            $('#olist').innerHTML = orders.length ? `<table><thead><tr><th>주문번호</th><th>주문자</th><th>상품</th><th>금액</th><th>상태</th><th></th></tr></thead><tbody>${orders.map(o => `<tr data-id="${o.id}">
                <td class="muted" style="font-size:12px">${esc(o.orderId)}</td><td>${esc(o.userName || '#' + o.userId)}</td>
                <td>${(o.orderItems || []).map(it => `${esc(it.productName)} × ${it.quantity}`).join('<br>')}</td><td>${won(o.totalPrice)}</td>
                <td><span class="badge ${BADGE(o.status)}">${STATUS_KO[o.status] || esc(o.status)}</span></td>
                <td>${(STAFF_ACTIONS[o.status] || []).map(([to, label]) => `<button class="btn sm ghost act" data-to="${to}">${label}</button>`).join('')}</td></tr>`).join('')}</tbody></table>` : '<p class="muted">조건에 맞는 주문이 없습니다.</p>';
            $('#opg').innerHTML = ''; mount($('#opg'), j.data, load);
            $('#olist').querySelectorAll('tr[data-id]').forEach(tr => tr.querySelectorAll('.act').forEach(b => b.onclick = async () => {
                try { const r = await api('PATCH', '/orders', { orderId: +tr.dataset.id, status: b.dataset.to }); toast(r.message); load(page); } catch (err) { toast(err.message, true); }
            }));
        };
        $('#of').onsubmit = e => { e.preventDefault(); load(0).catch(err => toast(err.message, true)); };
        await load();
    };

    adminTabs.brand = async (panel, admin) => {
        panel.innerHTML = '';
        const mine = (await api('GET', '/brands/owners')).data;
        const brandOpts = mine.map(b => `<option value="${b.id}">${esc(b.name)} (#${b.id})</option>`).join('');
        const subs = (await api('GET', '/sub-categories')).data;
        const subOpts = subs.map(s => `<option value="${s.id}">${esc(s.name)} (#${s.id})</option>`).join('');
        panel.append(formCard('브랜드 입점', `<label>브랜드명<input name="name" required></label><label>사업자 등록번호<input name="registrationNum" required></label>
            <label>소개<textarea name="context" required></textarea></label>
            <label>서브 카테고리<select name="subCategoryId" required>${subOpts || '<option value="">먼저 카테고리 탭에서 서브 카테고리를 만드세요</option>'}</select></label>
            <label>상태<select name="status"><option value="OPEN">OPEN (승인)</option><option value="APPLY">APPLY (신청)</option></select></label>`,
            d => api('POST', '/brands', { ...d, subCategoryId: +d.subCategoryId }).then(r => { adminTabs.brand(panel, admin); return r; })));
        panel.append(formCard('상품 등록', `<label>브랜드<select name="brandId" required>${brandOpts || '<option value="">먼저 브랜드를 만드세요</option>'}</select></label>
            <label>상품명<input name="name" required></label><label>가격<input name="price" type="number" min="0" required></label>
            <label>상태<select name="status"><option>AVAILABLE</option><option>SOLD_OUT</option><option>UNAVAILABLE</option><option>DISCONTINUED</option></select></label>
            <label>이미지 (선택)<input type="file" name="imageFile" accept="image/*"></label>`,
            (d, form) => {
                const fd = new FormData();
                fd.append('productRequestDto', new Blob([JSON.stringify({ name: d.name, price: +d.price, brandId: +d.brandId, status: d.status })], { type: 'application/json' }));
                if (form.imageFile.files[0]) fd.append('imageFile', form.imageFile.files[0]);
                return api('POST', `/products/brands/${d.brandId}`, fd).then(r => { loadProducts(); return r; });
            }));
        /* 내 브랜드 상품/옵션 관리 */
        const mgr = document.createElement('div'); mgr.className = 'card';
        mgr.innerHTML = `<h2 style="margin-top:0">내 브랜드 상품 관리</h2>${mine.length ? `<div class="row"><select id="mb">${brandOpts}</select><span class="muted" id="mcount"></span></div><div id="mp" style="margin-top:12px"></div><div id="mpg"></div>` : '<p class="muted">브랜드가 없습니다.</p>'}`;
        panel.append(mgr);
        const statusSel = (name, cur, opts) => `<select name="${name}" class="sm">${opts.map(s => `<option ${s === cur ? 'selected' : ''}>${s}</option>`).join('')}</select>`;
        const PSTAT = ['AVAILABLE', 'SOLD_OUT', 'UNAVAILABLE', 'DISCONTINUED'], OSTAT = ['AVAILABLE', 'SOLD_OUT'];
        const loadProducts = async (page = 0) => {
            if (!mine.length) return;
            const bid = $('#mb').value;
            const j = await api('GET', `/products/brands/${bid}${qs({ page, size: 10 })}`);
            const list = j.data.content;
            $('#mcount').textContent = `상품 ${j.data.totalElements}개`;
            if (!list.length) { $('#mp').innerHTML = '<p class="muted">이 브랜드에 상품이 없습니다. 위 폼에서 등록하세요.</p>'; $('#mpg').innerHTML = ''; return; }
            const details = await Promise.all(list.map(p => api('GET', `/products/${p.id}`).then(r => r.data).catch(() => ({ ...p, options: [] }))));
            $('#mp').innerHTML = details.map(p => `<div class="card" data-pid="${p.id}">
                <div class="row between"><div class="row"><img src="${img(p.imageUrl, p.id)}" alt="" style="width:48px;height:48px;object-fit:cover;border-radius:8px"><div><strong>${esc(p.name)}</strong> <span class="muted">#${p.id}</span><br><span>${won(p.price)}</span> <span class="badge ${p.status === 'AVAILABLE' ? 'ok' : 'warn'}">${esc(p.status)}</span></div></div>
                    <div><button class="btn sm ghost pedit">수정</button><button class="btn sm danger pdel">삭제</button></div></div>
                <table style="margin-top:10px"><thead><tr><th>옵션</th><th>상태</th><th></th></tr></thead><tbody>
                    ${(p.options || []).map(o => `<tr data-oid="${o.id}"><td>${esc(o.size)} / ${esc(o.color)}</td><td>${statusSel('status', o.status, OSTAT)}</td><td><button class="btn sm ghost osave">저장</button></td></tr>`).join('')}
                    <tr class="oadd"><td><input name="size" placeholder="사이즈" style="width:90px"> <input name="color" placeholder="색상" style="width:110px"></td><td>${statusSel('status', 'AVAILABLE', OSTAT)}</td><td><button class="btn sm primary oadd">옵션 추가</button></td></tr>
                </tbody></table></div>`).join('');
            $('#mpg').innerHTML = ''; mount($('#mpg'), j.data, loadProducts);
            $('#mp').querySelectorAll('[data-pid]').forEach(card => {
                const pid = +card.dataset.pid; const p = details.find(x => x.id === pid);
                $('.pedit', card).onclick = () => openModal(`<h2 style="margin-top:0">상품 수정</h2><label>상품명<input name="name" value="${esc(p.name)}" required></label><label>가격<input name="price" type="number" min="0" value="${Number(p.price)}" required></label>
                    <label>상태${statusSel('status', p.status, PSTAT)}</label><label>이미지 교체 (선택)<input type="file" name="imageFile" accept="image/*"></label>`, async (d, form) => {
                    const fd = new FormData();
                    fd.append('productUpdateDto', new Blob([JSON.stringify({ name: d.name, price: +d.price, status: d.status })], { type: 'application/json' }));
                    if (form.imageFile.files[0]) fd.append('imageFile', form.imageFile.files[0]);
                    const r = await api('PATCH', `/products/${pid}`, fd); toast(r.message); loadProducts(page);
                });
                $('.pdel', card).onclick = () => openModal(`<h2 style="margin-top:0">"${esc(p.name)}" 을(를) 삭제할까요?</h2><p class="muted">옵션·리뷰·주문 항목이 함께 삭제됩니다.</p>`, async () => {
                    await api('DELETE', `/products/${pid}`); toast('삭제되었습니다.'); loadProducts(page);
                });
                card.querySelectorAll('tr[data-oid]').forEach(tr => $('.osave', tr).onclick = async () => {
                    const o = p.options.find(x => x.id === +tr.dataset.oid);
                    try { const r = await api('PATCH', `/options/${tr.dataset.oid}`, { size: o.size, color: o.color, status: $('select', tr).value }); toast(r.message); } catch (err) { toast(err.message, true); }
                });
                const add = $('tr.oadd', card);
                $('button.oadd', add).onclick = async () => {
                    const size = $('[name=size]', add).value.trim(), color = $('[name=color]', add).value.trim();
                    if (!size || !color) { toast('사이즈와 색상을 입력하세요.', true); return; }
                    try { const r = await api('POST', `/options/products/${pid}`, { size, color, status: $('select', add).value }); toast(r.message); loadProducts(page); } catch (err) { toast(err.message, true); }
                };
            });
        };
        if (mine.length) { $('#mb').onchange = () => loadProducts(0).catch(err => toast(err.message, true)); await loadProducts(0); }
    };

    adminTabs.coupon = async (panel) => {
        panel.innerHTML = '';
        panel.append(formCard('쿠폰 발급', `<label>쿠폰명<input name="couponName" required></label><label>할인율 (%, 예: 10)<input name="discountRate" type="number" step="1" min="1" max="100" required></label>
            <label>종류<select name="couponType"><option value="UNIVERSAL">전체 적용</option><option value="SPECIFIC_BRAND">특정 브랜드</option><option value="SPECIFIC_PRODUCT">특정 상품</option></select></label>
            <label>브랜드 ID (특정 브랜드일 때)<input name="brandId" type="number"></label><label>상품 ID (특정 상품일 때)<input name="productId" type="number"></label>
            <label>최대 발급 수 (비우면 무제한)<input name="maxCount" type="number" min="1"></label>`,
            d => api('POST', '/coupons', { couponName: d.couponName, discountRate: +d.discountRate, couponType: d.couponType, isLimited: !!d.maxCount, maxCount: d.maxCount ? +d.maxCount : null,
                productId: d.productId ? +d.productId : null, brands: d.brandId ? [{ brandId: +d.brandId }] : [] })));
    };

    adminTabs.report = async (panel, admin, page = 0) => {
        const j = await api('GET', `/reports?page=${page}&size=10`);
        const list = j.data.content;
        panel.innerHTML = `<div class="card"><h2 style="margin-top:0">신고 목록</h2>${list.length ? `<table><thead><tr><th>#</th><th>대상</th><th>상태</th><th>내용</th><th></th></tr></thead><tbody>
            ${list.map(r => `<tr><td>${r.id}</td><td>${esc(r.targetType)} #${r.targetId}</td><td><span class="badge">${esc(r.status)}</span></td><td>${esc(r.content)}</td>
            <td><select data-id="${r.id}" class="st"><option value="">처리…</option>${['IN_PROGRESS','APPROVED','REJECTED','RESOLVED'].map(s=>`<option>${s}</option>`).join('')}</select></td></tr>`).join('')}</tbody></table>` : '<p class="muted">신고가 없습니다.</p>'}</div>`;
        mount(panel.firstElementChild, j.data, p => adminTabs.report(panel, admin, p));
        panel.querySelectorAll('.st').forEach(s => s.onchange = async () => {
            if (!s.value) return;
            const body = { status: s.value, rejectReason: s.value === 'REJECTED' ? prompt('반려 사유') || '' : null };
            try { await api('PATCH', `/reports/${s.dataset.id}/status`, body); toast('처리되었습니다.'); adminTabs.report(panel, admin, page); } catch (err) { toast(err.message, true); }
        });
    };

    adminTabs.stats = async (panel) => {
        const today = new Date().toISOString().slice(0, 10), monthAgo = new Date(Date.now() - 30 * 864e5).toISOString().slice(0, 10);
        panel.innerHTML = `<div class="card"><h2 style="margin-top:0">집계 실행</h2><p class="muted">통계는 하루 단위로 집계된 테이블을 읽습니다. 야간 배치(매일 00:00)를 기다리지 않으려면 날짜를 골라 즉시 집계하세요. 같은 날짜를 다시 돌리면 덮어씁니다.</p>
            <form class="row" id="agg"><input type="date" name="date" value="${today}"><button class="btn sm primary">이 날짜 집계</button><span class="muted" id="aggout"></span></form></div>
            <div class="card"><h2 style="margin-top:0">매출 통계</h2><form class="row" id="sf">
            <select name="kind"><option value="sales">매출액</option><option value="count">판매 수량</option></select>
            <select name="periodType"><option selected>DAY</option><option>MONTH</option><option>YEAR</option></select>
            <select name="orderStatus"><option value="">모든 주문 상태</option>${Object.entries(STATUS_KO).map(([k, v]) => `<option value="${k}" ${k === 'PAYMENTCOMPLETED' ? '' : ''}>${v}</option>`).join('')}</select>
            <input type="date" name="startDate" value="${monthAgo}"><input type="date" name="endDate" value="${today}"><button class="btn sm">조회</button></form><div id="out" style="margin-top:12px"></div></div>`;
        $('#agg').onsubmit = async e => {
            e.preventDefault();
            try { const r = await api('POST', `/admin/statistics/aggregate${qs(Object.fromEntries(new FormData(e.target)))}`); $('#aggout').textContent = `${r.date} 집계 완료: ${r.rows}건`; toast('집계되었습니다.'); $('#sf').requestSubmit(); } catch (err) { toast(err.message, true); }
        };
        $('#sf').onsubmit = async e => {
            e.preventDefault();
            const d = Object.fromEntries(new FormData(e.target)); const kind = d.kind; delete d.kind;
            try {
                const r = await api('GET', `/admin/statistics/${kind}${qs(d)}`);
                const h = r.headerData, rows = r.bodyDataList || [];
                // headerData.rowList = 날짜 축, columnList = 브랜드 축(+총합); bodyDataList[brand].dataList[date]
                const dates = h?.rowList || [], brands = h?.columnList || [];
                $('#out').innerHTML = dates.length ? `<table><thead><tr><th>브랜드 \\ 날짜</th>${dates.map(c => `<th>${esc(c)}</th>`).join('')}</tr></thead><tbody>${rows.map((b, i) => `<tr><th>${esc(brands[i] ?? '')}</th>${b.dataList.map(v => `<td>${kind === 'sales' ? won(v) : esc(v)}</td>`).join('')}</tr>`).join('')}</tbody></table>` : '<p class="muted">해당 기간에 집계된 데이터가 없습니다. 위에서 먼저 집계를 실행하세요.</p>';
            } catch (err) { toast(err.message, true); }
        };
        $('#sf').requestSubmit();
    };

    /* ---------- router ---------- */
    const TITLES = { wishes: '찜한 상품', home: '홈', login: '로그인', signup: '회원가입', brand: '브랜드', product: '상품', cart: '장바구니', pay: '결제', orders: '주문 내역', me: '내 정보', admin: '관리' };
    const route = async () => {
        const [, page = '', arg] = location.hash.split('/');
        document.title = `${TITLES[page || 'home'] || '無shinsa'} · 無shinsa`;
        window.scrollTo(0, 0);
        renderNav();
        $('#chat-fab').hidden = !tokens.access;
        const fn = pages[page || 'home'];
        if (!fn) { app.innerHTML = '<div class="empty">페이지를 찾을 수 없습니다.</div>'; return; }
        try { await fn(arg); } catch (err) { app.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
    };
    window.addEventListener('hashchange', route);
    // 다른 탭에서 로그인/로그아웃하면 이 탭도 따라간다
    window.addEventListener('storage', e => { if (e.key === 'accessToken') loadMe().then(route); });
    loadMe().then(route);
})();
