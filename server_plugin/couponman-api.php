<?php
/**
 * Plugin Name: CouponMan API
 * Description: REST API for CouponMan Android app - coupon registration and management
 * Version: 1.0.0
 * Author: CouponMan
 */

if (!defined('ABSPATH')) {
    exit;
}

// ============================================================
// DB 테이블 생성 (플러그인 활성화 시)
// ============================================================
register_activation_hook(__FILE__, 'couponman_create_tables');

function couponman_create_tables() {
    global $wpdb;
    $charset = $wpdb->get_charset_collate();
    $table = $wpdb->prefix . 'couponman_coupons';

    $sql = "CREATE TABLE IF NOT EXISTS {$table} (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        coupon_code VARCHAR(255) NOT NULL UNIQUE,
        corporate_name VARCHAR(255) DEFAULT '',
        employee_name VARCHAR(255) DEFAULT '',
        employee_phone VARCHAR(50) DEFAULT '',
        cash_balance DECIMAL(15,2) DEFAULT 0,
        point_balance DECIMAL(15,2) DEFAULT 0,
        expire_date VARCHAR(20) DEFAULT '',
        available_days VARCHAR(20) DEFAULT '1111111',
        payment_type VARCHAR(30) DEFAULT 'PREPAID',
        status VARCHAR(30) DEFAULT '사용 가능',
        registered_by BIGINT UNSIGNED DEFAULT 0,
        registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (id),
        KEY idx_coupon_code (coupon_code),
        KEY idx_corporate (corporate_name),
        KEY idx_status (status)
    ) {$charset};";

    require_once ABSPATH . 'wp-admin/includes/upgrade.php';
    dbDelta($sql);
}

// ============================================================
// JWT 인증 헬퍼
// ============================================================
function couponman_get_authenticated_user() {
    if (!function_exists('nusome_jwt_get_token_payload_from_request')) {
        // jwt-auth-nusome 플러그인 없으면 로그인 사용자 확인
        if (is_user_logged_in()) {
            return wp_get_current_user();
        }
        return new WP_Error('couponman_no_auth', '인증 플러그인이 없습니다.', ['status' => 500]);
    }

    $payload = nusome_jwt_get_token_payload_from_request();
    if (is_wp_error($payload)) {
        return $payload;
    }

    $user = get_user_by('id', (int) $payload['data']['user']['id']);
    if (!$user) {
        return new WP_Error('couponman_user_not_found', '사용자를 찾을 수 없습니다.', ['status' => 401]);
    }
    return $user;
}

// ============================================================
// REST API 등록
// ============================================================
add_action('rest_api_init', function () {

    // 쿠폰 등록 (단건)
    register_rest_route('couponman/v1', '/coupons', [
        [
            'methods'             => WP_REST_Server::CREATABLE,
            'callback'            => 'couponman_register_coupon',
            'permission_callback' => '__return_true',
        ],
        [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => 'couponman_get_coupons',
            'permission_callback' => '__return_true',
        ],
    ]);

    // 쿠폰 일괄 등록
    register_rest_route('couponman/v1', '/coupons/bulk', [
        'methods'             => WP_REST_Server::CREATABLE,
        'callback'            => 'couponman_bulk_register_coupons',
        'permission_callback' => '__return_true',
    ]);

    // 특정 쿠폰 조회/수정/삭제
    register_rest_route('couponman/v1', '/coupons/(?P<coupon_code>[A-Za-z0-9\-]+)', [
        [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => 'couponman_get_coupon_by_code',
            'permission_callback' => '__return_true',
        ],
        [
            'methods'             => WP_REST_Server::DELETABLE,
            'callback'            => 'couponman_delete_coupon',
            'permission_callback' => '__return_true',
        ],
    ]);

    // 등록 이력 조회
    register_rest_route('couponman/v1', '/history', [
        'methods'             => WP_REST_Server::READABLE,
        'callback'            => 'couponman_get_history',
        'permission_callback' => '__return_true',
    ]);
});

