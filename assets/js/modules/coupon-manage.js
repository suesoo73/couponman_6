/**
 * coupon-manage Module
 * Auto-extracted from index_v2.html
 */

// ========== 쿠폰 관리 관련 함수들 ==========

// 전체 쿠폰 목록 로드
async function loadAllCoupons() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        console.log('Loading coupons from:', `${serverUrl}/api/coupons`);
        console.log('Using token:', currentToken ? 'Present' : 'Missing');
        
        const response = await fetch(`${serverUrl}/api/coupons`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('Response data:', data);

        if (data.success) {
            currentCoupons = data.data || [];
            console.log('Loaded', currentCoupons.length, 'coupons');
            await loadCorporatesForCouponSearch();
            updateCouponList(currentCoupons);
            updateCouponStats(currentCoupons);
        } else {
            console.error('Server returned error:', data);
            alert('쿠폰 목록 로드 실패: ' + (data.message || data.error || 'Unknown error'));
        }
    } catch (error) {
        console.error('쿠폰 목록 로드 오류:', error);
        console.error('Error details:', error.stack);
        alert('쿠폰 목록 로드 오류: ' + error.message);
    }
}

// 쿠폰 검색용 거래처 목록 로드
async function loadCorporatesForCouponSearch() {
    if (!currentToken) return;

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
            const select = document.getElementById('couponSearchCorporate');
            select.innerHTML = '<option value="">전체 거래처</option>';
            
            data.data.forEach(corporate => {
                const option = document.createElement('option');
                option.value = corporate.customerId;
                option.textContent = corporate.name;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('거래처 목록 로드 오류:', error);
    }
}

// 쿠폰 검색
async function searchCoupons() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const corporateId = document.getElementById('couponSearchCorporate').value;
    const status = document.getElementById('couponSearchStatus').value;
    const couponCode = document.getElementById('couponSearchCode').value;
    const employeeInfo = document.getElementById('couponSearchEmployee').value;

    let filteredCoupons = [...currentCoupons];

    // 거래처 필터
    if (corporateId) {
        filteredCoupons = filteredCoupons.filter(coupon => 
            coupon.corporateId == corporateId
        );
    }

    // 상태 필터
    if (status) {
        filteredCoupons = filteredCoupons.filter(coupon => 
            coupon.status === status
        );
    }

    // 쿠폰 코드 필터
    if (couponCode) {
        filteredCoupons = filteredCoupons.filter(coupon => 
            coupon.fullCouponCode && coupon.fullCouponCode.toLowerCase().includes(couponCode.toLowerCase())
        );
    }

    // 직원 정보 필터
    if (employeeInfo) {
        filteredCoupons = filteredCoupons.filter(coupon => 
            (coupon.recipientName && coupon.recipientName.includes(employeeInfo)) ||
            (coupon.recipientPhone && coupon.recipientPhone.includes(employeeInfo))
        );
    }

    updateCouponList(filteredCoupons);
    updateCouponStats(filteredCoupons);
}

// 쿠폰 목록 새로고침
function refreshCouponList() {
    loadAllCoupons();
}

