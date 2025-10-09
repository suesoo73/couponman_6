/**
 * Notification utility functions
 * Handles displaying result messages to users
 */

/**
 * Display a result message to the user
 * @param {string} message - Message to display
 * @param {string} type - Type of message ('success' or 'error')
 */
function displayResult(message, type = 'success') {
    console.log('displayResult:', type, message);

    const existingResult = document.getElementById('systemSettingsResult');
    if (existingResult) {
        existingResult.remove();
    }

    const resultDiv = document.createElement('div');
    resultDiv.id = 'systemSettingsResult';
    resultDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        font-weight: bold;
        z-index: 9999;
        max-width: 400px;
        word-wrap: break-word;
        transition: all 0.3s ease;
        ${type === 'error'
            ? 'background: #fee; border: 2px solid #f66; color: #c33;'
            : 'background: #efe; border: 2px solid #6c6; color: #363;'}
    `;

    resultDiv.textContent = message;
    document.body.appendChild(resultDiv);

    setTimeout(() => {
        if (resultDiv && resultDiv.parentNode) {
            resultDiv.style.opacity = '0';
            setTimeout(() => {
                if (resultDiv.parentNode) {
                    resultDiv.parentNode.removeChild(resultDiv);
                }
            }, 300);
        }
    }, 5000);
}
