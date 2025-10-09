/**
 * Navigation Module
 * Handles section navigation and dropdown menu
 */

/**
 * Toggle navigation dropdown menu
 */
function toggleDropdown() {
    const dropdown = document.getElementById('navDropdown');
    const button = document.querySelector('.nav-button');

    if (dropdown.classList.contains('show')) {
        dropdown.classList.remove('show');
        button.classList.remove('active');
    } else {
        dropdown.classList.add('show');
        button.classList.add('active');
    }
}

// Close dropdown when clicking outside
window.onclick = function(event) {
    if (!event.target.matches('.nav-button') && !event.target.closest('.nav-dropdown')) {
        const dropdown = document.getElementById('navDropdown');
        const button = document.querySelector('.nav-button');
        if (dropdown.classList.contains('show')) {
            dropdown.classList.remove('show');
            button.classList.remove('active');
        }
    }
}

/**
 * Show a specific section
 * @param {string} sectionName - Name of the section to show
 */
function showSection(sectionName) {
    // Hide all sections
    const sections = document.querySelectorAll('.content-section');
    sections.forEach(section => {
        section.classList.remove('active');
    });

    // Show selected section
    const targetSection = document.getElementById(sectionName + 'Section');
    if (targetSection) {
        targetSection.classList.add('active');
        currentSection = sectionName;

        // Close dropdown
        const dropdown = document.getElementById('navDropdown');
        const button = document.querySelector('.nav-button');
        dropdown.classList.remove('show');
        button.classList.remove('active');

        console.log('Section switched to:', sectionName);

        // Initialize section-specific functions
        if (sectionName === 'systemSettings') {
            console.log('System settings section activated');
        } else if (sectionName === 'employee') {
            console.log('Employee section activated');
            loadCorporatesForEmployeeTab();
        } else if (sectionName === 'coupon') {
            console.log('Coupon section activated');
            loadCorporatesForCouponTab();
        } else if (sectionName === 'couponManagement') {
            console.log('Coupon management section activated');
            if (currentToken) {
                loadAllCoupons();
            }
        } else if (sectionName === 'send') {
            console.log('Send section activated');
            loadCorporatesForSendTab();
        } else if (sectionName === 'deliveryHistory') {
            console.log('Delivery history section activated');
            loadDeliveryHistory();
        } else if (sectionName === 'emailConfig') {
            console.log('Email config section activated');
            loadEmailConfig();
        } else if (sectionName === 'smsConfig') {
            console.log('SMS config section activated');
            loadSmsConfig();
        } else if (sectionName === 'managementInfo') {
            console.log('Management info section activated');
            // Auto-set default date range (current month)
            const today = new Date();
            const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
            document.getElementById('statsStartDate').value = firstDay.toISOString().split('T')[0];
            document.getElementById('statsEndDate').value = today.toISOString().split('T')[0];
        }
    }
}

/**
 * Show management info tab
 * @param {string} tabName - Name of the tab to show
 */
function showManagementTab(tabName) {
    // 모든 탭 버튼 비활성화
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    // 모든 탭 콘텐츠 숨김
    document.querySelectorAll('.management-tab-content').forEach(content => {
        content.classList.remove('active');
    });

    // 선택된 탭 활성화 (클릭된 버튼 찾기)
    document.querySelectorAll('.tab-button').forEach(btn => {
        if (btn.getAttribute('onclick') && btn.getAttribute('onclick').includes(`'${tabName}'`)) {
            btn.classList.add('active');
        }
    });
    document.getElementById(tabName + 'Tab').classList.add('active');

    // 월간정산보고서 탭이 활성화될 때 거래처 목록 로드
    if (tabName === 'monthlyReport') {
        loadCorporatesForMonthlyReport();
        setCurrentMonth();
    }
}

/**
 * Check server status
 */
async function checkServerStatus() {
    const serverUrl = document.getElementById('serverUrl').value;
    const statusDiv = document.getElementById('serverStatus');

    statusDiv.innerHTML = '<span class="loading"></span> 확인 중...';

    try {
        const response = await fetch(serverUrl + '/api', {
            method: 'GET',
            timeout: 5000
        });

        if (response.ok) {
            const data = await response.json();
            statusDiv.innerHTML = '<div class="status-indicator status-online">🟢 서버 온라인</div>';
        } else {
            statusDiv.innerHTML = '<div class="status-indicator status-offline">🔴 서버 응답 오류</div>';
        }
    } catch (error) {
        statusDiv.innerHTML = '<div class="status-indicator status-offline">🔴 서버 오프라인</div>';
    }
}