// 쿠폰 목록 UI 업데이트
function updateCouponList(coupons) {
    const tbody = document.getElementById('couponListBody');
    tbody.innerHTML = '';

    if (coupons.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="11" style="text-align: center; padding: 40px; color: #666;">
                    검색 조건에 맞는 쿠폰이 없습니다.
                </td>
            </tr>
        `;
        return;
    }

    coupons.forEach(coupon => {
        const row = document.createElement('tr');
        
        const statusColor = getStatusColor(coupon.status);
        const statusText = getStatusText(coupon.status);
        
        row.innerHTML = `
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">
                <input type="checkbox" class="coupon-checkbox" data-coupon-id="${coupon.couponId}" onchange="updateSelectedCoupons()">
            </td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; font-family: monospace; font-size: 12px;">${coupon.fullCouponCode || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.corporateName || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.recipientName || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${coupon.recipientPhone || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: right; font-weight: bold; color: #e53e3e;">${(coupon.cashBalance || 0).toLocaleString()}원</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: right; font-weight: bold; color: #38a169;">${(coupon.pointBalance || 0).toLocaleString()}P</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">
                <span style="background: ${statusColor}; color: white; padding: 4px 8px; border-radius: 4px; font-size: 11px;">
                    ${statusText}
                </span>
            </td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">${coupon.expireDate || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center; font-size: 12px;">${coupon.createdAt ? coupon.createdAt.split(' ')[0] : 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">
                <button onclick="editCoupon(${coupon.couponId})" class="btn" style="padding: 4px 8px; font-size: 11px; background: #4299e1; color: white; margin-right: 5px;">✏️ 수정</button>
                <button onclick="deleteCoupon(${coupon.couponId})" class="btn" style="padding: 4px 8px; font-size: 11px; background: #e53e3e; color: white;">🗑️ 삭제</button>
            </td>
        `;
        
        tbody.appendChild(row);
    });
}

// 쿠폰 상태 색상 반환
function getStatusColor(status) {
    switch (status) {
        case 'ACTIVE': return '#48bb78';
        case 'USED': return '#ed8936';
        case 'EXPIRED': return '#e53e3e';
        case 'SUSPENDED': return '#718096';
        case 'TERMINATED': return '#e53e3e';
        default: return '#718096';
    }
}

// 쿠폰 상태 텍스트 반환
function getStatusText(status) {
    switch (status) {
        case 'ACTIVE': return '사용 가능';
        case 'USED': return '사용됨';
        case 'EXPIRED': return '만료됨';
        case 'SUSPENDED': return '일시 중지';
        case 'TERMINATED': return '해지됨';
        default: return status;
    }
}

// 쿠폰 통계 업데이트
function updateCouponStats(coupons) {
    const total = coupons.length;
    const active = coupons.filter(c => c.status === 'ACTIVE').length;
    const used = coupons.filter(c => c.status === 'USED').length;
    const expired = coupons.filter(c => c.status === 'EXPIRED').length;

    document.getElementById('totalCoupons').textContent = total;
    document.getElementById('activeCoupons').textContent = active;
    document.getElementById('usedCoupons').textContent = used;
    document.getElementById('expiredCoupons').textContent = expired;
}

// 전체 선택 토글
function toggleSelectAllCoupons() {
    const selectAll = document.getElementById('selectAllCoupons');
    const checkboxes = document.querySelectorAll('.coupon-checkbox');
    
    checkboxes.forEach(checkbox => {
        checkbox.checked = selectAll.checked;
    });
    
    updateSelectedCoupons();
}

// 선택된 쿠폰 업데이트
function updateSelectedCoupons() {
    const checkboxes = document.querySelectorAll('.coupon-checkbox:checked');
    selectedCouponIds = Array.from(checkboxes).map(cb => parseInt(cb.dataset.couponId));
    
    const selectAll = document.getElementById('selectAllCoupons');
    const totalCheckboxes = document.querySelectorAll('.coupon-checkbox').length;
    
    if (selectedCouponIds.length === 0) {
        selectAll.indeterminate = false;
        selectAll.checked = false;
    } else if (selectedCouponIds.length === totalCheckboxes) {
        selectAll.indeterminate = false;
        selectAll.checked = true;
    } else {
        selectAll.indeterminate = true;
        selectAll.checked = false;
    }
}

// 쿠폰 수정
function editCoupon(couponId) {
    const coupon = currentCoupons.find(c => c.couponId === couponId);
    if (!coupon) return;

    // 모달에 데이터 채우기
    document.getElementById('editCouponId').value = coupon.couponId;
    document.getElementById('editCouponCode').value = coupon.fullCouponCode || '';
    document.getElementById('editEmployeeName').value = coupon.recipientName || '';
    document.getElementById('editCashBalance').value = coupon.cashBalance || 0;
    document.getElementById('editPointBalance').value = coupon.pointBalance || 0;
    document.getElementById('editCouponStatus').value = coupon.status || 'ACTIVE';
    document.getElementById('editExpireDate').value = coupon.expireDate || '';

    // 사용 가능 요일 설정
    const availableDays = coupon.availableDays || '1111111';
    const dayValues = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
    dayValues.forEach((day, index) => {
        const checkbox = document.querySelector(`.edit-available-day[value="${day}"]`);
        if (checkbox) {
            checkbox.checked = availableDays[index] === '1';
        }
    });

    // 모달 표시
    document.getElementById('couponEditModal').style.display = 'block';
}

// 쿠폰 수정 모달 닫기
function closeCouponEditModal() {
    document.getElementById('couponEditModal').style.display = 'none';
}

// 쿠폰 수정 제출
async function submitCouponEdit(event) {
    event.preventDefault();
    
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const couponId = document.getElementById('editCouponId').value;
    const serverUrl = document.getElementById('serverUrl').value;

    // 사용 가능 요일 수집
    const dayCheckboxes = document.querySelectorAll('.edit-available-day');
    let availableDays = '';
    dayCheckboxes.forEach(checkbox => {
        availableDays += checkbox.checked ? '1' : '0';
    });

    const updateData = {
        cashBalance: parseFloat(document.getElementById('editCashBalance').value) || 0,
        pointBalance: parseFloat(document.getElementById('editPointBalance').value) || 0,
        status: document.getElementById('editCouponStatus').value,
        expireDate: document.getElementById('editExpireDate').value,
        availableDays: availableDays
    };

    try {
        const response = await fetch(`${serverUrl}/api/coupons/${couponId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(updateData)
        });

        const data = await response.json();

        if (data.success) {
            alert('쿠폰이 성공적으로 수정되었습니다.');
            closeCouponEditModal();
            refreshCouponList();
        } else {
            alert('쿠폰 수정 실패: ' + data.message);
        }
    } catch (error) {
        console.error('쿠폰 수정 오류:', error);
        alert('쿠폰 수정 오류: ' + error.message);
    }
}

