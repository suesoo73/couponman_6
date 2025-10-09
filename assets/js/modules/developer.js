/**
 * developer Module
 * Auto-extracted from index_v2.html
 */

// === 개발자 인증 시스템 ===
// Note: DEV_CREDENTIALS and isDeveloperAuthenticated are defined in auth.js
// Note: simpleDecrypt function is defined in auth.js

// 개발자 로그인 모달 표시
function showDeveloperLogin() {
    if (isDeveloperAuthenticated) {
        showSection('developer');
        return;
    }
    
    document.getElementById('developerLoginModal').style.display = 'block';
    document.getElementById('devUserId').focus();
    
    // Enter 키 이벤트 리스너 추가
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

// 개발자 로그인 모달 닫기
function closeDeveloperLoginModal() {
    document.getElementById('developerLoginModal').style.display = 'none';
    document.getElementById('devUserId').value = '';
    document.getElementById('devPassword').value = '';
    document.getElementById('devAuthResult').innerHTML = '';
}

// 개발자 인증
function authenticateDeveloper() {
    const inputId = document.getElementById('devUserId').value;
    const inputPass = document.getElementById('devPassword').value;
    const resultDiv = document.getElementById('devAuthResult');
    
    if (!inputId || !inputPass) {
        resultDiv.innerHTML = '<div style="color: #dc3545; padding: 10px; background: #f8d7da; border-radius: 4px;">ID와 비밀번호를 입력하세요.</div>';
        return;
    }
    
    // 디버깅용 로그
    console.log('[DEV-AUTH] 입력된 값:', { inputId, inputPass });
    console.log('[DEV-AUTH] 암호화된 자격증명:', DEV_CREDENTIALS);
    
    // 암호화된 자격증명 검증
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
        
        // 인증 성공 시 디버깅 로그 제거
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
        
        // 보안 로그 (개발자 도구 콘솔에 기록)
        console.warn('[DEV-AUTH] 무허가 개발자 모드 접근 시도:', {
            timestamp: new Date().toISOString(),
            attemptedId: inputId,
            ip: 'client-side',
            userAgent: navigator.userAgent
        });
    }
}

// 개발자 메뉴 업데이트
function updateDeveloperMenu() {
    const menuButton = document.getElementById('developerMenuButton');
    const emailConfigMenu = document.getElementById('emailConfigMenu');
    const smsConfigMenu = document.getElementById('smsConfigMenu');
    
    if (isDeveloperAuthenticated) {
        menuButton.innerHTML = '<span class="icon">👨‍💻</span>개발자 정보 (인증됨)';
        menuButton.style.color = '#28a745';
        
        // 개발자 인증 시 이메일/SMS 설정 메뉴 표시
        if (emailConfigMenu) emailConfigMenu.style.display = 'block';
        if (smsConfigMenu) smsConfigMenu.style.display = 'block';
        
        console.log('[DEV-MENU] 이메일/SMS 설정 메뉴 활성화됨');
    } else {
        menuButton.innerHTML = '<span class="icon">👨‍💻</span>개발자 메뉴';
        menuButton.style.color = '#dc3545';
        
        // 개발자 미인증 시 이메일/SMS 설정 메뉴 숨김
        if (emailConfigMenu) emailConfigMenu.style.display = 'none';
        if (smsConfigMenu) smsConfigMenu.style.display = 'none';
        
        console.log('[DEV-MENU] 이메일/SMS 설정 메뉴 비활성화됨');
    }
}

// 개발자 정보 로드
function loadDeveloperInfo() {
    if (!isDeveloperAuthenticated) return;
    
    // 브라우저 정보 업데이트
    document.getElementById('browserInfo').textContent = navigator.appName + ' ' + navigator.appVersion.split(' ')[0];
    document.getElementById('screenRes').textContent = screen.width + 'x' + screen.height;
    document.getElementById('userAgent').textContent = navigator.userAgent;
    
    // 데이터베이스 상태 확인
    checkDatabaseStatus();
}

// 개발자 로그아웃
function developerLogout() {
    if (confirm('개발자 모드를 종료하시겠습니까?')) {
        isDeveloperAuthenticated = false;
        updateDeveloperMenu();
        showSection('welcome');
        alert('개발자 모드가 종료되었습니다.');
    }
}

