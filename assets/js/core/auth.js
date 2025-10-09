/**
 * Authentication Module
 * Handles user and developer authentication
 */

/**
 * User login
 */
async function login() {
    const serverUrl = document.getElementById('serverUrl').value;
    const userId = document.getElementById('userId').value;
    const password = document.getElementById('password').value;
    const resultDiv = document.getElementById('loginResult');

    if (!userId || !password) {
        resultDiv.innerHTML = '<span class="status error">사용자 ID와 패스워드를 입력하세요</span>';
        return;
    }

    resultDiv.innerHTML = '<span class="loading"></span> 로그인 중...';

    try {
        const response = await fetch(`${serverUrl}/api/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8'
            },
            body: JSON.stringify({ userId, password })
        });

        const data = await response.json();

        if (data.success) {
            currentToken = data.accessToken;
            localStorage.setItem('apiToken', currentToken);
            resultDiv.innerHTML = '<span class="status success">✅ 로그인 성공!</span>';
            displayToken(currentToken);
        } else {
            resultDiv.innerHTML = `<span class="status error">❌ 로그인 실패: ${data.message}</span>`;
        }
    } catch (error) {
        resultDiv.innerHTML = `<span class="status error">❌ 오류: ${error.message}</span>`;
    }
}

/**
 * Display access token
 * @param {string} token - Access token to display
 */
function displayToken(token) {
    document.getElementById('accessToken').textContent = token;
    document.getElementById('tokenDisplay').style.display = 'block';
}

/**
 * Clear authentication token
 */
function clearToken() {
    currentToken = '';
    localStorage.removeItem('apiToken');
    document.getElementById('tokenDisplay').style.display = 'none';
    document.getElementById('loginResult').innerHTML = '<span class="status warning">토큰이 삭제되었습니다</span>';
}

// === Developer Authentication System ===

// Encrypted developer credentials (Base64 encoded)
const DEV_CREDENTIALS = {
    // "nusome" -> Base64: bnVzb21l
    id: "bnVzb21l",
    // "research88!!" -> Base64: cmVzZWFyY2g4OCEh
    pass: "cmVzZWFyY2g4OCEh"
};

// Developer authentication state
let isDeveloperAuthenticated = false;

/**
 * Base64 decryption
 * @param {string} encoded - Base64 encoded string
 * @returns {string} Decoded string
 */
function simpleDecrypt(encoded) {
    try {
        return atob(encoded);
    } catch (e) {
        console.error('[DEV-AUTH] Base64 디코딩 실패:', e);
        return '';
    }
}

/**
 * Show developer login modal
 */
function showDeveloperLogin() {
    if (isDeveloperAuthenticated) {
        showSection('developer');
        return;
    }

    document.getElementById('developerLoginModal').style.display = 'block';
    document.getElementById('devUserId').focus();

    // Enter key event listeners
    document.getElementById('devUserId').onkeypress = function(e) {
        if (e.key === 'Enter') {
            document.getElementById('devPassword').focus();
        }
    };
    document.getElementById('devPassword').onkeypress = function(e) {
        if (e.key === 'Enter') {
            authenticateDeveloper();
        }
    };
}

/**
 * Close developer login modal
 */
function closeDeveloperLoginModal() {
    document.getElementById('developerLoginModal').style.display = 'none';
    document.getElementById('devUserId').value = '';
    document.getElementById('devPassword').value = '';
    document.getElementById('devAuthResult').innerHTML = '';
}

/**
 * Authenticate developer
 */
function authenticateDeveloper() {
    const inputId = document.getElementById('devUserId').value;
    const inputPass = document.getElementById('devPassword').value;
    const resultDiv = document.getElementById('devAuthResult');

    if (!inputId || !inputPass) {
        resultDiv.innerHTML = '<div style="color: #dc3545; padding: 10px; background: #f8d7da; border-radius: 4px;">ID와 비밀번호를 입력하세요.</div>';
        return;
    }

    // Debugging logs
    console.log('[DEV-AUTH] 입력된 값:', { inputId, inputPass });
    console.log('[DEV-AUTH] 암호화된 자격증명:', DEV_CREDENTIALS);

    // Verify credentials
    const validId = simpleDecrypt(DEV_CREDENTIALS.id);
    const validPass = simpleDecrypt(DEV_CREDENTIALS.pass);

    console.log('[DEV-AUTH] 복호화된 값:', { validId, validPass });
    console.log('[DEV-AUTH] 비교 결과:', {
        idMatch: inputId === validId,
        passMatch: inputPass === validPass
    });

    if (inputId === validId && inputPass === validPass) {
        isDeveloperAuthenticated = true;
        resultDiv.innerHTML = '<div style="color: #28a745; padding: 10px; background: #d4edda; border-radius: 4px;">✅ 인증 성공! 개발자 모드로 이동합니다...</div>';

        // Clear debugging logs on success
        console.clear();
        console.log('[DEV-AUTH] ✅ 개발자 모드 활성화됨');

        setTimeout(() => {
            closeDeveloperLoginModal();
            showSection('developer');
            updateDeveloperMenu();
            loadDeveloperInfo();
        }, 1500);
    } else {
        resultDiv.innerHTML = '<div style="color: #dc3545; padding: 10px; background: #f8d7da; border-radius: 4px;">❌ 인증 실패! ID 또는 비밀번호가 잘못되었습니다.</div>';

        // Security log
        console.warn('[DEV-AUTH] 무허가 개발자 모드 접근 시도:', {
            timestamp: new Date().toISOString(),
            attemptedId: inputId,
            ip: 'client-side',
            userAgent: navigator.userAgent
        });
    }
}

/**
 * Developer logout
 */
function developerLogout() {
    if (confirm('개발자 모드를 종료하시겠습니까?')) {
        isDeveloperAuthenticated = false;
        updateDeveloperMenu();
        showSection('welcome');
        alert('개발자 모드가 종료되었습니다.');
    }
}

/**
 * Update developer menu visibility and state
 */
function updateDeveloperMenu() {
    const menuButton = document.getElementById('developerMenuButton');
    const emailConfigMenu = document.getElementById('emailConfigMenu');
    const smsConfigMenu = document.getElementById('smsConfigMenu');

    if (isDeveloperAuthenticated) {
        menuButton.innerHTML = '<span class="icon">👨‍💻</span>개발자 정보 (인증됨)';
        menuButton.style.color = '#28a745';

        // Show email/SMS config menus
        if (emailConfigMenu) emailConfigMenu.style.display = 'block';
        if (smsConfigMenu) smsConfigMenu.style.display = 'block';

        console.log('[DEV-MENU] 이메일/SMS 설정 메뉴 활성화됨');
    } else {
        menuButton.innerHTML = '<span class="icon">👨‍💻</span>개발자 메뉴';
        menuButton.style.color = '#dc3545';

        // Hide email/SMS config menus
        if (emailConfigMenu) emailConfigMenu.style.display = 'none';
        if (smsConfigMenu) smsConfigMenu.style.display = 'none';

        console.log('[DEV-MENU] 이메일/SMS 설정 메뉴 비활성화됨');
    }
}
