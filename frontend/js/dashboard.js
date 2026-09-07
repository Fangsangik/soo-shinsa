// 대시보드 관리
class DashboardManager {
    constructor() {
        this.currentPage = 0;
        this.pageSize = APP_CONFIG.PAGE_SIZE;
        this.searchTimer = null;
    }

    // 업주 대시보드 표시
    async showVendorDashboard() {
        if (!requireAuth('VENDOR')) return;

        hideAllDashboards();
        document.getElementById('vendorDashboard').style.display = 'block';
        
        await this.loadVendorData();
    }

    // 관리자 대시보드 표시
    async showAdminDashboard() {
        if (!requireAuth('ADMIN')) return;

        hideAllDashboards();
        document.getElementById('adminDashboard').style.display = 'block';
        
        await this.loadAdminData();
    }

    // 일반 사용자 대시보드 표시
    async showUserDashboard() {
        hideAllDashboards();
        document.getElementById('userDashboard').style.display = 'block';
        
        await this.loadUserData();
    }

    // 업주 데이터 로드
    async loadVendorData() {
        try {
            const brands = await API.getVendorBrands();
            this.renderVendorStats(brands);
            this.renderVendorBrands(brands);
        } catch (error) {
            console.error('Error loading vendor data:', error);
        }
    }

    // 업주 통계 렌더링
    renderVendorStats(brands) {
        const total = brands.length;
        const pending = brands.filter(b => b.status === 'APPLY').length;
        const approved = brands.filter(b => b.status === 'OPEN').length;
        const rejected = brands.filter(b => b.status === 'REJECT').length;

        document.getElementById('totalBrands').textContent = total;
        document.getElementById('pendingBrands').textContent = pending;
        document.getElementById('approvedBrands').textContent = approved;
        document.getElementById('rejectedBrands').textContent = rejected;
    }

    // 업주 브랜드 목록 렌더링
    renderVendorBrands(brands) {
        const container = document.getElementById('vendorBrandList');
        
        if (brands.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🏪</div>
                    <h3>등록된 브랜드가 없습니다</h3>
                    <p>첫 번째 브랜드를 신청해보세요!</p>
                </div>
            `;
            return;
        }

        container.innerHTML = brands.map(brand => this.createBrandCard(brand)).join('');
    }

    // 관리자 데이터 로드
    async loadAdminData() {
        try {
            const response = await API.getPendingBrands();
            const pendingBrands = response.content || [];
            
            document.getElementById('adminPendingCount').textContent = response.totalElements || 0;
            document.getElementById('monthlyProcessed').textContent = '0'; // TODO: 실제 데이터
            
            this.renderPendingBrands(pendingBrands);
        } catch (error) {
            console.error('Error loading admin data:', error);
        }
    }

    // 승인 대기 브랜드 렌더링
    renderPendingBrands(brands) {
        const container = document.getElementById('adminPendingBrands');
        
        if (brands.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">✅</div>
                    <h3>승인 대기 중인 브랜드가 없습니다</h3>
                    <p>모든 브랜드가 처리되었습니다!</p>
                </div>
            `;
            return;
        }

        container.innerHTML = brands.map(brand => this.createPendingBrandCard(brand)).join('');
    }

    // 일반 사용자 데이터 로드
    async loadUserData() {
        try {
            const brands = await API.getAllBrands();
            // OPEN 상태의 브랜드만 필터링
            const approvedBrands = Array.isArray(brands) ? 
                brands.filter(b => b.status === 'OPEN') : [];
            this.renderUserBrands(approvedBrands);
        } catch (error) {
            console.error('Error loading user data:', error);
        }
    }

