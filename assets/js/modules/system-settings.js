/**
 * system-settings Module
 * Auto-extracted from index_v2.html
 */

// System settings functions
async function loadSystemSettings() {
    if (!currentToken) {
        displayResult('먼저 로그인해주세요.', 'error');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/system/settings`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        if (response.ok) {
            const settings = await response.json();
            console.log('시스템 설정 불러오기 성공:', settings);
            applySettingsToUI(settings);
            displayResult('시스템 설정을 성공적으로 불러왔습니다.');
        } else {
            const error = await response.json();
            console.error('설정 불러오기 실패:', error);
            displayResult(`설정 불러오기 실패: ${error.message || response.statusText}`, 'error');
        }
    } catch (error) {
        console.error('설정 불러오기 중 오류:', error);
        displayResult(`설정 불러오기 중 오류: ${error.message}`, 'error');
    }
}

async function saveSystemSettings() {
    if (!currentToken) {
        displayResult('먼저 로그인해주세요.', 'error');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    const settings = collectSettingsFromUI();
    
    try {
        const response = await fetch(`${serverUrl}/api/system/settings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(settings)
        });
        
        if (response.ok) {
            const result = await response.json();
            console.log('시스템 설정 저장 성공:', result);
            displayResult('시스템 설정이 성공적으로 저장되었습니다.');
            showSettingsPreview(settings);
        } else {
            const error = await response.json();
            console.error('설정 저장 실패:', error);
            displayResult(`설정 저장 실패: ${error.message || response.statusText}`, 'error');
        }
    } catch (error) {
        console.error('설정 저장 중 오류:', error);
        displayResult(`설정 저장 중 오류: ${error.message}`, 'error');
    }
}

function collectSettingsFromUI() {
    return {
        enable_time_based_deduction: document.getElementById('enableTimeBasedDeduction').checked.toString(),
        breakfast_start_time: document.getElementById('breakfastStartTime').value,
        breakfast_end_time: document.getElementById('breakfastEndTime').value,
        breakfast_cash_deduction: document.getElementById('breakfastCashDeduction').value,
        breakfast_point_deduction: document.getElementById('breakfastPointDeduction').value,
        lunch_start_time: document.getElementById('lunchStartTime').value,
        lunch_end_time: document.getElementById('lunchEndTime').value,
        lunch_cash_deduction: document.getElementById('lunchCashDeduction').value,
        lunch_point_deduction: document.getElementById('lunchPointDeduction').value,
        dinner_start_time: document.getElementById('dinnerStartTime').value,
        dinner_end_time: document.getElementById('dinnerEndTime').value,
        dinner_cash_deduction: document.getElementById('dinnerCashDeduction').value,
        dinner_point_deduction: document.getElementById('dinnerPointDeduction').value,
        default_cash_deduction: document.getElementById('defaultCashDeduction').value,
        default_point_deduction: document.getElementById('defaultPointDeduction').value,
        allow_negative_balance: document.getElementById('allowNegativeBalance').checked.toString(),
        parking_registration_url: document.getElementById('parkingRegistrationUrl')?.value || ''
    };
}

function applySettingsToUI(settings) {
    document.getElementById('enableTimeBasedDeduction').checked = settings.enable_time_based_deduction === 'true';
    document.getElementById('breakfastStartTime').value = settings.breakfast_start_time || '06:00';
    document.getElementById('breakfastEndTime').value = settings.breakfast_end_time || '10:59';
    document.getElementById('breakfastCashDeduction').value = settings.breakfast_cash_deduction || '3000';
    document.getElementById('breakfastPointDeduction').value = settings.breakfast_point_deduction || '0';
    document.getElementById('lunchStartTime').value = settings.lunch_start_time || '11:00';
    document.getElementById('lunchEndTime').value = settings.lunch_end_time || '14:59';
    document.getElementById('lunchCashDeduction').value = settings.lunch_cash_deduction || '5000';
    document.getElementById('lunchPointDeduction').value = settings.lunch_point_deduction || '0';
    document.getElementById('dinnerStartTime').value = settings.dinner_start_time || '15:00';
    document.getElementById('dinnerEndTime').value = settings.dinner_end_time || '21:59';
    document.getElementById('dinnerCashDeduction').value = settings.dinner_cash_deduction || '7000';
    document.getElementById('dinnerPointDeduction').value = settings.dinner_point_deduction || '0';
    document.getElementById('defaultCashDeduction').value = settings.default_cash_deduction || '1000';
    document.getElementById('defaultPointDeduction').value = settings.default_point_deduction || '0';
    document.getElementById('allowNegativeBalance').checked = settings.allow_negative_balance === 'true';

    // 주차등록 URL 설정
    const parkingUrlInput = document.getElementById('parkingRegistrationUrl');
    if (parkingUrlInput) {
        parkingUrlInput.value = settings.parking_registration_url || '';
    }
}

