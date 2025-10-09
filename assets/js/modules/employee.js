/**
 * employee Module
 * Auto-extracted from index_v2.html
 */

// ========== 직원 관리 함수들 ==========

// 직원 탭을 위한 거래처 목록 로드
async function loadCorporatesForEmployeeTab() {
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
            const select = document.getElementById('corporateSelectForEmployee');
            select.innerHTML = '<option value="">거래처를 선택하세요</option>';
            
            data.data.forEach(corporate => {
                const option = document.createElement('option');
                option.value = corporate.customerId;
                option.textContent = corporate.name;
                option.dataset.corporateName = corporate.name;
                select.appendChild(option);
            });
            
            // 거래처 선택 시 직원 목록 로드
            select.onchange = function() {
                if (this.value) {
                    loadEmployeesByCorporate(this.value);
                    document.getElementById('addEmployeeBtn').disabled = false;
                    document.getElementById('bulkImportBtn').disabled = false;
                } else {
                    document.getElementById('employeeList').innerHTML = '<p style="text-align: center; color: #666;">거래처를 선택하면 직원 목록이 표시됩니다.</p>';
                    document.getElementById('addEmployeeBtn').disabled = true;
                    document.getElementById('bulkImportBtn').disabled = true;
                }
            };
        }
    } catch (error) {
        console.error('Error loading corporates:', error);
    }
}

// 거래처별 직원 목록 로드
async function loadEmployeesByCorporate(corporateId) {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    const employeeList = document.getElementById('employeeList');
    
    employeeList.innerHTML = '<p style="text-align: center;"><span class="loading"></span> 직원 목록 로딩 중...</p>';
    
    try {
        const response = await fetch(`${serverUrl}/api/corporates/${corporateId}/employees`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            displayEmployeeList(data.data);
        } else {
            employeeList.innerHTML = '<p style="text-align: center; color: red;">직원 목록을 불러오는데 실패했습니다.</p>';
        }
    } catch (error) {
        employeeList.innerHTML = '<p style="text-align: center; color: red;">오류: ' + error.message + '</p>';
    }
}

