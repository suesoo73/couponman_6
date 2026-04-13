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
// 관리자 메뉴 등록
// ============================================================
add_action('admin_menu', function () {
    add_menu_page(
        '쿠폰 관리',
        '🎫 쿠폰 관리',
        'manage_options',
        'couponman',
        'couponman_admin_page',
        'dashicons-tickets-alt',
        30
    );
    add_submenu_page('couponman', '등록된 쿠폰', '등록된 쿠폰', 'manage_options', 'couponman', 'couponman_admin_page');
    add_submenu_page('couponman', '등록 이력', '등록 이력', 'manage_options', 'couponman-history', 'couponman_admin_history_page');
});

// ============================================================
// 관리자 페이지: 등록된 쿠폰 목록
// ============================================================
function couponman_admin_page() {
    global $wpdb;
    $table = $wpdb->prefix . 'couponman_coupons';

    // 삭제 처리
    if (isset($_POST['couponman_delete']) && check_admin_referer('couponman_delete_nonce')) {
        $id = intval($_POST['couponman_delete']);
        $wpdb->delete($table, ['id' => $id]);
        echo '<div class="notice notice-success"><p>쿠폰이 삭제되었습니다.</p></div>';
    }

    // 검색/필터
    $search    = sanitize_text_field($_GET['s'] ?? '');
    $filter_status = sanitize_text_field($_GET['status'] ?? '');
    $per_page  = 20;
    $page      = max(1, intval($_GET['paged'] ?? 1));
    $offset    = ($page - 1) * $per_page;

    $where  = ['1=1'];
    $params = [];
    if ($search) {
        $where[]  = '(coupon_code LIKE %s OR corporate_name LIKE %s OR employee_name LIKE %s)';
        $like = '%' . $wpdb->esc_like($search) . '%';
        $params = array_merge($params, [$like, $like, $like]);
    }
    if ($filter_status) {
        $where[]  = 'status = %s';
        $params[] = $filter_status;
    }
    $where_sql = implode(' AND ', $where);

    $total = $params
        ? $wpdb->get_var($wpdb->prepare("SELECT COUNT(*) FROM {$table} WHERE {$where_sql}", $params))
        : $wpdb->get_var("SELECT COUNT(*) FROM {$table} WHERE {$where_sql}");

    $rows = $params
        ? $wpdb->get_results($wpdb->prepare(
            "SELECT * FROM {$table} WHERE {$where_sql} ORDER BY registered_at DESC LIMIT %d OFFSET %d",
            array_merge($params, [$per_page, $offset])
          ))
        : $wpdb->get_results("SELECT * FROM {$table} WHERE {$where_sql} ORDER BY registered_at DESC LIMIT {$per_page} OFFSET {$offset}");

    $total_pages = ceil($total / $per_page);
    $base_url = admin_url('admin.php?page=couponman');

    // 상태별 건수
    $stats = $wpdb->get_results("SELECT status, COUNT(*) as cnt FROM {$table} GROUP BY status");
    $stat_map = [];
    foreach ($stats as $s) $stat_map[$s->status] = $s->cnt;

    ?>
    <div class="wrap">
        <h1 class="wp-heading-inline">🎫 등록된 쿠폰</h1>
        <hr class="wp-header-end">

        <!-- 통계 -->
        <div style="display:flex;gap:16px;margin:16px 0;">
            <?php
            $all_count = array_sum(array_column($stats, 'cnt'));
            $stat_cards = [
                ['label'=>'전체', 'count'=>$all_count, 'color'=>'#667eea'],
                ['label'=>'사용 가능', 'count'=>$stat_map['사용 가능'] ?? 0, 'color'=>'#48bb78'],
                ['label'=>'사용됨', 'count'=>$stat_map['사용됨'] ?? 0, 'color'=>'#ed8936'],
                ['label'=>'만료됨', 'count'=>$stat_map['만료됨'] ?? 0, 'color'=>'#e53e3e'],
            ];
            foreach ($stat_cards as $sc): ?>
            <div style="background:white;border:1px solid #e0e0e0;border-radius:8px;padding:14px 24px;text-align:center;min-width:100px;border-top:4px solid <?= $sc['color'] ?>;">
                <div style="font-size:24px;font-weight:700;color:<?= $sc['color'] ?>;"><?= $sc['count'] ?></div>
                <div style="font-size:12px;color:#666;"><?= $sc['label'] ?></div>
            </div>
            <?php endforeach; ?>
        </div>

        <!-- 검색 폼 -->
        <form method="get" style="margin-bottom:16px;display:flex;gap:8px;align-items:center;">
            <input type="hidden" name="page" value="couponman">
            <input type="text" name="s" value="<?= esc_attr($search) ?>" placeholder="쿠폰코드, 거래처, 직원명 검색..."
                style="padding:8px 12px;border:1px solid #ccc;border-radius:4px;width:280px;">
            <select name="status" style="padding:8px;border:1px solid #ccc;border-radius:4px;">
                <option value="">전체 상태</option>
                <?php foreach (['사용 가능','사용됨','만료됨','일시 중지','해지됨'] as $st): ?>
                <option value="<?= $st ?>" <?= $filter_status === $st ? 'selected' : '' ?>><?= $st ?></option>
                <?php endforeach; ?>
            </select>
            <button type="submit" class="button">🔍 검색</button>
            <?php if ($search || $filter_status): ?>
            <a href="<?= $base_url ?>" class="button">초기화</a>
            <?php endif; ?>
        </form>

        <p style="color:#666;font-size:13px;">총 <strong><?= number_format($total) ?></strong>건 (현재 페이지: <?= $page ?>/<?= max(1,$total_pages) ?>)</p>

        <!-- 쿠폰 테이블 -->
        <table class="wp-list-table widefat fixed striped">
            <thead>
                <tr>
                    <th style="width:40px;">#</th>
                    <th>쿠폰 코드</th>
                    <th style="width:130px;">거래처</th>
                    <th style="width:90px;">직원명</th>
                    <th style="width:120px;">전화번호</th>
                    <th style="width:100px;text-align:right;">현금잔고</th>
                    <th style="width:90px;text-align:center;">만료일</th>
                    <th style="width:80px;text-align:center;">상태</th>
                    <th style="width:140px;text-align:center;">등록일시</th>
                    <th style="width:60px;text-align:center;">삭제</th>
                </tr>
            </thead>
            <tbody>
            <?php if (empty($rows)): ?>
                <tr><td colspan="10" style="text-align:center;padding:30px;color:#888;">등록된 쿠폰이 없습니다.</td></tr>
            <?php else: foreach ($rows as $r):
                $status_colors = ['사용 가능'=>'#d4edda;color:#155724','사용됨'=>'#fff3cd;color:#856404','만료됨'=>'#f8d7da;color:#721c24'];
                $sc = $status_colors[$r->status] ?? '#e2e3e5;color:#495057';
            ?>
                <tr>
                    <td><?= $r->id ?></td>
                    <td style="font-family:monospace;font-size:11px;"><?= esc_html($r->coupon_code) ?></td>
                    <td><?= esc_html($r->corporate_name) ?></td>
                    <td><?= esc_html($r->employee_name) ?></td>
                    <td><?= esc_html($r->employee_phone) ?></td>
                    <td style="text-align:right;"><?= number_format($r->cash_balance) ?>원</td>
                    <td style="text-align:center;"><?= esc_html($r->expire_date) ?></td>
                    <td style="text-align:center;">
                        <span style="padding:2px 8px;border-radius:10px;font-size:11px;background:<?= $sc ?>;">
                            <?= esc_html($r->status) ?>
                        </span>
                    </td>
                    <td style="text-align:center;font-size:12px;"><?= esc_html($r->registered_at) ?></td>
                    <td style="text-align:center;">
                        <form method="post" onsubmit="return confirm('이 쿠폰을 삭제하시겠습니까?');">
                            <?php wp_nonce_field('couponman_delete_nonce'); ?>
                            <input type="hidden" name="couponman_delete" value="<?= $r->id ?>">
                            <button type="submit" class="button button-small" style="color:#e53e3e;border-color:#e53e3e;">삭제</button>
                        </form>
                    </td>
                </tr>
            <?php endforeach; endif; ?>
            </tbody>
        </table>

        <!-- 페이지네이션 -->
        <?php if ($total_pages > 1): ?>
        <div style="margin-top:16px;display:flex;gap:4px;align-items:center;">
            <?php if ($page > 1): ?>
            <a href="<?= $base_url ?>&paged=<?= $page-1 ?>&s=<?= urlencode($search) ?>&status=<?= urlencode($filter_status) ?>" class="button">‹ 이전</a>
            <?php endif; ?>
            <?php for ($i = max(1,$page-3); $i <= min($total_pages,$page+3); $i++): ?>
            <a href="<?= $base_url ?>&paged=<?= $i ?>&s=<?= urlencode($search) ?>&status=<?= urlencode($filter_status) ?>"
               class="button <?= $i === $page ? 'button-primary' : '' ?>"><?= $i ?></a>
            <?php endfor; ?>
            <?php if ($page < $total_pages): ?>
            <a href="<?= $base_url ?>&paged=<?= $page+1 ?>&s=<?= urlencode($search) ?>&status=<?= urlencode($filter_status) ?>" class="button">다음 ›</a>
            <?php endif; ?>
        </div>
        <?php endif; ?>
    </div>
    <?php
}