// 쿠폰 삭제
function deleteCoupon(couponId) {
    console.log('[DELETE-COUPON] ===== 삭제 프로세스 시작 =====');
    console.log('[DELETE-COUPON] 삭제하려는 쿠폰 ID:', couponId);
    console.log('[DELETE-COUPON] 현재 쿠폰 목록 개수:', currentCoupons ? currentCoupons.length : 'undefined');
    console.log('[DELETE-COUPON] currentCoupons 배열:', currentCoupons);
    
    const coupon = currentCoupons.find(c => c.couponId === couponId || c.id === couponId);
    console.log('[DELETE-COUPON] 찾은 쿠폰:', coupon);
    
    if (!coupon) {
        console.error('[DELETE-COUPON] ❌ 쿠폰을 찾을 수 없습니다!');
        console.log('[DELETE-COUPON] 쿠폰 검색 조건: couponId === ' + couponId + ' || id === ' + couponId);
        alert('삭제할 쿠폰을 찾을 수 없습니다.');
        return;
    }

    console.log('[DELETE-COUPON] ✅ 쿠폰을 찾았습니다. 모달에 정보 표시 시작');
    
    // 삭제 확인 모달에 정보 표시
    document.getElementById('deleteCouponCode').textContent = coupon.fullCouponCode || coupon.code || 'N/A';
    document.getElementById('deleteEmployeeName').textContent = coupon.recipientName || 'N/A';
    document.getElementById('deleteCashBalance').textContent = (coupon.cashBalance || 0).toLocaleString();
    document.getElementById('deletePointBalance').textContent = (coupon.pointBalance || 0).toLocaleString();

    // 전역 변수에 삭제할 쿠폰 ID 저장
    window.deletingCouponId = coupon.couponId || coupon.id;
    console.log('[DELETE-COUPON] window.deletingCouponId 설정됨:', window.deletingCouponId);

    // 모달 표시
    document.getElementById('couponDeleteModal').style.display = 'block';
    console.log('[DELETE-COUPON] 모달이 표시되었습니다');
}

// 쿠폰 삭제 모달 닫기
function closeCouponDeleteModal() {
    document.getElementById('couponDeleteModal').style.display = 'none';
    window.deletingCouponId = null;
}