function showSettingsPreview(settings) {
    const preview = document.getElementById('settingsPreview');
    const content = document.getElementById('previewContent');
    
    const isTimeBasedEnabled = settings.enable_time_based_deduction === 'true';
    const allowNegative = settings.allow_negative_balance === 'true';
    
    content.innerHTML = `
        <div style="font-family: monospace; font-size: 12px; line-height: 1.6;">
            <div><strong>시간대별 차감:</strong> ${isTimeBasedEnabled ? '✅ 활성화' : '❌ 비활성화'}</div>
            <div><strong>마이너스 잔고:</strong> ${allowNegative ? '✅ 허용' : '❌ 금지'}</div>
            <hr style="margin: 10px 0;">
            <div><strong>🌅 아침 (${settings.breakfast_start_time} - ${settings.breakfast_end_time}):</strong> 현금 ${parseInt(settings.breakfast_cash_deduction).toLocaleString()}원, 포인트 ${parseInt(settings.breakfast_point_deduction).toLocaleString()}P</div>
            <div><strong>🌞 점심 (${settings.lunch_start_time} - ${settings.lunch_end_time}):</strong> 현금 ${parseInt(settings.lunch_cash_deduction).toLocaleString()}원, 포인트 ${parseInt(settings.lunch_point_deduction).toLocaleString()}P</div>
            <div><strong>🌙 저녁 (${settings.dinner_start_time} - ${settings.dinner_end_time}):</strong> 현금 ${parseInt(settings.dinner_cash_deduction).toLocaleString()}원, 포인트 ${parseInt(settings.dinner_point_deduction).toLocaleString()}P</div>
            <div><strong>🕒 기본 (시간대 외):</strong> 현금 ${parseInt(settings.default_cash_deduction).toLocaleString()}원, 포인트 ${parseInt(settings.default_point_deduction).toLocaleString()}P</div>
        </div>
    `;
    
    preview.style.display = 'block';
    
    setTimeout(() => {
        preview.style.display = 'none';
    }, 5000);
}

function displayResult(message, type = 'success') {
    console.log('displayResult:', type, message);
    
    const existingResult = document.getElementById('systemSettingsResult');
    if (existingResult) {
        existingResult.remove();
    }
    
    const resultDiv = document.createElement('div');
    resultDiv.id = 'systemSettingsResult';
    resultDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        font-weight: bold;
        z-index: 9999;
        max-width: 400px;
        word-wrap: break-word;
        transition: all 0.3s ease;
        ${type === 'error' 
            ? 'background: #fee; border: 2px solid #f66; color: #c33;' 
            : 'background: #efe; border: 2px solid #6c6; color: #363;'}
    `;
    
    resultDiv.textContent = message;
    document.body.appendChild(resultDiv);
    
    setTimeout(() => {
        if (resultDiv && resultDiv.parentNode) {
            resultDiv.style.opacity = '0';
            setTimeout(() => {
                if (resultDiv.parentNode) {
                    resultDiv.parentNode.removeChild(resultDiv);
                }
            }, 300);
        }
    }, 5000);
    
    if (type === 'success') {
        console.log('✅ 시스템 설정 작업 완료:', message);
    } else {
        console.error('❌ 시스템 설정 오류:', message);
    }
}

