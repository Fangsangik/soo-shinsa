// 인증 관리
class AuthManager {
    constructor() {
        this.token = utils.storage.get(APP_CONFIG.TOKEN_KEY);
        this.user = utils.storage.get(APP_CONFIG.USER_KEY);
        this.refreshToken = utils.storage.get(APP_CONFIG.REFRESH_TOKEN_KEY);
    }

    // 로그인
    async login(email, password) {
        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.LOGIN}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) {
                throw new Error('로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
            }

            const data = await response.json();
            
            // 토큰 저장
            this.token = data.data.accessToken;
            this.refreshToken = data.data.refreshToken;
            this.user = { email: data.data.email };

            utils.storage.set(APP_CONFIG.TOKEN_KEY, this.token);
            utils.storage.set(APP_CONFIG.REFRESH_TOKEN_KEY, this.refreshToken);
            utils.storage.set(APP_CONFIG.USER_KEY, this.user);

            return data;
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    }

    // 로그아웃
    logout() {
        this.token = null;
        this.user = null;
        this.refreshToken = null;
        
        utils.storage.clear();
        
        // UI 업데이트
        this.updateUI();
        showDashboardSelector();
    }

    // 인증 상태 확인
    isAuthenticated() {
        return !!this.token;
    }

    // 토큰 가져오기
    getToken() {
        return this.token;
    }

    // 사용자 정보 가져오기
    getUser() {
        return this.user;
    }

    // 사용자 역할 추측 (실제로는 JWT에서 파싱해야 함)
    getUserRole() {
        if (!this.user || !this.user.email) return 'USER';
        
        // 임시로 이메일 기반으로 역할 판단
        if (this.user.email.includes('admin')) return 'ADMIN';
        if (this.user.email.includes('vendor')) return 'VENDOR';
        return 'USER';
    }

    // UI 업데이트
    updateUI() {
        const userProfile = document.getElementById('userProfile');
        const userName = document.getElementById('userName');

        if (this.isAuthenticated() && userProfile && userName) {
            userProfile.style.display = 'flex';
            userName.textContent = this.user.email;
        } else if (userProfile) {
            userProfile.style.display = 'none';
        }
    }

    // 권한 확인
    hasRole(requiredRole) {
        const userRole = this.getUserRole();
        
        // ADMIN은 모든 권한 보유
        if (userRole === 'ADMIN') return true;
        
        // VENDOR는 VENDOR, USER 권한 보유
        if (userRole === 'VENDOR' && (requiredRole === 'VENDOR' || requiredRole === 'USER')) {
            return true;
        }
        
        // USER는 USER 권한만 보유
        if (userRole === 'USER' && requiredRole === 'USER') {
            return true;
        }
        
        return userRole === requiredRole;
    }

    // 인증 헤더 생성
    getAuthHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }
}

// 전역 AuthManager 인스턴스
const authManager = new AuthManager();

// 로그인 폼 처리
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('loginEmail').value;
            const password = document.getElementById('loginPassword').value;
            const submitButton = loginForm.querySelector('button[type="submit"]');
            
            try {
                // 로딩 상태
                submitButton.disabled = true;
                submitButton.innerHTML = '<span class="loading"></span> 로그인 중...';
                
                await authManager.login(email, password);
                
                // 성공
                showNotification('로그인에 성공했습니다!', 'success');
                closeModal('loginModal');
                authManager.updateUI();
                
                // 폼 초기화
                loginForm.reset();
                
            } catch (error) {
                showNotification(error.message, 'error');
            } finally {
                // 로딩 상태 해제
                submitButton.disabled = false;
                submitButton.innerHTML = '로그인';
            }
        });
    }
    
    // 초기 UI 업데이트
    authManager.updateUI();
});

// 로그아웃 함수
function logout() {
    authManager.logout();
    showNotification('로그아웃되었습니다.', 'info');
}

// 로그인 모달 표시
function showLogin() {
    if (authManager.isAuthenticated()) {
        showNotification('이미 로그인되어 있습니다.', 'info');
        return;
    }
    
    const modal = document.getElementById('loginModal');
    modal.style.display = 'block';
}

// 인증 필요 함수들
function requireAuth(requiredRole = null) {
    if (!authManager.isAuthenticated()) {
        showNotification('로그인이 필요합니다.', 'warning');
        showLogin();
        return false;
    }
    
    if (requiredRole && !authManager.hasRole(requiredRole)) {
        showNotification('접근 권한이 없습니다.', 'error');
        return false;
    }
    
    return true;
}