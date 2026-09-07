// API 설정
const API_CONFIG = {
    BASE_URL: 'http://localhost:8080/api/v1',
    ENDPOINTS: {
        LOGIN: '/users/login',
        BRANDS: '/brands',
        ADMIN_PENDING: '/brands/admin/pending',
        ADMIN_APPROVE: '/brands/admin/{id}/approve',
        ADMIN_REJECT: '/brands/admin/{id}/reject',
        VENDOR_BRANDS: '/brands/vendor',
        CATEGORIES: '/categories',
        PRODUCT_SEARCH: '/products/search'
    }
};

// 앱 설정
const APP_CONFIG = {
    TOKEN_KEY: 'soo_shinsa_token',
    USER_KEY: 'soo_shinsa_user',
    REFRESH_TOKEN_KEY: 'soo_shinsa_refresh_token',
    PAGE_SIZE: 10
};

// 상태 매핑
const STATUS_MAP = {
    APPLY: { text: '승인 대기', class: 'apply' },
    OPEN: { text: '승인됨', class: 'open' },
    REJECT: { text: '거절됨', class: 'refuse' }
};

// 역할 매핑
const ROLE_MAP = {
    ADMIN: '관리자',
    VENDOR: '업주',
    CUSTOMER: '일반 사용자'
};

// 유틸리티 함수들
const utils = {
    // 로컬 스토리지 관리
    storage: {
        set(key, value) {
            localStorage.setItem(key, JSON.stringify(value));
        },
        get(key) {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : null;
        },
        remove(key) {
            localStorage.removeItem(key);
        },
        clear() {
            localStorage.clear();
        }
    },

    // 날짜 포맷팅
    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    // 상태 변환
    getStatusInfo(status) {
        return STATUS_MAP[status] || { text: status, class: 'default' };
    },

    // URL 파라미터 교체
    replaceUrlParams(url, params) {
        let result = url;
        Object.keys(params).forEach(key => {
            result = result.replace(`{${key}}`, params[key]);
        });
        return result;
    },

    // 디바운스 함수
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
};