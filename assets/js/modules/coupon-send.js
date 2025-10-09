/**
 * coupon-send Module
 * Auto-extracted from index_v2.html
 */

// === 쿠폰 발송 관련 함수들 ===

// 쿠폰 발송용 거래처 로드
async function loadCorporatesForSendTab() {
    if (!currentToken) {
        console.error('[SEND-CORPORATE] 토큰이 없음');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/corporates`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('sendCorporateSelect');
            select.innerHTML = '<option value="">거래처를 선택하세요</option>';
            
            // API 응답에서 거래처 목록은 data.data에 있음
            if (data.data && Array.isArray(data.data)) {
                data.data.forEach(corp => {
                    const option = document.createElement('option');
                    option.value = corp.customerId;
                    option.textContent = corp.name;
                    select.appendChild(option);
                });
            } else {
                console.error('[SEND-CORPORATE] 거래처 데이터 형식 오류:', data);
                alert('거래처 데이터를 불러올 수 없습니다.');
            }
        } else {
            alert('거래처 로드 실패: ' + (data.message || '알 수 없는 오류'));
        }
    } catch (error) {
        console.error('[SEND-CORPORATE] 거래처 로드 오류:', error);
        alert('거래처 로드 오류: ' + error.message);
    }
}

// 발송용 쿠폰 로드
async function loadCouponsForSend() {
    const corporateId = document.getElementById('sendCorporateSelect').value;
    const sendPanel = document.getElementById('couponSendPanel');
    const sendInstructions = document.getElementById('sendInstructions');

    if (!corporateId) {
        sendPanel.style.display = 'none';
        sendInstructions.style.display = 'block';
        return;
    }

    sendInstructions.style.display = 'none';
    sendPanel.style.display = 'block';

    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        // v1과 동일한 API 경로 사용: /api/corporates/{id}/coupons
        const response = await fetch(`${serverUrl}/api/corporates/${corporateId}/coupons`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json; charset=utf-8'
            }
        });

        const data = await response.json();

        if (data.success) {
            // API 응답에서 쿠폰 목록은 data.data에 있음
            const coupons = data.data || [];
            
            // 각 쿠폰에 대해 발송 상태 확인
            const couponsWithDeliveryStatus = await Promise.all(coupons.map(async (coupon) => {
                try {
                    // 발송 이력 조회
                    const deliveryResponse = await fetch(`${serverUrl}/api/coupon-send/history?couponId=${coupon.couponId}`, {
                        method: 'GET',
                        headers: {
                            'Authorization': `Bearer ${currentToken}`,
                            'Content-Type': 'application/json; charset=utf-8'
                        }
                    });
                    
                    const deliveryData = await deliveryResponse.json();
                    const deliveries = deliveryData.success ? deliveryData.data : [];
                    
                    // 발송 유형별 상태 확인
                    const emailSent = deliveries.some(d => d.type === 'email' && (d.status === 'sent' || d.status === 'delivered'));
                    const smsSent = deliveries.some(d => d.type === 'sms' && (d.status === 'sent' || d.status === 'delivered'));
                    const kakaoSent = deliveries.some(d => d.type === 'kakao' && (d.status === 'sent' || d.status === 'delivered'));
                    
                    return {
                        ...coupon,
                        emailSent,
                        smsSent,
                        kakaoSent,
                        recipientName: coupon.recipientName || '수신자',
                        recipientPhone: coupon.recipientPhone || '-',
                        recipientEmail: coupon.recipientEmail || '-'
                    };
                } catch (error) {
                    console.error('발송 기록 조회 오류:', error);
                    return {
                        ...coupon,
                        emailSent: false,
                        smsSent: false,
                        kakaoSent: false,
                        recipientName: coupon.recipientName || '수신자',
                        recipientPhone: coupon.recipientPhone || '-',
                        recipientEmail: coupon.recipientEmail || '-'
                    };
                }
            }));

            // 전역 변수에 저장 (발송 함수에서 사용)
            window.currentCoupons = couponsWithDeliveryStatus;

            updateSendSummary(couponsWithDeliveryStatus);
            updateCouponSendTable(couponsWithDeliveryStatus);
        } else {
            alert('쿠폰 로드 실패: ' + (data.message || '알 수 없는 오류'));
        }
    } catch (error) {
        console.error('쿠폰 로드 오류:', error);
        alert('쿠폰 로드 오류: ' + error.message);
    }
}


// 발송 요약 정보 업데이트
function updateSendSummary(coupons) {
    const totalCount = coupons.length;
    const emailSentCount = coupons.filter(c => c.emailSent).length;
    const smsSentCount = coupons.filter(c => c.smsSent).length;
    const kakaoSentCount = coupons.filter(c => c.kakaoSent).length;

    const emailAvailableCount = coupons.filter(c => c.recipientEmail && c.recipientEmail !== '-').length;
    const phoneAvailableCount = coupons.filter(c => c.recipientPhone && c.recipientPhone !== '-').length;

    document.getElementById('sendSummary').innerHTML = `
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
            <div>
                <strong>📊 전체 쿠폰:</strong> ${totalCount}개
            </div>
            <div>
                <strong>📧 이메일 발송:</strong> ${emailSentCount}/${emailAvailableCount}개
            </div>
            <div>
                <strong>📱 SMS 발송:</strong> ${smsSentCount}/${phoneAvailableCount}개
            </div>
            <div>
                <strong>💬 카카오톡 발송:</strong> ${kakaoSentCount}/${totalCount}개
            </div>
        </div>
    `;
}

// 쿠폰 발송 테이블 업데이트
function updateCouponSendTable(coupons) {
    const tbody = document.getElementById('couponSendBody');
    tbody.innerHTML = '';

    coupons.forEach(coupon => {
        const row = document.createElement('tr');
        
        // 발송 상태 표시
        const statusHtml = `
            <div style="display: flex; flex-direction: column; gap: 2px; font-size: 11px;">
                <span style="color: ${coupon.emailSent ? '#28a745' : '#6c757d'};">📧 ${coupon.emailSent ? '발송됨' : '미발송'}</span>
                <span style="color: ${coupon.smsSent ? '#28a745' : '#6c757d'};">📱 ${coupon.smsSent ? '발송됨' : '미발송'}</span>
                <span style="color: ${coupon.kakaoSent ? '#28a745' : '#6c757d'};">💬 ${coupon.kakaoSent ? '발송됨' : '미발송'}</span>
            </div>
        `;
        
        // 발송 액션 버튼
        const actionButtons = `
            <div style="display: flex; gap: 5px; justify-content: center;">
                <button onclick="sendIndividualEmail(${coupon.couponId})" 
                        style="background: ${coupon.emailSent ? '#6c757d' : '#dc3545'}; color: white; border: none; padding: 4px 8px; border-radius: 4px; font-size: 11px;"
                        ${!coupon.recipientEmail || coupon.recipientEmail === '-' ? 'disabled' : ''}>
                    📧
                </button>
                <button onclick="sendIndividualSMS(${coupon.couponId})" 
                        style="background: ${coupon.smsSent ? '#6c757d' : '#28a745'}; color: white; border: none; padding: 4px 8px; border-radius: 4px; font-size: 11px;"
                        ${!coupon.recipientPhone || coupon.recipientPhone === '-' ? 'disabled' : ''}>
                    📱
                </button>
                <button onclick="sendIndividualKakao(${coupon.couponId})" 
                        style="background: ${coupon.kakaoSent ? '#6c757d' : '#ffc107'}; color: white; border: none; padding: 4px 8px; border-radius: 4px; font-size: 11px;">
                    💬
                </button>
            </div>
        `;

        row.innerHTML = `
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.fullCouponCode || coupon.couponCode || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.recipientName || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.recipientPhone || '-'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.recipientEmail || '-'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${statusHtml}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${actionButtons}</td>
        `;
        
        tbody.appendChild(row);
    });
}

// 이메일 설정 확인
async function checkEmailSettings() {
    console.log('[EMAIL-CHECK] 이메일 설정 확인 시작');
    
    if (!currentToken) {
        console.error('[EMAIL-CHECK] 토큰이 없음');
        return false;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/email-config`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        if (!response.ok) {
            console.error('[EMAIL-CHECK] 이메일 설정 조회 실패:', response.status);
            return false;
        }
        
        const data = await response.json();
        console.log('[EMAIL-CHECK] 이메일 설정:', data);
        
        if (data.success && data.config) {
            const config = data.config;
            const hasBasicSettings = config.smtpHost && config.smtpPort && config.username;
            console.log('[EMAIL-CHECK] 기본 설정 완료 여부:', hasBasicSettings);
            return hasBasicSettings;
        }
        
        return false;
    } catch (error) {
        console.error('[EMAIL-CHECK] 이메일 설정 확인 오류:', error);
        return false;
    }
}