// 로컬 스토리지 클리어
function clearAllLocalStorage() {
    if (confirm('모든 로컬 스토리지 데이터를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) {
        const beforeCount = localStorage.length;
        localStorage.clear();
        alert(`로컬 스토리지가 클리어되었습니다.\n삭제된 항목 수: ${beforeCount}개`);
    }
}

// 시스템 로그 내보내기
function exportSystemLogs() {
    const logs = {
        timestamp: new Date().toISOString(),
        url: window.location.href,
        userAgent: navigator.userAgent,
        screenResolution: screen.width + 'x' + screen.height,
        localStorageItems: localStorage.length,
        sessionStorageItems: sessionStorage.length,
        currentToken: currentToken ? '인증됨' : '미인증',
        serverUrl: document.getElementById('serverUrl').value
    };
    
    const blob = new Blob([JSON.stringify(logs, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `system-logs-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
}

// 디버그 정보 표시
function showDebugInfo() {
    const info = `
=== 시스템 디버그 정보 ===
현재 시간: ${new Date().toLocaleString()}
URL: ${window.location.href}
토큰 상태: ${currentToken ? '인증됨 (' + currentToken.substring(0, 20) + '...)' : '미인증'}
로컬 스토리지: ${localStorage.length}개 항목
세션 스토리지: ${sessionStorage.length}개 항목
화면 해상도: ${screen.width}x${screen.height}
브라우저: ${navigator.appName} ${navigator.appVersion}
플랫폼: ${navigator.platform}
쿠키 활성화: ${navigator.cookieEnabled ? '예' : '아니오'}
온라인 상태: ${navigator.onLine ? '온라인' : '오프라인'}
    `.trim();
    
    alert(info);
}

// 전체 API 테스트
async function testAllAPIs() {
    if (!currentToken) {
        alert('먼저 일반 사용자 로그인을 해주세요.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    const endpoints = [
        '/api/corporates',
        '/api/employees', 
        '/api/coupons',
        '/api/email-config',
        '/api/sms-config'
    ];
    
    let results = '=== API 테스트 결과 ===\n';
    
    for (const endpoint of endpoints) {
        try {
            const response = await fetch(`${serverUrl}${endpoint}`, {
                headers: { 'Authorization': `Bearer ${currentToken}` }
            });
            results += `${endpoint}: ${response.status} (${response.ok ? 'OK' : 'ERROR'})\n`;
        } catch (error) {
            results += `${endpoint}: ERROR (${error.message})\n`;
        }
    }
    
    alert(results);
}

// 사업자 설정 저장
async function saveBusinessSettings() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const businessNumber = document.getElementById('businessNumber').value.trim();
    const companyName = document.getElementById('companyName').value.trim();
    const representativeName = document.getElementById('representativeName').value.trim();
    const businessPhone = document.getElementById('businessPhone').value.trim();
    const businessAddress = document.getElementById('businessAddress').value.trim();
    
    // 사업자등록번호 유효성 검사
    if (businessNumber && (businessNumber.length !== 10 || !/^\d{10}$/.test(businessNumber))) {
        alert('사업자등록번호는 10자리 숫자여야 합니다.');
        document.getElementById('businessNumber').focus();
        return;
    }

    const settings = {
        business_number: businessNumber,
        company_name: companyName,
        representative_name: representativeName,
        phone_number: businessPhone,
        address: businessAddress
    };
    
    console.log('[BUSINESS-SETTINGS] 사업자 설정 저장 시작:', settings);
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/business-settings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(settings)
        });

        console.log('[BUSINESS-SETTINGS] 응답 상태:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('[BUSINESS-SETTINGS] 저장 실패:', response.status, errorText);
            alert(`사업자 설정 저장 실패: ${response.status} - ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('[BUSINESS-SETTINGS] API 응답:', data);
        
        if (data.success) {
            console.log('[BUSINESS-SETTINGS] 사업자 설정 저장 성공');
            alert('사업자 설정이 성공적으로 저장되었습니다.\n이제 쿠폰 발행 시 올바른 사업자등록번호가 사용됩니다.');
            
            // 현재 사업자등록번호 표시 업데이트
            updateCurrentBusinessNumber(data.business_number || businessNumber);
        } else {
            console.error('[BUSINESS-SETTINGS] 저장 실패:', data.message);
            alert('사업자 설정 저장 실패: ' + (data.message || '알 수 없는 오류'));
        }
    } catch (error) {
        console.error('[BUSINESS-SETTINGS] 사업자 설정 저장 예외:', error);
        alert('사업자 설정 저장 오류: ' + error.message);
    }
}

// 사업자 설정 불러오기
async function loadBusinessSettings() {
    console.log('[BUSINESS-SETTINGS] 사업자 설정 불러오기 시작');
    
    if (!currentToken) {
        console.warn('[BUSINESS-SETTINGS] 토큰이 없어서 설정 불러오기 건너뜀');
        return;
    }

    const serverUrlElement = document.getElementById('serverUrl');
    const serverUrl = serverUrlElement ? serverUrlElement.value : 'http://localhost:8080';
    console.log('[BUSINESS-SETTINGS] 사용 중인 서버 URL:', serverUrl);
    
    try {
        // SMS 설정과 동일한 방식으로 사업자등록번호 가져오기
        console.log('[BUSINESS-SETTINGS] SMS config에서 사업자등록번호 가져오기');
        let businessNumber = '0000000000';
        
        try {
            const smsResponse = await fetch(`${serverUrl}/api/sms-config`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                    'Authorization': `Bearer ${currentToken}`
                }
            });

            if (smsResponse.ok) {
                const smsData = await smsResponse.json();
                if (smsData.success && smsData.config && smsData.config.businessId) {
                    businessNumber = smsData.config.businessId;
                    console.log('[BUSINESS-SETTINGS] SMS config에서 사업자등록번호 획득:', businessNumber);
                }
            }
        } catch (smsError) {
            console.warn('[BUSINESS-SETTINGS] SMS config 불러오기 실패:', smsError);
        }

        // 기본 사업자 설정 불러오기
        const response = await fetch(`${serverUrl}/api/business-settings`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log('[BUSINESS-SETTINGS] 응답 상태:', response.status);

        if (!response.ok) {
            if (response.status === 404) {
                console.info('[BUSINESS-SETTINGS] 저장된 사업자 설정이 없음 (첫 설정)');
                // SMS config에서 가져온 사업자등록번호로 초기화
                document.getElementById('businessNumber').value = businessNumber;
                updateCurrentBusinessNumber(businessNumber);
                initializeDefaultBusinessSettings();
            } else {
                console.error('[BUSINESS-SETTINGS] 설정 불러오기 실패:', response.status);
                alert('사업자 설정 불러오기 실패: ' + response.status);
            }
            return;
        }

        const data = await response.json();
        console.log('[BUSINESS-SETTINGS] 불러온 데이터:', data);

        if (data.success && data.settings) {
            const settings = data.settings;
            
            // 폼 필드에 설정값 적용 (사업자등록번호는 SMS config에서 가져온 값 우선 사용)
            document.getElementById('businessNumber').value = businessNumber;
            document.getElementById('companyName').value = settings.company_name || '';
            document.getElementById('representativeName').value = settings.representative_name || '';
            document.getElementById('businessPhone').value = settings.phone_number || '';
            document.getElementById('businessAddress').value = settings.address || '';
            
            // 현재 사업자등록번호 표시 업데이트
            updateCurrentBusinessNumber(businessNumber);
            
            console.log('[BUSINESS-SETTINGS] 사업자 설정 불러오기 완료 (사업자등록번호:', businessNumber + ')');
            alert('사업자 설정을 성공적으로 불러왔습니다.');
        } else {
            console.warn('[BUSINESS-SETTINGS] 설정 데이터가 없거나 실패:', data);
            // SMS config에서 가져온 사업자등록번호로 초기화
            document.getElementById('businessNumber').value = businessNumber;
            updateCurrentBusinessNumber(businessNumber);
            initializeDefaultBusinessSettings();
        }
    } catch (error) {
        console.error('[BUSINESS-SETTINGS] 사업자 설정 불러오기 예외:', error);
        alert('사업자 설정 불러오기 오류: ' + error.message);
        initializeDefaultBusinessSettings();
    }
}

// 기본 사업자 설정 초기화
function initializeDefaultBusinessSettings() {
    console.log('[BUSINESS-SETTINGS] 기본값으로 초기화');
    document.getElementById('businessNumber').value = '';
    document.getElementById('companyName').value = '';
    document.getElementById('representativeName').value = '';
    document.getElementById('businessPhone').value = '';
    document.getElementById('businessAddress').value = '';
    updateCurrentBusinessNumber('0000000000');
}

// 현재 사업자등록번호 표시 업데이트
function updateCurrentBusinessNumber(businessNumber) {
    const currentBusinessNumberElement = document.getElementById('currentBusinessNumber');
    if (currentBusinessNumberElement) {
        currentBusinessNumberElement.textContent = businessNumber || '0000000000';
        
        // 기본값인 경우 빨간색, 설정된 경우 파란색
        if (businessNumber && businessNumber !== '0000000000') {
            currentBusinessNumberElement.style.color = '#28a745';
        } else {
            currentBusinessNumberElement.style.color = '#dc3545';
        }
    }
}

// 이메일 설정 불러오기
async function loadEmailConfig() {
    console.log('[EMAIL-CONFIG] 이메일 설정 불러오기 시작');
    
    if (!currentToken) {
        console.warn('[EMAIL-CONFIG] 토큰이 없어서 설정 불러오기 건너뜀');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    console.log('[EMAIL-CONFIG] 서버 URL:', serverUrl);

    try {
        console.log('[EMAIL-CONFIG] 설정 조회 API 호출');
        const response = await fetch(`${serverUrl}/api/email-config`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json; charset=utf-8'
            }
        });

        console.log('[EMAIL-CONFIG] 설정 조회 응답 상태:', response.status, response.statusText);

        if (!response.ok) {
            if (response.status === 404) {
                console.info('[EMAIL-CONFIG] 저장된 이메일 설정이 없음 (첫 설정)');
                initializeDefaultEmailSettings();
            } else {
                console.error('[EMAIL-CONFIG] 설정 조회 HTTP 오류:', response.status);
                alert('이메일 설정 불러오기 실패: ' + response.status);
            }
            return;
        }

        const data = await response.json();
        console.log('[EMAIL-CONFIG] 설정 조회 응답 데이터:', data);
        
        if (data.success && data.data) {
            const config = data.data;
            console.log('[EMAIL-CONFIG] 불러온 설정:', {
                smtpHost: config.smtpHost,
                smtpPort: config.smtpPort,
                security: config.security,
                username: config.username,
                useAuth: config.useAuth,
                senderName: config.senderName,
                senderEmail: config.senderEmail,
                passwordSet: config.password ? '설정됨' : '미설정'
            });
            
            // SMTP 서버 설정
            if (document.getElementById('smtpHost')) {
                document.getElementById('smtpHost').value = config.smtpHost || '';
            }
            if (document.getElementById('smtpPort')) {
                document.getElementById('smtpPort').value = config.smtpPort || '';
            }
            
            // 보안 설정
            const securityRadio = document.querySelector(`input[name="security"][value="${config.security || 'tls'}"]`);
            if (securityRadio) securityRadio.checked = true;
            
            // 인증 정보
            if (document.getElementById('smtpUsername')) {
                document.getElementById('smtpUsername').value = config.username || '';
            }
            if (document.getElementById('smtpPassword')) {
                document.getElementById('smtpPassword').value = config.password || '';
            }
            if (document.getElementById('useAuth')) {
                document.getElementById('useAuth').checked = config.useAuth !== false;
            }
            
            // 발송자 정보
            if (document.getElementById('senderName')) {
                document.getElementById('senderName').value = config.senderName || '';
            }
            if (document.getElementById('senderEmail')) {
                document.getElementById('senderEmail').value = config.senderEmail || '';
            }
            
            // 이메일 템플릿
            if (document.getElementById('emailSubject')) {
                document.getElementById('emailSubject').value = config.emailSubject || '[쿠폰 발송] {{회사명}} 쿠폰이 발급되었습니다';
            }
            if (document.getElementById('emailTemplate')) {
                document.getElementById('emailTemplate').value = config.emailTemplate || `안녕하세요 {{이름}}님,

{{회사명}}에서 쿠폰이 발급되었습니다.

쿠폰 코드: {{쿠폰코드}}
충전 금액: {{충전금액}}원
포인트: {{포인트}}P
유효기간: {{유효기간}}

감사합니다.`;
            }
            
            console.log('[EMAIL-CONFIG] 폼 필드 업데이트 완료');
            alert('이메일 설정을 성공적으로 불러왔습니다.');
        } else {
            console.warn('[EMAIL-CONFIG] 설정 데이터가 없거나 실패:', data);
            initializeDefaultEmailSettings();
        }
    } catch (error) {
        console.error('[EMAIL-CONFIG] 이메일 설정 불러오기 예외:', error);
        alert('이메일 설정 불러오기 오류: ' + error.message);
        initializeDefaultEmailSettings();
    }
}

// 기본 이메일 설정 초기화
function initializeDefaultEmailSettings() {
    console.log('[EMAIL-CONFIG] 기본 이메일 설정으로 초기화');
    
    // 기본값 설정
    if (document.getElementById('smtpHost')) document.getElementById('smtpHost').value = '';
    if (document.getElementById('smtpPort')) document.getElementById('smtpPort').value = '587';
    if (document.getElementById('smtpUsername')) document.getElementById('smtpUsername').value = '';
    if (document.getElementById('smtpPassword')) document.getElementById('smtpPassword').value = '';
    if (document.getElementById('useAuth')) document.getElementById('useAuth').checked = true;
    if (document.getElementById('senderName')) document.getElementById('senderName').value = '쿠폰관리시스템';
    if (document.getElementById('senderEmail')) document.getElementById('senderEmail').value = '';
    if (document.getElementById('emailSubject')) document.getElementById('emailSubject').value = '[쿠폰 발송] {{회사명}} 쿠폰이 발급되었습니다';
    
    // 기본 이메일 템플릿
    if (document.getElementById('emailTemplate')) {
        document.getElementById('emailTemplate').value = `안녕하세요 {{이름}}님,

{{회사명}}에서 쿠폰이 발급되었습니다.

쿠폰 코드: {{쿠폰코드}}
충전 금액: {{충전금액}}원
포인트: {{포인트}}P
유효기간: {{유효기간}}

감사합니다.`;
    }
    
    // 기본 보안 설정 (TLS)
    const tlsRadio = document.querySelector(`input[name="security"][value="tls"]`);
    if (tlsRadio) tlsRadio.checked = true;
}

// 이메일 설정 저장
async function saveEmailConfig() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    // 폼 데이터 수집 (index.html과 동일한 형식)
    const emailConfig = {
        smtpHost: document.getElementById('smtpHost')?.value?.trim() || '',
        smtpPort: document.getElementById('smtpPort')?.value || '587',
        security: document.querySelector('input[name="security"]:checked')?.value || 'tls',
        username: document.getElementById('smtpUsername')?.value?.trim() || '',
        password: document.getElementById('smtpPassword')?.value || '',
        useAuth: document.getElementById('useAuth')?.checked !== false,
        senderName: document.getElementById('senderName')?.value?.trim() || '',
        senderEmail: document.getElementById('senderEmail')?.value?.trim() || '',
        emailSubject: document.getElementById('emailSubject')?.value?.trim() || '[쿠폰 발송] {{회사명}} 쿠폰이 발급되었습니다',
        emailTemplate: document.getElementById('emailTemplate')?.value || ''
    };

    // 기본 유효성 검사
    if (!emailConfig.smtpHost) {
        alert('SMTP 호스트를 입력해주세요.');
        document.getElementById('smtpHost')?.focus();
        return;
    }

    if (!emailConfig.username) {
        alert('사용자명/이메일을 입력해주세요.');
        document.getElementById('smtpUsername')?.focus();
        return;
    }

    console.log('[EMAIL-CONFIG] 이메일 설정 저장 시작:', emailConfig);
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/email-config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(emailConfig)
        });

        console.log('[EMAIL-CONFIG] 응답 상태:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('[EMAIL-CONFIG] 저장 실패:', response.status, errorText);
            alert(`이메일 설정 저장 실패: ${response.status} - ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('[EMAIL-CONFIG] API 응답:', data);
        
        if (data.success) {
            console.log('[EMAIL-CONFIG] 이메일 설정 저장 성공');
            alert('이메일 설정이 성공적으로 저장되었습니다.');
        } else {
            console.error('[EMAIL-CONFIG] 저장 실패:', data.message);
            alert('이메일 설정 저장 실패: ' + (data.message || '알 수 없는 오류'));
        }
    } catch (error) {
        console.error('[EMAIL-CONFIG] 이메일 설정 저장 예외:', error);
        alert('이메일 설정 저장 오류: ' + error.message);
    }
}

// 이메일 연결 테스트
async function testEmailConnection() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    console.log('[EMAIL-CONFIG] 이메일 연결 테스트 시작');
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/email-config/test`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log('[EMAIL-CONFIG] 테스트 응답 상태:', response.status);

        const data = await response.json();
        console.log('[EMAIL-CONFIG] 테스트 API 응답:', data);
        
        if (data.success) {
            console.log('[EMAIL-CONFIG] 이메일 연결 테스트 성공');
            alert('✅ 이메일 서버 연결에 성공했습니다!\n설정이 올바르게 구성되었습니다.');
        } else {
            console.error('[EMAIL-CONFIG] 연결 테스트 실패:', data.message);
            alert('❌ 이메일 서버 연결 실패:\n' + (data.message || '알 수 없는 오류'));
        }
    } catch (error) {
        console.error('[EMAIL-CONFIG] 이메일 연결 테스트 예외:', error);
        alert('❌ 이메일 연결 테스트 오류:\n' + error.message);
    }
}

