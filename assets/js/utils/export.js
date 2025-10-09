/**
 * Export utility functions
 * Handles exporting data to CSV/Excel formats
 */

/**
 * Export coupon data to CSV
 */
function exportCouponData() {
    if (currentCoupons.length === 0) {
        alert('내보낼 데이터가 없습니다.');
        return;
    }

    // CSV 헤더
    const headers = ['쿠폰코드', '거래처', '직원명', '전화번호', '현금잔고', '포인트잔고', '상태', '만료일', '생성일'];

    // CSV 데이터
    const csvData = currentCoupons.map(coupon => [
        coupon.fullCouponCode || '',
        coupon.corporateName || '',
        coupon.recipientName || '',
        coupon.recipientPhone || '',
        coupon.cashBalance || 0,
        coupon.pointBalance || 0,
        getStatusText(coupon.status),
        coupon.expireDate || '',
        coupon.createdAt ? coupon.createdAt.split(' ')[0] : ''
    ]);

    // CSV 문자열 생성
    const csvContent = [headers, ...csvData]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n');

    // BOM 추가 (Excel에서 한글 깨짐 방지)
    const bom = '\uFEFF';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });

    // 다운로드
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `쿠폰목록_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/**
 * Export statistics to Excel/CSV
 */
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

/**
 * Export monthly report to Excel/CSV
 */
async function exportMonthlyReportToExcel() {
    const year = document.getElementById('monthlyReportYear').value;
    const month = document.getElementById('monthlyReportMonth').value;
    const corporateSelect = document.getElementById('monthlyReportCorporate');
    const corporateName = corporateSelect.options[corporateSelect.selectedIndex].text;

    // 현재 표시된 데이터 가져오기
    const tableBody = document.getElementById('monthlyReportBody');
    const rows = tableBody.querySelectorAll('tr');

    if (rows.length === 0 || rows[0].cells.length === 1) {
        alert('먼저 월간보고서를 조회해주세요.');
        return;
    }

    // CSV 헤더
    const headers = ['거래처명', '직원명', '사용횟수', '차감현금', '차감포인트', '현금잔액', '포인트잔액'];

    // CSV 데이터
    const csvData = [];
    rows.forEach(row => {
        if (row.cells.length > 1) { // 메시지 행이 아닌 데이터 행만
            const rowData = [];
            for (let i = 0; i < 7; i++) { // 상세보기 버튼 제외
                rowData.push(row.cells[i].textContent.trim());
            }
            csvData.push(rowData);
        }
    });

    // CSV 문자열 생성
    const csvContent = [headers, ...csvData]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n');

    // BOM 추가 (Excel에서 한글 깨짐 방지)
    const bom = '\uFEFF';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });

    // 파일명 생성
    const corporateId = document.getElementById('monthlyReportCorporate').value;
    const filename = `월간정산보고서_${year}년${month}월_${corporateId ? corporateName : '전체'}_${new Date().toISOString().split('T')[0]}.csv`;

    // 다운로드
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/**
 * Export system logs to JSON
 */
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