// ============================================================
// 쿠폰 단건 등록
// ============================================================
function couponman_register_coupon(WP_REST_Request $request) {
    global $wpdb;

    $user = couponman_get_authenticated_user();
    if (is_wp_error($user)) return $user;

    $coupon_code    = sanitize_text_field($request->get_param('couponCode'));
    $corporate_name = sanitize_text_field($request->get_param('corporateName') ?? '');
    $employee_name  = sanitize_text_field($request->get_param('employeeName') ?? '');
    $employee_phone = sanitize_text_field($request->get_param('employeePhone') ?? '');
    $cash_balance   = floatval($request->get_param('cashBalance') ?? 0);
    $point_balance  = floatval($request->get_param('pointBalance') ?? 0);
    $expire_date    = sanitize_text_field($request->get_param('expireDate') ?? '');
    $available_days = sanitize_text_field($request->get_param('availableDays') ?? '1111111');
    $payment_type   = sanitize_text_field($request->get_param('paymentType') ?? 'PREPAID');
    $status         = sanitize_text_field($request->get_param('status') ?? '사용 가능');

    if (empty($coupon_code)) {
        return new WP_Error('couponman_missing_code', '쿠폰 코드는 필수입니다.', ['status' => 400]);
    }

    $table = $wpdb->prefix . 'couponman_coupons';

    // 중복 확인
    $existing = $wpdb->get_var($wpdb->prepare(
        "SELECT id FROM {$table} WHERE coupon_code = %s", $coupon_code
    ));
    if ($existing) {
        return new WP_Error('couponman_duplicate', '이미 등록된 쿠폰 코드입니다: ' . $coupon_code, ['status' => 409]);
    }

    $result = $wpdb->insert($table, [
        'coupon_code'    => $coupon_code,
        'corporate_name' => $corporate_name,
        'employee_name'  => $employee_name,
        'employee_phone' => $employee_phone,
        'cash_balance'   => $cash_balance,
        'point_balance'  => $point_balance,
        'expire_date'    => $expire_date,
        'available_days' => $available_days,
        'payment_type'   => $payment_type,
        'status'         => $status,
        'registered_by'  => $user->ID,
        'registered_at'  => current_time('mysql'),
    ]);

    if ($result === false) {
        return new WP_Error('couponman_db_error', 'DB 저장 실패: ' . $wpdb->last_error, ['status' => 500]);
    }

    return new WP_REST_Response([
        'success'     => true,
        'message'     => '쿠폰이 등록되었습니다.',
        'id'          => $wpdb->insert_id,
        'coupon_code' => $coupon_code,
    ], 201);
}

// ============================================================
// 쿠폰 일괄 등록
// ============================================================
function couponman_bulk_register_coupons(WP_REST_Request $request) {
    global $wpdb;

    $user = couponman_get_authenticated_user();
    if (is_wp_error($user)) return $user;

    $coupons = $request->get_param('coupons');
    if (empty($coupons) || !is_array($coupons)) {
        return new WP_Error('couponman_missing_data', 'coupons 배열이 필요합니다.', ['status' => 400]);
    }

    $table    = $wpdb->prefix . 'couponman_coupons';
    $success  = [];
    $failed   = [];

    foreach ($coupons as $coupon) {
        $coupon_code = sanitize_text_field($coupon['couponCode'] ?? '');
        if (empty($coupon_code)) {
            $failed[] = ['couponCode' => '', 'reason' => '쿠폰 코드 누락'];
            continue;
        }

        // 중복 확인
        $existing = $wpdb->get_var($wpdb->prepare(
            "SELECT id FROM {$table} WHERE coupon_code = %s", $coupon_code
        ));
        if ($existing) {
            $failed[] = ['couponCode' => $coupon_code, 'reason' => '이미 등록된 쿠폰'];
            continue;
        }

        $result = $wpdb->insert($table, [
            'coupon_code'    => $coupon_code,
            'corporate_name' => sanitize_text_field($coupon['corporateName'] ?? ''),
            'employee_name'  => sanitize_text_field($coupon['employeeName'] ?? ''),
            'employee_phone' => sanitize_text_field($coupon['employeePhone'] ?? ''),
            'cash_balance'   => floatval($coupon['cashBalance'] ?? 0),
            'point_balance'  => floatval($coupon['pointBalance'] ?? 0),
            'expire_date'    => sanitize_text_field($coupon['expireDate'] ?? ''),
            'available_days' => sanitize_text_field($coupon['availableDays'] ?? '1111111'),
            'payment_type'   => sanitize_text_field($coupon['paymentType'] ?? 'PREPAID'),
            'status'         => sanitize_text_field($coupon['status'] ?? '사용 가능'),
            'registered_by'  => $user->ID,
            'registered_at'  => current_time('mysql'),
        ]);

        if ($result === false) {
            $failed[] = ['couponCode' => $coupon_code, 'reason' => 'DB 오류: ' . $wpdb->last_error];
        } else {
            $success[] = ['couponCode' => $coupon_code, 'id' => $wpdb->insert_id];
        }
    }

    return new WP_REST_Response([
        'success'       => true,
        'total'         => count($coupons),
        'successCount'  => count($success),
        'failedCount'   => count($failed),
        'successList'   => $success,
        'failedList'    => $failed,
    ], 200);
}