// 프로바이더 프리셋 함수들
function applyGmailPreset() {
    document.getElementById('smtpHost').value = 'smtp.gmail.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    console.log('[EMAIL-CONFIG] Gmail 프리셋 적용됨');
}

function applyOutlookPreset() {
    document.getElementById('smtpHost').value = 'smtp-mail.outlook.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    console.log('[EMAIL-CONFIG] Outlook 프리셋 적용됨');
}

function applyYahooPreset() {
    document.getElementById('smtpHost').value = 'smtp.mail.yahoo.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    console.log('[EMAIL-CONFIG] Yahoo 프리셋 적용됨');
}

function applyNaverPreset() {
    document.getElementById('smtpHost').value = 'smtp.naver.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    console.log('[EMAIL-CONFIG] Naver 프리셋 적용됨');
}

function applyEcountPreset() {
    document.getElementById('smtpHost').value = 'wsmtp.ecount.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    console.log('[EMAIL-CONFIG] Ecount 프리셋 적용됨');
}

// 데이터베이스 상태 확인
async function checkDatabaseStatus() {
    const dbInfoDiv = document.getElementById('dbInfo');
    dbInfoDiv.innerHTML = '데이터베이스 상태를 확인하고 있습니다...';
    
    if (!currentToken) {
        dbInfoDiv.innerHTML = 'ERROR: 토큰이 없습니다. 먼저 로그인하세요.';
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const startTime = Date.now();
        const response = await fetch(`${serverUrl}/api/corporates`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        if (response.ok) {
            const data = await response.json();
            const responseTime = Date.now() - startTime;
            dbInfoDiv.innerHTML = `
STATUS: CONNECTED ✅
Response Time: ${responseTime}ms
Data Count: ${data.data ? data.data.length : 'N/A'}
Server: ${serverUrl}
Last Check: ${new Date().toLocaleString()}
            `;
        } else {
            dbInfoDiv.innerHTML = `
STATUS: ERROR ❌
HTTP Status: ${response.status}
Server: ${serverUrl}
Last Check: ${new Date().toLocaleString()}
            `;
        }
    } catch (error) {
        dbInfoDiv.innerHTML = `
STATUS: CONNECTION FAILED ❌
Error: ${error.message}
Server: ${serverUrl}
Last Check: ${new Date().toLocaleString()}
        `;
    }
}

// showSection 함수에 개발자 섹션 처리 추가
const originalShowSection = showSection;
showSection = function(sectionName) {
    // 개발자 전용 섹션 보호
    if (sectionName === 'developer') {
        if (!isDeveloperAuthenticated) {
            alert('개발자 인증이 필요합니다.');
            showDeveloperLogin();
            return;
        }
        loadDeveloperInfo();
    }
    
    // 이메일/SMS 설정 섹션 보호
    if (sectionName === 'emailConfig' || sectionName === 'smsConfig') {
        if (!isDeveloperAuthenticated) {
            alert('🔒 이 기능은 개발자 전용입니다.\n개발자 인증이 필요합니다.');
            showDeveloperLogin();
            return;
        }
        
        // 개발자 인증됨 - 설정 로드
        if (sectionName === 'emailConfig') {
            console.log('[DEV-ACCESS] 이메일 설정 섹션 접근 허용');
            setTimeout(loadEmailConfig, 100); // 섹션 로드 후 설정 불러오기
        } else if (sectionName === 'smsConfig') {
            console.log('[DEV-ACCESS] SMS 설정 섹션 접근 허용');
            setTimeout(loadSmsConfig, 100); // 섹션 로드 후 설정 불러오기
        }
    }
    
    originalShowSection(sectionName);
};

// === 가격 설정 관련 함수들 ===

// 시간대별 차감 기능 활성화/비활성화 토글
function toggleTimeBasedControls() {
    const checkbox = document.getElementById('enableTimeBasedDeduction');
    const controls = document.getElementById('timeBasedControls');
    const defaultDescription = document.getElementById('defaultSettingsDescription');
    
    if (checkbox.checked) {
        // 시간대별 설정 활성화
        controls.style.opacity = '1';
        controls.style.pointerEvents = 'auto';
        
        // 기본 설정 설명 변경 (시간대 외 용도)
        defaultDescription.textContent = '설정된 시간대에 해당하지 않는 경우 사용되는 차감액';
        defaultDescription.style.color = '#666';
        
        console.log('[PRICE-SETTINGS] 시간대별 가격 설정 모드 활성화');
    } else {
        // 시간대별 설정 비활성화
        controls.style.opacity = '0.5';
        controls.style.pointerEvents = 'none';
        
        // 기본 설정을 주요 설정으로 강조
        defaultDescription.innerHTML = '<strong style="color: #28a745;">✅ 활성 상태: 모든 시간대에 이 기본 가격이 적용됩니다</strong>';
        
        console.log('[PRICE-SETTINGS] 기본 가격 설정 모드 활성화 (시간대별 비활성화)');
    }
}

// 시스템 설정 저장
async function saveSystemSettings() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const settings = {
        enableTimeBasedDeduction: document.getElementById('enableTimeBasedDeduction').checked,
        allowNegativeBalance: document.getElementById('allowNegativeBalance').checked,
        pointDeductionMethod: document.getElementById('pointDeductionMethod').value,
        breakfast: {
            startTime: document.getElementById('breakfastStartTime').value,
            endTime: document.getElementById('breakfastEndTime').value,
            cashDeduction: parseInt(document.getElementById('breakfastCashDeduction').value) || 0
        },
        lunch: {
            startTime: document.getElementById('lunchStartTime').value,
            endTime: document.getElementById('lunchEndTime').value,
            cashDeduction: parseInt(document.getElementById('lunchCashDeduction').value) || 0
        },
        dinner: {
            startTime: document.getElementById('dinnerStartTime').value,
            endTime: document.getElementById('dinnerEndTime').value,
            cashDeduction: parseInt(document.getElementById('dinnerCashDeduction').value) || 0
        },
        default: {
            cashDeduction: parseInt(document.getElementById('defaultCashDeduction').value) || 0
        }
    };
    
    console.log('[PRICE-SETTINGS] 가격 설정 저장 시작:', settings);
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/price-settings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(settings)
        });

        console.log('[PRICE-SETTINGS] 응답 상태:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('[PRICE-SETTINGS] 저장 실패:', response.status, errorText);
            alert(`가격 설정 저장 실패: ${response.status} - ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('[PRICE-SETTINGS] API 응답:', data);
        
        if (data.success) {
            console.log('[PRICE-SETTINGS] 가격 설정 저장 성공');
            alert('가격 설정이 성공적으로 저장되었습니다.');
        } else {
            console.error('[PRICE-SETTINGS] 저장 실패:', data.message);
            alert('가격 설정 저장 실패: ' + (data.message || '알 수 없는 오류'));
        }
    } catch (error) {
        console.error('[PRICE-SETTINGS] 가격 설정 저장 예외:', error);
        alert('가격 설정 저장 오류: ' + error.message);
    }
}

// 시스템 설정 불러오기
async function loadSystemSettings() {
    console.log('[PRICE-SETTINGS] 가격 설정 불러오기 시작');
    
    if (!currentToken) {
        console.warn('[PRICE-SETTINGS] 토큰이 없어서 설정 불러오기 건너뜀');
        // 초기 상태 설정
        const checkbox = document.getElementById('enableTimeBasedDeduction');
        if (checkbox && !checkbox.checked) {
            toggleTimeBasedControls(); // 초기에는 비활성화 상태로 설정
        }
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/price-settings`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log('[PRICE-SETTINGS] 응답 상태:', response.status);

        if (!response.ok) {
            if (response.status === 404) {
                console.info('[PRICE-SETTINGS] 저장된 가격 설정이 없음 (첫 설정)');
                // 기본값으로 초기화
                initializeDefaultPriceSettings();
            } else {
                console.error('[PRICE-SETTINGS] 설정 불러오기 실패:', response.status);
                alert('가격 설정 불러오기 실패: ' + response.status);
            }
            return;
        }

        const data = await response.json();
        console.log('[PRICE-SETTINGS] 불러온 데이터:', data);

        if (data.success && data.settings) {
            const settings = data.settings;
            
            // 폼 필드에 설정값 적용
            document.getElementById('enableTimeBasedDeduction').checked = settings.enableTimeBasedDeduction || false;
            document.getElementById('allowNegativeBalance').checked = settings.allowNegativeBalance || false;
            document.getElementById('pointDeductionMethod').value = settings.pointDeductionMethod || '후순위';
            
            if (settings.breakfast) {
                document.getElementById('breakfastStartTime').value = settings.breakfast.startTime || '07:00';
                document.getElementById('breakfastEndTime').value = settings.breakfast.endTime || '10:59';
                document.getElementById('breakfastCashDeduction').value = settings.breakfast.cashDeduction || 3000;
            }
            
            if (settings.lunch) {
                document.getElementById('lunchStartTime').value = settings.lunch.startTime || '11:00';
                document.getElementById('lunchEndTime').value = settings.lunch.endTime || '14:59';
                document.getElementById('lunchCashDeduction').value = settings.lunch.cashDeduction || 5000;
            }
            
            if (settings.dinner) {
                document.getElementById('dinnerStartTime').value = settings.dinner.startTime || '15:00';
                document.getElementById('dinnerEndTime').value = settings.dinner.endTime || '21:59';
                document.getElementById('dinnerCashDeduction').value = settings.dinner.cashDeduction || 7000;
            }
            
            if (settings.default) {
                document.getElementById('defaultCashDeduction').value = settings.default.cashDeduction || 4000;
            }
            
            // 시간대별 컨트롤 상태 업데이트
            toggleTimeBasedControls();
            
            console.log('[PRICE-SETTINGS] 가격 설정 불러오기 완료');
            alert('가격 설정을 성공적으로 불러왔습니다.');
        } else {
            console.warn('[PRICE-SETTINGS] 설정 데이터가 없거나 실패:', data);
            initializeDefaultPriceSettings();
        }
    } catch (error) {
        console.error('[PRICE-SETTINGS] 가격 설정 불러오기 예외:', error);
        alert('가격 설정 불러오기 오류: ' + error.message);
        initializeDefaultPriceSettings();
    }
}