// 쿠폰 삭제 확인
async function confirmCouponDelete() {
    console.log('[CONFIRM-DELETE] ===== 삭제 확인 시작 =====');
    console.log('[CONFIRM-DELETE] currentToken:', currentToken ? 'Present' : 'Missing');
    console.log('[CONFIRM-DELETE] window.deletingCouponId:', window.deletingCouponId);
    
    if (!currentToken || !window.deletingCouponId) {
        console.error('[CONFIRM-DELETE] ❌ 삭제 조건 미충족');
        console.log('[CONFIRM-DELETE] currentToken 상태:', currentToken);
        console.log('[CONFIRM-DELETE] window.deletingCouponId 상태:', window.deletingCouponId);
        alert('삭제할 쿠폰 정보가 없습니다.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    const couponId = window.deletingCouponId;
    const deleteUrl = `${serverUrl}/api/coupons/${couponId}`;
    
    console.log('[CONFIRM-DELETE] 서버 URL:', serverUrl);
    console.log('[CONFIRM-DELETE] 쿠폰 ID:', couponId);
    console.log('[CONFIRM-DELETE] 삭제 API URL:', deleteUrl);

    try {
        console.log('[CONFIRM-DELETE] API 호출 시작...');
        const response = await fetch(deleteUrl, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        console.log('[CONFIRM-DELETE] API 응답 상태:', response.status);
        console.log('[CONFIRM-DELETE] API 응답 OK:', response.ok);
        console.log('[CONFIRM-DELETE] API 응답 헤더:', response.headers);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('[CONFIRM-DELETE] API 응답 데이터:', data);

        if (data.success) {
            console.log('[CONFIRM-DELETE] ✅ 삭제 성공!');
            alert('쿠폰이 성공적으로 삭제되었습니다.');
            closeCouponDeleteModal();
            refreshCouponList();
        } else {
            console.error('[CONFIRM-DELETE] ❌ 삭제 실패:', data.message);
            alert('쿠폰 삭제 실패: ' + data.message);
        }
    } catch (error) {
        console.error('[CONFIRM-DELETE] ❌ 오류 발생:', error);
        console.error('[CONFIRM-DELETE] 오류 스택:', error.stack);
        alert('쿠폰 삭제 오류: ' + error.message);
    }
    
    console.log('[CONFIRM-DELETE] ===== 삭제 확인 종료 =====');
}

// 일괄 상태 변경
function bulkStatusUpdate() {
    if (selectedCouponIds.length === 0) {
        alert('변경할 쿠폰을 선택해주세요.');
        return;
    }

    document.getElementById('selectedCouponCount').textContent = selectedCouponIds.length;
    document.getElementById('bulkStatusModal').style.display = 'block';
}

// 일괄 상태 변경 모달 닫기
function closeBulkStatusModal() {
    document.getElementById('bulkStatusModal').style.display = 'none';
}

// 일괄 상태 변경 확인
async function confirmBulkStatusUpdate() {
    if (!currentToken || selectedCouponIds.length === 0) {
        alert('변경할 쿠폰이 선택되지 않았습니다.');
        return;
    }

    const newStatus = document.getElementById('bulkNewStatus').value;
    const serverUrl = document.getElementById('serverUrl').value;

    let successCount = 0;
    let failCount = 0;

    try {
        for (const couponId of selectedCouponIds) {
            const response = await fetch(`${serverUrl}/api/coupons/${couponId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${currentToken}`
                },
                body: JSON.stringify({ status: newStatus })
            });

            const data = await response.json();
            if (data.success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        alert(`일괄 상태 변경 완료!\n성공: ${successCount}개, 실패: ${failCount}개`);
        closeBulkStatusModal();
        refreshCouponList();

    } catch (error) {
        console.error('일괄 상태 변경 오류:', error);
        alert('일괄 상태 변경 오류: ' + error.message);
    }
}

// 일괄 삭제
function bulkDeleteCoupons() {
    if (selectedCouponIds.length === 0) {
        alert('삭제할 쿠폰을 선택해주세요.');
        return;
    }

    document.getElementById('bulkDeleteCouponCount').textContent = selectedCouponIds.length;
    document.getElementById('bulkDeleteModal').style.display = 'block';
}

// 일괄 삭제 모달 닫기
function closeBulkDeleteModal() {
    document.getElementById('bulkDeleteModal').style.display = 'none';
}

// 일괄 삭제 확인
async function confirmBulkDelete() {
    if (!currentToken || selectedCouponIds.length === 0) {
        alert('삭제할 쿠폰이 선택되지 않았습니다.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    let successCount = 0;
    let failCount = 0;

    try {
        for (const couponId of selectedCouponIds) {
            try {
                const response = await fetch(`${serverUrl}/api/coupons/${couponId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${currentToken}`,
                        'Content-Type': 'application/json'
                    }
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } else {
                    failCount++;
                }
            } catch (error) {
                failCount++;
                console.error(`쿠폰 ${couponId} 삭제 오류:`, error);
            }
        }

        closeBulkDeleteModal();
        alert(`일괄 삭제 완료\n성공: ${successCount}개\n실패: ${failCount}개`);
        
        // 선택된 쿠폰 ID 초기화
        selectedCouponIds = [];
        document.getElementById('selectAllCoupons').checked = false;
        
        // 쿠폰 목록 새로고침
        refreshCouponList();

    } catch (error) {
        closeBulkDeleteModal();
        alert('일괄 삭제 오류: ' + error.message);
    }
}

// 데이터 내보내기
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

// 월간정산보고서 관련 함수들

// 거래처 목록을 월간보고서용 셀렉트박스에 로드
async function loadCorporatesForMonthlyReport() {
    if (!currentToken) {
        console.log('토큰이 없습니다.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    if (!serverUrl) {
        console.log('서버 URL이 없습니다.');
        return;
    }
    
    try {
        const response = await fetch(`${serverUrl}/api/corporates`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('monthlyReportCorporate');
            
            // 기존 옵션 제거 (첫 번째 "전체" 옵션은 유지)
            select.innerHTML = '<option value="">전체</option>';
            
            // 거래처 옵션 추가
            data.data.forEach(corporate => {
                const option = document.createElement('option');
                option.value = corporate.customerId;
                option.textContent = corporate.name;
                option.dataset.corporateName = corporate.name;
                select.appendChild(option);
            });
            
            // 거래처 선택 시 월간보고서 자동 새로고침
            select.onchange = function() {
                if (document.getElementById('monthlyReportBody').children.length > 0 && 
                    !document.getElementById('monthlyReportBody').children[0].textContent.includes('조회 버튼을 클릭')) {
                    // 이미 데이터가 로드되어 있으면 자동 새로고침
                    loadMonthlyReport();
                }
            };
        } else {
            console.error('거래처 목록 로드 실패:', data.message);
        }
    } catch (error) {
        console.error('거래처 목록 로드 실패:', error);
    }
}

// 현재 월로 초기 설정
function setCurrentMonth() {
    const now = new Date();
    document.getElementById('monthlyReportYear').value = now.getFullYear().toString();
    document.getElementById('monthlyReportMonth').value = String(now.getMonth() + 1).padStart(2, '0');
}

// 월간보고서 데이터 로드
async function loadMonthlyReport() {
    const serverUrl = document.getElementById('serverUrl').value;
    const year = document.getElementById('monthlyReportYear').value;
    const month = document.getElementById('monthlyReportMonth').value;
    const corporateId = document.getElementById('monthlyReportCorporate').value;

    if (!serverUrl || !currentToken) {
        alert('서버 설정을 먼저 완료해주세요.');
        return;
    }

    if (!year || !month) {
        alert('년도와 월을 선택해주세요.');
        return;
    }

    // 선택한 월의 시작일과 종료일 계산
    const startDate = `${year}-${month}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const endDate = `${year}-${month}-${String(lastDay).padStart(2, '0')}`;

    console.log(`[MONTHLY-REPORT] 조회 기간: ${startDate} ~ ${endDate}`);

    try {
        // 1. 거래처 목록 가져오기 (전체 또는 선택한 거래처)
        const corporates = await fetchCorporatesForReport(serverUrl, corporateId);

        if (!corporates || corporates.length === 0) {
            alert('조회할 거래처가 없습니다.');
            return;
        }

        // 2. 각 거래처별로 거래 내역 다운로드 및 계산
        const allEmployeeReports = [];
        let totalUsage = 0;
        let totalCash = 0;
        let totalPoints = 0;
        let totalCashBalance = 0;
        let totalPointBalance = 0;

        for (const corporate of corporates) {
            console.log(`[MONTHLY-REPORT] 거래처 조회 중: ${corporate.name} (ID: ${corporate.customerId})`);

            // /api/statistics/corporate/{id} API를 사용하여 거래처별 상세 통계 가져오기
            const url = `${serverUrl}/api/statistics/corporate/${corporate.customerId}?startDate=${startDate}&endDate=${endDate}`;

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${currentToken}`,
                    'Content-Type': 'application/json; charset=utf-8'
                }
            });

            if (response.ok) {
                const data = await response.json();

                if (data.success && data.data && data.data.employees) {
                    // 직원별 데이터 처리
                    data.data.employees.forEach(emp => {
                        const usageCount = emp.usedCoupons || 0;

                        // 클라이언트에서 차감액 계산 (서버 부하 감소)
                        let cashUsed = 0;
                        let pointsUsed = 0;

                        if (emp.transactions && Array.isArray(emp.transactions)) {
                            // transactions 배열에서 각 쿠폰의 사용액을 합산
                            emp.transactions.forEach(txn => {
                                cashUsed += (txn.cashUsed || 0);
                                pointsUsed += (txn.pointUsed || 0);
                            });
                        }

                        allEmployeeReports.push({
                            corporateName: corporate.name,
                            employeeName: emp.employeeName || 'N/A',
                            usageCount: usageCount,
                            deductedCash: cashUsed,
                            deductedPoints: pointsUsed,
                            cashBalance: emp.totalCashBalance || 0,
                            pointBalance: emp.totalPointBalance || 0,
                            employeeId: emp.employeeId || ''
                        });

                        // 전체 합계 계산
                        totalUsage += usageCount;
                        totalCash += cashUsed;
                        totalPoints += pointsUsed;
                        totalCashBalance += (emp.totalCashBalance || 0);
                        totalPointBalance += (emp.totalPointBalance || 0);
                    });
                }
            } else {
                console.warn(`[MONTHLY-REPORT] 거래처 ${corporate.name} 조회 실패: ${response.status}`);
            }
        }

        // 3. 계산된 데이터 표시
        const reportData = {
            summary: {
                totalUsage: totalUsage,
                totalCash: totalCash,
                totalPoints: totalPoints,
                cashBalance: totalCashBalance,
                pointBalance: totalPointBalance
            },
            employees: allEmployeeReports
        };

        console.log('[MONTHLY-REPORT] 계산 완료:', reportData);
        displayMonthlyReportData(reportData);

    } catch (error) {
        console.error('[MONTHLY-REPORT] 조회 실패:', error);
        alert('월간보고서 조회 중 오류가 발생했습니다: ' + error.message);
    }
}