    // 일반 사용자 브랜드 렌더링
    renderUserBrands(brands) {
        const container = document.getElementById('userBrandGrid');
        
        if (brands.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🔍</div>
                    <h3>승인된 브랜드가 없습니다</h3>
                    <p>곧 새로운 브랜드들이 입점될 예정입니다!</p>
                </div>
            `;
            return;
        }

        container.innerHTML = brands.map(brand => this.createUserBrandCard(brand)).join('');
    }

    // 브랜드 카드 생성 (업주용)
    createBrandCard(brand) {
        const statusInfo = utils.getStatusInfo(brand.status);
        
        return `
            <div class="brand-card ${statusInfo.class}">
                <div class="brand-card-header">
                    <div>
                        <h3 class="brand-name">${brand.name}</h3>
                        <p class="brand-registration">${brand.registrationNum}</p>
                    </div>
                    <span class="status-badge ${statusInfo.class}">${statusInfo.text}</span>
                </div>
                <p class="brand-context">${brand.context}</p>
                <div class="brand-details">
                    <div class="brand-detail-item">
                        <span class="brand-detail-label">신청일</span>
                        <span class="brand-detail-value">${utils.formatDate(brand.createdAt)}</span>
                    </div>
                    ${brand.approvalDate ? `
                    <div class="brand-detail-item">
                        <span class="brand-detail-label">승인일</span>
                        <span class="brand-detail-value">${utils.formatDate(brand.approvalDate)}</span>
                    </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    // 승인 대기 브랜드 카드 생성 (관리자용)
    createPendingBrandCard(brand) {
        return `
            <div class="pending-brand">
                <div class="pending-brand-header">
                    <div class="pending-brand-info">
                        <h3>${brand.name}</h3>
                        <p class="pending-brand-registration">사업자번호: ${brand.registrationNum}</p>
                    </div>
                    <span class="status-badge apply">승인 대기</span>
                </div>
                <p class="pending-brand-context">${brand.context}</p>
                
                <div class="approval-actions">
                    <button onclick="showApprovalForm(${brand.id})" class="btn-success">
                        ✅ 승인
                    </button>
                    <button onclick="showRejectionForm(${brand.id})" class="btn-danger">
                        ❌ 거절
                    </button>
                </div>

                <!-- 승인 폼 -->
                <div id="approvalForm${brand.id}" class="approval-form">
                    <h4>승인 처리</h4>
                    <textarea id="approvalReason${brand.id}" placeholder="승인 사유를 입력하세요..." class="approval-textarea" required></textarea>
                    <textarea id="approvalComment${brand.id}" placeholder="관리자 코멘트 (선택사항)" class="approval-textarea"></textarea>
                    <div class="button-group">
                        <button onclick="processBrandApproval(${brand.id})" class="btn-success">승인하기</button>
                        <button onclick="hideApprovalForm(${brand.id})" class="btn-secondary">취소</button>
                    </div>
                </div>

                <!-- 거절 폼 -->
                <div id="rejectionForm${brand.id}" class="approval-form">
                    <h4>거절 처리</h4>
                    <textarea id="rejectionReason${brand.id}" placeholder="거절 사유를 입력하세요..." class="approval-textarea" required></textarea>
                    <textarea id="rejectionComment${brand.id}" placeholder="관리자 코멘트 (선택사항)" class="approval-textarea"></textarea>
                    <div class="button-group">
                        <button onclick="processBrandRejection(${brand.id})" class="btn-danger">거절하기</button>
                        <button onclick="hideRejectionForm(${brand.id})" class="btn-secondary">취소</button>
                    </div>
                </div>
            </div>
        `;
    }

    // 일반 사용자 브랜드 카드 생성
    createUserBrandCard(brand) {
        return `
            <div class="brand-card hover-lift">
                <div class="brand-card-header">
                    <div>
                        <h3 class="brand-name">${brand.name}</h3>
                        <p class="brand-registration">사업자번호: ${brand.registrationNum}</p>
                    </div>
                    <span class="status-badge open">승인됨</span>
                </div>
                <p class="brand-context">${brand.context}</p>
                <div class="brand-details">
                    <div class="brand-detail-item">
                        <span class="brand-detail-label">승인일</span>
                        <span class="brand-detail-value">${utils.formatDate(brand.approvalDate)}</span>
                    </div>
                </div>
            </div>
        `;
    }
}

// 전역 DashboardManager 인스턴스
const dashboardManager = new DashboardManager();

// 대시보드 전환 함수들
function showVendorDashboard() {
    dashboardManager.showVendorDashboard();
}

function showAdminDashboard() {
    dashboardManager.showAdminDashboard();
}

function showUserDashboard() {
    dashboardManager.showUserDashboard();
}

function showDashboardSelector() {
    hideAllDashboards();
    document.getElementById('shoppingHome').style.display = 'block';
}

function hideAllDashboards() {
    const dashboards = ['shoppingHome', 'vendorDashboard', 'adminDashboard', 'userDashboard'];
    dashboards.forEach(id => {
        const element = document.getElementById(id);
        if (element) element.style.display = 'none';
    });
}

// 승인/거절 폼 관리
function showApprovalForm(brandId) {
    hideRejectionForm(brandId);
    const form = document.getElementById(`approvalForm${brandId}`);
    form.classList.add('active');
}

function hideApprovalForm(brandId) {
    const form = document.getElementById(`approvalForm${brandId}`);
    form.classList.remove('active');
}

function showRejectionForm(brandId) {
    hideApprovalForm(brandId);
    const form = document.getElementById(`rejectionForm${brandId}`);
    form.classList.add('active');
}

function hideRejectionForm(brandId) {
    const form = document.getElementById(`rejectionForm${brandId}`);
    form.classList.remove('active');
}

// 브랜드 승인 처리
async function processBrandApproval(brandId) {
    const reasonElement = document.getElementById(`approvalReason${brandId}`);
    const commentElement = document.getElementById(`approvalComment${brandId}`);
    
    const reason = reasonElement.value.trim();
    const comment = commentElement.value.trim();
    
    if (!reason) {
        showNotification('승인 사유를 입력해주세요.', 'warning');
        reasonElement.focus();
        return;
    }
    
    try {
        await API.approveBrand(brandId, reason, comment);
        hideApprovalForm(brandId);
        dashboardManager.loadAdminData(); // 데이터 새로고침
    } catch (error) {
        console.error('Error approving brand:', error);
    }
}

// 브랜드 거절 처리
async function processBrandRejection(brandId) {
    const reasonElement = document.getElementById(`rejectionReason${brandId}`);
    const commentElement = document.getElementById(`rejectionComment${brandId}`);
    
    const reason = reasonElement.value.trim();
    const comment = commentElement.value.trim();
    
    if (!reason) {
        showNotification('거절 사유를 입력해주세요.', 'warning');
        reasonElement.focus();
        return;
    }
    
    try {
        await API.rejectBrand(brandId, reason, comment);
        hideRejectionForm(brandId);
        dashboardManager.loadAdminData(); // 데이터 새로고침
    } catch (error) {
        console.error('Error rejecting brand:', error);
    }
}