// 기본 가격 설정 초기화
function initializeDefaultPriceSettings() {
    console.log('[PRICE-SETTINGS] 기본값으로 초기화');
    document.getElementById('enableTimeBasedDeduction').checked = false;
    document.getElementById('allowNegativeBalance').checked = false;
    document.getElementById('pointDeductionMethod').value = '후순위';
    document.getElementById('defaultCashDeduction').value = 4000;
    document.getElementById('breakfastCashDeduction').value = 3000;
    document.getElementById('lunchCashDeduction').value = 5000;
    document.getElementById('dinnerCashDeduction').value = 7000;
    toggleTimeBasedControls();
}

// 현재 시간에 따른 차감액 계산
function calculateCurrentDeduction() {
    const enableTimeBasedDeduction = document.getElementById('enableTimeBasedDeduction').checked;
    
    if (!enableTimeBasedDeduction) {
        // 시간대별 기능이 비활성화된 경우 기본 설정 사용
        const cashDeduction = parseInt(document.getElementById('defaultCashDeduction').value) || 0;
        const pointMethod = document.getElementById('pointDeductionMethod').value;
        
        console.log('[PRICE-CALC] 기본 가격 적용 - 현금:', cashDeduction + '원, 포인트 방식:', pointMethod);
        return {
            cash: cashDeduction,
            pointMethod: pointMethod,
            period: '기본'
        };
    }
    
    // 현재 시간 구하기
    const now = new Date();
    const currentTime = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0');
    
    // 시간대별 설정 확인
    const breakfast = {
        start: document.getElementById('breakfastStartTime').value,
        end: document.getElementById('breakfastEndTime').value,
        cash: parseInt(document.getElementById('breakfastCashDeduction').value) || 0
    };
    
    const lunch = {
        start: document.getElementById('lunchStartTime').value,
        end: document.getElementById('lunchEndTime').value,
        cash: parseInt(document.getElementById('lunchCashDeduction').value) || 0
    };
    
    const dinner = {
        start: document.getElementById('dinnerStartTime').value,
        end: document.getElementById('dinnerEndTime').value,
        cash: parseInt(document.getElementById('dinnerCashDeduction').value) || 0
    };
    
    const defaultDeduction = {
        cash: parseInt(document.getElementById('defaultCashDeduction').value) || 0
    };
    
    const pointMethod = document.getElementById('pointDeductionMethod').value;
    
    // 현재 시간이 어느 시간대에 속하는지 확인
    if (isTimeInRange(currentTime, breakfast.start, breakfast.end)) {
        console.log('[PRICE-CALC] 아침 시간대 적용 - 현금:', breakfast.cash + '원, 포인트 방식:', pointMethod);
        return { cash: breakfast.cash, pointMethod: pointMethod, period: '아침' };
    } else if (isTimeInRange(currentTime, lunch.start, lunch.end)) {
        console.log('[PRICE-CALC] 점심 시간대 적용 - 현금:', lunch.cash + '원, 포인트 방식:', pointMethod);
        return { cash: lunch.cash, pointMethod: pointMethod, period: '점심' };
    } else if (isTimeInRange(currentTime, dinner.start, dinner.end)) {
        console.log('[PRICE-CALC] 저녁 시간대 적용 - 현금:', dinner.cash + '원, 포인트 방식:', pointMethod);
        return { cash: dinner.cash, pointMethod: pointMethod, period: '저녁' };
    } else {
        console.log('[PRICE-CALC] 시간대 외 기본 설정 적용 - 현금:', defaultDeduction.cash + '원, 포인트 방식:', pointMethod);
        return { cash: defaultDeduction.cash, pointMethod: pointMethod, period: '시간대 외' };
    }
}

