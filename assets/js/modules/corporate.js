/**
 * Corporate Management Module
 * Handles all corporate (거래처) related operations
 */

/**
 * Load all corporates
 */
async function loadAllCorporates() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    const corporateList = document.getElementById('corporateList');

    corporateList.innerHTML = '<p style="text-align: center;"><span class="loading"></span> 거래처 목록을 불러오는 중...</p>';

    try {
        const response = await fetch(`${serverUrl}/api/corporates`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const data = await response.json();

        if (data.success) {
            displayCorporateList(data.data);
        } else {
            corporateList.innerHTML = `<p style="text-align: center; color: #f56565;">❌ ${data.message}</p>`;
        }
    } catch (error) {
        corporateList.innerHTML = `<p style="text-align: center; color: #f56565;">❌ 오류: ${error.message}</p>`;
    }
}

/**
 * Display corporate list
 * @param {Array} corporates - List of corporates
 */
function displayCorporateList(corporates) {
    const corporateList = document.getElementById('corporateList');

    if (!corporates || corporates.length === 0) {
        corporateList.innerHTML = '<p style="text-align: center; color: #666;">등록된 거래처가 없습니다.</p>';
        return;
    }

    let html = `<div style="margin-bottom: 10px; font-weight: bold; color: #667eea;">총 ${corporates.length}개의 거래처</div>`;
    html += '<div style="display: grid; gap: 15px;">';

    corporates.forEach(corporate => {
        html += `
            <div class="card" style="padding: 20px;">
                <div style="display: flex; justify-content: space-between; align-items: start;">
                    <div style="flex: 1;">
                        <h4 style="color: #333; margin: 0 0 10px 0; font-size: 18px;">${corporate.name}</h4>
                        <div style="color: #666; font-size: 14px; line-height: 1.5;">
                            ${corporate.businessNumber ? `<div><strong>사업자등록번호:</strong> ${formatBusinessNumber(corporate.businessNumber)}</div>` : ''}
                            ${corporate.representative ? `<div><strong>대표자:</strong> ${corporate.representative}</div>` : ''}
                            ${corporate.phone ? `<div><strong>연락처:</strong> ${corporate.phone}</div>` : ''}
                            ${corporate.email ? `<div><strong>이메일:</strong> ${corporate.email}</div>` : ''}
                            ${corporate.address ? `<div><strong>주소:</strong> ${corporate.address}</div>` : ''}
                            ${corporate.createdAt ? `<div style="margin-top: 10px; color: #999; font-size: 12px;"><strong>등록일:</strong> ${formatDate(corporate.createdAt)}</div>` : ''}
                        </div>
                    </div>
                    <div style="margin-left: 20px;">
                        <button onclick="editCorporate(${corporate.customerId})" class="btn btn-success" style="padding: 8px 15px; font-size: 14px; margin-right: 5px;">수정</button>
                        <button onclick="deleteCorporate(${corporate.customerId}, '${corporate.name}')" class="btn btn-danger" style="padding: 8px 15px; font-size: 14px;">삭제</button>
                    </div>
                </div>
            </div>
        `;
    });

    html += '</div>';
    corporateList.innerHTML = html;
}

/**
 * Search corporates by name
 */
