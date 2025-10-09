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

