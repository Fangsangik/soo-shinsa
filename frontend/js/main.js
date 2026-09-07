// 메인 앱 초기화 및 유틸리티 함수들

// DOM 로드 완료 후 초기화
document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
    setupEventListeners();
});

// 앱 초기화
function initializeApp() {
    // 인증 상태 확인 및 UI 업데이트
    authManager.updateUI();
    
    // 초기 화면 표시
    showShoppingHome();
    
    console.log('🏪 수신사 브랜드 관리 시스템이 시작되었습니다.');
}

// 이벤트 리스너 설정
function setupEventListeners() {
    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', (event) => {
        const modals = document.querySelectorAll('.modal');
        modals.forEach(modal => {
            if (event.target === modal) {
                modal.style.display = 'none';
            }
        });
    });
    
    // ESC 키로 모달 닫기
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            const activeModal = document.querySelector('.modal[style*="block"]');
            if (activeModal) {
                activeModal.style.display = 'none';
            }
        }
    });
    
    // 브랜드 신청 폼 처리
    const brandForm = document.getElementById('brandForm');
    if (brandForm) {
        brandForm.addEventListener('submit', handleBrandSubmission);
    }
    
    // 상품 검색 (입력 디바운스 + 버튼/엔터)
    const searchInput = document.querySelector('.search-input');
    const searchBtn = document.querySelector('.search-btn');
    if (searchInput) {
        searchInput.addEventListener('input', utils.debounce(() => searchProducts(searchInput.value), 300));
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') searchProducts(searchInput.value);
        });
    }
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', () => searchProducts(searchInput.value));
    }
}

// 브랜드 신청 폼 처리
async function handleBrandSubmission(event) {
    event.preventDefault();
    
    if (!requireAuth('VENDOR')) return;
    
    const form = event.target;
    const submitButton = form.querySelector('button[type="submit"]');
    
    // 폼 데이터 수집
    const brandData = {
        registrationNum: document.getElementById('registrationNum').value.trim(),
        name: document.getElementById('brandName').value.trim(),
        context: document.getElementById('brandContext').value.trim(),
        subCategoryId: parseInt(document.getElementById('subCategoryId').value)
    };
    
    // 유효성 검사
    if (!validateBrandData(brandData)) {
        return;
    }
    
    try {
        // 로딩 상태
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="loading"></span> 신청 중...';
        
        // API 호출
        await API.createBrand(brandData);
        
        // 성공 처리
        closeModal('brandFormModal');
        form.reset();
        
        // 업주 대시보드가 활성화되어 있으면 데이터 새로고침
        const vendorDashboard = document.getElementById('vendorDashboard');
        if (vendorDashboard.style.display === 'block') {
            dashboardManager.loadVendorData();
        }
        
    } catch (error) {
        console.error('Brand submission error:', error);
    } finally {
        // 로딩 상태 해제
        submitButton.disabled = false;
        submitButton.innerHTML = '신청하기';
    }
}

// 브랜드 데이터 유효성 검사
function validateBrandData(data) {
    if (!data.registrationNum) {
        showNotification('사업자등록번호를 입력해주세요.', 'warning');
        document.getElementById('registrationNum').focus();
        return false;
    }
    
    if (!/^\d{3}-\d{2}-\d{5}$/.test(data.registrationNum)) {
        showNotification('사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)', 'warning');
        document.getElementById('registrationNum').focus();
        return false;
    }
    
    if (!data.name) {
        showNotification('브랜드명을 입력해주세요.', 'warning');
        document.getElementById('brandName').focus();
        return false;
    }
    
    if (data.name.length < 2 || data.name.length > 50) {
        showNotification('브랜드명은 2-50자 사이로 입력해주세요.', 'warning');
        document.getElementById('brandName').focus();
        return false;
    }
    
    if (!data.context) {
        showNotification('브랜드 소개를 입력해주세요.', 'warning');
        document.getElementById('brandContext').focus();
        return false;
    }
    
    if (data.context.length < 10 || data.context.length > 500) {
        showNotification('브랜드 소개는 10-500자 사이로 입력해주세요.', 'warning');
        document.getElementById('brandContext').focus();
        return false;
    }
    
    if (!data.subCategoryId) {
        showNotification('카테고리를 선택해주세요.', 'warning');
        document.getElementById('subCategoryId').focus();
        return false;
    }
    
    return true;
}

// 모달 관리
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        
        // 폼 초기화
        const form = modal.querySelector('form');
        if (form) {
            form.reset();
        }
    }
}

