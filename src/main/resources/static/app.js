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
    const api = async (method, path, body, retry = true) => {
        const headers = {};
        if (tokens.access) headers.Authorization = 'Bearer ' + tokens.access;
        if (body && !(body instanceof FormData)) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(body); }
        const res = await fetch(path, { method, headers, body });
        if (res.status === 401 && retry && tokens.refresh) {
            const ok = await refreshToken();
            if (ok) return api(method, path, body instanceof FormData ? body : (body && JSON.parse(body)), false);
        }
        if (res.status === 204) return null;
        const text = await res.text();
        let json = null; try { json = text ? JSON.parse(text) : null; } catch { /* html error page */ }
        if (!res.ok) {
            if (res.status === 401 || res.status === 403) { if (!tokens.access) location.hash = '#/login'; }
            throw new Error(json?.message || json?.error || `요청 실패 (${res.status})`);
        }
        return json;
    };
    const refreshToken = async () => {
        try {
            const res = await fetch('/users/refresh', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: tokens.refresh, accessToken: tokens.access }) });
            if (!res.ok) throw 0;
            const j = await res.json(); tokens.set(j.data); return true;
        } catch { tokens.clear(); return false; }
    };
    const loadMe = async () => {
        if (!tokens.access) { state.user = null; return null; }
        try { state.user = (await api('GET', '/users')).data; } catch { state.user = null; }
        return state.user;
    };
    const isStaff = () => ['ADMIN', 'VENDOR'].includes(state.user?.role);
    const requireLogin = () => { if (!tokens.access) { toast('로그인이 필요합니다.'); location.hash = '#/login'; return false; } return true; };

    /* ---------- nav ---------- */
    const renderNav = () => {
        const here = location.hash.split('/')[1] || '';
        const link = (h, t) => `<a href="#/${h}" class="${here === h ? 'active' : ''}">${t}</a>`;
        nav.innerHTML = tokens.access
            ? link('', '브랜드') + link('cart', '장바구니') + link('orders', '주문') + (isStaff() ? link('admin', '관리') : '') + link('me', esc(state.user?.name || '내 정보'))
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
            </form>
            <div class="grid" id="g" style="margin-top:16px"></div>`;
        const load = async () => {
            const f = Object.fromEntries(new FormData($('#filter')));
            const j = await api('GET', `/products/brands/${id}${qs({ ...f, page: 0, size: 60 })}`);
            const list = j.data.content;
            $('#g').innerHTML = list.length ? list.map(p => `
                <a class="tile" href="#/product/${p.id}">
                    <div class="thumb"><img src="${img(p.imageUrl, p.id)}" alt="" loading="lazy"></div>
                    <div class="body"><div class="name">${esc(p.name)}</div><div class="price">${won(p.price)}</div>
                    <span class="badge ${p.status === 'AVAILABLE' ? 'ok' : 'warn'}">${esc(p.status)}</span></div>
                </a>`).join('') : '<div class="empty">상품이 없습니다.</div>';
        };
        $('#filter').onsubmit = e => { e.preventDefault(); load().catch(err => toast(err.message, true)); };
        $('#report').onclick = () => reportDialog('BRAND', id);
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
                    <button class="btn ghost sm" type="button" id="report">신고</button>
                </div>
            </form></div></div></div>
            <h2>리뷰</h2>
            <form class="row" id="rf"><select name="minRate"><option value="">최소 별점</option>${[1,2,3,4,5].map(n=>`<option>${n}</option>`).join('')}</select>
                <select name="maxRate"><option value="">최대 별점</option>${[1,2,3,4,5].map(n=>`<option>${n}</option>`).join('')}</select><button class="btn sm">필터</button></form>
            <div id="reviews" style="margin-top:12px"></div>`;
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
        const loadReviews = async () => {
            const f = Object.fromEntries(new FormData($('#rf')));
            const j = await api('GET', `/reviews/products/${id}${qs({ ...f, page: 0, size: 50 })}`);
            const list = j.data.content;
            $('#reviews').innerHTML = list.length ? list.map(r => `<div class="card">
                <div class="row between"><span class="stars">${'★'.repeat(r.rate)}${'☆'.repeat(5 - r.rate)}</span>
                <button class="btn ghost sm" data-report="${r.id}">신고</button></div>
                <p style="margin:8px 0 0">${esc(r.content)}</p>${r.imageUrl ? `<img src="${esc(r.imageUrl)}" style="max-width:240px;border-radius:8px;margin-top:8px">` : ''}
            </div>`).join('') : '<div class="empty">아직 리뷰가 없습니다. 구매 후 주문 페이지에서 리뷰를 남길 수 있어요.</div>';
            $('#reviews').querySelectorAll('[data-report]').forEach(b => b.onclick = () => reportDialog('REVIEW', +b.dataset.report));
        };
        $('#rf').onsubmit = e => { e.preventDefault(); loadReviews().catch(err => toast(err.message, true)); };
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
            <div class="row between" style="margin-top:16px"><strong>총 ${won(total)}</strong><button class="btn primary" id="order">전체 주문하기</button></div>`;
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
                if (!coupons.length) { toast('사용 가능한 쿠폰이 없습니다.'); return; }
                openModal(`<h2 style="margin-top:0">쿠폰 적용</h2><label>쿠폰<select name="couponId">${coupons.map(c => `<option value="${c.id}">${esc(c.couponName)} · ${Number(c.discountRate)}% (~${esc(c.expirationDate)})</option>`).join('')}</select></label>`, async d => {
                    const r = await api('POST', `/carts/${id}/apply-coupon`, { couponId: +d.couponId });
                    toast(`${r.message} 할인가 ${won(r.data.discountedPrice)}`); pages.cart();
                });
            };
        });
        $('#order').onclick = async () => {
            try {
                const r = await api('POST', '/orders/carts'); toast(r.message);
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

    pages.orders = async () => {
        if (!requireLogin()) return;
        app.innerHTML = `<h1>주문 내역</h1>
            <form class="row" id="f"><label>시작일<input type="date" name="startDate"></label><label>종료일<input type="date" name="endDate"></label><button class="btn sm" style="align-self:end">조회</button></form>
            <div id="list" style="margin-top:16px"></div>`;
        const load = async () => {
            const f = Object.fromEntries(new FormData($('#f')));
            const j = await api('GET', `/orders/users${qs({ ...f, page: 0, size: 50 })}`);
            const orders = j.data.content;
            $('#list').innerHTML = orders.length ? orders.map(o => `<div class="card" data-id="${o.id}">
                <div class="row between"><div><strong>${esc(o.orderId)}</strong> <span class="badge ${o.status === 'PAYMENTCOMPLETED' ? 'ok' : o.status === 'BEFOREPAYMENT' ? 'warn' : ''}">${STATUS_KO[o.status] || esc(o.status)}</span></div>
                    <div>${o.status === 'BEFOREPAYMENT' ? `<a class="btn primary sm" href="#/pay/${o.id}">결제하기</a>` : ''}
                    ${(ORDER_ACTIONS[o.status] || []).map(([to, label]) => `<button class="btn sm ${to === 'CANCEL' ? 'danger' : 'ghost'} act" data-to="${to}">${label}</button>`).join('')}</div></div>
                <table style="margin-top:12px"><tbody>${(o.orderItems || []).map(it => `<tr>
                    <td><a href="#/product/${it.productId}"><u>${esc(it.productName)}</u></a> × ${it.quantity}</td>
                    <td>${won(it.discountPrice ?? it.price)}</td>
                    <td style="text-align:right">${o.status !== 'BEFOREPAYMENT' && o.status !== 'CANCEL' && o.status !== 'RETURN' ? `<button class="btn sm ghost review" data-item="${it.orderItemId}" data-pid="${it.productId}">리뷰 작성</button>` : ''}</td></tr>`).join('')}
                </tbody></table>
                <div class="row between" style="margin-top:8px"><span class="muted">총 결제금액</span><strong>${won(o.totalPrice)}</strong></div>
            </div>`).join('') : '<div class="empty">주문 내역이 없습니다.</div>';
            $('#list').querySelectorAll('.card').forEach(card => {
                const id = +card.dataset.id;
                card.querySelectorAll('.act').forEach(b => b.onclick = async () => {
                    try { const r = await api('PATCH', '/orders', { orderId: id, status: b.dataset.to }); toast(r.message); load(); } catch (err) { toast(err.message, true); }
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
        $('#f').onsubmit = e => { e.preventDefault(); load().catch(err => toast(err.message, true)); };
        await load();
    };

    pages.me = async () => {
        if (!requireLogin()) return;
        const u = (await api('GET', '/users')).data;
        app.innerHTML = `<h1>내 정보</h1><div class="card">
            <table><tbody><tr><th>이름</th><td>${esc(u.name)}</td></tr><tr><th>이메일</th><td>${esc(localStorage.getItem('email'))}</td></tr>
            <tr><th>전화번호</th><td>${esc(u.phoneNum)}</td></tr><tr><th>역할</th><td>${esc(u.role)}</td></tr>
            ${u.userGrade ? `<tr><th>등급</th><td>${esc(u.userGrade)} (적립률 ${esc(u.pointRate)}, 승급 기준 ${esc(u.requirement)})</td></tr>` : ''}</tbody></table></div>
            <h2>정보 수정</h2><form class="stack" id="f">
            <label>이름<input name="name" value="${esc(u.name)}"></label><label>전화번호<input name="phoneNum" value="${esc(u.phoneNum)}"></label>
            <label>기존 비밀번호<input name="oldPassword" type="password" required></label><label>새 비밀번호<input name="newPassword" type="password" required></label>
            <button class="btn primary">저장</button></form>
            <h2>계정</h2><div class="row"><button class="btn" id="logout">로그아웃</button><button class="btn danger" id="leave">회원 탈퇴</button></div>`;
        $('#f').onsubmit = async e => { e.preventDefault(); try { const r = await api('PATCH', '/users', Object.fromEntries(new FormData(e.target))); toast('저장되었습니다.'); pages.me(); } catch (err) { toast(err.message, true); } };
        $('#logout').onclick = async () => { try { await api('POST', '/users/logout'); } catch { /* token already invalid */ } tokens.clear(); toast('로그아웃되었습니다.'); location.hash = '#/'; };
        $('#leave').onclick = () => openModal(`<h2 style="margin-top:0">정말 탈퇴하시겠어요?</h2><label>비밀번호 확인<input name="password" type="password" required></label>`, async d => {
            await api('POST', '/users/leave', d); tokens.clear(); toast('탈퇴 처리되었습니다.'); location.hash = '#/';
        });
    };

    /* ---------- admin / vendor ---------- */
    pages.admin = async () => {
        if (!requireLogin()) return;
        if (!isStaff()) { app.innerHTML = '<div class="empty">관리자 또는 입점 사업자만 접근할 수 있습니다.</div>'; return; }
        const admin = state.user.role === 'ADMIN';
        const tabs = [['brand', '브랜드/상품'], ['coupon', '쿠폰'], ...(admin ? [['category', '카테고리'], ['report', '신고 관리'], ['stats', '통계']] : [])];
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
            d => api('POST', '/brands', { ...d, subCategoryId: +d.subCategoryId })));
        panel.append(formCard('상품 등록', `<label>브랜드<select name="brandId" required>${brandOpts || '<option value="">먼저 브랜드를 만드세요</option>'}</select></label>
            <label>상품명<input name="name" required></label><label>가격<input name="price" type="number" min="0" required></label>
            <label>상태<select name="status"><option>AVAILABLE</option><option>SOLD_OUT</option><option>UNAVAILABLE</option><option>DISCONTINUED</option></select></label>
            <label>이미지 (선택)<input type="file" name="imageFile" accept="image/*"></label>`,
            (d, form) => {
                const fd = new FormData();
                fd.append('productRequestDto', new Blob([JSON.stringify({ name: d.name, price: +d.price, brandId: +d.brandId, status: d.status })], { type: 'application/json' }));
                if (form.imageFile.files[0]) fd.append('imageFile', form.imageFile.files[0]);
                return api('POST', `/products/brands/${d.brandId}`, fd);
            }));
        panel.append(formCard('상품 옵션 추가', `<label>상품 ID<input name="productId" type="number" required></label><label>사이즈<input name="size" required placeholder="M"></label>
            <label>색상<input name="color" required placeholder="Black"></label><label>상태<select name="status"><option>AVAILABLE</option><option>SOLD_OUT</option></select></label>`,
            d => api('POST', `/options/products/${d.productId}`, { size: d.size, color: d.color, status: d.status })));
        const list = document.createElement('div'); list.className = 'card';
        list.innerHTML = `<h2 style="margin-top:0">내 브랜드</h2>${mine.length ? `<table><tbody>${mine.map(b => `<tr><td>#${b.id}</td><td><a href="#/brand/${b.id}"><u>${esc(b.name)}</u></a></td><td class="muted">${esc(b.registrationNum)}</td></tr>`).join('')}</tbody></table>` : '<p class="muted">없음</p>'}`;
        panel.append(list);
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

    adminTabs.report = async (panel) => {
        const j = await api('GET', '/reports?page=0&size=100');
        const list = j.data.content;
        panel.innerHTML = `<div class="card"><h2 style="margin-top:0">신고 목록</h2>${list.length ? `<table><thead><tr><th>#</th><th>대상</th><th>상태</th><th>내용</th><th></th></tr></thead><tbody>
            ${list.map(r => `<tr><td>${r.id}</td><td>${esc(r.targetType)} #${r.targetId}</td><td><span class="badge">${esc(r.status)}</span></td><td>${esc(r.content)}</td>
            <td><select data-id="${r.id}" class="st"><option value="">처리…</option>${['IN_PROGRESS','APPROVED','REJECTED','RESOLVED'].map(s=>`<option>${s}</option>`).join('')}</select></td></tr>`).join('')}</tbody></table>` : '<p class="muted">신고가 없습니다.</p>'}</div>`;
        panel.querySelectorAll('.st').forEach(s => s.onchange = async () => {
            if (!s.value) return;
            const body = { status: s.value, rejectReason: s.value === 'REJECTED' ? prompt('반려 사유') || '' : null };
            try { await api('PATCH', `/reports/${s.dataset.id}/status`, body); toast('처리되었습니다.'); adminTabs.report(panel); } catch (err) { toast(err.message, true); }
        });
    };

    adminTabs.stats = async (panel) => {
        panel.innerHTML = `<div class="card"><h2 style="margin-top:0">매출 통계</h2><form class="row" id="sf">
            <select name="kind"><option value="sales">매출액</option><option value="count">판매 수량</option></select>
            <select name="periodType"><option>DAY</option><option selected>MONTH</option><option>YEAR</option></select>
            <input type="date" name="startDate"><input type="date" name="endDate"><button class="btn sm">조회</button></form><div id="out" style="margin-top:12px"></div></div>`;
        $('#sf').onsubmit = async e => {
            e.preventDefault();
            const d = Object.fromEntries(new FormData(e.target)); const kind = d.kind; delete d.kind;
            try {
                const r = await api('GET', `/admin/statistics/${kind}${qs(d)}`);
                const h = r.headerData, rows = r.bodyDataList || [];
                $('#out').innerHTML = h ? `<table><thead><tr>${(h.columnList || []).map(c => `<th>${esc(c)}</th>`).join('')}</tr></thead><tbody>${rows.map(b => `<tr>${b.dataList.map(v => `<td>${esc(v)}</td>`).join('')}</tr>`).join('')}</tbody></table>` : '<p class="muted">데이터 없음</p>';
            } catch (err) { toast(err.message, true); }
        };
    };

    /* ---------- router ---------- */
    const route = async () => {
        const [, page = '', arg] = location.hash.split('/');
        renderNav();
        const fn = pages[page || 'home'];
        if (!fn) { app.innerHTML = '<div class="empty">페이지를 찾을 수 없습니다.</div>'; return; }
        try { await fn(arg); } catch (err) { app.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
    };
    window.addEventListener('hashchange', route);
    loadMe().then(route);
})();
