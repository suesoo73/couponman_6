/**
 * delivery-history Module
 * Auto-extracted from index_v2.html
 */

// === 발송 기록 관련 함수들 ===

// 전역 변수
let deliveryHistoryData = [];
let currentPage = 1;
const itemsPerPage = 20;

// 발송 기록 로드
async function loadDeliveryHistory() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/coupon-send/history`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            deliveryHistoryData = data.data || [];
            
            // 데이터 구조 상세 로그
            console.log('[DELIVERY-HISTORY] 서버 응답 데이터:', data);
            console.log('[DELIVERY-HISTORY] 총 데이터 개수:', deliveryHistoryData.length);
            
            if (deliveryHistoryData.length > 0) {
                const firstItem = deliveryHistoryData[0];
                console.log('[DELIVERY-HISTORY] 첫 번째 항목 전체 구조:', firstItem);
                console.log('[DELIVERY-HISTORY] 첫 번째 항목 status 값:', firstItem.status);
                console.log('[DELIVERY-HISTORY] 첫 번째 항목 deliveryStatus 값:', firstItem.deliveryStatus);
                console.log('[DELIVERY-HISTORY] 첫 번째 항목 모든 키:', Object.keys(firstItem));
            }
            
            updateDeliveryStats();
            displayDeliveryHistory();
        } else {
            alert('발송 기록 로드 실패: ' + data.message);
        }
    } catch (error) {
        alert('발송 기록 로드 오류: ' + error.message);
    }
}

// 발송 기록 통계 업데이트
function updateDeliveryStats() {
    const stats = {
        total: deliveryHistoryData.length,
        email: deliveryHistoryData.filter(d => d.type === 'email').length,
        sms: deliveryHistoryData.filter(d => d.type === 'sms').length,
        kakao: deliveryHistoryData.filter(d => d.type === 'kakao').length,
        sent: deliveryHistoryData.filter(d => d.status === 'sent').length,
        failed: deliveryHistoryData.filter(d => d.status === 'failed').length,
        pending: deliveryHistoryData.filter(d => d.status === 'pending').length
    };

    const statsHtml = `
        <div style="background: #e6f3ff; padding: 15px; border-radius: 8px; text-align: center;">
            <div style="font-size: 24px; font-weight: bold; color: #667eea;">${stats.total}</div>
            <div style="color: #666;">전체 발송</div>
        </div>
        <div style="background: #e6ffed; padding: 15px; border-radius: 8px; text-align: center;">
            <div style="font-size: 24px; font-weight: bold; color: #48bb78;">${stats.sent}</div>
            <div style="color: #666;">발송 성공</div>
        </div>
        <div style="background: #fff2f0; padding: 15px; border-radius: 8px; text-align: center;">
            <div style="font-size: 24px; font-weight: bold; color: #ff4d4f;">${stats.failed}</div>
            <div style="color: #666;">발송 실패</div>
        </div>
        <div style="background: #fff7e6; padding: 15px; border-radius: 8px; text-align: center;">
            <div style="font-size: 24px; font-weight: bold; color: #fa8c16;">${stats.pending}</div>
            <div style="color: #666;">대기 중</div>
        </div>
    `;

    document.getElementById('deliveryStats').innerHTML = statsHtml;
}

// 발송 기록 표시
function displayDeliveryHistory(filteredData = null) {
    const dataToShow = filteredData || deliveryHistoryData;
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const pageData = dataToShow.slice(startIndex, endIndex);
    
    const tbody = document.querySelector('#deliveryHistoryTable tbody');
    tbody.innerHTML = '';
    
    if (pageData.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 20px; color: #666;">
                    발송 기록이 없습니다.
                </td>
            </tr>
        `;
        return;
    }
    
    pageData.forEach((delivery, index) => {
        const row = document.createElement('tr');
        
        // 각 항목의 status 값 상세 로그
        console.log(`[DELIVERY-HISTORY] 항목 ${index} - deliveryId: ${delivery.deliveryId}, status: '${delivery.status}', deliveryStatus: '${delivery.deliveryStatus}'`);
        
        // 발송 유형 아이콘
        const typeIcons = {
            'email': '📧',
            'sms': '📱',
            'kakao': '💬'
        };
        
        row.innerHTML = `
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${delivery.deliveryId}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${delivery.couponCode || delivery.couponId || 'N/A'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">
                ${typeIcons[delivery.type] || '❓'} ${delivery.type ? delivery.type.toUpperCase() : 'UNKNOWN'}
            </td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${delivery.recipient || '-'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${delivery.subject || '-'}">${delivery.subject || '-'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${getStatusBadge(delivery.status)}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px;">${delivery.sentAt ? new Date(delivery.sentAt).toLocaleString('ko-KR') : '-'}</td>
            <td style="border: 1px solid #e0e0e0; padding: 8px; text-align: center;">
                <button onclick="showDeliveryDetails(${delivery.deliveryId})" 
                        style="background: #667eea; color: white; border: none; padding: 4px 8px; border-radius: 3px; cursor: pointer; font-size: 12px;">
                    상세
                </button>
            </td>
        `;
        
        tbody.appendChild(row);
    });
    
    // 페이지네이션 업데이트
    updatePagination(dataToShow.length);
}