// 거래처 목록 가져오기 (전체 또는 특정 거래처)
async function fetchCorporatesForReport(serverUrl, corporateId) {
    if (corporateId) {
        // 특정 거래처만 조회
        const response = await fetch(`${serverUrl}/api/corporates/${corporateId}`, {
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json; charset=utf-8'
            }
        });

        if (response.ok) {
            const data = await response.json();
            return data.success ? [data.data] : [];
        }
        return [];
    } else {
        // 전체 거래처 조회
        const response = await fetch(`${serverUrl}/api/corporates`, {
            headers: {
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json; charset=utf-8'
            }
        });

        if (response.ok) {
            const data = await response.json();
            return data.success ? data.data : [];
        }
        return [];
    }
}

// Note: 서버 부하 감소를 위해 차감액 계산을 클라이언트에서 수행
// 서버는 transactions 배열(쿠폰별 충전/사용 정보)만 제공하고, 클라이언트가 합산 계산

// 월간보고서 데이터 표시
function displayMonthlyReportData(data) {
    // 요약 정보 업데이트
    document.getElementById('monthlyTotalUsage').textContent = data.summary.totalUsage.toLocaleString();
    document.getElementById('monthlyTotalCash').textContent = data.summary.totalCash.toLocaleString() + '원';
    document.getElementById('monthlyTotalPoints').textContent = data.summary.totalPoints.toLocaleString() + 'P';
    document.getElementById('monthlyCashBalance').textContent = data.summary.cashBalance.toLocaleString() + '원';
    document.getElementById('monthlyPointBalance').textContent = data.summary.pointBalance.toLocaleString() + 'P';
    
    // 직원별 상세 테이블 업데이트
    const tbody = document.getElementById('monthlyReportBody');
    tbody.innerHTML = '';
    
    data.employees.forEach(emp => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td style="border: 1px solid #e0e0e0; padding: 10px;">${emp.corporateName}</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px;">${emp.employeeName}</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: center;">${emp.usageCount.toLocaleString()}</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${emp.deductedCash.toLocaleString()}원</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${emp.deductedPoints.toLocaleString()}P</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${emp.cashBalance.toLocaleString()}원</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: right;">${emp.pointBalance.toLocaleString()}P</td>
            <td style="border: 1px solid #e0e0e0; padding: 10px; text-align: center;">
                <button onclick="viewEmployeeDetail('${emp.employeeId}', '${emp.employeeName}')" class="btn" style="font-size: 12px; padding: 6px 12px;">상세보기</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 데모 데이터 표시 (API 연동 전 테스트용)
function displayDemoMonthlyData() {
    const corporateSelect = document.getElementById('monthlyReportCorporate');
    const selectedCorporate = corporateSelect.value;
    const selectedCorporateName = selectedCorporate ? 
        corporateSelect.options[corporateSelect.selectedIndex].text : '';
    
    // 전체 데모 데이터
    const allDemoData = [
        {
            corporateName: "삼성전자",
            employeeName: "김철수",
            usageCount: 22,
            deductedCash: 11000,
            deductedPoints: 220,
            cashBalance: 89000,
            pointBalance: 780,
            employeeId: "emp001"
        },
        {
            corporateName: "삼성전자",
            employeeName: "이영희",
            usageCount: 18,
            deductedCash: 9000,
            deductedPoints: 180,
            cashBalance: 91000,
            pointBalance: 820,
            employeeId: "emp002"
        },
        {
            corporateName: "LG화학",
            employeeName: "박민수",
            usageCount: 25,
            deductedCash: 12500,
            deductedPoints: 250,
            cashBalance: 87500,
            pointBalance: 750,
            employeeId: "emp003"
        },
        {
            corporateName: "LG화학",
            employeeName: "이수진",
            usageCount: 20,
            deductedCash: 10000,
            deductedPoints: 200,
            cashBalance: 90000,
            pointBalance: 800,
            employeeId: "emp004"
        },
        {
            corporateName: "현대자동차",
            employeeName: "정민호",
            usageCount: 15,
            deductedCash: 7500,
            deductedPoints: 150,
            cashBalance: 92500,
            pointBalance: 850,
            employeeId: "emp005"
        }
    ];
    
    // 거래처 선택에 따라 데이터 필터링
    let filteredEmployees = allDemoData;
    if (selectedCorporate && selectedCorporateName !== '전체') {
        filteredEmployees = allDemoData.filter(emp => emp.corporateName === selectedCorporateName);
    }
    
    // 필터링된 데이터로 요약 통계 계산
    const summary = {
        totalUsage: filteredEmployees.reduce((sum, emp) => sum + emp.usageCount, 0),
        totalCash: filteredEmployees.reduce((sum, emp) => sum + emp.deductedCash, 0),
        totalPoints: filteredEmployees.reduce((sum, emp) => sum + emp.deductedPoints, 0),
        cashBalance: filteredEmployees.reduce((sum, emp) => sum + emp.cashBalance, 0),
        pointBalance: filteredEmployees.reduce((sum, emp) => sum + emp.pointBalance, 0)
    };
    
    const demoData = {
        summary: summary,
        employees: filteredEmployees
    };
    
    displayMonthlyReportData(demoData);
}

// 직원 상세보기
function viewEmployeeDetail(employeeId, employeeName) {
    alert(`${employeeName} 직원의 상세 내역을 조회합니다.\n직원 ID: ${employeeId}\n\n(상세 내역 기능은 추후 구현 예정)`);
}

// 월간보고서 Excel 다운로드
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

// 페이지 로드 시 개발자 메뉴 상태 초기화
document.addEventListener('DOMContentLoaded', function() {
    updateDeveloperMenu(); // 초기 메뉴 상태 설정 (이메일/SMS 메뉴 숨김)

    // 가격 설정 초기화
    setTimeout(function() {
        const enableCheckbox = document.getElementById('enableTimeBasedDeduction');
        if (enableCheckbox) {
            toggleTimeBasedControls(); // 초기 컨트롤 상태 설정
        }
    }, 100);
});
