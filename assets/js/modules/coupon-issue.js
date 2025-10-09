/**
 * coupon-issue Module
 * Auto-extracted from index_v2.html
 */

// ========== 쿠폰 발행 함수들 ==========

let couponEmployees = [];

// 쿠폰 탭을 위한 거래처 목록 로드
async function loadCorporatesForCouponTab() {
    if (!currentToken) {
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
            const select = document.getElementById('couponCorporateSelect');
            select.innerHTML = '<option value="">거래처를 선택하세요</option>';
            
            data.data.forEach(corporate => {
                const option = document.createElement('option');
                option.value = corporate.customerId;
                option.textContent = corporate.name;
                option.dataset.corporateName = corporate.name;
                select.appendChild(option);
            });

            // 만료일 기본값 설정 (1개월 후)
            setExpireDate(1);
        }
    } catch (error) {
        console.error('Error loading corporates for coupon:', error);
    }
}

// 쿠폰 발행을 위한 직원 목록 로드
async function loadEmployeesForCoupon() {
    const corporateId = document.getElementById('couponCorporateSelect').value;
    
    if (!corporateId) {
        document.getElementById('couponSettingsPanel').style.display = 'none';
        document.getElementById('employeeDistributionPanel').style.display = 'none';
        return;
    }

    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/corporates/${corporateId}/employees`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            couponEmployees = data.data;
            document.getElementById('couponSettingsPanel').style.display = 'block';
            
            if (couponEmployees.length > 0) {
                calculateDistribution();
            } else {
                alert('해당 거래처에 등록된 직원이 없습니다.');
                document.getElementById('employeeDistributionPanel').style.display = 'none';
            }
        } else {
            alert('직원 목록을 불러오는데 실패했습니다: ' + data.message);
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}

// 포인트 입력 방식 토글
function togglePointInput() {
    const pointType = document.querySelector('input[name="pointType"]:checked').value;
    const pointDescription = document.getElementById('pointDescription');
    const pointValue = document.getElementById('pointValue');
    
    if (pointType === 'percentage') {
        pointDescription.textContent = '현금 충전액의 % 만큼 포인트 지급';
        pointValue.placeholder = '10';
        // 기존 값이 있으면 포맷 다시 적용
        if (pointValue.value) {
            formatPointInput(pointValue);
        }
    } else if (pointType === 'fixed') {
        pointDescription.textContent = '쿠폰 1장당 고정 포인트 지급';
        pointValue.placeholder = '1,000';
        // 기존 값이 있으면 포맷 다시 적용
        if (pointValue.value) {
            formatPointInput(pointValue);
        }
    } else if (pointType === 'total') {
        pointDescription.textContent = '총 포인트를 직원들에게 균등 분배';
        pointValue.placeholder = '100,000';
        // 기존 값이 있으면 포맷 다시 적용
        if (pointValue.value) {
            formatPointInput(pointValue);
        }
    }
    
    calculateDistribution();
}

// 천단위 콤마 포맷팅 함수
function formatCurrencyInput(input) {
    // 입력값에서 숫자만 추출
    let value = input.value.replace(/[^\d]/g, '');
    
    // 빈 값 처리
    if (value === '') {
        input.value = '';
        calculateDistribution();
        return;
    }
    
    // 천단위마다 콤마 추가
    let formattedValue = parseInt(value).toLocaleString();
    input.value = formattedValue;
    
    // 배분 재계산
    calculateDistribution();
}

// 포인트 입력 포맷팅 함수 (정률일 때는 소수점 허용, 정액/총포인트일 때는 콤마 표시)
function formatPointInput(input) {
    const pointType = document.querySelector('input[name="pointType"]:checked').value;
    
    if (pointType === 'percentage') {
        // 정률일 때는 소수점 허용, 콤마 없음
        let value = input.value.replace(/[^\d.]/g, '');
        // 소수점이 여러 개 있으면 첫 번째만 유지
        let parts = value.split('.');
        if (parts.length > 2) {
            value = parts[0] + '.' + parts.slice(1).join('');
        }
        input.value = value;
    } else {
        // 정액/총포인트일 때는 천단위 콤마 적용
        let value = input.value.replace(/[^\d]/g, '');
        if (value === '') {
            input.value = '';
        } else {
            input.value = parseInt(value).toLocaleString();
        }
    }
    
    calculateDistribution();
}

// 콤마가 포함된 문자열에서 숫자값 추출
function getNumericValue(formattedString) {
    if (!formattedString) return 0;
    return parseFloat(formattedString.replace(/,/g, '')) || 0;
}

// 배분 계산
function calculateDistribution() {
    if (couponEmployees.length === 0) {
        return;
    }

    const totalCash = getNumericValue(document.getElementById('totalCashAmount').value);
    const pointType = document.querySelector('input[name="pointType"]:checked').value;
    const pointValue = getNumericValue(document.getElementById('pointValue').value);
    
    const employeeCount = couponEmployees.length;
    const cashPerEmployee = totalCash / employeeCount;
    
    let pointPerEmployee = 0;
    if (pointType === 'percentage') {
        pointPerEmployee = Math.round((cashPerEmployee * pointValue / 100) * 100) / 100; // 소수점 2자리
    } else if (pointType === 'fixed') {
        pointPerEmployee = pointValue;
    } else if (pointType === 'total') {
        // 총 포인트를 직원 수로 나누어 균등 배분
        pointPerEmployee = Math.round((pointValue / employeeCount) * 100) / 100;
    }

    // 테이블 업데이트
    updateEmployeeDistributionTable(cashPerEmployee, pointPerEmployee);
    
    // 요약 정보 업데이트
    updateDistributionSummary(totalCash, pointPerEmployee * employeeCount, employeeCount);
    
    // 패널 표시
    document.getElementById('employeeDistributionPanel').style.display = 'block';
    
    // 발행 버튼 활성화 조건 체크
    const canIssue = totalCash > 0 && document.getElementById('expireDate').value;
    document.getElementById('issueCouponsBtn').disabled = !canIssue;
}

// 직원별 배분 테이블 업데이트
function updateEmployeeDistributionTable(cashPerEmployee, pointPerEmployee) {
    const tbody = document.getElementById('employeeDistributionBody');
    let html = '';
    
    couponEmployees.forEach(employee => {
        const displayName = employee.name || employee.phone;
        html += `
            <tr>
                <td style="border: 1px solid #e0e0e0; padding: 12px;">${displayName}</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px;">${employee.phone}</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px;">${employee.email || '-'}</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: right; font-weight: bold; color: #e53e3e;">
                    ₩${cashPerEmployee.toLocaleString()}
                </td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: right; font-weight: bold; color: #38a169;">
                    ${pointPerEmployee.toLocaleString()}P
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

// 배분 요약 정보 업데이트
function updateDistributionSummary(totalCash, totalPoints, employeeCount) {
    const summary = document.getElementById('distributionSummary');
    const pointType = document.querySelector('input[name="pointType"]:checked').value;
    const pointValue = document.getElementById('pointValue').value;
    
    let pointDescription = '';
    if (pointType === 'percentage') {
        pointDescription = `현금의 ${pointValue}%`;
    } else if (pointType === 'fixed') {
        pointDescription = `쿠폰당 ${pointValue.toLocaleString()}P`;
    } else if (pointType === 'total') {
        pointDescription = `총 ${pointValue.toLocaleString()}P 균등배분`;
    }
    
    summary.innerHTML = `
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
            <div>
                <strong style="color: #2c5aa0;">👥 대상 직원:</strong><br>
                <span style="font-size: 18px; font-weight: bold;">${employeeCount}명</span>
            </div>
            <div>
                <strong style="color: #e53e3e;">💰 총 현금:</strong><br>
                <span style="font-size: 18px; font-weight: bold;">₩${totalCash.toLocaleString()}</span>
            </div>
            <div>
                <strong style="color: #38a169;">🎯 총 포인트:</strong><br>
                <span style="font-size: 18px; font-weight: bold;">${totalPoints.toLocaleString()}P</span><br>
                <small style="color: #666;">(${pointDescription})</small>
            </div>
            <div>
                <strong style="color: #667eea;">📊 직원당 평균:</strong><br>
                <span style="font-size: 14px;">현금: ₩${(totalCash/employeeCount).toLocaleString()}</span><br>
                <span style="font-size: 14px;">포인트: ${(totalPoints/employeeCount).toLocaleString()}P</span>
            </div>
        </div>
    `;
}

// 만료일 빠른 설정
function setExpireDate(months) {
    const today = new Date();
    const expireDate = new Date(today);
    expireDate.setMonth(expireDate.getMonth() + months);
    
    // YYYY-MM-DD 형식으로 변환
    const formattedDate = expireDate.toISOString().split('T')[0];
    document.getElementById('expireDate').value = formattedDate;
    
    // 배분 재계산 (만료일 변경 시 발행 버튼 활성화 상태 업데이트)
    calculateDistribution();
}

// 사용 가능 요일 문자열 생성 (7자리 0과 1로 구성된 문자열)
// 일월화수목금토 순서로 사용가능하면 1, 불가능하면 0
function getAvailableDaysString() {
    const dayOrder = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
    let binaryString = '';
    
    for (const day of dayOrder) {
        const checkbox = document.querySelector(`.available-day[value="${day}"]`);
        binaryString += checkbox && checkbox.checked ? '1' : '0';
    }
    
    return binaryString;
}

// 쿠폰 일괄 발행
async function issueCoupons() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    if (couponEmployees.length === 0) {
        alert('발행할 직원이 없습니다.');
        return;
    }

    const totalCash = getNumericValue(document.getElementById('totalCashAmount').value);
    const pointType = document.querySelector('input[name="pointType"]:checked').value;
    const pointValue = getNumericValue(document.getElementById('pointValue').value);
    const expireDate = document.getElementById('expireDate').value;
    const availableDays = getAvailableDaysString();

    if (!totalCash || !expireDate) {
        alert('필수 정보를 모두 입력해주세요.');
        return;
    }

    const employeeCount = couponEmployees.length;
    const cashPerEmployee = Math.round((totalCash / employeeCount) * 100) / 100;
    
    let pointPerEmployee = 0;
    if (pointType === 'percentage') {
        pointPerEmployee = Math.round((cashPerEmployee * pointValue / 100) * 100) / 100;
    } else if (pointType === 'fixed') {
        pointPerEmployee = pointValue;
    } else if (pointType === 'total') {
        pointPerEmployee = Math.round((pointValue / employeeCount) * 100) / 100;
    }

    const corporateName = document.getElementById('couponCorporateSelect').options[document.getElementById('couponCorporateSelect').selectedIndex].textContent;
    
    if (!confirm(`${corporateName}의 ${employeeCount}명 직원에게 쿠폰을 발행하시겠습니까?\n\n` +
                `• 직원당 현금: ₩${cashPerEmployee.toLocaleString()}\n` +
                `• 직원당 포인트: ${pointPerEmployee.toLocaleString()}P\n` +
                `• 만료일: ${expireDate}`)) {
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    const issueBtn = document.getElementById('issueCouponsBtn');
    
    issueBtn.disabled = true;
    issueBtn.textContent = '발행 중...';
    
    let successCount = 0;
    let failCount = 0;
    const errors = [];

    try {
        for (let i = 0; i < couponEmployees.length; i++) {
            const employee = couponEmployees[i];
            
            try {
                // 쿠폰 데이터 준비
                // Full coupon code는 서버에서 자동 생성됨:
                // {발급자_사업자등록번호}-{사용가능요일(7자리)}-{coupon_id(10자리)}-{결제유형코드}-{패리티(3자리)}

                // UI에서 선택된 결제 유형 가져오기
                const selectedPaymentType = document.querySelector('input[name="paymentType"]:checked');
                const paymentTypeValue = selectedPaymentType ? selectedPaymentType.value.toUpperCase() : 'PREPAID';

                const couponData = {
                    employeeId: employee.employeeId,
                    cashBalance: cashPerEmployee,
                    pointBalance: pointPerEmployee,
                    expireDate: expireDate,
                    status: 'ACTIVE',
                    paymentType: paymentTypeValue,  // 결제유형: PREPAID(1), POSTPAID(2), CUSTOM(3)
                    availableDays: availableDays  // 7자리 이진 문자열 (일월화수목금토)
                };

                const response = await fetch(`${serverUrl}/api/coupons`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                        'Authorization': `Bearer ${currentToken}`
                    },
                    body: JSON.stringify(couponData)
                });

                const data = await response.json();

                if (data.success) {
                    successCount++;
                } else {
                    failCount++;
                    const displayName = employee.name || employee.phone;
                    errors.push(`${displayName}: ${data.message}`);
                }
            } catch (error) {
                failCount++;
                const displayName = employee.name || employee.phone;
                errors.push(`${displayName}: ${error.message}`);
            }

            // 진행률 업데이트
            issueBtn.textContent = `발행 중... (${i + 1}/${couponEmployees.length})`;
        }

        // 결과 알림
        let message = `쿠폰 발행 완료!\n성공: ${successCount}건, 실패: ${failCount}건`;
        if (errors.length > 0) {
            message += '\n\n실패 내역:\n' + errors.slice(0, 5).join('\n');
            if (errors.length > 5) {
                message += `\n... 외 ${errors.length - 5}건 더`;
            }
        }

        alert(message);

        if (successCount > 0) {
            // 성공 시 폼 리셋
            document.getElementById('totalCashAmount').value = '';
            document.getElementById('pointValue').value = '';
            document.getElementById('employeeDistributionPanel').style.display = 'none';
        }

    } catch (error) {
        alert('쿠폰 발행 오류: ' + error.message);
    } finally {
        issueBtn.disabled = false;
        issueBtn.textContent = '🎫 쿠폰 일괄 발행';
    }
}

