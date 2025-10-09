/**
 * sms-config Module
 * Auto-extracted from index_v2.html
 */

// === SMS 설정 관련 함수들 ===

// SMS 설정 저장
async function saveSmsConfig() {
    console.log('[SMS-CONFIG] SMS 설정 저장 시작');
    
    if (!currentToken) {
        console.error('[SMS-CONFIG] 토큰이 없음');
        alert('먼저 로그인해주세요.');
        return;
    }

    const smsConfig = {
        apiUrl: document.getElementById('smsApiUrl').value,
        senderNumber: document.getElementById('smsSenderNumber').value,
        senderName: document.getElementById('smsSenderName').value,
        testMode: document.getElementById('smsTestMode').checked,
        smsTemplate: document.getElementById('smsTemplate') ? document.getElementById('smsTemplate').value : '',
        kakaoTemplate: document.getElementById('kakaoTemplate') ? document.getElementById('kakaoTemplate').value : ''
    };

    console.log('[SMS-CONFIG] 저장할 설정:', smsConfig);

    if (!smsConfig.senderNumber) {
        alert('발송자 번호를 입력해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    console.log('[SMS-CONFIG] 서버 URL:', serverUrl);

    try {
        console.log('[SMS-CONFIG] API 요청 시작');
        const response = await fetch(`${serverUrl}/api/sms-config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(smsConfig)
        });

        console.log('[SMS-CONFIG] 응답 상태:', response.status);

        if (!response.ok) {
            console.error('[SMS-CONFIG] HTTP 오류:', response.status);
            const errorText = await response.text();
            console.error('[SMS-CONFIG] 오류 응답:', errorText);
            alert(`SMS 설정 저장 HTTP 오류: ${response.status} - ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('[SMS-CONFIG] API 응답 데이터:', data);
        
        if (data.success) {
            console.log('[SMS-CONFIG] SMS 설정 저장 성공');
            alert('SMS 설정이 저장되었습니다.');
            
            // 저장 후 즉시 설정을 다시 로드하여 확인
            console.log('[SMS-CONFIG] 저장 확인을 위해 설정 다시 로드');
            setTimeout(() => {
                loadSmsConfig();
            }, 500);
        } else {
            console.error('[SMS-CONFIG] SMS 설정 저장 실패:', data.message);
            alert('SMS 설정 저장 실패: ' + data.message);
        }
    } catch (error) {
        console.error('[SMS-CONFIG] SMS 설정 저장 예외:', error);
        alert('SMS 설정 저장 오류: ' + error.message);
    }
}

// SMS 설정 불러오기
async function loadSmsConfig() {
    console.log('[SMS-CONFIG] SMS 설정 불러오기 시작');
    
    if (!currentToken) {
        console.warn('[SMS-CONFIG] 토큰이 없어서 설정 불러오기 건너뜀');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    console.log('[SMS-CONFIG] 서버 URL:', serverUrl);

    try {
        console.log('[SMS-CONFIG] 설정 불러오기 API 요청');
        const response = await fetch(`${serverUrl}/api/sms-config`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log('[SMS-CONFIG] 응답 상태:', response.status);

        if (!response.ok) {
            console.error('[SMS-CONFIG] 설정 불러오기 실패:', response.status);
            if (response.status === 404) {
                console.info('[SMS-CONFIG] 저장된 설정이 없음 (첫 설정)');
            }
            return;
        }

        const data = await response.json();
        console.log('[SMS-CONFIG] 불러온 데이터:', data);

        if (data.success && data.config) {
            const config = data.config;
            console.log('[SMS-CONFIG] 설정 데이터:', config);

            // 폼 필드에 설정값 적용
            if (config.businessId) {
                // 사업자등록번호 표시 (포맷팅)
                const formatted = config.businessId.replace(/(\d{3})(\d{2})(\d{5})/, '$1-$2-$3');
                document.getElementById('displayBusinessNumber').textContent = formatted + ' (관리자 설정에서 가져옴)';
            }
            if (config.apiUrl) document.getElementById('smsApiUrl').value = config.apiUrl;
            if (config.senderNumber) document.getElementById('smsSenderNumber').value = config.senderNumber;
            if (config.senderName) document.getElementById('smsSenderName').value = config.senderName;
            if (config.testMode !== undefined) document.getElementById('smsTestMode').checked = config.testMode;
            if (config.smsTemplate && document.getElementById('smsTemplate')) document.getElementById('smsTemplate').value = config.smsTemplate;
            if (config.kakaoTemplate && document.getElementById('kakaoTemplate')) document.getElementById('kakaoTemplate').value = config.kakaoTemplate;

            console.log('[SMS-CONFIG] 폼 필드 업데이트 완료');
        } else {
            console.warn('[SMS-CONFIG] 설정 데이터가 없거나 실패:', data);
        }
    } catch (error) {
        console.error('[SMS-CONFIG] SMS 설정 불러오기 예외:', error);
    }
}

// SMS 연결 테스트
async function testSmsConnection() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const smsConfig = {
        apiUrl: document.getElementById('smsApiUrl').value,
        senderNumber: document.getElementById('smsSenderNumber').value,
        testMode: true // 연결 테스트는 항상 테스트 모드
    };

    if (!smsConfig.senderNumber) {
        alert('발송자 번호를 입력해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        console.log('[SMS-TEST] SMS 연결 테스트 시작');
        const response = await fetch(`${serverUrl}/api/sms-test`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(smsConfig)
        });

        const data = await response.json();
        console.log('[SMS-TEST] 테스트 응답:', data);

        if (data.success) {
            alert('✅ SMS API 연결 성공!\n' + (data.message || '연결이 정상적으로 작동합니다.'));
        } else {
            alert('❌ SMS API 연결 실패!\n' + (data.message || '연결 설정을 확인해주세요.'));
        }
    } catch (error) {
        console.error('[SMS-TEST] 연결 테스트 오류:', error);
        alert('❌ SMS 연결 테스트 오류: ' + error.message);
    }
}

