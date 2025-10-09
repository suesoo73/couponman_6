/**
 * Application Initialization
 * Main entry point and global state management
 */

// Global state variables
let currentToken = localStorage.getItem('apiToken') || '';
let currentSection = 'welcome';
let currentCoupons = [];
let selectedCouponIds = [];

/**
 * Initialize application on page load
 */
window.onload = function() {
    if (currentToken) {
        displayToken(currentToken);
    }
    showSection('welcome');
};

/**
 * Initialize on DOM content loaded
 */
document.addEventListener('DOMContentLoaded', function() {
    updateDeveloperMenu(); // 초기 메뉴 상태 설정 (이메일/SMS 메뉴 숨김)

    // 가격 설정 초기화
    setTimeout(function() {
        const enableCheckbox = document.getElementById('enableTimeBasedDeduction');
        if (enableCheckbox && typeof toggleTimeBasedControls === 'function') {
            toggleTimeBasedControls(); // 초기 컨트롤 상태 설정
        }
    }, 100);
});