// SMS 설정 확인 함수
async function checkSmsSettings() {
    console.log('[SMS-CHECK] SMS 설정 확인 시작');
    
    if (!currentToken) {
        console.error('[SMS-CHECK] 토큰이 없음');
        return false;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/sms-config`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        if (!response.ok) {
            console.error('[SMS-CHECK] SMS 설정 조회 실패:', response.status);
            return false;
        }
        
        const data = await response.json();
        console.log('[SMS-CHECK] SMS 설정:', data);
        
        if (data.success && data.config) {
            const config = data.config;
            const hasBasicSettings = config.businessId && config.senderNumber;
            console.log('[SMS-CHECK] 기본 설정 완료 여부:', hasBasicSettings);
            console.log('[SMS-CHECK] 사업자등록번호:', config.businessId ? '설정됨' : '미설정');
            console.log('[SMS-CHECK] 발송자 번호:', config.senderNumber ? '설정됨' : '미설정');
            return hasBasicSettings;
        }
        
        return false;
    } catch (error) {
        console.error('[SMS-CHECK] SMS 설정 확인 오류:', error);
        return false;
    }
}

// 전체 이메일 발송
async function sendAllEmails() {
    console.log('[EMAIL-BULK] 전체 이메일 발송 시작');
    
    if (!window.currentCoupons || window.currentCoupons.length === 0) {
        alert('발송할 쿠폰이 없습니다.');
        return;
    }
    
    const unsentCoupons = window.currentCoupons.filter(c => !c.emailSent && c.recipientEmail && c.recipientEmail !== '-');
    console.log(`[EMAIL-BULK] 발송 대상 쿠폰 수: ${unsentCoupons.length}`);
    
    if (unsentCoupons.length === 0) {
        alert('이메일 발송 가능한 쿠폰이 없습니다.');
        return;
    }
    
    // 이메일 설정 확인
    console.log('[EMAIL-BULK] 이메일 설정 확인 중...');
    const emailConfigured = await checkEmailSettings();
    if (!emailConfigured) {
        console.warn('[EMAIL-BULK] 이메일 설정이 완료되지 않음');
        if (!confirm('이메일 설정이 완료되지 않았습니다. 그래도 계속하시겠습니까? (실제 이메일은 발송되지 않습니다)')) {
            return;
        }
    }
    
    if (!confirm(`${unsentCoupons.length}개의 쿠폰을 이메일로 발송하시겠습니까?`)) {
        return;
    }
    
    console.log('[EMAIL-BULK] 개별 이메일 발송 시작');
    let successCount = 0;
    let errorCount = 0;
    
    for (let i = 0; i < unsentCoupons.length; i++) {
        const coupon = unsentCoupons[i];
        console.log(`[EMAIL-BULK] 발송 진행 (${i + 1}/${unsentCoupons.length}) - 쿠폰ID: ${coupon.couponId}`);
        
        try {
            const success = await sendIndividualEmail(coupon.couponId, false); // false = 개별 알림 안함
            if (success) {
                successCount++;
                console.log(`[EMAIL-BULK] 발송 성공 - 쿠폰ID: ${coupon.couponId}`);
            } else {
                errorCount++;
                console.error(`[EMAIL-BULK] 발송 실패 - 쿠폰ID: ${coupon.couponId}`);
            }
        } catch (error) {
            errorCount++;
            console.error(`[EMAIL-BULK] 발송 예외 - 쿠폰ID: ${coupon.couponId}`, error);
        }
        
        // 발송 간격 (서버 부하 방지)
        if (i < unsentCoupons.length - 1) {
            await new Promise(resolve => setTimeout(resolve, 100)); // 100ms 간격
        }
    }
    
    console.log(`[EMAIL-BULK] 전체 발송 완료 - 성공: ${successCount}, 실패: ${errorCount}`);
    alert(`이메일 발송 완료: ${successCount}/${unsentCoupons.length}건 성공${errorCount > 0 ? `, ${errorCount}건 실패` : ''}`);
    loadCouponsForSend(); // 테이블 새로고침
}

// 전체 SMS 발송
async function sendAllSMS() {
    console.log('[SMS-BULK] 전체 SMS 발송 시작');
    
    if (!window.currentCoupons || window.currentCoupons.length === 0) {
        alert('발송할 쿠폰이 없습니다.');
        return;
    }
    
    const unsentCoupons = window.currentCoupons.filter(c => !c.smsSent && c.recipientPhone && c.recipientPhone !== '-');
    
    if (unsentCoupons.length === 0) {
        alert('SMS 발송 가능한 쿠폰이 없습니다.');
        return;
    }
    
    // SMS 설정 확인
    console.log('[SMS-BULK] SMS 설정 확인 중...');
    const smsConfigured = await checkSmsSettings();
    if (!smsConfigured) {
        console.warn('[SMS-BULK] SMS 설정이 완료되지 않음');
        if (!confirm('SMS 설정이 완료되지 않았습니다. 그래도 계속하시겠습니까? (실제 SMS는 발송되지 않습니다)')) {
            return;
        }
    }
    
    if (!confirm(`${unsentCoupons.length}개의 쿠폰을 SMS로 발송하시겠습니까?`)) {
        return;
    }
    
    console.log('[SMS-BULK] 개별 SMS 발송 시작');
    let successCount = 0;
    for (const coupon of unsentCoupons) {
        const success = await sendIndividualSMS(coupon.couponId, false); // false = 개별 알림 안함
        if (success) successCount++;
    }
    
    alert(`SMS 발송 완료: ${successCount}/${unsentCoupons.length}건`);
    loadCouponsForSend(); // 테이블 새로고침
}

// 전체 카카오톡 발송
async function sendAllKakao() {
    console.log('[KAKAO-BULK] 전체 카카오톡 발송 시작');
    
    if (!window.currentCoupons || window.currentCoupons.length === 0) {
        alert('발송할 쿠폰이 없습니다.');
        return;
    }
    
    const unsentCoupons = window.currentCoupons.filter(c => !c.kakaoSent);
    
    if (unsentCoupons.length === 0) {
        alert('카카오톡 발송 가능한 쿠폰이 없습니다.');
        return;
    }
    
    // SMS 설정 확인 (카카오톡도 같은 API 사용)
    console.log('[KAKAO-BULK] SMS 설정 확인 중...');
    const smsConfigured = await checkSmsSettings();
    if (!smsConfigured) {
        console.warn('[KAKAO-BULK] SMS 설정이 완료되지 않음');
        if (!confirm('SMS 설정이 완료되지 않았습니다. 그래도 계속하시겠습니까? (실제 카카오톡은 발송되지 않습니다)')) {
            return;
        }
    }
    
    if (!confirm(`${unsentCoupons.length}개의 쿠폰을 카카오톡으로 발송하시겠습니까?`)) {
        return;
    }
    
    console.log('[KAKAO-BULK] 개별 카카오톡 발송 시작');
    let successCount = 0;
    for (const coupon of unsentCoupons) {
        const success = await sendIndividualKakao(coupon.couponId, false); // false = 개별 알림 안함
        if (success) successCount++;
    }
    
    alert(`카카오톡 발송 완료: ${successCount}/${unsentCoupons.length}건`);
    loadCouponsForSend(); // 테이블 새로고침
}

// 개별 이메일 발송
async function sendIndividualEmail(couponId, showAlert = true) {
    console.log(`[EMAIL-SEND] 이메일 발송 시작 - 쿠폰ID: ${couponId}, 알림표시: ${showAlert}`);
    
    if (!currentToken) {
        console.error('[EMAIL-SEND] 토큰이 없음');
        if (showAlert) alert('먼저 로그인해주세요.');
        return false;
    }
    console.log(`[EMAIL-SEND] 토큰 확인됨: ${currentToken.substring(0, 20)}...`);
    
    const coupon = window.currentCoupons?.find(c => c.couponId === couponId);
    if (!coupon) {
        console.error(`[EMAIL-SEND] 쿠폰을 찾을 수 없음 - 쿠폰ID: ${couponId}, 전체 쿠폰 수: ${window.currentCoupons?.length || 0}`);
        if (showAlert) alert('쿠폰 정보를 찾을 수 없습니다.');
        return false;
    }
    console.log(`[EMAIL-SEND] 쿠폰 정보 확인됨:`, {
        couponId: coupon.couponId,
        recipientName: coupon.recipientName,
        recipientEmail: coupon.recipientEmail,
        fullCouponCode: coupon.fullCouponCode
    });
    
    if (!coupon.recipientEmail || coupon.recipientEmail === '-') {
        console.error(`[EMAIL-SEND] 이메일 주소 없음 - 수신자: ${coupon.recipientName}, 이메일: ${coupon.recipientEmail}`);
        if (showAlert) alert('이메일 주소가 없습니다.');
        return false;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    console.log(`[EMAIL-SEND] 서버 URL: ${serverUrl}`);
    
    const requestData = {
        couponId: couponId,
        recipientEmail: coupon.recipientEmail,
        recipientName: coupon.recipientName,
        subject: `[쿠폰 발송] ${coupon.recipientName}님의 쿠폰이 발급되었습니다`,
        message: `안녕하세요 ${coupon.recipientName}님,\n\n쿠폰이 발급되었습니다.\n쿠폰번호: ${coupon.fullCouponCode || coupon.couponCode || 'N/A'}\n\n감사합니다.`
    };
    console.log(`[EMAIL-SEND] 요청 데이터:`, requestData);
    
    try {
        console.log(`[EMAIL-SEND] API 호출 시작: ${serverUrl}/api/coupon-send/email`);
        const response = await fetch(`${serverUrl}/api/coupon-send/email`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(requestData)
        });
        
        console.log(`[EMAIL-SEND] API 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            console.error(`[EMAIL-SEND] HTTP 오류: ${response.status} ${response.statusText}`);
            const errorText = await response.text();
            console.error(`[EMAIL-SEND] 오류 응답 내용:`, errorText);
            if (showAlert) alert(`이메일 발송 HTTP 오류: ${response.status} - ${errorText}`);
            return false;
        }
        
        const data = await response.json();
        console.log(`[EMAIL-SEND] API 응답 데이터:`, data);
        
        if (data.success) {
            console.log(`[EMAIL-SEND] 이메일 발송 성공 - 쿠폰ID: ${couponId}`);
            
            // 쿠폰 상태 업데이트 (메모리에서)
            if (coupon) {
                coupon.emailSent = true;
                console.log(`[EMAIL-SEND] 쿠폰 상태 업데이트 완료 - emailSent: true`);
            }
            
            // 개별 발송인 경우에만 테이블 새로고침
            if (showAlert) {
                loadCouponsForSend(); // 테이블 새로고침
                alert('이메일 발송이 완료되었습니다.');
            }
            
            return true;
        } else {
            console.error(`[EMAIL-SEND] 이메일 발송 실패 - 쿠폰ID: ${couponId}, 메시지: ${data.message}`);
            if (showAlert) alert('이메일 발송 실패: ' + data.message);
            return false;
        }
    } catch (error) {
        console.error(`[EMAIL-SEND] 예외 발생:`, error);
        console.error(`[EMAIL-SEND] 스택 트레이스:`, error.stack);
        if (showAlert) alert('이메일 발송 오류: ' + error.message);
        return false;
    }
}

