/**
 * Formatting utility functions
 * Handles date, phone number, business number, currency, and point formatting
 */

/**
 * Format business number (사업자등록번호)
 * @param {string} businessNumber - 10 digit business number
 * @returns {string} Formatted business number (XXX-XX-XXXXX)
 */
function formatBusinessNumber(businessNumber) {
    if (!businessNumber || businessNumber.length !== 10) return businessNumber;
    return businessNumber.substring(0, 3) + '-' + businessNumber.substring(3, 5) + '-' + businessNumber.substring(5);
}

/**
 * Format date string to Korean locale
 * @param {string} dateString - Date string to format
 * @returns {string} Formatted date string
 */
function formatDate(dateString) {
    try {
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR');
    } catch (e) {
        return dateString;
    }
}

/**
 * Format phone number (핸드폰 번호)
 * @param {string} phone - Phone number
 * @returns {string} Formatted phone number (XXX-XXXX-XXXX)
 */
function formatPhoneNumber(phone) {
    const cleaned = phone.replace(/\D/g, '');
    if (cleaned.length === 11 && cleaned.startsWith('010')) {
        return cleaned.substring(0, 3) + '-' + cleaned.substring(3, 7) + '-' + cleaned.substring(7);
    }
    return phone;
}

/**
 * Format currency input with thousand separators
 * @param {HTMLInputElement} input - Input element
 */
function formatCurrencyInput(input) {
    // 입력값에서 숫자만 추출
    let value = input.value.replace(/[^\d]/g, '');

    // 빈 값 처리
    if (value === '') {
        input.value = '';
        if (typeof calculateDistribution === 'function') {
            calculateDistribution();
        }
        return;
    }

    // 천단위마다 콤마 추가
    let formattedValue = parseInt(value).toLocaleString();
    input.value = formattedValue;

    // 배분 재계산
    if (typeof calculateDistribution === 'function') {
        calculateDistribution();
    }
}

/**
 * Format point input based on point type
 * @param {HTMLInputElement} input - Input element
 */
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

    if (typeof calculateDistribution === 'function') {
        calculateDistribution();
    }
}

/**
 * Extract numeric value from formatted string
 * @param {string} formattedString - String with commas
 * @returns {number} Numeric value
 */
function getNumericValue(formattedString) {
    if (!formattedString) return 0;
    return parseFloat(formattedString.replace(/,/g, '')) || 0;
}