// 시간 범위 확인 유틸리티 함수
function isTimeInRange(currentTime, startTime, endTime) {
    return currentTime >= startTime && currentTime <= endTime;
}

// 현재 차감액 테스트 함수 (개발자 도구에서 호출 가능)
function testCurrentDeduction() {
    const deduction = calculateCurrentDeduction();
    const now = new Date();
    const timeString = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0');
    const pointMethod = document.getElementById('pointDeductionMethod').value;
    
    alert(`현재 시간: ${timeString}\n적용 시간대: ${deduction.period}\n현금 차감: ${deduction.cash}원\n포인트 차감방식: ${pointMethod}`);
    return deduction;
}

// 포인트 차감 방식 변경 시 경고 메시지
function showPointMethodWarning() {
    const selectedMethod = document.getElementById('pointDeductionMethod').value;
    
    let warningMessage = '';
    
    if (selectedMethod === '후순위') {
        warningMessage = `⚠️ 포인트 차감 방식이 '후순위 차감'으로 변경됩니다.
        
📋 후순위 차감 방식:
• 먼저 현금을 차감합니다
• 현금이 부족할 경우에만 부족분을 포인트로 차감합니다
• 예시: 요금 5000원, 현금 3000원 → 현금 3000원 차감 + 포인트 2000P 차감

⚠️ 이 변경사항은 즉시 모든 결제에 적용됩니다.
계속하시겠습니까?`;
    } else if (selectedMethod === '비례') {
        warningMessage = `⚠️ 포인트 차감 방식이 '비례 차감'으로 변경됩니다.
        
📋 비례 차감 방식:
• 현금과 포인트를 보유 비율에 따라 동시에 차감합니다
• 현금 70%, 포인트 30% 보유 시 → 요금의 70%는 현금, 30%는 포인트로 차감
• 예시: 요금 5000원, 현금 7000원, 포인트 3000P → 현금 3500원 + 포인트 1500P 차감

⚠️ 이 변경사항은 즉시 모든 결제에 적용됩니다.
계속하시겠습니까?`;
    }
    
    if (!confirm(warningMessage)) {
        // 사용자가 취소하면 이전 값으로 되돌리기
        const previousValue = selectedMethod === '후순위' ? '비례' : '후순위';
        document.getElementById('pointDeductionMethod').value = previousValue;
        console.log('[PRICE-SETTINGS] 포인트 차감 방식 변경이 취소됨');
    } else {
        console.log('[PRICE-SETTINGS] 포인트 차감 방식 변경됨:', selectedMethod);
    }
}