// 개별 SMS 발송
async function sendIndividualSMS(couponId, showAlert = true) {
    if (!currentToken) {
        if (showAlert) alert('먼저 로그인해주세요.');
        return false;
    }
    
    const coupon = window.currentCoupons?.find(c => c.couponId === couponId);
    if (!coupon) {
        if (showAlert) alert('쿠폰 정보를 찾을 수 없습니다.');
        return false;
    }
    
    if (!coupon.recipientPhone || coupon.recipientPhone === '-') {
        if (showAlert) alert('전화번호가 없습니다.');
        return false;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/coupon-send/sms`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({
                couponId: couponId,
                recipientPhone: coupon.recipientPhone
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            // 쿠폰 상태 업데이트 (메모리에서)
            if (coupon) {
                coupon.smsSent = true;
                console.log(`[SMS-SEND] 쿠폰 상태 업데이트 완료 - smsSent: true`);
            }
            
            // 개별 발송인 경우에만 테이블 새로고침
            if (showAlert) {
                loadCouponsForSend(); // 테이블 새로고침
                alert('SMS 발송이 완료되었습니다.');
            }
            return true;
        } else {
            if (showAlert) alert('SMS 발송 실패: ' + data.message);
            return false;
        }
    } catch (error) {
        if (showAlert) alert('SMS 발송 오류: ' + error.message);
        return false;
    }
}

// 개별 카카오톡 발송
async function sendIndividualKakao(couponId, showAlert = true) {
    if (!currentToken) {
        if (showAlert) alert('먼저 로그인해주세요.');
        return false;
    }
    
    const coupon = window.currentCoupons?.find(c => c.couponId === couponId);
    if (!coupon) {
        if (showAlert) alert('쿠폰 정보를 찾을 수 없습니다.');
        return false;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/coupon-send/kakao`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({
                couponId: couponId
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            // 쿠폰 상태 업데이트 (메모리에서)
            if (coupon) {
                coupon.kakaoSent = true;
                console.log(`[KAKAO-SEND] 쿠폰 상태 업데이트 완료 - kakaoSent: true`);
            }
            
            // 개별 발송인 경우에만 테이블 새로고침
            if (showAlert) {
                loadCouponsForSend(); // 테이블 새로고침
                alert('카카오톡 발송이 완료되었습니다.');
            }
            return true;
        } else {
            if (showAlert) alert('카카오톡 발송 실패: ' + data.message);
            return false;
        }
    } catch (error) {
        if (showAlert) alert('카카오톡 발송 오류: ' + error.message);
        return false;
    }
}