// 직원 목록 표시
function displayEmployeeList(employees) {
    const employeeList = document.getElementById('employeeList');
    
    if (!employees || employees.length === 0) {
        employeeList.innerHTML = '<p style="text-align: center; color: #666;">등록된 직원이 없습니다.</p>';
        return;
    }
    
    let html = '<div style="display: grid; gap: 15px;">';
    
    employees.forEach(employee => {
        html += `
            <div class="card" style="padding: 15px;">
                <div style="display: flex; justify-content: space-between; align-items: start;">
                    <div style="flex: 1;">
                        <h4 style="margin: 0 0 10px 0; color: #333;">${employee.name || employee.phone}</h4>
                        <div style="color: #666; font-size: 14px;">
                            <p style="margin: 5px 0;"><strong>부서:</strong> ${employee.department || '-'}</p>
                            <p style="margin: 5px 0;"><strong>핸드폰:</strong> ${employee.phone}</p>
                            <p style="margin: 5px 0;"><strong>이메일:</strong> ${employee.email || '-'}</p>
                            <p style="margin: 5px 0; font-size: 12px; color: #999;">등록일: ${formatDate(employee.createdAt)}</p>
                        </div>
                    </div>
                    <div style="display: flex; gap: 10px;">
                        <button onclick="editEmployee(${employee.employeeId})" class="btn btn-success" style="padding: 8px 15px; font-size: 14px;">수정</button>
                        <button onclick="deleteEmployee(${employee.employeeId}, '${employee.name}')" class="btn btn-danger" style="padding: 8px 15px; font-size: 14px;">삭제</button>
                    </div>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    employeeList.innerHTML = html;
}

// 새 직원 추가 폼 표시
function showCreateEmployeeForm() {
    const select = document.getElementById('corporateSelectForEmployee');
    const corporateId = select.value;
    const corporateName = select.options[select.selectedIndex].dataset.corporateName;
    
    if (!corporateId) {
        alert('먼저 거래처를 선택해주세요.');
        return;
    }
    
    document.getElementById('employeeModalTitle').textContent = '새 직원 추가';
    document.getElementById('employeeId').value = '';
    document.getElementById('employeeCorporateId').value = corporateId;
    document.getElementById('employeeCorporateName').value = corporateName;
    document.getElementById('employeeName').value = '';
    document.getElementById('employeePhone').value = '';
    document.getElementById('employeeEmail').value = '';
    document.getElementById('employeeDepartment').value = '';
    
    document.getElementById('employeeModal').style.display = 'block';
}

// 직원 수정
async function editEmployee(employeeId) {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/employees/${employeeId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            const employee = data.data;
            const select = document.getElementById('corporateSelectForEmployee');
            const corporateName = select.options[select.selectedIndex].dataset.corporateName;
            
            document.getElementById('employeeModalTitle').textContent = '직원 정보 수정';
            document.getElementById('employeeId').value = employee.employeeId;
            document.getElementById('employeeCorporateId').value = employee.corporateId;
            document.getElementById('employeeCorporateName').value = corporateName;
            document.getElementById('employeeName').value = employee.name;
            document.getElementById('employeePhone').value = employee.phone;
            document.getElementById('employeeEmail').value = employee.email || '';
            document.getElementById('employeeDepartment').value = employee.department || '';
            
            document.getElementById('employeeModal').style.display = 'block';
        } else {
            alert('직원 정보를 불러오는데 실패했습니다: ' + data.message);
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}

// 직원 모달 닫기
function closeEmployeeModal() {
    document.getElementById('employeeModal').style.display = 'none';
}

// 직원 폼 제출
async function submitEmployeeForm(event) {
    event.preventDefault();
    
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    const employeeId = document.getElementById('employeeId').value;
    const isUpdate = employeeId !== '';
    
    const employeeData = {
        corporateId: parseInt(document.getElementById('employeeCorporateId').value),
        name: document.getElementById('employeeName').value.trim(),
        phone: document.getElementById('employeePhone').value.trim(),
        email: document.getElementById('employeeEmail').value.trim(),
        department: document.getElementById('employeeDepartment').value.trim()
    };
    
    if (!employeeData.phone) {
        alert('핸드폰 번호는 필수입니다.');
        return;
    }
    
    // 전화번호 중복 검사 (새 직원 등록 시 또는 전화번호 변경 시)
    try {
        const checkResponse = await fetch(`${serverUrl}/api/employees/check-phone`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({ 
                phone: employeeData.phone,
                excludeEmployeeId: isUpdate ? employeeId : null
            })
        });
        
        const checkData = await checkResponse.json();
        
        if (checkData.phoneExists) {
            const existingEmployee = checkData.existingEmployee;
            const corporateName = checkData.corporateName;
            
            const confirmMessage = `이미 등록된 전화번호입니다.\n\n` +
                `등록된 직원 정보:\n` +
                `• 이름: ${existingEmployee.name}\n` +
                `• 소속 거래처: ${corporateName}\n` +
                `• 부서: ${existingEmployee.department || '미지정'}\n\n` +
                `그래도 이 전화번호로 직원을 등록하시겠습니까?`;
            
            if (!confirm(confirmMessage)) {
                return; // 사용자가 취소를 선택한 경우
            }
        }
    } catch (error) {
        console.log('전화번호 중복 검사 실패:', error);
        // 중복 검사 실패 시에도 계속 진행 (서버에서 처리)
    }
    
    try {
        const url = isUpdate ? `${serverUrl}/api/employees/${employeeId}` : `${serverUrl}/api/employees`;
        const method = isUpdate ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify(employeeData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            // 경고 메시지가 있는 경우 먼저 표시
            if (data.warning) {
                alert('경고: ' + data.warning + '\n\n' + data.message);
            } else {
                alert(isUpdate ? '직원 정보가 성공적으로 수정되었습니다.' : '직원이 성공적으로 추가되었습니다.');
            }
            closeEmployeeModal();
            loadEmployeesByCorporate(employeeData.corporateId);
        } else {
            alert('오류: ' + data.message);
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}

// 직원 삭제
async function deleteEmployee(employeeId, employeeName) {
    if (!confirm(`정말로 "${employeeName}" 직원을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`)) {
        return;
    }
    
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }
    
    const serverUrl = document.getElementById('serverUrl').value;
    
    try {
        const response = await fetch(`${serverUrl}/api/employees/${employeeId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert('직원이 성공적으로 삭제되었습니다.');
            const corporateId = document.getElementById('corporateSelectForEmployee').value;
            if (corporateId) {
                loadEmployeesByCorporate(corporateId);
            }
        } else {
            alert('삭제 실패: ' + data.message);
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}

// ========== Excel 데이터 일괄 등록 함수들 ==========

// Excel 데이터 일괄 등록 폼 표시
function showBulkImportForm() {
    const select = document.getElementById('corporateSelectForEmployee');
    if (!select.value) {
        alert('먼저 거래처를 선택해주세요.');
        return;
    }
    
    document.getElementById('bulkDataInput').value = '';
    document.getElementById('dataPreview').style.display = 'none';
    document.getElementById('bulkImportSubmitBtn').disabled = true;
    document.getElementById('bulkImportModal').style.display = 'block';
}

// Excel 데이터 일괄 등록 모달 닫기
function closeBulkImportModal() {
    document.getElementById('bulkImportModal').style.display = 'none';
}

// 데이터 미리보기
function previewBulkData() {
    const input = document.getElementById('bulkDataInput').value.trim();
    if (!input) {
        alert('먼저 Excel 데이터를 붙여넣어주세요.');
        return;
    }

    try {
        const parsedData = parseExcelData(input);
        displayDataPreview(parsedData);
        document.getElementById('dataPreview').style.display = 'block';
        document.getElementById('bulkImportSubmitBtn').disabled = parsedData.validRows.length === 0;
    } catch (error) {
        alert('데이터 파싱 오류: ' + error.message);
    }
}

// Excel 데이터 파싱
function parseExcelData(input) {
    const lines = input.split('\n').filter(line => line.trim());
    const validRows = [];
    const invalidRows = [];
    
    lines.forEach((line, index) => {
        // 탭, 쉼표, 공백으로 구분된 데이터 처리
        const cells = line.split(/\t|,|\s{2,}/).map(cell => cell.trim()).filter(cell => cell);
        
        if (cells.length < 1) {
            invalidRows.push({
                lineNumber: index + 1,
                data: line,
                error: '최소 핸드폰 번호가 필요합니다'
            });
            return;
        }

        // 첫 번째 열이 핸드폰 번호인 경우와 이름이 먼저인 경우 구분
        let name = '', phone = '', email = '', department = '';
        
        // 첫 번째 셀이 핸드폰 번호 형식인지 확인
        const phonePattern = /^010-?\d{4}-?\d{4}$/;
        const firstCellClean = cells[0].replace(/\s/g, '');
        
        if (phonePattern.test(firstCellClean)) {
            // 첫 번째 열이 핸드폰 번호인 경우: phone, email, department 순서
            phone = cells[0] || '';
            email = cells[1] || '';
            department = cells[2] || '';
        } else {
            // 기본 순서: name, phone, email, department
            name = cells[0] || '';
            phone = cells[1] || '';
            email = cells[2] || '';
            department = cells[3] || '';
        }
        
        // 필수 필드 검증 (핸드폰 번호만 필수)
        if (!phone) {
            invalidRows.push({
                lineNumber: index + 1,
                data: line,
                error: '핸드폰 번호는 필수입니다'
            });
            return;
        }

        // 핸드폰 번호 형식 간단 검증
        const cleanPhone = phone.replace(/\s/g, '');
        if (!phonePattern.test(cleanPhone)) {
            invalidRows.push({
                lineNumber: index + 1,
                data: line,
                error: '핸드폰 번호 형식이 올바르지 않습니다'
            });
            return;
        }

        validRows.push({
            lineNumber: index + 1,
            name: name,
            phone: formatPhoneNumber(cleanPhone),
            email: email || '',
            department: department || ''
        });
    });

    return { validRows, invalidRows };
}

// 핸드폰 번호 형식 정리
function formatPhoneNumber(phone) {
    const cleaned = phone.replace(/\D/g, '');
    if (cleaned.length === 11 && cleaned.startsWith('010')) {
        return cleaned.substring(0, 3) + '-' + cleaned.substring(3, 7) + '-' + cleaned.substring(7);
    }
    return phone;
}

// 데이터 미리보기 표시
function displayDataPreview(parsedData) {
    const { validRows, invalidRows } = parsedData;
    
    let html = '';
    
    // 유효한 데이터 테이블
    if (validRows.length > 0) {
        html += '<h5 style="color: #48bb78; margin: 10px 0;">✅ 등록 가능한 데이터 (' + validRows.length + '건)</h5>';
        html += '<table style="width: 100%; border-collapse: collapse; font-size: 14px; margin-bottom: 20px;">';
        html += '<thead style="background: #f8f9fa;"><tr>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">줄</th>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">이름</th>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">핸드폰</th>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">이메일</th>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">부서</th>';
        html += '</tr></thead><tbody>';
        
        validRows.forEach(row => {
            html += '<tr>';
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px;">${row.lineNumber}</td>`;
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px;">${row.name}</td>`;
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px;">${row.phone}</td>`;
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px;">${row.email}</td>`;
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px;">${row.department}</td>`;
            html += '</tr>';
        });
        
        html += '</tbody></table>';
    }
    
    // 오류 데이터 테이블
    if (invalidRows.length > 0) {
        html += '<h5 style="color: #f56565; margin: 10px 0;">❌ 오류가 있는 데이터 (' + invalidRows.length + '건)</h5>';
        html += '<table style="width: 100%; border-collapse: collapse; font-size: 14px;">';
        html += '<thead style="background: #fed7d7;"><tr>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">줄</th>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">데이터</th>';
        html += '<th style="border: 1px solid #e0e0e0; padding: 8px; text-align: left;">오류 내용</th>';
        html += '</tr></thead><tbody>';
        
        invalidRows.forEach(row => {
            html += '<tr>';
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px;">${row.lineNumber}</td>`;
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px; font-family: monospace;">${row.data}</td>`;
            html += `<td style="border: 1px solid #e0e0e0; padding: 8px; color: #e53e3e;">${row.error}</td>`;
            html += '</tr>';
        });
        
        html += '</tbody></table>';
    }
    
    document.getElementById('previewTable').innerHTML = html;
    
    // 요약 정보
    const summaryHtml = `
        <strong>총 ${validRows.length + invalidRows.length}줄</strong> - 
        <span style="color: #48bb78;">등록 가능: ${validRows.length}건</span>, 
        <span style="color: #f56565;">오류: ${invalidRows.length}건</span>
    `;
    document.getElementById('previewSummary').innerHTML = summaryHtml;
}

// Excel 데이터 일괄 등록 처리
async function processBulkImport() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const input = document.getElementById('bulkDataInput').value.trim();
    if (!input) {
        alert('데이터를 입력해주세요.');
        return;
    }

    try {
        const parsedData = parseExcelData(input);
        const { validRows } = parsedData;

        if (validRows.length === 0) {
            alert('등록할 수 있는 유효한 데이터가 없습니다.');
            return;
        }

        if (!confirm(`총 ${validRows.length}명의 직원을 등록하시겠습니까?`)) {
            return;
        }

        const corporateId = parseInt(document.getElementById('corporateSelectForEmployee').value);
        const serverUrl = document.getElementById('serverUrl').value;
        
        let successCount = 0;
        let failCount = 0;
        const errors = [];

        // 등록 진행 상태 표시
        const submitBtn = document.getElementById('bulkImportSubmitBtn');
        submitBtn.disabled = true;
        submitBtn.textContent = '등록 중...';

        for (let i = 0; i < validRows.length; i++) {
            const row = validRows[i];
            
            try {
                const employeeData = {
                    corporateId: corporateId,
                    name: row.name || row.phone,  // 이름이 없으면 핸드폰 번호 사용
                    phone: row.phone,
                    email: row.email,
                    department: row.department
                };

                const response = await fetch(`${serverUrl}/api/employees`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                        'Authorization': `Bearer ${currentToken}`
                    },
                    body: JSON.stringify(employeeData)
                });

                const data = await response.json();

                if (data.success) {
                    successCount++;
                } else {
                    failCount++;
                    errors.push(`${row.lineNumber}줄 (${row.name}): ${data.message}`);
                }
            } catch (error) {
                failCount++;
                errors.push(`${row.lineNumber}줄 (${row.name}): ${error.message}`);
            }

            // 진행률 업데이트
            submitBtn.textContent = `등록 중... (${i + 1}/${validRows.length})`;
        }

        // 결과 알림
        let message = `등록 완료!\n성공: ${successCount}건, 실패: ${failCount}건`;
        if (errors.length > 0) {
            message += '\n\n실패 내역:\n' + errors.slice(0, 5).join('\n');
            if (errors.length > 5) {
                message += `\n... 외 ${errors.length - 5}건 더`;
            }
        }

        alert(message);

        if (successCount > 0) {
            closeBulkImportModal();
            loadEmployeesByCorporate(corporateId);
        }

    } catch (error) {
        alert('일괄 등록 오류: ' + error.message);
    } finally {
        const submitBtn = document.getElementById('bulkImportSubmitBtn');
        submitBtn.disabled = false;
        submitBtn.textContent = '일괄 등록';
    }
}