function showBrandForm() {
    if (!requireAuth('VENDOR')) return;
    
    const modal = document.getElementById('brandFormModal');
    modal.style.display = 'block';
    
    // 첫 번째 입력 필드에 포커스
    setTimeout(() => {
        document.getElementById('registrationNum').focus();
    }, 100);
}

// 알림 시스템
function showNotification(message, type = 'info', duration = 5000) {
    const notification = document.getElementById('notification');
    
    // 기존 클래스 제거
    notification.className = 'notification';
    
    // 새 클래스 및 메시지 설정
    notification.classList.add(type);
    notification.textContent = message;
    
    // 표시
    notification.classList.add('show');
    
    // 자동 숨김
    setTimeout(() => {
        notification.classList.remove('show');
    }, duration);
}

// 페이지 새로고침
function refreshCurrentDashboard() {
    const vendorDashboard = document.getElementById('vendorDashboard');
    const adminDashboard = document.getElementById('adminDashboard');
    const userDashboard = document.getElementById('userDashboard');
    
    if (vendorDashboard.style.display === 'block') {
        dashboardManager.loadVendorData();
    } else if (adminDashboard.style.display === 'block') {
        dashboardManager.loadAdminData();
    } else if (userDashboard.style.display === 'block') {
        dashboardManager.loadUserData();
    }
}

// 에러 처리
window.addEventListener('error', (event) => {
    console.error('Global error:', event.error);
    showNotification('예상치 못한 오류가 발생했습니다. 페이지를 새로고침해주세요.', 'error');
});

// 네트워크 상태 모니터링
window.addEventListener('online', () => {
    showNotification('네트워크 연결이 복구되었습니다.', 'success');
});

window.addEventListener('offline', () => {
    showNotification('네트워크 연결이 끊어졌습니다. 연결을 확인해주세요.', 'warning');
});

// Admin 관련 함수들
function showAdminOptions() {
    const modal = document.getElementById('adminOptionsModal');
    modal.style.display = 'block';
}

function showAdminLogin() {
    closeModal('adminOptionsModal');
    const modal = document.getElementById('adminLoginModal');
    modal.style.display = 'block';
}

function showAdminSignup() {
    closeModal('adminOptionsModal');
    const modal = document.getElementById('adminSignupModal');
    modal.style.display = 'block';
}

// 관리자 로그인 폼 처리
document.addEventListener('DOMContentLoaded', () => {
    const adminLoginForm = document.getElementById('adminLoginForm');
    if (adminLoginForm) {
        adminLoginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('adminLoginEmail').value;
            const password = document.getElementById('adminLoginPassword').value;
            const submitButton = e.target.querySelector('button[type="submit"]');
            
            try {
                submitButton.disabled = true;
                submitButton.innerHTML = '<span class="loading"></span> 로그인 중...';
                
                await authManager.login(email, password);
                
                showNotification('관리자 로그인에 성공했습니다!', 'success');
                closeModal('adminLoginModal');
                authManager.updateUI();
                showAdminDashboard();
                
                adminLoginForm.reset();
                
            } catch (error) {
                showNotification(error.message, 'error');
            } finally {
                submitButton.disabled = false;
                submitButton.innerHTML = '로그인';
            }
        });
    }
    
    // 관리자 회원가입 폼 처리
    const adminSignupForm = document.getElementById('adminSignupForm');
    if (adminSignupForm) {
        adminSignupForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const formData = {
                email: document.getElementById('adminSignupEmail').value,
                name: document.getElementById('adminSignupName').value,
                phoneNum: document.getElementById('adminSignupPhone').value,
                password: document.getElementById('adminSignupPassword').value,
                adminKey: document.getElementById('adminSecretKey').value
            };
            
            const submitButton = e.target.querySelector('button[type="submit"]');
            
            try {
                submitButton.disabled = true;
                submitButton.innerHTML = '<span class="loading"></span> 계정 생성 중...';
                
                const response = await fetch(`${API_CONFIG.BASE_URL}/users/signin`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });
                
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '관리자 계정 생성에 실패했습니다.');
                }
                
                showNotification('관리자 계정이 성공적으로 생성되었습니다!', 'success');
                closeModal('adminSignupModal');
                adminSignupForm.reset();
                
            } catch (error) {
                showNotification(error.message, 'error');
            } finally {
                submitButton.disabled = false;
                submitButton.innerHTML = '관리자 계정 생성';
            }
        });
    }
});

