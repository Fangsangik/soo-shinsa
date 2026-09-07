// API 호출 관리
class ApiManager {
    constructor() {
        this.baseUrl = API_CONFIG.BASE_URL;
    }

    // 기본 fetch 래퍼
    async request(url, options = {}) {
        const config = {
            headers: authManager.getAuthHeaders(),
            ...options,
            headers: {
                ...authManager.getAuthHeaders(),
                ...options.headers
            }
        };

        try {
            const response = await fetch(`${this.baseUrl}${url}`, config);
            
            // 401 에러 처리 (토큰 만료)
            if (response.status === 401) {
                authManager.logout();
                showNotification('인증이 만료되었습니다. 다시 로그인해주세요.', 'warning');
                showLogin();
                throw new Error('Authentication expired');
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }

    // GET 요청
    async get(url, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const fullUrl = queryString ? `${url}?${queryString}` : url;
        return this.request(fullUrl);
    }

    // POST 요청
    async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    // PATCH 요청
    async patch(url, data) {
        return this.request(url, {
            method: 'PATCH',
            body: JSON.stringify(data)
        });
    }

    // DELETE 요청
    async delete(url) {
        return this.request(url, {
            method: 'DELETE'
        });
    }

    // 브랜드 관련 API
    async getBrands(page = 0, size = APP_CONFIG.PAGE_SIZE) {
        return this.get(API_CONFIG.ENDPOINTS.BRANDS, { page, size });
    }

    async createBrand(brandData) {
        return this.post(API_CONFIG.ENDPOINTS.BRANDS, brandData);
    }

    async getVendorBrands() {
        return this.get(API_CONFIG.ENDPOINTS.VENDOR_BRANDS);
    }

    async getPendingBrands(page = 0, size = APP_CONFIG.PAGE_SIZE) {
        return this.get(API_CONFIG.ENDPOINTS.ADMIN_PENDING, { page, size });
    }

    async approveBrand(brandId, approvalData) {
        const url = utils.replaceUrlParams(API_CONFIG.ENDPOINTS.ADMIN_APPROVE, { id: brandId });
        return this.patch(url, approvalData);
    }

    async rejectBrand(brandId, rejectionData) {
        const url = utils.replaceUrlParams(API_CONFIG.ENDPOINTS.ADMIN_REJECT, { id: brandId });
        return this.patch(url, rejectionData);
    }

    // 카테고리 API
    async getCategories() {
        return this.get(API_CONFIG.ENDPOINTS.CATEGORIES);
    }

    // 상품 통합 검색
    async searchProducts(params = {}) {
        return this.get(API_CONFIG.ENDPOINTS.PRODUCT_SEARCH, params);
    }
}

// 전역 ApiManager 인스턴스
const apiManager = new ApiManager();

// API 호출 헬퍼 함수들
const API = {
    // 브랜드 목록 가져오기
    async getAllBrands(page = 0, size = 10) {
        try {
            const response = await apiManager.getBrands(page, size);
            // 백엔드가 Page 를 돌려주므로 content 를 꺼낸다
            const data = response.data;
            return Array.isArray(data) ? data : (data?.content || []);
        } catch (error) {
            console.error('Error fetching brands:', error);
            showNotification('브랜드 목록을 가져오는데 실패했습니다.', 'error');
            return [];
        }
    },

    // 업주 브랜드 목록
    async getVendorBrands() {
        try {
            const response = await apiManager.getVendorBrands();
            return response.data || [];
        } catch (error) {
            console.error('Error fetching vendor brands:', error);
            showNotification('내 브랜드 목록을 가져오는데 실패했습니다.', 'error');
            return [];
        }
    },

    // 브랜드 생성
    async createBrand(brandData) {
        try {
            const response = await apiManager.createBrand(brandData);
            showNotification('브랜드 신청이 완료되었습니다!', 'success');
            return response.data;
        } catch (error) {
            console.error('Error creating brand:', error);
            showNotification(error.message || '브랜드 신청에 실패했습니다.', 'error');
            throw error;
        }
    },

    // 승인 대기 브랜드 목록
    async getPendingBrands(page = 0, size = 10) {
        try {
            const response = await apiManager.getPendingBrands(page, size);
            return response.data || { content: [], totalElements: 0 };
        } catch (error) {
            console.error('Error fetching pending brands:', error);
            showNotification('승인 대기 브랜드를 가져오는데 실패했습니다.', 'error');
            return { content: [], totalElements: 0 };
        }
    },

    // 브랜드 승인
    async approveBrand(brandId, approvalReason, adminComment = '') {
        try {
            const response = await apiManager.approveBrand(brandId, {
                approvalReason,
                adminComment
            });
            showNotification('브랜드 승인이 완료되었습니다!', 'success');
            return response.data;
        } catch (error) {
            console.error('Error approving brand:', error);
            showNotification(error.message || '브랜드 승인에 실패했습니다.', 'error');
            throw error;
        }
    },

    // 브랜드 거절
    async rejectBrand(brandId, rejectionReason, adminComment = '') {
        try {
            const response = await apiManager.rejectBrand(brandId, {
                rejectionReason,
                adminComment
            });
            showNotification('브랜드 거절이 완료되었습니다.', 'info');
            return response.data;
        } catch (error) {
            console.error('Error rejecting brand:', error);
            showNotification(error.message || '브랜드 거절에 실패했습니다.', 'error');
            throw error;
        }
    },

    // 상품 통합 검색 (Page 객체를 그대로 돌려준다)
    async searchProducts({ keyword = '', categoryId, minPrice, maxPrice, page = 0, size = 12 } = {}) {
        const params = { page, size };
        if (keyword) params.nameKeyword = keyword;
        if (categoryId) params.categoryId = categoryId;
        if (minPrice != null) params.minPrice = minPrice;
        if (maxPrice != null) params.maxPrice = maxPrice;
        try {
            const response = await apiManager.searchProducts(params);
            return response.data || { content: [], totalElements: 0 };
        } catch (error) {
            console.error('Error searching products:', error);
            showNotification('상품 검색에 실패했습니다.', 'error');
            return { content: [], totalElements: 0 };
        }
    },

    // 카테고리 목록
    async getCategories() {
        try {
            const response = await apiManager.getCategories();
            return response.data || [];
        } catch (error) {
            console.error('Error fetching categories:', error);
            return [];
        }
    }
};