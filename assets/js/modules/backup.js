/**
 * Backup/Restore Module
 * Handles database backup and restore functionality
 */

/**
 * Backup database to JSON file
 */
async function backupDatabase() {
    const statusDiv = document.getElementById('backupStatus');
    const serverUrl = document.getElementById('serverUrl').value;

    if (!currentToken) {
        statusDiv.innerHTML = '<div class="alert alert-danger">로그인이 필요합니다.</div>';
        return;
    }

    try {
        statusDiv.innerHTML = '<div class="alert alert-info">백업 중...</div>';

        const response = await fetch(serverUrl + '/api/backup', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + currentToken
            }
        });

        if (!response.ok) {
            throw new Error('백업 요청 실패: ' + response.status);
        }

        const result = await response.json();

        if (result.success) {
            // JSON 데이터를 파일로 다운로드
            const dataStr = JSON.stringify(result, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });

            // 파일명 생성 (타임스탬프 포함)
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
            const filename = `couponman_backup_${timestamp}.json`;

            // 다운로드 링크 생성
            const downloadLink = document.createElement('a');
            downloadLink.href = URL.createObjectURL(dataBlob);
            downloadLink.download = filename;
            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);

            // 통계 정보 표시
            const stats = result.data;
            const corporateCount = stats.corporates ? stats.corporates.length : 0;
            const employeeCount = stats.employees ? stats.employees.length : 0;
            const couponCount = stats.coupons ? stats.coupons.length : 0;
            const transactionCount = stats.transactions ? stats.transactions.length : 0;

            statusDiv.innerHTML = `
                <div class="alert alert-success">
                    <h4>✅ 백업 완료!</h4>
                    <p><strong>파일명:</strong> ${filename}</p>
                    <p><strong>백업 시간:</strong> ${stats.timestamp || new Date().toLocaleString()}</p>
                    <p><strong>백업 데이터:</strong></p>
                    <ul>
                        <li>거래처: ${corporateCount}개</li>
                        <li>직원: ${employeeCount}명</li>
                        <li>쿠폰: ${couponCount}개</li>
                        <li>거래내역: ${transactionCount}건</li>
                    </ul>
                </div>
            `;
        } else {
            statusDiv.innerHTML = `<div class="alert alert-danger">백업 실패: ${result.message}</div>`;
        }
    } catch (error) {
        console.error('Backup error:', error);
        statusDiv.innerHTML = `<div class="alert alert-danger">백업 중 오류 발생: ${error.message}</div>`;
    }
}

/**
 * Restore database from JSON file
 */
async function restoreDatabase() {
    const statusDiv = document.getElementById('restoreStatus');
    const serverUrl = document.getElementById('serverUrl').value;
    const fileInput = document.getElementById('restoreFile');

    if (!currentToken) {
        statusDiv.innerHTML = '<div class="alert alert-danger">로그인이 필요합니다.</div>';
        return;
    }

    if (!fileInput.files || fileInput.files.length === 0) {
        statusDiv.innerHTML = '<div class="alert alert-danger">복구할 파일을 선택하세요.</div>';
        return;
    }

    // 사용자 확인
    const confirmed = confirm(
        '⚠️ 경고!\n\n' +
        '데이터 복구를 실행하면 현재 데이터베이스의 모든 데이터가 삭제되고 백업 파일의 데이터로 교체됩니다.\n\n' +
        '이 작업은 되돌릴 수 없습니다!\n\n' +
        '정말 복구하시겠습니까?'
    );

    if (!confirmed) {
        statusDiv.innerHTML = '<div class="alert alert-warning">복구가 취소되었습니다.</div>';
        return;
    }

    const file = fileInput.files[0];

    try {
        statusDiv.innerHTML = '<div class="alert alert-info">파일 읽는 중...</div>';

        // 파일 읽기
        const fileContent = await readFileAsText(file);

        statusDiv.innerHTML = '<div class="alert alert-info">복구 중... (이 작업은 시간이 걸릴 수 있습니다)</div>';

        // JSON 파싱 검증
        let backupData;
        try {
            backupData = JSON.parse(fileContent);
        } catch (e) {
            throw new Error('잘못된 JSON 파일 형식입니다.');
        }

        // 백업 데이터 검증
        if (!backupData.data) {
            throw new Error('유효하지 않은 백업 파일입니다.');
        }

        // 복구 API 호출
        const response = await fetch(serverUrl + '/api/restore', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + currentToken
            },
            body: JSON.stringify(backupData)
        });

        if (!response.ok) {
            throw new Error('복구 요청 실패: ' + response.status);
        }

        const result = await response.json();

        if (result.success) {
            const restored = result.restored || {};

            statusDiv.innerHTML = `
                <div class="alert alert-success">
                    <h4>✅ 복구 완료!</h4>
                    <p><strong>복구된 데이터:</strong></p>
                    <ul>
                        <li>거래처: ${restored.corporates || 0}개</li>
                        <li>직원: ${restored.employees || 0}명</li>
                        <li>쿠폰: ${restored.coupons || 0}개</li>
                        <li>거래내역: ${restored.transactions || 0}건</li>
                    </ul>
                    <p style="color: #155724; margin-top: 10px;">
                        복구가 완료되었습니다. 데이터가 정상적으로 복원되었는지 확인하세요.
                    </p>
                </div>
            `;

            // 파일 입력 초기화
            fileInput.value = '';
        } else {
            statusDiv.innerHTML = `<div class="alert alert-danger">복구 실패: ${result.message}</div>`;
        }
    } catch (error) {
        console.error('Restore error:', error);
        statusDiv.innerHTML = `<div class="alert alert-danger">복구 중 오류 발생: ${error.message}</div>`;
    }
}

/**
 * Read file as text
 * @param {File} file - File to read
 * @returns {Promise<string>} File content as text
 */
function readFileAsText(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();

        reader.onload = (event) => {
            resolve(event.target.result);
        };

        reader.onerror = (error) => {
            reject(new Error('파일 읽기 실패: ' + error));
        };

        reader.readAsText(file);
    });
}
