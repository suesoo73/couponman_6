# Developer Guide - Modular Coupon Manager System

## Quick Start

### Running the Application
Simply open `index.html` in a web browser. All CSS and JavaScript files will be loaded automatically.

### File Organization

```
Project Structure:
- index.html           → Main entry point
- assets/
  - css/              → All stylesheets
  - js/
    - utils/          → Utility functions (formatting, notifications, exports)
    - core/           → Core functionality (auth, navigation, app init)
    - modules/        → Business logic modules (corporate, employee, coupon, etc.)
```

## Working with Modules

### Adding a New Module

1. Create a new file in `assets/js/modules/your-module.js`
2. Add JSDoc comments describing the module
3. Add the script reference in `index.html` before `app.js`

Example:
```javascript
/**
 * Your Module Name
 * Description of what this module does
 */

// Your functions here
async function yourFunction() {
    // Implementation
}
```

### Module Dependencies

All modules have access to:
- Global variables: `currentToken`, `currentSection`, `currentCoupons`, `selectedCouponIds`
- Utility functions from `utils/` folder
- Core functions from `core/` folder

### Adding New Functions

#### Utility Functions
Add to appropriate file in `assets/js/utils/`:
- `formatters.js` - Data formatting functions
- `notifications.js` - User notification functions
- `export.js` - Export/download functions

#### Core Functions
Add to appropriate file in `assets/js/core/`:
- `auth.js` - Authentication related
- `navigation.js` - Navigation and routing
- `api.js` - API utilities (currently placeholder)

#### Business Logic
Add to appropriate module in `assets/js/modules/` or create a new module file.

## CSS Organization

### Adding New Styles

Add styles to the appropriate CSS file:
- `base.css` - Global resets and body styles
- `layout.css` - Page layout and structure
- `navigation.css` - Navigation menus and tabs
- `components.css` - Reusable UI components
- `tables.css` - Table styles
- `modals.css` - Modal/dialog styles
- `animations.css` - Animations and transitions
- `responsive.css` - Media queries for responsive design

### Naming Conventions

Follow BEM-like naming:
- `.component-name` - Component
- `.component-name__element` - Element within component
- `.component-name--modifier` - Variant of component

## Common Tasks

### Adding a New API Endpoint

1. Add function to appropriate module file
2. Use the existing pattern:

```javascript
async function yourApiCall() {
    if (!currentToken) {
        alert('먼저 로그인해주세요.');
        return;
    }

    const serverUrl = document.getElementById('serverUrl').value;

    try {
        const response = await fetch(`${serverUrl}/api/your-endpoint`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const data = await response.json();

        if (data.success) {
            // Handle success
        } else {
            // Handle error
        }
    } catch (error) {
        alert('오류: ' + error.message);
    }
}
```

### Adding a New Section

1. Add HTML for the section in `index.html`:
```html
<div id="yourSection" class="content-section">
    <div class="section-header">
        <span class="icon">🎯</span>
        <h2>Your Section Title</h2>
    </div>
    <!-- Your content here -->
</div>
```

2. Add navigation item in dropdown menu:
```html
<a class="dropdown-item" onclick="showSection('yourSection')">
    <span class="icon">🎯</span>Your Menu Item
</a>
```

3. Add initialization logic in `navigation.js` if needed:
```javascript
else if (sectionName === 'yourSection') {
    console.log('Your section activated');
    // Your initialization code
}
```

### Adding Export Functionality

Add to `assets/js/utils/export.js`:

```javascript
/**
 * Export your data
 */
function exportYourData() {
    // Create CSV data
    const headers = ['Column1', 'Column2', 'Column3'];
    const csvData = yourData.map(item => [
        item.field1,
        item.field2,
        item.field3
    ]);

    // Generate CSV
    const csvContent = [headers, ...csvData]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n');

    // Download
    const bom = '\uFEFF';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `your-data_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
}
```

## Debugging

### Console Logging
Many functions include console.log statements. Check browser console for:
- API request/response details
- Authentication status
- Section navigation events
- Data loading events

### Common Issues

1. **Functions not found**: Check that the script is loaded in correct order in `index.html`
2. **Styles not applying**: Check CSS file load order and specificity
3. **API calls failing**: Verify `currentToken` is set and server URL is correct
4. **Section not showing**: Check that section ID matches in HTML and JavaScript

## Testing Checklist

After making changes, test:

- [ ] Login/logout functionality
- [ ] All navigation menu items
- [ ] CRUD operations in each section
- [ ] Export functions
- [ ] Developer mode access
- [ ] Responsive design on mobile
- [ ] Browser console for errors

## Performance Tips

1. **Lazy Loading**: Consider loading large modules only when needed
2. **Caching**: Browser will cache individual CSS/JS files
3. **Minification**: Use build tools to minify for production
4. **Code Splitting**: Further split large modules if needed

## Security Notes

1. **Token Storage**: Currently uses localStorage - consider security implications
2. **Developer Credentials**: Base64 encoding is NOT encryption - update for production
3. **Input Validation**: Always validate user inputs
4. **XSS Prevention**: Be careful with innerHTML - sanitize data

## Getting Help

- Check `REFACTORING_SUMMARY.md` for overview
- See `FILE_STRUCTURE.txt` for file locations
- Run `validate_structure.sh` to verify all files are present
- Review original `index_v2.html` for reference

## Future Enhancements

Consider:
1. TypeScript migration for type safety
2. Build process (Webpack/Vite) for optimization
3. Framework migration (React/Vue/Svelte)
4. API centralization in `api.js`
5. State management system
6. Unit tests for critical functions
7. E2E tests for user flows
