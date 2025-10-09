/**
 * email-config Module
 * Auto-extracted from index_v2.html
 */

// === 이메일 설정 관련 함수들 ===

// 이메일 설정 저장
async function saveEmailConfig() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    console.log('[EMAIL-CONFIG] 이메일 설정 저장 시작');
    
    // 폼 데이터 수집 (index.html과 동일한 구조)
    const emailConfig = {
        smtpHost: document.getElementById('smtpHost').value,
        smtpPort: document.getElementById('smtpPort').value,
        security: document.querySelector('input[name="security"]:checked').value,
        username: document.getElementById('smtpUsername').value,
        password: document.getElementById('smtpPassword').value,
        useAuth: document.getElementById('useAuth').checked,
        senderName: document.getElementById('senderName').value,
        senderEmail: document.getElementById('senderEmail').value,
        emailSubject: document.getElementById('emailSubject').value,
        emailTemplate: document.getElementById('emailTemplate').value
    };

    console.log('[EMAIL-CONFIG] 저장할 설정 데이터:', emailConfig);

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        console.log(`[EMAIL-CONFIG] API 호출: POST ${serverUrl}/api/email-config`);
        const response = await fetch(`${serverUrl}/api/email-config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(emailConfig)
        });

        console.log(`[EMAIL-CONFIG] 응답 상태: ${response.status}`);

        if (response.ok) {
            const data = await response.json();
            console.log('[EMAIL-CONFIG] 응답 데이터:', data);
            
            if (data.success) {
                alert('이메일 설정이 저장되었습니다.');
                // 저장 후 설정 다시 로드
                loadEmailConfig();
            } else {
                alert('이메일 설정 저장 실패: ' + data.message);
            }
        } else {
            const errorText = await response.text();
            console.error(`[EMAIL-CONFIG] HTTP 오류: ${response.status}, 응답: ${errorText}`);
            alert(`이메일 설정 저장 실패: HTTP ${response.status}`);
        }
    } catch (error) {
        console.error('[EMAIL-CONFIG] 네트워크 오류:', error);
        alert('이메일 설정 저장 오류: ' + error.message);
    }
}

// 이메일 설정 로드
async function loadEmailConfig() {
    if (!currentToken) {
        return;
    }

    console.log('[EMAIL-CONFIG] 이메일 설정 로드 시작');
    
    const serverUrl = document.getElementById('serverUrl').value;

    try {
        console.log(`[EMAIL-CONFIG] API 호출: GET ${serverUrl}/api/email-config`);
        const response = await fetch(`${serverUrl}/api/email-config`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log(`[EMAIL-CONFIG] 응답 상태: ${response.status}`);

        if (response.ok) {
            const data = await response.json();
            console.log('[EMAIL-CONFIG] 응답 데이터:', data);

            if (data.success) {
                const config = data.config || {};
                console.log('[EMAIL-CONFIG] 설정 데이터:', config);
                
                // 폼 필드 채우기
                document.getElementById('smtpHost').value = config.smtpHost || '';
                document.getElementById('smtpPort').value = config.smtpPort || '587';
                
                // 보안 설정 라디오 버튼
                const securityValue = config.security || 'tls';
                const securityRadio = document.querySelector(`input[name="security"][value="${securityValue}"]`);
                if (securityRadio) {
                    securityRadio.checked = true;
                }
                
                document.getElementById('useAuth').checked = config.useAuth !== false;
                document.getElementById('smtpUsername').value = config.username || '';
                document.getElementById('smtpPassword').value = config.password || '';
                document.getElementById('senderName').value = config.senderName || '쿠폰관리시스템';
                document.getElementById('senderEmail').value = config.senderEmail || '';
                document.getElementById('emailSubject').value = config.emailSubject || '[쿠폰 발송] {{이름}}님의 쿠폰이 발급되었습니다';
                
                // 기본 템플릿
                const defaultTemplate = `안녕하세요 {{이름}}님,

{{회사명}}에서 쿠폰이 발급되었습니다.

쿠폰 코드: {{쿠폰코드}}
충전 금액: {{충전금액}}원
포인트: {{포인트}}P
유효기간: {{유효기간}}

감사합니다.`;
                
                document.getElementById('emailTemplate').value = config.emailTemplate || defaultTemplate;
                
                console.log('[EMAIL-CONFIG] 설정 로드 완료');
            } else {
                console.log('[EMAIL-CONFIG] 설정이 없어 기본값 사용');
                // 기본값들은 이미 HTML에 설정되어 있음
            }
        } else {
            console.error(`[EMAIL-CONFIG] HTTP 오류: ${response.status}`);
        }
    } catch (error) {
        console.error('[EMAIL-CONFIG] 네트워크 오류:', error);
    }
}

// 이메일 연결 테스트
async function testEmailConnection() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    // 필수 필드 확인
    const smtpHost = document.getElementById('smtpHost').value;
    const smtpPort = document.getElementById('smtpPort').value;
    const smtpUsername = document.getElementById('smtpUsername').value;

    if (!smtpHost || !smtpPort || !smtpUsername) {
        alert('SMTP 호스트, 포트, 사용자명은 필수 입력 항목입니다.');
        return;
    }

    console.log('[EMAIL-CONFIG] 이메일 연결 테스트 시작');

    // 테스트용 설정 데이터 (v1과 동일한 구조)
    const testConfig = {
        smtpHost: smtpHost,
        smtpPort: smtpPort,  // 문자열로 전송 (v1과 동일)
        security: document.querySelector('input[name="security"]:checked').value,
        useAuth: document.getElementById('useAuth').checked,
        username: smtpUsername,
        password: document.getElementById('smtpPassword').value
    };

    console.log('[EMAIL-CONFIG] 테스트 설정:', testConfig);

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        console.log(`[EMAIL-CONFIG] 연결 테스트 API 호출: POST ${serverUrl}/api/email-config/test`);
        const response = await fetch(`${serverUrl}/api/email-config/test`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(testConfig)
        });

        console.log(`[EMAIL-CONFIG] 테스트 응답 상태: ${response.status}`);

        if (response.ok) {
            const data = await response.json();
            console.log('[EMAIL-CONFIG] 테스트 응답:', data);
            
            if (data.success) {
                alert('✅ 이메일 서버 연결 성공!\n' + (data.message || ''));
            } else {
                alert('❌ 이메일 서버 연결 실패:\n' + data.message);
            }
        } else {
            const errorText = await response.text();
            console.error(`[EMAIL-CONFIG] 테스트 HTTP 오류: ${response.status}, 응답: ${errorText}`);
            alert(`❌ 이메일 서버 연결 테스트 실패: HTTP ${response.status}`);
        }
    } catch (error) {
        console.error('[EMAIL-CONFIG] 테스트 네트워크 오류:', error);
        alert('❌ 이메일 연결 테스트 오류: ' + error.message);
    }
}

// === 이메일 프로바이더 프리셋 함수들 ===

// Gmail 프리셋
function applyGmailPreset() {
    document.getElementById('smtpHost').value = 'smtp.gmail.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    alert('Gmail SMTP 설정이 적용되었습니다.\n사용자명과 앱 비밀번호를 입력하세요.');
}

// Outlook 프리셋
function applyOutlookPreset() {
    document.getElementById('smtpHost').value = 'smtp-mail.outlook.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    alert('Outlook SMTP 설정이 적용되었습니다.\n사용자명과 비밀번호를 입력하세요.');
}

// Yahoo 프리셋
function applyYahooPreset() {
    document.getElementById('smtpHost').value = 'smtp.mail.yahoo.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    alert('Yahoo SMTP 설정이 적용되었습니다.\n앱 비밀번호 사용을 권장합니다.');
}

// Naver 프리셋
function applyNaverPreset() {
    document.getElementById('smtpHost').value = 'smtp.naver.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    alert('Naver SMTP 설정이 적용되었습니다.\n네이버 이메일과 비밀번호를 입력하세요.');
}

// Ecount 프리셋
function applyEcountPreset() {
    document.getElementById('smtpHost').value = 'smtp.ecounterp.com';
    document.getElementById('smtpPort').value = '587';
    document.querySelector('input[name="security"][value="tls"]').checked = true;
    document.getElementById('useAuth').checked = true;
    document.getElementById('smtpUsername').value = 'ecounterp@ecounterp.com';
    alert('Ecount SMTP 설정이 적용되었습니다.\n비밀번호를 입력하세요.');
}