// ============================================================
// 관리자 페이지: 등록 이력 (최근 200건)
// ============================================================
function couponman_admin_history_page() {
    global $wpdb;
    $table = $wpdb->prefix . 'couponman_coupons';
    $rows  = $wpdb->get_results("SELECT coupon_code, corporate_name, employee_name, cash_balance, status, registered_at FROM {$table} ORDER BY registered_at DESC LIMIT 200");
    ?>
    <div class="wrap">
        <h1>📜 등록 이력 (최근 200건)</h1>
        <table class="wp-list-table widefat fixed striped" style="margin-top:16px;">
            <thead>
                <tr>
                    <th>쿠폰 코드</th>
                    <th style="width:130px;">거래처</th>
                    <th style="width:90px;">직원명</th>
                    <th style="width:110px;text-align:right;">금액</th>
                    <th style="width:80px;text-align:center;">상태</th>
                    <th style="width:150px;text-align:center;">등록일시</th>
                </tr>
            </thead>
            <tbody>
            <?php if (empty($rows)): ?>
                <tr><td colspan="6" style="text-align:center;padding:30px;color:#888;">이력이 없습니다.</td></tr>
            <?php else: foreach ($rows as $r): ?>
                <tr>
                    <td style="font-family:monospace;font-size:11px;"><?= esc_html($r->coupon_code) ?></td>
                    <td><?= esc_html($r->corporate_name) ?></td>
                    <td><?= esc_html($r->employee_name) ?></td>
                    <td style="text-align:right;"><?= number_format($r->cash_balance) ?>원</td>
                    <td style="text-align:center;font-size:11px;"><?= esc_html($r->status) ?></td>
                    <td style="text-align:center;font-size:12px;"><?= esc_html($r->registered_at) ?></td>
                </tr>
            <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
    <?php
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
        corporate_id BIGINT UNSIGNED DEFAULT 0,
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
        KEY idx_corporate_id (corporate_id),
        KEY idx_corporate (corporate_name),
        KEY idx_status (status)
    ) {$charset};";

    require_once ABSPATH . 'wp-admin/includes/upgrade.php';
    dbDelta($sql);

    // 기존 설치본에 corporate_id 컬럼이 없으면 추가
    $columns = $wpdb->get_col("SHOW COLUMNS FROM {$table} LIKE 'corporate_id'");
    if (empty($columns)) {
        $wpdb->query("ALTER TABLE {$table} ADD COLUMN corporate_id BIGINT UNSIGNED DEFAULT 0 AFTER coupon_code");
        $wpdb->query("ALTER TABLE {$table} ADD KEY idx_corporate_id (corporate_id)");
    }
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
    $corporate_id   = intval($request->get_param('corporateId') ?? 0);
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
        'corporate_id'   => $corporate_id,
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
            'corporate_id'   => intval($coupon['corporateId'] ?? 0),
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