// ============================================================
// 쿠폰 목록 조회
// ============================================================
function couponman_get_coupons(WP_REST_Request $request) {
    global $wpdb;

    $user = couponman_get_authenticated_user();
    if (is_wp_error($user)) return $user;

    $table         = $wpdb->prefix . 'couponman_coupons';
    $corporate     = sanitize_text_field($request->get_param('corporate') ?? '');
    $status        = sanitize_text_field($request->get_param('status') ?? '');
    $per_page      = min(200, max(1, intval($request->get_param('per_page') ?? 50)));
    $page          = max(1, intval($request->get_param('page') ?? 1));
    $offset        = ($page - 1) * $per_page;

    $where  = ['1=1'];
    $params = [];

    if ($corporate) {
        $where[]  = 'corporate_name LIKE %s';
        $params[] = '%' . $wpdb->esc_like($corporate) . '%';
    }
    if ($status) {
        $where[]  = 'status = %s';
        $params[] = $status;
    }

    $where_sql = implode(' AND ', $where);

    $total = $wpdb->get_var($params
        ? $wpdb->prepare("SELECT COUNT(*) FROM {$table} WHERE {$where_sql}", $params)
        : "SELECT COUNT(*) FROM {$table} WHERE {$where_sql}"
    );

    $rows = $params
        ? $wpdb->get_results($wpdb->prepare(
            "SELECT * FROM {$table} WHERE {$where_sql} ORDER BY registered_at DESC LIMIT %d OFFSET %d",
            array_merge($params, [$per_page, $offset])
          ))
        : $wpdb->get_results(
            "SELECT * FROM {$table} WHERE {$where_sql} ORDER BY registered_at DESC LIMIT {$per_page} OFFSET {$offset}"
          );

    return new WP_REST_Response([
        'success'  => true,
        'total'    => (int) $total,
        'page'     => $page,
        'per_page' => $per_page,
        'data'     => $rows,
    ]);
}

// ============================================================
// 쿠폰 코드로 단건 조회
// ============================================================
function couponman_get_coupon_by_code(WP_REST_Request $request) {
    global $wpdb;

    $user = couponman_get_authenticated_user();
    if (is_wp_error($user)) return $user;

    $table       = $wpdb->prefix . 'couponman_coupons';
    $coupon_code = sanitize_text_field($request->get_param('coupon_code'));

    $row = $wpdb->get_row($wpdb->prepare(
        "SELECT * FROM {$table} WHERE coupon_code = %s", $coupon_code
    ));

    if (!$row) {
        return new WP_Error('couponman_not_found', '쿠폰을 찾을 수 없습니다.', ['status' => 404]);
    }

    return new WP_REST_Response(['success' => true, 'data' => $row]);
}

// ============================================================
// 쿠폰 삭제
// ============================================================
function couponman_delete_coupon(WP_REST_Request $request) {
    global $wpdb;

    $user = couponman_get_authenticated_user();
    if (is_wp_error($user)) return $user;

    $table       = $wpdb->prefix . 'couponman_coupons';
    $coupon_code = sanitize_text_field($request->get_param('coupon_code'));

    $result = $wpdb->delete($table, ['coupon_code' => $coupon_code]);
    if (!$result) {
        return new WP_Error('couponman_not_found', '쿠폰을 찾을 수 없거나 삭제 실패.', ['status' => 404]);
    }

    return new WP_REST_Response(['success' => true, 'message' => '쿠폰이 삭제되었습니다.']);
}

// ============================================================
// 등록 이력 (최근 100건)
// ============================================================
function couponman_get_history(WP_REST_Request $request) {
    global $wpdb;

    $user = couponman_get_authenticated_user();
    if (is_wp_error($user)) return $user;

    $table = $wpdb->prefix . 'couponman_coupons';
    $limit = min(200, max(1, intval($request->get_param('limit') ?? 100)));

    $rows = $wpdb->get_results(
        "SELECT coupon_code, corporate_name, employee_name, cash_balance, status, registered_at
         FROM {$table}
         ORDER BY registered_at DESC
         LIMIT {$limit}"
    );

    return new WP_REST_Response([
        'success' => true,
        'count'   => count($rows),
        'data'    => $rows,
    ]);
}