async function searchCorporates() {
    const searchTerm = document.getElementById('searchInput').value.trim();
    if (!searchTerm) {
        alert('검색할 회사명을 입력해주세요.');
        return;
    }

    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    const corporateList = document.getElementById('corporateList');

    corporateList.innerHTML = '<p style="text-align: center;"><span class="loading"></span> 검색 중...</p>';

    try {
        const response = await fetch(`${serverUrl}/api/corporates/search?name=${encodeURIComponent(searchTerm)}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const data = await response.json();

        if (data.success) {
            displayCorporateList(data.data);
        } else {
            corporateList.innerHTML = `<p style="text-align: center; color: #f56565;">❌ ${data.message}</p>`;
        }
    } catch (error) {
        corporateList.innerHTML = `<p style="text-align: center; color: #f56565;">❌ 오류: ${error.message}</p>`;
    }
}

/**
 * Show create corporate form
 */
function showCreateCorporateForm() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    document.getElementById('modalTitle').textContent = '새 거래처 추가';
    document.getElementById('corporateId').value = '';
    document.getElementById('corporateForm').reset();
    document.getElementById('corporateModal').style.display = 'block';
}

/**
 * Edit corporate
 * @param {number} corporateId - Corporate ID
 */
async function editCorporate(corporateId) {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        const response = await fetch(`${serverUrl}/api/corporates/${corporateId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const data = await response.json();

        if (data.success) {
            const corporate = data.data;

            document.getElementById('modalTitle').textContent = '거래처 수정';
            document.getElementById('corporateId').value = corporate.customerId;
            document.getElementById('corporateName').value = corporate.name || '';
            document.getElementById('businessNumber').value = corporate.businessNumber || '';
            document.getElementById('representative').value = corporate.representative || '';
            document.getElementById('phone').value = corporate.phone || '';
            document.getElementById('email').value = corporate.email || '';
            document.getElementById('address').value = corporate.address || '';

            document.getElementById('corporateModal').style.display = 'block';
        } else {
            alert('거래처 정보를 불러오는데 실패했습니다: ' + data.message);
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}

/**
 * Close corporate modal
 */
function closeCorporateModal() {
    document.getElementById('corporateModal').style.display = 'none';
}

/**
 * Submit corporate form
 * @param {Event} event - Form submit event
 */
async function submitCorporateForm(event) {
    event.preventDefault();

    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;
    const corporateId = document.getElementById('corporateId').value;
    const isUpdate = corporateId !== '';

    const corporateData = {
        name: document.getElementById('corporateName').value.trim(),
        businessNumber: document.getElementById('businessNumber').value.trim(),
        representative: document.getElementById('representative').value.trim(),
        phone: document.getElementById('phone').value.trim(),
        email: document.getElementById('email').value.trim(),
        address: document.getElementById('address').value.trim()
    };

    if (!corporateData.name) {
        alert('회사명은 필수입니다.');
        return;
    }

    console.log('=== CORPORATE FORM SUBMIT ===');
    console.log('Method:', isUpdate ? 'PUT' : 'POST');
    console.log('URL:', isUpdate ? `${serverUrl}/api/corporates/${corporateId}` : `${serverUrl}/api/corporates`);
    console.log('Data:', JSON.stringify(corporateData, null, 2));
    console.log('Token:', currentToken ? 'Present' : 'Missing');

    try {
        const url = isUpdate ? `${serverUrl}/api/corporates/${corporateId}` : `${serverUrl}/api/corporates`;
        const method = isUpdate ? 'PUT' : 'POST';

        const requestOptions = {
            method: method,
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(corporateData)
        };

        console.log('Request options:', requestOptions);

        const response = await fetch(url, requestOptions);
        console.log('Response status:', response.status);
        console.log('Response headers:', [...response.headers.entries()]);

        const data = await response.json();
        console.log('Response data:', data);

        if (data.success) {
            alert(isUpdate ? '거래처가 성공적으로 수정되었습니다.' : '거래처가 성공적으로 추가되었습니다.');
            closeCorporateModal();
            loadAllCorporates(); // 목록 새로고침
        } else {
            alert('오류: ' + data.message);
        }
    } catch (error) {
        console.error('Fetch error:', error);
        alert('오류: ' + error.message);
    }
}

/**
 * Delete corporate
 * @param {number} corporateId - Corporate ID
 * @param {string} corporateName - Corporate name
 */
async function deleteCorporate(corporateId, corporateName) {
    if (!confirm(`정말로 "${corporateName}" 거래처를 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`)) {
        return;
    }

    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        const response = await fetch(`${serverUrl}/api/corporates/${corporateId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const data = await response.json();

        if (data.success) {
            alert('거래처가 성공적으로 삭제되었습니다.');
            loadAllCorporates(); // 목록 새로고침
        } else {
            alert('삭제 실패: ' + data.message);
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}

// Initialize search on Enter key
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchCorporates();
            }
        });
    }
});
