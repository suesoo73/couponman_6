#!/bin/bash

echo "==================================="
echo "Validating Refactored File Structure"
echo "==================================="
echo ""

errors=0
warnings=0

# Check CSS files
echo "Checking CSS files..."
css_files=("base.css" "layout.css" "navigation.css" "components.css" "tables.css" "modals.css" "animations.css" "responsive.css")
for file in "${css_files[@]}"; do
    if [ -f "assets/css/$file" ]; then
        echo "  ✓ assets/css/$file"
    else
        echo "  ✗ MISSING: assets/css/$file"
        ((errors++))
    fi
done

echo ""
echo "Checking JavaScript Utility files..."
util_files=("formatters.js" "notifications.js" "export.js")
for file in "${util_files[@]}"; do
    if [ -f "assets/js/utils/$file" ]; then
        echo "  ✓ assets/js/utils/$file"
    else
        echo "  ✗ MISSING: assets/js/utils/$file"
        ((errors++))
    fi
done

echo ""
echo "Checking JavaScript Core files..."
core_files=("api.js" "auth.js" "navigation.js" "app.js")
for file in "${core_files[@]}"; do
    if [ -f "assets/js/core/$file" ]; then
        echo "  ✓ assets/js/core/$file"
    else
        echo "  ✗ MISSING: assets/js/core/$file"
        ((errors++))
    fi
done

echo ""
echo "Checking JavaScript Module files..."
module_files=("corporate.js" "employee.js" "system-settings.js" "coupon-issue.js" "coupon-manage.js" "coupon-send.js" "delivery-history.js" "email-config.js" "sms-config.js" "management-info.js" "developer.js")
for file in "${module_files[@]}"; do
    if [ -f "assets/js/modules/$file" ]; then
        echo "  ✓ assets/js/modules/$file"
    else
        echo "  ✗ MISSING: assets/js/modules/$file"
        ((errors++))
    fi
done

echo ""
echo "Checking main HTML file..."
if [ -f "index.html" ]; then
    echo "  ✓ index.html"
    # Check if it has the right structure
    if grep -q "assets/css/base.css" index.html && grep -q "assets/js/core/app.js" index.html; then
        echo "    ✓ Contains correct CSS and JS references"
    else
        echo "    ⚠ WARNING: Missing expected CSS/JS references"
        ((warnings++))
    fi
else
    echo "  ✗ MISSING: index.html"
    ((errors++))
fi

echo ""
echo "Checking original file (should be preserved)..."
if [ -f "index_v2.html" ]; then
    echo "  ✓ index_v2.html (original backup preserved)"
else
    echo "  ⚠ WARNING: index_v2.html not found (original file missing)"
    ((warnings++))
fi

echo ""
echo "==================================="
echo "Validation Complete"
echo "==================================="
echo "Errors: $errors"
echo "Warnings: $warnings"
echo ""

if [ $errors -eq 0 ]; then
    echo "✓ All required files are present!"
    echo "✓ Refactoring completed successfully!"
    exit 0
else
    echo "✗ Some files are missing. Please review the errors above."
    exit 1
fi