// 쇼핑몰 기능들
function showShoppingHome() {
    hideAllSections();
    document.getElementById('shoppingHome').style.display = 'block';
    loadFeaturedBrands();
}

function hideAllSections() {
    const sections = ['shoppingHome', 'vendorDashboard', 'adminDashboard', 'userDashboard'];
    sections.forEach(id => {
        const element = document.getElementById(id);
        if (element) element.style.display = 'none';
    });
}

function scrollToProducts() {
    document.getElementById('featuredBrands').scrollIntoView({ 
        behavior: 'smooth' 
    });
}

function filterByCategory(categoryId) {
    searchProducts('', categoryId);
}

// 상품 통합 검색. 키워드가 비고 카테고리도 없으면 추천 브랜드 화면으로 되돌린다.
async function searchProducts(keyword = '', categoryId = null) {
    const showcase = document.getElementById('brandShowcase');
    if (!showcase) return;

    const term = (keyword || '').trim();
    if (!term && !categoryId) {
        loadFeaturedBrands();
        return;
    }

    showcase.innerHTML = '<div class="empty-state"><span class="loading"></span> 검색 중...</div>';
    const result = await API.searchProducts({ keyword: term, categoryId });
    const products = result.content || [];

    if (products.length === 0) {
        showcase.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🔍</div>
                <h3>검색 결과가 없습니다</h3>
                <p>다른 검색어로 다시 시도해보세요.</p>
            </div>
        `;
        return;
    }

    showcase.innerHTML = products.map(p => `
        <div class="brand-showcase-card">
            <div class="brand-logo">${p.imageUrl ? `<img src="${p.imageUrl}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:inherit;">` : (p.name || '?').charAt(0)}</div>
            <h3>${p.name}</h3>
            <p>${Number(p.price).toLocaleString('ko-KR')}원</p>
        </div>
    `).join('');
}

function showAllBrands() {
    showUserDashboard();
}

function showCart() {
    // TODO: 장바구니 기능 구현
    showNotification('장바구니 기능은 개발 예정입니다.', 'info');
}

async function loadFeaturedBrands() {
    const showcase = document.getElementById('brandShowcase');
    
    try {
        // 승인된 브랜드들을 가져옴
        const brands = await API.getAllBrands(0, 50);
        const approvedBrands = brands.filter(brand => brand.status === 'OPEN').slice(0, 6);
        
        if (approvedBrands.length === 0) {
            showcase.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🏪</div>
                    <h3>아직 등록된 브랜드가 없습니다</h3>
                    <p>다양한 브랜드들이 곧 등록될 예정입니다.</p>
                </div>
            `;
            return;
        }
        
        showcase.innerHTML = approvedBrands.map(brand => `
            <div class="brand-showcase-card" onclick="viewBrandDetails('${brand.id}')">
                <div class="brand-logo">${brand.name.charAt(0)}</div>
                <h3>${brand.name}</h3>
                <p>${brand.context}</p>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading featured brands:', error);
        showcase.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <h3>브랜드를 불러오지 못했습니다</h3>
                <p>잠시 후 다시 시도해주세요.</p>
            </div>
        `;
    }
}

function viewBrandDetails(brandId) {
    // TODO: 브랜드 상세 페이지 구현
    showNotification('브랜드 상세 페이지를 준비 중입니다.', 'info');
}

// 기존 showDashboardSelector 함수를 showShoppingHome으로 대체
function showDashboardSelector() {
    showShoppingHome();
}

// 개발자 모드 감지
if (typeof process !== 'undefined' && process?.env?.NODE_ENV === 'development') {
    console.log('🔧 개발자 모드에서 실행 중입니다.');
    
    // 개발자 도구
    window.dev = {
        auth: authManager,
        api: apiManager,
        dashboard: dashboardManager,
        showNotification,
        API
    };
}

// 브라우저 뒤로가기/앞으로가기 처리
window.addEventListener('popstate', (event) => {
    // 현재는 SPA이므로 쇼핑몰 홈으로 이동
    showShoppingHome();
});

// 성능 모니터링 (개발 환경)
if ('performance' in window) {
    window.addEventListener('load', () => {
        setTimeout(() => {
            const perfData = performance.getEntriesByType('navigation')[0];
            console.log(`🚀 페이지 로드 시간: ${Math.round(perfData.loadEventEnd - perfData.loadEventStart)}ms`);
        }, 0);
    });
}