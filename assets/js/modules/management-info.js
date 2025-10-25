/**
 * management-info Module
 * Auto-extracted from index_v2.html
 */

// === 경영정보 관련 함수들 ===

// 경영 통계 로드
async function loadManagementStats() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const startDate = document.getElementById('statsStartDate').value;
    const endDate = document.getElementById('statsEndDate').value;
    
    if (!startDate || !endDate) {
        alert('시작일과 종료일을 모두 선택해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        console.log(`[MANAGEMENT-STATS] API 호출: GET ${serverUrl}/api/statistics/corporate?startDate=${startDate}&endDate=${endDate}`);
        
        // 거래처별 통계 조회
        const response = await fetch(`${serverUrl}/api/statistics/corporate?startDate=${startDate}&endDate=${endDate}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        // 먼저 응답 텍스트를 확인
        const responseText = await response.text();
        console.log('[MANAGEMENT-STATS] API Response:', responseText);
        console.log('[MANAGEMENT-STATS] Response status:', response.status);
        
        let data;
        try {
            data = JSON.parse(responseText);
        } catch (parseError) {
            console.error('[MANAGEMENT-STATS] JSON Parse Error:', parseError);
            console.error('[MANAGEMENT-STATS] Response was:', responseText);
            alert('서버 응답을 파싱할 수 없습니다: ' + responseText.substring(0, 200));
            return;
        }

        if (data.success) {
            displayManagementStats(data.data);
        } else {
            alert('통계 조회 실패: ' + data.message);
        }
    } catch (error) {
        console.error('[MANAGEMENT-STATS] 네트워크 오류:', error);
        alert('통계 조회 중 오류가 발생했습니다: ' + error.message);
    }
}

// 경영 통계 표시
function displayManagementStats(statsData) {
    console.log('[MANAGEMENT-STATS] 통계 데이터 표시:', statsData);
    
    // 전체 요약 통계 업데이트
    document.getElementById('totalCorporates').textContent = statsData.summary?.totalCorporates || 0;
    document.getElementById('totalIssuedCoupons').textContent = statsData.summary?.totalIssuedCoupons || 0;
    document.getElementById('totalUsedCoupons').textContent = statsData.summary?.totalUsedCoupons || 0;
    
    const usageRate = (statsData.summary?.totalIssuedCoupons || 0) > 0 
        ? (((statsData.summary?.totalUsedCoupons || 0) / (statsData.summary?.totalIssuedCoupons || 1)) * 100).toFixed(1) + '%'
        : '0%';
    document.getElementById('totalUsageRate').textContent = usageRate;

    // 거래처별 상세 통계 테이블 업데이트
    const tableBody = document.getElementById('corporateStatsBody');
    tableBody.innerHTML = '';

    if (statsData.corporateStats && statsData.corporateStats.length > 0) {
        statsData.corporateStats.forEach(stat => {
            const row = document.createElement('tr');
            const corporateUsageRate = stat.issuedCoupons > 0 
                ? ((stat.usedCoupons / stat.issuedCoupons) * 100).toFixed(1) + '%'
                : '0%';

            row.innerHTML = `
                <td style="border: 1px solid #e0e0e0; padding: 8px;">${stat.corporateName}</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${stat.employeeCount || 0}</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${stat.issuedCoupons || 0}</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${stat.usedCoupons || 0}</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${(stat.issuedCoupons || 0) - (stat.usedCoupons || 0)}</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${corporateUsageRate}</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${(stat.totalCashValue || 0).toLocaleString()}원</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${(stat.totalPointValue || 0).toLocaleString()}P</td>
                <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">
                    <button class="btn" style="background: #667eea; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px;" 
                            onclick="showCorporateDetail(${stat.corporateId}, '${stat.corporateName}')">
                        상세보기
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    } else {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="9" style="text-align: center; padding: 20px; border: 1px solid #e0e0e0;">조회된 데이터가 없습니다.</td>';
        tableBody.appendChild(row);
    }
}

// 거래처 상세 통계 표시
async function showCorporateDetail(corporateId, corporateName) {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const startDate = document.getElementById('statsStartDate').value;
    const endDate = document.getElementById('statsEndDate').value;
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        console.log(`[CORPORATE-DETAIL] API 호출: GET ${serverUrl}/api/statistics/corporate/${corporateId}?startDate=${startDate}&endDate=${endDate}`);
        
        const response = await fetch(`${serverUrl}/api/statistics/corporate/${corporateId}?startDate=${startDate}&endDate=${endDate}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const responseText = await response.text();
        console.log('[CORPORATE-DETAIL] API Response:', responseText);
        
        let data = JSON.parse(responseText);

        if (data.success) {
            document.getElementById('detailModalTitle').textContent = `${corporateName} 상세 통계`;
            
            const modalContent = document.getElementById('detailModalContent');
            modalContent.innerHTML = `
                <div class="detail-stats">
                    <h4>📊 직원별 쿠폰 사용 현황</h4>
                    <div style="max-height: 400px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                        <table style="width: 100%; border-collapse: collapse;">
                            <thead style="background: #f8f9fa; position: sticky; top: 0;">
                                <tr>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: left;">직원명</th>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">발행 쿠폰</th>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">사용 쿠폰</th>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">미사용 쿠폰</th>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">사용률</th>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">현금 잔고</th>
                                    <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">포인트 잔고</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${(data.data.employees || []).map(emp => {
                                    const empUsageRate = emp.issuedCoupons > 0 
                                        ? ((emp.usedCoupons / emp.issuedCoupons) * 100).toFixed(1) + '%'
                                        : '0%';
                                    return `
                                        <tr>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px;">${emp.employeeName || 'N/A'}</td>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${emp.issuedCoupons || 0}</td>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${emp.usedCoupons || 0}</td>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${(emp.issuedCoupons || 0) - (emp.usedCoupons || 0)}</td>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${empUsageRate}</td>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${(emp.totalCashBalance || 0).toLocaleString()}원</td>
                                            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${(emp.totalPointBalance || 0).toLocaleString()}P</td>
                                        </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
            
            document.getElementById('corporateDetailModal').style.display = 'block';
        } else {
            alert('상세 통계 조회 실패: ' + data.message);
        }
    } catch (error) {
        console.error('[CORPORATE-DETAIL] 오류:', error);
        alert('상세 통계 조회 중 오류가 발생했습니다: ' + error.message);
    }
}

// 상세 모달 닫기
function closeCorporateDetailModal() {
    document.getElementById('corporateDetailModal').style.display = 'none';
}

// 엑셀 다운로드
function exportStatsToExcel() {
    const table = document.getElementById('corporateStatsTable');
    const rows = Array.from(table.querySelectorAll('tr'));

    let csvContent = '';
    rows.forEach(row => {
        const cols = Array.from(row.querySelectorAll('th, td'));
        const rowData = cols.slice(0, -1).map(col => col.textContent.trim()); // 상세보기 버튼 열 제외
        csvContent += rowData.join(',') + '\n';
    });

    // BOM 추가하여 한글 깨짐 방지
    const bom = '\uFEFF';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');

    const startDate = document.getElementById('statsStartDate').value;
    const endDate = document.getElementById('statsEndDate').value;
    const fileName = `쿠폰통계_${startDate}_${endDate}.csv`;

    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();

    console.log('[EXCEL-EXPORT] 엑셀 파일 다운로드:', fileName);
}

// === 월간정산보고서 관련 함수들 ===

// 월간정산보고서 탭 초기화 (거래처 목록 + 현재 월 설정)
function loadCorporatesForMonthlyReport() {
    loadMonthlyReportCorporates();
    setCurrentMonth();
}

// 현재 월을 기본값으로 설정
function setCurrentMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');

    document.getElementById('monthlyReportYear').value = year;
    document.getElementById('monthlyReportMonth').value = month;

    console.log(`[MONTHLY-REPORT] 현재 월 설정: ${year}년 ${month}월`);
}

// 월간정산보고서 거래처 목록 로드
async function loadMonthlyReportCorporates() {
    if (!currentToken) {
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    const selectElement = document.getElementById('monthlyReportCorporate');

    try {
        console.log('[MONTHLY-REPORT] 거래처 목록 로드');

        const response = await fetch(`${serverUrl}/api/corporates`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();

        if (data.success && data.data) {
            // 기존 옵션 유지하고 거래처 추가
            selectElement.innerHTML = '<option value="">전체</option>';

            data.data.forEach(corporate => {
                const option = document.createElement('option');
                option.value = corporate.customerId || corporate.id;
                option.textContent = corporate.name;
                selectElement.appendChild(option);
            });

            console.log('[MONTHLY-REPORT] 거래처 목록 로드 완료:', data.data.length);
        }
    } catch (error) {
        console.error('[MONTHLY-REPORT] 거래처 목록 로드 실패:', error);
    }
}

// 월간정산보고서 조회
async function loadMonthlyReport() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const year = document.getElementById('monthlyReportYear').value;
    const month = document.getElementById('monthlyReportMonth').value;
    const corporateId = document.getElementById('monthlyReportCorporate').value;
    const serverUrl = document.getElementById('serverUrl').value;

    console.log(`[MONTHLY-REPORT] 월간보고서 조회: ${year}년 ${month}월, 거래처 ID: ${corporateId || '전체'}`);

    try {
        // API 호출 - 해당 월의 거래 내역 조회
        let apiUrl = `${serverUrl}/api/transactions/monthly?year=${year}&month=${month}`;
        if (corporateId) {
            apiUrl += `&corporateId=${corporateId}`;
        }

        console.log(`[MONTHLY-REPORT] API 호출: GET ${apiUrl}`);

        const response = await fetch(apiUrl, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json'
            }
        });

        const responseText = await response.text();
        console.log('[MONTHLY-REPORT] API Response:', responseText);

        let data;
        try {
            data = JSON.parse(responseText);
        } catch (parseError) {
            console.error('[MONTHLY-REPORT] JSON Parse Error:', parseError);
            alert('서버 응답을 파싱할 수 없습니다.');
            return;
        }

        if (data.success) {
            displayMonthlyReport(data.data, year, month);
        } else {
            alert('월간보고서 조회 실패: ' + data.message);
        }
    } catch (error) {
        console.error('[MONTHLY-REPORT] 오류:', error);
        alert('월간보고서 조회 중 오류가 발생했습니다: ' + error.message);
    }
}

// 월간정산보고서 표시
function displayMonthlyReport(reportData, year, month) {
    console.log('[MONTHLY-REPORT] 보고서 데이터 표시:', reportData);

    // 월간 요약 정보 업데이트
    document.getElementById('monthlyTotalUsage').textContent = (reportData.summary?.totalUsageCount || 0).toLocaleString();
    document.getElementById('monthlyTotalCash').textContent = (reportData.summary?.totalCashDeducted || 0).toLocaleString() + '원';
    document.getElementById('monthlyTotalPoints').textContent = (reportData.summary?.totalPointsDeducted || 0).toLocaleString() + 'P';
    document.getElementById('monthlyCashBalance').textContent = (reportData.summary?.remainingCashBalance || 0).toLocaleString() + '원';
    document.getElementById('monthlyPointBalance').textContent = (reportData.summary?.remainingPointBalance || 0).toLocaleString() + 'P';

    // 직원별 상세 내역 테이블 업데이트
    const tableBody = document.getElementById('monthlyReportBody');
    tableBody.innerHTML = '';

    if (reportData.employeeDetails && reportData.employeeDetails.length > 0) {
        reportData.employeeDetails.forEach(employee => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: left;">${employee.corporateName || 'N/A'}</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: left;">${employee.employeeName || 'N/A'}</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">${(employee.usageCount || 0).toLocaleString()}</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: right;">${(employee.cashDeducted || 0).toLocaleString()}원</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: right;">${(employee.pointsDeducted || 0).toLocaleString()}P</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: right;">${(employee.cashBalance || 0).toLocaleString()}원</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: right;">${(employee.pointBalance || 0).toLocaleString()}P</td>
                <td style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">
                    <button class="btn" style="background: #667eea; color: white; padding: 6px 12px; border-radius: 4px; font-size: 13px;"
                            onclick="showEmployeeUsageDetail(${employee.employeeId}, '${employee.employeeName}', '${year}', '${month}')">
                        📅 일자별 보기
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    } else {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="8" style="text-align: center; padding: 20px; border: 1px solid #e0e0e0;">조회된 데이터가 없습니다.</td>';
        tableBody.appendChild(row);
    }
}

// 직원별 일자별 사용 내역 상세 보기
async function showEmployeeUsageDetail(employeeId, employeeName, year, month) {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;

    console.log(`[EMPLOYEE-DAILY-USAGE] 직원별 일자별 조회: ${employeeName} (${year}년 ${month}월)`);

    try {
        // API 호출 - 직원의 일자별 사용 내역 조회
        const apiUrl = `${serverUrl}/api/transactions/employee/${employeeId}/daily?year=${year}&month=${month}`;

        console.log(`[EMPLOYEE-DAILY-USAGE] API 호출: GET ${apiUrl}`);

        const response = await fetch(apiUrl, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json'
            }
        });

        const responseText = await response.text();
        console.log('[EMPLOYEE-DAILY-USAGE] API Response:', responseText);

        let data;
        try {
            data = JSON.parse(responseText);
        } catch (parseError) {
            console.error('[EMPLOYEE-DAILY-USAGE] JSON Parse Error:', parseError);
            alert('서버 응답을 파싱할 수 없습니다.');
            return;
        }

        if (data.success) {
            displayEmployeeUsageDetail(data.data, employeeName, year, month);
        } else {
            alert('일자별 사용 내역 조회 실패: ' + data.message);
        }
    } catch (error) {
        console.error('[EMPLOYEE-DAILY-USAGE] 오류:', error);
        alert('일자별 사용 내역 조회 중 오류가 발생했습니다: ' + error.message);
    }
}

// 직원별 일자별 사용 내역 표시
function displayEmployeeUsageDetail(dailyData, employeeName, year, month) {
    console.log('[EMPLOYEE-DAILY-USAGE] 일자별 데이터 표시:', dailyData);

    // 모달 제목 설정
    document.getElementById('employeeUsageModalTitle').textContent = `${employeeName} - ${year}년 ${month}월 일자별 식권 사용 내역`;

    // 일자별 테이블 생성
    const modalContent = document.getElementById('employeeUsageModalContent');

    let tableHTML = `
        <div style="margin-bottom: 20px;">
            <h4 style="margin-bottom: 15px;">📅 일자별 사용 회수</h4>
            <div style="max-height: 500px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                <table style="width: 100%; border-collapse: collapse;">
                    <thead style="background: #f8f9fa; position: sticky; top: 0;">
                        <tr>
                            <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">일자</th>
                            <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">요일</th>
                            <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">사용 횟수</th>
                            <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: right;">차감 현금</th>
                            <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: right;">차감 포인트</th>
                            <th style="border: 1px solid #e0e0e0; padding: 12px; text-align: center;">상세 시간</th>
                        </tr>
                    </thead>
                    <tbody>`;

    if (dailyData.dailyUsage && dailyData.dailyUsage.length > 0) {
        dailyData.dailyUsage.forEach(day => {
            const date = new Date(`${year}-${month}-${day.day}`);
            const dayOfWeek = ['일', '월', '화', '수', '목', '금', '토'][date.getDay()];

            // 사용 시간 목록 생성
            const timesList = (day.usageTimes || [])
                .map(time => `<span style="display: inline-block; background: #e6f7ff; padding: 3px 8px; border-radius: 4px; margin: 2px; font-size: 12px;">${time}</span>`)
                .join(' ');

            tableHTML += `
                <tr>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: center;">${month}월 ${day.day}일</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: center;">${dayOfWeek}</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: center; font-weight: bold; color: #1890ff;">${day.usageCount || 0}회</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${(day.cashDeducted || 0).toLocaleString()}원</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${(day.pointsDeducted || 0).toLocaleString()}P</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: left;">
                        ${timesList || '-'}
                    </td>
                </tr>
            `;
        });

        // 합계 행 추가
        const totalUsage = dailyData.dailyUsage.reduce((sum, day) => sum + (day.usageCount || 0), 0);
        const totalCash = dailyData.dailyUsage.reduce((sum, day) => sum + (day.cashDeducted || 0), 0);
        const totalPoints = dailyData.dailyUsage.reduce((sum, day) => sum + (day.pointsDeducted || 0), 0);

        tableHTML += `
                <tr style="background: #f0f0f0; font-weight: bold;">
                    <td colspan="2" style="border: 1px solid #e0e0e0; padding: 10px; text-align: center;">합계</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: center; color: #1890ff;">${totalUsage}회</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${totalCash.toLocaleString()}원</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${totalPoints.toLocaleString()}P</td>
                    <td style="border: 1px solid #e0e0e0; padding: 10px;"></td>
                </tr>
        `;
    } else {
        tableHTML += `
                <tr>
                    <td colspan="6" style="border: 1px solid #e0e0e0; padding: 20px; text-align: center;">
                        해당 기간에 사용 내역이 없습니다.
                    </td>
                </tr>
        `;
    }

    tableHTML += `
                    </tbody>
                </table>
            </div>
        </div>
    `;

    modalContent.innerHTML = tableHTML;

    // 모달 표시
    document.getElementById('employeeUsageModal').style.display = 'block';
}

// 직원 사용 내역 모달 닫기
function closeEmployeeUsageModal() {
    document.getElementById('employeeUsageModal').style.display = 'none';
}

// 월간보고서 엑셀 다운로드
function exportMonthlyReportToExcel() {
    const year = document.getElementById('monthlyReportYear').value;
    const month = document.getElementById('monthlyReportMonth').value;
    const table = document.getElementById('monthlyReportTable');
    const rows = Array.from(table.querySelectorAll('tr'));

    let csvContent = '';
    rows.forEach(row => {
        const cols = Array.from(row.querySelectorAll('th, td'));
        const rowData = cols.slice(0, -1).map(col => col.textContent.trim()); // 상세보기 버튼 열 제외
        csvContent += rowData.join(',') + '\n';
    });

    // BOM 추가하여 한글 깨짐 방지
    const bom = '\uFEFF';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');

    const fileName = `월간정산보고서_${year}년${month}월.csv`;

    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();

    console.log('[EXCEL-EXPORT] 월간보고서 엑셀 다운로드:', fileName);
}