// 상태 배지 생성
function getStatusBadge(status) {
    const badges = {
        'sent': '<span style="background: #52c41a; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">발송됨</span>',
        'failed': '<span style="background: #ff4d4f; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">실패</span>',
        'pending': '<span style="background: #fa8c16; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">대기</span>'
    };
    return badges[status] || '<span style="background: #d9d9d9; color: #666; padding: 2px 8px; border-radius: 12px; font-size: 12px;">알 수 없음</span>';
}

// 발송 기록 필터링
function filterDeliveryHistory() {
    const searchType = document.getElementById('deliverySearchType').value;
    const searchStatus = document.getElementById('deliverySearchStatus').value;
    const searchDate = document.getElementById('deliverySearchDate').value;
    
    let filteredData = deliveryHistoryData;
    
    // 발송 유형 필터
    if (searchType) {
        filteredData = filteredData.filter(d => d.type === searchType);
    }
    
    // 상태 필터
    if (searchStatus) {
        filteredData = filteredData.filter(d => d.status === searchStatus);
    }
    
    // 날짜 필터
    if (searchDate) {
        filteredData = filteredData.filter(d => {
            if (!d.sentAt) return false;
            const deliveryDate = new Date(d.sentAt).toISOString().split('T')[0];
            return deliveryDate === searchDate;
        });
    }
    
    currentPage = 1; // 필터 시 첫 페이지로 리셋
    displayDeliveryHistory(filteredData);
}

// 발송 상세 정보 표시
function showDeliveryDetails(deliveryId) {
    const delivery = deliveryHistoryData.find(d => d.deliveryId === deliveryId);
    if (!delivery) {
        alert('발송 기록을 찾을 수 없습니다.');
        return;
    }
    
    const details = `
발송 ID: ${delivery.deliveryId}
쿠폰 ID: ${delivery.couponId}
쿠폰 번호: ${delivery.couponCode || 'N/A'}
발송 유형: ${delivery.type ? delivery.type.toUpperCase() : 'UNKNOWN'}
수신자: ${delivery.recipient}
제목: ${delivery.subject || '-'}
내용: ${delivery.message || '-'}
상태: ${delivery.status}
발송일시: ${delivery.sentAt ? new Date(delivery.sentAt).toLocaleString('ko-KR') : '-'}
생성일시: ${delivery.createdAt ? new Date(delivery.createdAt).toLocaleString('ko-KR') : '-'}
메타데이터: ${delivery.metadata || '-'}
    `;
    
    alert(details);
}

// 페이지네이션 업데이트
function updatePagination(totalItems) {
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    const pagination = document.getElementById('deliveryPagination');
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let paginationHtml = '';
    
    // 이전 버튼
    if (currentPage > 1) {
        paginationHtml += `<button onclick="changePage(${currentPage - 1})" style="margin: 0 2px; padding: 6px 12px; border: 1px solid #ddd; background: white; cursor: pointer;">이전</button>`;
    }
    
    // 페이지 번호
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        const isActive = i === currentPage;
        paginationHtml += `<button onclick="changePage(${i})" style="margin: 0 2px; padding: 6px 12px; border: 1px solid #ddd; background: ${isActive ? '#667eea' : 'white'}; color: ${isActive ? 'white' : 'black'}; cursor: pointer;">${i}</button>`;
    }
    
    // 다음 버튼
    if (currentPage < totalPages) {
        paginationHtml += `<button onclick="changePage(${currentPage + 1})" style="margin: 0 2px; padding: 6px 12px; border: 1px solid #ddd; background: white; cursor: pointer;">다음</button>`;
    }
    
    pagination.innerHTML = paginationHtml;
}

// 페이지 변경
function changePage(page) {
    currentPage = page;
    displayDeliveryHistory();
}

