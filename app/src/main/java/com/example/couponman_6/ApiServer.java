package com.example.couponman_6;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 네트워크 관련 imports
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.util.Base64;

// 파일 I/O imports
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class ApiServer extends NanoHTTPD {

    private static final String TAG = "ApiServer";
    private Gson gson;
    private List<Map<String, Object>> coupons;
    private Map<String, String> activeTokens;
    private Context context;
    private SharedPreferences sharedPreferences;
    private CorporateDAO corporateDAO;
    private EmployeeDAO employeeDAO;
    private CouponDAO couponDAO;
    private CouponDeliveryDAO couponDeliveryDAO;

    public ApiServer(int port, Context context) {
        super(port);
        this.context = context;
        gson = new Gson();
        activeTokens = new HashMap<>();
        sharedPreferences = context.getSharedPreferences("AdminSettings", Context.MODE_PRIVATE);
        corporateDAO = new CorporateDAO(context);
        corporateDAO.open();
        employeeDAO = new EmployeeDAO(context);
        employeeDAO.open();
        couponDAO = new CouponDAO(context);
        couponDAO.open();
        couponDeliveryDAO = new CouponDeliveryDAO(context);
        couponDeliveryDAO.open();
        initializeSampleData();
    }

    @Override
    public void stop() {
        super.stop();
        if (corporateDAO != null) {
            corporateDAO.close();
        }
        if (employeeDAO != null) {
            employeeDAO.close();
        }
        if (couponDAO != null) {
            couponDAO.close();
        }
        if (couponDeliveryDAO != null) {
            couponDeliveryDAO.close();
        }
        Log.i(TAG, "API Server stopped and database connection closed");
    }

    private void initializeSampleData() {
        coupons = new ArrayList<>();
        
        Map<String, Object> coupon1 = new HashMap<>();
        coupon1.put("id", "COUPON001");
        coupon1.put("name", "10% 할인 쿠폰");
        coupon1.put("discount", 10);
        coupon1.put("type", "percentage");
        coupon1.put("isUsed", false);
        coupons.add(coupon1);

        Map<String, Object> coupon2 = new HashMap<>();
        coupon2.put("id", "COUPON002");
        coupon2.put("name", "5000원 할인 쿠폰");
        coupon2.put("discount", 5000);
        coupon2.put("type", "amount");
        coupon2.put("isUsed", false);
        coupons.add(coupon2);

        Map<String, Object> coupon3 = new HashMap<>();
        coupon3.put("id", "COUPON003");
        coupon3.put("name", "무료배송 쿠폰");
        coupon3.put("discount", 0);
        coupon3.put("type", "shipping");
        coupon3.put("isUsed", true);
        coupons.add(coupon3);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        String clientIP = session.getHeaders().get("remote-addr");
        
        // 요청 로그 출력
        Log.i(TAG, String.format("API Request: %s %s from %s", method, uri, clientIP));
        Log.i(TAG, "Headers: " + session.getHeaders().toString());

        Response response = newFixedLengthResponse(Response.Status.NOT_FOUND, 
                MIME_PLAINTEXT, "404 Not Found");
        
        // CORS 헤더 추가
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        response.addHeader("Access-Control-Max-Age", "3600");

        if (Method.OPTIONS.equals(method)) {
            Log.i(TAG, "CORS Preflight request handled for: " + uri);
            Response optionsResponse = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");
            optionsResponse.addHeader("Access-Control-Allow-Origin", "*");
            optionsResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            optionsResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            return optionsResponse;
        }

        try {
            switch (uri) {
                case "/":
                case "/api":
                    response = handleApiInfo();
                    break;
                case "/api/login":
                    if (Method.POST.equals(method)) {
                        response = handleLogin(session);
                    }
                    break;
                case "/api/coupons":
                    if (!isAuthorized(session)) {
                        response = createUnauthorizedResponse();
                    } else if (Method.GET.equals(method)) {
                        response = handleGetCoupons();
                    } else if (Method.POST.equals(method)) {
                        response = handleCreateCoupon(session);
                    }
                    break;
                case "/api/coupons/validate":
                    if (!isAuthorized(session)) {
                        response = createUnauthorizedResponse();
                    } else if (Method.POST.equals(method)) {
                        response = handleValidateCoupon(session);
                    }
                    break;
                case "/api/server/status":
                    response = handleServerStatus();
                    break;
                case "/api/corporates":
                    if (!isAuthorized(session)) {
                        response = createUnauthorizedResponse();
                    } else if (Method.GET.equals(method)) {
                        response = handleGetCorporates(session);
                    } else if (Method.POST.equals(method)) {
                        response = handleCreateCorporate(session);
                    }
                    break;
                default:
                    if (uri.startsWith("/api/coupons/")) {
                        if (!isAuthorized(session)) {
                            response = createUnauthorizedResponse();
                        } else {
                            String couponId = uri.substring("/api/coupons/".length());
                            if (Method.GET.equals(method)) {
                                response = handleGetCoupon(couponId);
                            } else if (Method.PUT.equals(method)) {
                                response = handleUpdateCoupon(couponId, session);
                            }
                        }
                    } else if (uri.startsWith("/api/corporates/")) {
                        if (!isAuthorized(session)) {
                            response = createUnauthorizedResponse();
                        } else {
                            String path = uri.substring("/api/corporates/".length());
                            if (path.equals("search")) {
                                response = handleSearchCorporates(session);
                            } else if (path.contains("/employees")) {
                                // 거래처별 직원 관련 API
                                String[] parts = path.split("/");
                                if (parts.length >= 2) {
                                    String corporateId = parts[0];
                                    if (Method.GET.equals(method)) {
                                        response = handleGetEmployeesByCorporate(corporateId);
                                    }
                                }
                            } else if (path.contains("/coupons")) {
                                // 거래처별 쿠폰 관련 API
                                String[] parts = path.split("/");
                                if (parts.length >= 2) {
                                    String corporateId = parts[0];
                                    if (Method.GET.equals(method)) {
                                        response = handleGetCouponsByCorporate(corporateId);
                                    }
                                }
                            } else {
                                String corporateId = path;
                                if (Method.GET.equals(method)) {
                                    response = handleGetCorporate(corporateId);
                                } else if (Method.PUT.equals(method)) {
                                    response = handleUpdateCorporate(corporateId, session);
                                } else if (Method.DELETE.equals(method)) {
                                    response = handleDeleteCorporate(corporateId);
                                }
                            }
                        }
                    } else if (uri.startsWith("/api/employees")) {
                        if (!isAuthorized(session)) {
                            response = createUnauthorizedResponse();
                        } else if (uri.equals("/api/employees")) {
                            if (Method.GET.equals(method)) {
                                response = handleGetEmployees();
                            } else if (Method.POST.equals(method)) {
                                response = handleCreateEmployee(session);
                            }
                        } else if (uri.startsWith("/api/employees/")) {
                            String employeeId = uri.substring("/api/employees/".length());
                            if (Method.GET.equals(method)) {
                                response = handleGetEmployee(employeeId);
                            } else if (Method.PUT.equals(method)) {
                                response = handleUpdateEmployee(employeeId, session);
                            } else if (Method.DELETE.equals(method)) {
                                response = handleDeleteEmployee(employeeId);
                            }
                        }
                    } else if (uri.startsWith("/api/email-config")) {
                        if (!isAuthorized(session)) {
                            response = createUnauthorizedResponse();
                        } else if (uri.equals("/api/email-config")) {
                            if (Method.GET.equals(method)) {
                                response = handleGetEmailConfig();
                            } else if (Method.POST.equals(method)) {
                                response = handleSaveEmailConfig(session);
                            }
                        } else if (uri.equals("/api/email-config/test")) {
                            if (Method.POST.equals(method)) {
                                response = handleTestEmailConnection(session);
                            }
                        }
                    } else if (uri.startsWith("/api/sms-config")) {
                        if (!isAuthorized(session)) {
                            response = createUnauthorizedResponse();
                        } else if (uri.equals("/api/sms-config")) {
                            if (Method.GET.equals(method)) {
                                response = handleGetSmsConfig();
                            } else if (Method.POST.equals(method)) {
                                response = handleSaveSmsConfig(session);
                            }
                        } else if (uri.equals("/api/sms-config/test")) {
                            if (Method.POST.equals(method)) {
                                response = handleTestSmsConnection(session);
                            }
                        }
                    } else if (uri.startsWith("/api/coupon-send")) {
                        if (!isAuthorized(session)) {
                            response = createUnauthorizedResponse();
                        } else if (uri.equals("/api/coupon-send/email")) {
                            if (Method.POST.equals(method)) {
                                response = handleSendCouponEmail(session);
                            }
                        } else if (uri.equals("/api/coupon-send/sms")) {
                            if (Method.POST.equals(method)) {
                                response = handleSendCouponSMS(session);
                            }
                        } else if (uri.equals("/api/coupon-send/kakao")) {
                            if (Method.POST.equals(method)) {
                                response = handleSendCouponKakao(session);
                            }
                        } else if (uri.startsWith("/api/coupon-send/")) {
                            String path = uri.substring("/api/coupon-send/".length());
                            if (path.equals("history")) {
                                if (Method.GET.equals(method)) {
                                    response = handleGetDeliveryHistory(session);
                                }
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing request: " + uri, e);
            response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                    MIME_PLAINTEXT, "Internal Server Error: " + e.getMessage());
        }

        // 모든 응답에 CORS 헤더 추가
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        
        // 응답 로그
        Log.i(TAG, String.format("API Response: %s %s -> %s", method, uri, response.getStatus()));
        
        return response;
    }

    private Response handleApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "쿠폰 매니저 API 서버");
        info.put("version", "2.0.0");
        info.put("status", "running");
        info.put("authentication", "Bearer Token Required");
        info.put("endpoints", new String[]{
                "POST /api/login - 로그인 (userId, password 필요)",
                "GET /api/coupons - 모든 쿠폰 조회 (인증 필요)",
                "GET /api/coupons/{id} - 특정 쿠폰 조회 (인증 필요)",
                "POST /api/coupons - 새 쿠폰 생성 (인증 필요)",
                "PUT /api/coupons/{id} - 쿠폰 업데이트 (인증 필요)",
                "POST /api/coupons/validate - 쿠폰 검증 (인증 필요)",
                "GET /api/corporates - 모든 거래처 조회 (인증 필요)",
                "GET /api/corporates/{id} - 특정 거래처 조회 (인증 필요)",
                "POST /api/corporates - 새 거래처 생성 (인증 필요)",
                "PUT /api/corporates/{id} - 거래처 업데이트 (인증 필요)",
                "DELETE /api/corporates/{id} - 거래처 삭제 (인증 필요)",
                "GET /api/corporates/search?name={name} - 거래처 이름으로 검색 (인증 필요)",
                "GET /api/corporates/{id}/employees - 거래처별 직원 목록 조회 (인증 필요)",
                "GET /api/corporates/{id}/coupons - 거래처별 쿠폰 목록 조회 (인증 필요)",
                "GET /api/employees - 모든 직원 조회 (인증 필요)",
                "GET /api/employees/{id} - 특정 직원 조회 (인증 필요)",
                "POST /api/employees - 새 직원 생성 (인증 필요)",
                "PUT /api/employees/{id} - 직원 업데이트 (인증 필요)",
                "DELETE /api/employees/{id} - 직원 삭제 (인증 필요)",
                "GET /api/email-config - 이메일 설정 조회 (인증 필요)",
                "POST /api/email-config - 이메일 설정 저장 (인증 필요)",
                "POST /api/email-config/test - 이메일 연결 테스트 (인증 필요)",
                "POST /api/coupon-send/email - 쿠폰 이메일 발송 (인증 필요)",
                "POST /api/coupon-send/sms - 쿠폰 SMS 발송 (인증 필요)",
                "POST /api/coupon-send/kakao - 쿠폰 카카오톡 발송 (인증 필요)",
                "GET /api/coupon-send/history - 발송 기록 조회 (인증 필요)",
                "GET /api/server/status - 서버 상태 조회"
        });

        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(info));
    }

    private Response handleGetCoupons() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", coupons);
        result.put("count", coupons.size());

        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
    }

    private Response handleGetCoupon(String couponId) {
        for (Map<String, Object> coupon : coupons) {
            if (couponId.equals(coupon.get("id"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", coupon);
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            }
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "쿠폰을 찾을 수 없습니다: " + couponId);
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
    }

    private Response handleValidateCoupon(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            Map<String, Object> request = gson.fromJson(postData, Map.class);
            String couponId = (String) request.get("couponId");

            for (Map<String, Object> coupon : coupons) {
                if (couponId.equals(coupon.get("id"))) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("valid", !(Boolean) coupon.get("isUsed"));
                    result.put("coupon", coupon);
                    return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("valid", false);
            error.put("message", "존재하지 않는 쿠폰입니다");
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(error));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "요청 처리 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleCreateCoupon(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            Map<String, Object> newCoupon = gson.fromJson(postData, Map.class);
            newCoupon.put("isUsed", false);
            coupons.add(newCoupon);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", newCoupon);
            result.put("message", "쿠폰이 생성되었습니다");

            return newFixedLengthResponse(Response.Status.CREATED, "application/json; charset=utf-8", gson.toJson(result));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰 생성 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleUpdateCoupon(String couponId, IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            Map<String, Object> updateData = gson.fromJson(postData, Map.class);

            for (Map<String, Object> coupon : coupons) {
                if (couponId.equals(coupon.get("id"))) {
                    coupon.putAll(updateData);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("data", coupon);
                    result.put("message", "쿠폰이 업데이트되었습니다");
                    return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰을 찾을 수 없습니다: " + couponId);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰 업데이트 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("uptime", System.currentTimeMillis());
        status.put("couponsCount", coupons.size());
        status.put("memory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(status));
    }

    private Response handleLogin(IHTTPSession session) {
        Log.i(TAG, "Login request received");
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            Log.i(TAG, "Login request body: " + (postData != null ? postData : "null"));
            
            if (postData == null || postData.trim().isEmpty()) {
                Log.w(TAG, "Empty login request body");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> request = gson.fromJson(postData, Map.class);
            String userId = (String) request.get("userId");
            String password = (String) request.get("password");

            Log.i(TAG, "Login attempt for user: " + userId);

            String savedUserId = sharedPreferences.getString("admin_user_id", "admin");
            String savedPassword = sharedPreferences.getString("admin_password", "admin123");
            
            Log.i(TAG, "Saved credentials - ID: " + savedUserId);

            if (savedUserId.equals(userId) && savedPassword.equals(password)) {
                String token = generateAccessToken();
                activeTokens.put(token, userId);
                
                Log.i(TAG, "Login successful for user: " + userId + ", token: " + token.substring(0, 10) + "...");
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("accessToken", token);
                result.put("message", "로그인 성공");
                result.put("expiresIn", "3600");
                
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Log.w(TAG, "Login failed for user: " + userId + " (invalid credentials)");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "아이디 또는 패스워드가 올바르지 않습니다");
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json; charset=utf-8", gson.toJson(error));
            }
        } catch (Exception e) {
            Log.e(TAG, "Login processing error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private boolean isAuthorized(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String authHeader = headers.get("authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        
        String token = authHeader.substring(7);
        return activeTokens.containsKey(token);
    }

    private Response createUnauthorizedResponse() {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "인증이 필요합니다. 먼저 /api/login 으로 로그인하세요.");
        error.put("error", "UNAUTHORIZED");
        
        Response response = newFixedLengthResponse(Response.Status.UNAUTHORIZED, 
                "application/json; charset=utf-8", gson.toJson(error));
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    /**
     * HTTP 요청의 body에서 JSON 데이터를 추출하는 유틸리티 메소드
     */
    private String extractRequestBody(IHTTPSession session) {
        try {
            // HTTP 메소드 및 헤더 확인
            Method method = session.getMethod();
            Map<String, String> headers = session.getHeaders();
            String contentType = headers.get("content-type");
            
            Log.i(TAG, "Request method: " + method);
            Log.i(TAG, "Content-Type: " + contentType);
            
            // JSON Content-Type 감지 로깅
            if (contentType != null && contentType.toLowerCase().contains("application/json; charset=utf-8")) {
                Log.i(TAG, "Detected JSON content type: " + contentType);
            }
            
            // 방법 1: NanoHTTPD의 기본 parseBody 방식
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            
            // 모든 body 내용 로깅
            Log.i(TAG, "Body contents: " + body.toString());
            
            // postData 키 확인
            if (body.containsKey("postData")) {
                String postData = body.get("postData");
                Log.i(TAG, "Found postData: " + (postData != null ? postData.length() + " chars" : "null"));
                if (postData != null) {
                    Log.i(TAG, "postData content: [" + postData + "]");
                    return postData;
                }
            }
            
            // JSON 형태의 데이터 찾기
            for (Map.Entry<String, String> entry : body.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Log.i(TAG, "Body entry - Key: [" + key + "], Value: [" + (value != null ? value : "null") + "]");
                
                // JSON 형태의 데이터인지 확인
                if (value != null && value.trim().startsWith("{") && value.trim().endsWith("}")) {
                    Log.i(TAG, "Found JSON-like data in key: " + key);
                    return value;
                }
                
                // 키 자체가 JSON일 수 있음 (form-encoded의 경우)
                if (key != null && key.trim().startsWith("{") && key.trim().endsWith("}")) {
                    Log.i(TAG, "Found JSON data in key itself: " + key);
                    return key;
                }
            }
            
            // 방법 2: 모든 body 키에서 파일 경로 확인 및 처리
            for (Map.Entry<String, String> entry : body.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                Log.i(TAG, "Checking entry - Key: [" + key + "], Value: [" + (value != null ? value : "null") + "]");
                
                // 값이 파일 경로인지 확인
                if (value != null && (value.startsWith("/") || value.contains("NanoHTTPD") || value.contains("cache"))) {
                    Log.i(TAG, "Value appears to be a file path, attempting to read: " + value);
                    
                    try {
                        File tempFile = new File(value);
                        if (tempFile.exists() && tempFile.canRead()) {
                            Log.i(TAG, "File exists and is readable: " + value);
                            
                            StringBuilder fileContent = new StringBuilder();
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(tempFile), "UTF-8"))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    fileContent.append(line).append("\n");
                                }
                            }
                            
                            String fileData = fileContent.toString().trim();
                            Log.i(TAG, "File content read successfully: [" + fileData + "]");
                            
                            // JSON 형태인지 확인
                            if (fileData.startsWith("{") && fileData.endsWith("}")) {
                                Log.i(TAG, "File contains valid JSON data");
                                return fileData;
                            }
                            
                            // URL 디코딩 시도
                            try {
                                String decoded = java.net.URLDecoder.decode(fileData, "UTF-8");
                                Log.i(TAG, "URL decoded file content: [" + decoded + "]");
                                
                                if (decoded.trim().startsWith("{") && decoded.trim().endsWith("}")) {
                                    return decoded.trim();
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "URL decode failed for file content: " + e.getMessage());
                            }
                            
                            return fileData;
                        } else {
                            Log.w(TAG, "File does not exist or cannot be read: " + value + 
                                      " (exists: " + tempFile.exists() + ", canRead: " + tempFile.canRead() + ")");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading file: " + value, e);
                    }
                }
                
                // 키 자체가 JSON인지 확인
                if (key != null && key.trim().startsWith("{") && key.trim().endsWith("}")) {
                    Log.i(TAG, "Found JSON data in key: " + key);
                    return key.trim();
                }
                
                // 값이 직접적인 JSON인지 확인
                if (value != null && value.trim().startsWith("{") && value.trim().endsWith("}")) {
                    Log.i(TAG, "Found JSON data in value: " + value);
                    return value.trim();
                }
                
                // URL 디코딩 시도
                if (value != null && !value.isEmpty()) {
                    try {
                        String decoded = java.net.URLDecoder.decode(value, "UTF-8");
                        Log.i(TAG, "URL decoded value: [" + decoded + "]");
                        
                        if (decoded.trim().startsWith("{") && decoded.trim().endsWith("}")) {
                            return decoded.trim();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "URL decode failed for value: " + e.getMessage());
                    }
                }
            }
            
            // 방법 3: 다른 키들 검사
            if (!body.isEmpty()) {
                String firstKey = body.keySet().iterator().next();
                String firstValue = body.get(firstKey);
                
                Log.i(TAG, "Using first body entry - Key: [" + firstKey + "], Value: [" + firstValue + "]");
                
                // 값 우선 확인
                if (firstValue != null && !firstValue.trim().isEmpty()) {
                    return firstValue;
                }
                
                // 키 확인
                if (firstKey != null && !firstKey.trim().isEmpty()) {
                    return firstKey;
                }
            }
            
            Log.w(TAG, "No body data found");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting request body", e);
            return null;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedPassword = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedPassword) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    // Corporate Management Handlers

    private Response handleGetCorporates(IHTTPSession session) {
        try {
            Log.i(TAG, "Getting all corporates");
            List<Corporate> corporates = corporateDAO.getAllCorporates();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", corporates);
            result.put("count", corporates.size());
            
            Log.i(TAG, "Retrieved " + corporates.size() + " corporates");
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
        } catch (Exception e) {
            Log.e(TAG, "Error getting corporates", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 조회 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleGetCorporate(String corporateId) {
        try {
            int id = Integer.parseInt(corporateId);
            Corporate corporate = corporateDAO.getCorporateById(id);
            
            if (corporate != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", corporate);
                
                Log.i(TAG, "Retrieved corporate: " + corporate.getName());
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처를 찾을 수 없습니다: " + corporateId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error getting corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 조회 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleCreateCorporate(IHTTPSession session) {
        try {
            Log.i(TAG, "=== CREATE CORPORATE REQUEST ===");
            String postData = extractRequestBody(session);
            Log.i(TAG, "Raw postData content: [" + postData + "]");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                Log.w(TAG, "Create corporate failed: empty body");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // JSON 형식 검증
            if (!postData.trim().startsWith("{")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "잘못된 JSON 형식입니다: " + postData.substring(0, Math.min(50, postData.length())) + "...");
                Log.w(TAG, "Invalid JSON format in create request: " + postData);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> data = gson.fromJson(postData, Map.class);
            
            Corporate corporate = new Corporate();
            corporate.setName((String) data.get("name"));
            corporate.setBusinessNumber((String) data.get("businessNumber"));
            corporate.setRepresentative((String) data.get("representative"));
            corporate.setPhone((String) data.get("phone"));
            corporate.setEmail((String) data.get("email"));
            corporate.setAddress((String) data.get("address"));
            
            if (!corporate.isValidForSave()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "필수 정보가 누락되었습니다. 회사명은 반드시 입력해야 합니다.");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 사업자등록번호 중복 체크
            if (corporate.getBusinessNumber() != null && !corporate.getBusinessNumber().trim().isEmpty()) {
                if (corporateDAO.isBusinessNumberExists(corporate.getBusinessNumber())) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "이미 등록된 사업자등록번호입니다");
                    return newFixedLengthResponse(Response.Status.CONFLICT, "application/json; charset=utf-8", gson.toJson(error));
                }
            }
            
            long id = corporateDAO.insertCorporate(corporate);
            
            if (id > 0) {
                corporate.setCustomerId((int) id);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", corporate);
                result.put("message", "거래처가 성공적으로 생성되었습니다");
                
                Log.i(TAG, "Corporate created: " + corporate.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.CREATED, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처 생성에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 생성 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleUpdateCorporate(String corporateId, IHTTPSession session) {
        try {
            Log.i(TAG, "=== UPDATE CORPORATE REQUEST === ID: " + corporateId);
            int id = Integer.parseInt(corporateId);
            
            String postData = extractRequestBody(session);
            Log.i(TAG, "Raw postData content: [" + postData + "]");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                Log.w(TAG, "Update corporate failed: empty body");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // JSON 형식 검증
            if (!postData.trim().startsWith("{")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "잘못된 JSON 형식입니다: " + postData.substring(0, Math.min(50, postData.length())) + "...");
                Log.w(TAG, "Invalid JSON format in update request: " + postData);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Corporate existing = corporateDAO.getCorporateById(id);
            if (existing == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처를 찾을 수 없습니다: " + corporateId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> data = gson.fromJson(postData, Map.class);
            
            existing.setName((String) data.get("name"));
            existing.setBusinessNumber((String) data.get("businessNumber"));
            existing.setRepresentative((String) data.get("representative"));
            existing.setPhone((String) data.get("phone"));
            existing.setEmail((String) data.get("email"));
            existing.setAddress((String) data.get("address"));
            
            if (!existing.isValidForSave()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "필수 정보가 누락되었습니다. 회사명은 반드시 입력해야 합니다.");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            int rowsAffected = corporateDAO.updateCorporate(existing);
            
            if (rowsAffected > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", existing);
                result.put("message", "거래처가 성공적으로 업데이트되었습니다");
                
                Log.i(TAG, "Corporate updated: " + existing.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처 업데이트에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error updating corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleDeleteCorporate(String corporateId) {
        try {
            int id = Integer.parseInt(corporateId);
            
            Corporate existing = corporateDAO.getCorporateById(id);
            if (existing == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처를 찾을 수 없습니다: " + corporateId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            int rowsAffected = corporateDAO.deleteCorporate(id);
            
            if (rowsAffected > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "거래처가 성공적으로 삭제되었습니다");
                result.put("deletedCorporate", existing);
                
                Log.i(TAG, "Corporate deleted: " + existing.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처 삭제에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error deleting corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleSearchCorporates(IHTTPSession session) {
        try {
            Map<String, String> params = session.getParms();
            String name = params.get("name");
            
            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "검색할 회사명을 입력해주세요");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            List<Corporate> corporates = corporateDAO.searchCorporatesByName(name);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", corporates);
            result.put("count", corporates.size());
            result.put("searchTerm", name);
            
            Log.i(TAG, "Corporate search for '" + name + "' returned " + corporates.size() + " results");
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
        } catch (Exception e) {
            Log.e(TAG, "Error searching corporates", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 검색 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    // 직원 관련 핸들러 메소드들
    private Response handleGetEmployees() {
        try {
            List<Employee> employees = employeeDAO.getAllEmployees();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", employees);
            result.put("count", employees.size());
            
            Log.i(TAG, "Retrieved all employees: " + employees.size());
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting employees", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "직원 목록 조회 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleGetEmployeesByCorporate(String corporateId) {
        try {
            int id = Integer.parseInt(corporateId);
            List<Employee> employees = employeeDAO.getEmployeesByCorporateId(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", employees);
            result.put("count", employees.size());
            result.put("corporateId", id);
            
            Log.i(TAG, "Retrieved employees for corporate " + id + ": " + employees.size());
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error getting employees by corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "직원 목록 조회 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleGetCouponsByCorporate(String corporateId) {
        try {
            int id = Integer.parseInt(corporateId);
            List<Coupon> coupons = couponDAO.getCouponsByCorporateId(id);
            
            // 쿠폰에 수신자 정보 추가 및 쿠폰 코드 확인/생성
            for (Coupon coupon : coupons) {
                try {
                    // 직원 정보 설정
                    Employee employee = employeeDAO.getEmployeeById(coupon.getEmployeeId());
                    if (employee != null) {
                        coupon.setRecipientName(employee.getName());
                        coupon.setRecipientPhone(employee.getPhone());
                        coupon.setRecipientEmail(employee.getEmail());
                    }
                    
                    // 쿠폰 코드가 없는 경우 생성
                    if (coupon.getFullCouponCode() == null || coupon.getFullCouponCode().trim().isEmpty()) {
                        Log.d(TAG, "[CORPORATE-COUPONS] 쿠폰 코드가 없음 - 생성 시작, ID: " + coupon.getCouponId());
                        String generatedCode = coupon.generateFullCouponCode(context);
                        coupon.setFullCouponCode(generatedCode);
                        couponDAO.updateCouponCode(coupon.getCouponId(), generatedCode);
                        Log.d(TAG, "[CORPORATE-COUPONS] 쿠폰 코드 생성 완료 - ID: " + coupon.getCouponId() + ", 코드: " + generatedCode);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load employee info for coupon " + coupon.getCouponId(), e);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", coupons);
            result.put("count", coupons.size());
            result.put("corporateId", id);
            
            Log.i(TAG, "Retrieved coupons for corporate " + id + ": " + coupons.size());
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error getting coupons by corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰 목록 조회 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleGetEmployee(String employeeId) {
        try {
            int id = Integer.parseInt(employeeId);
            Employee employee = employeeDAO.getEmployeeById(id);
            
            if (employee == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "직원을 찾을 수 없습니다: " + employeeId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", employee);
            
            Log.i(TAG, "Retrieved employee: " + employee.getName());
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 직원 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error getting employee", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "직원 조회 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleCreateEmployee(IHTTPSession session) {
        Log.i(TAG, "=== CREATE EMPLOYEE REQUEST ===");
        
        String postData = extractRequestBody(session);
        Log.i(TAG, "Raw postData content: [" + postData + "]");
        
        if (postData == null || postData.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "요청 본문이 비어있습니다");
            Log.w(TAG, "Create employee failed: empty body");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        }

        try {
            Employee newEmployee = gson.fromJson(postData, Employee.class);
            
            if (!newEmployee.isValidForSave()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "필수 필드가 누락되었습니다 (거래처 ID, 이름, 핸드폰번호는 필수입니다)");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 핸드폰 번호 중복 확인
            if (employeeDAO.isPhoneExists(newEmployee.getPhone())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 등록된 핸드폰 번호입니다: " + newEmployee.getPhone());
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            long id = employeeDAO.insertEmployee(newEmployee);
            
            if (id > 0) {
                newEmployee.setEmployeeId((int) id);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "직원이 성공적으로 생성되었습니다");
                result.put("data", newEmployee);
                
                Log.i(TAG, "Employee created: " + newEmployee.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.CREATED, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "직원 생성에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating employee", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "직원 생성 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleUpdateEmployee(String employeeId, IHTTPSession session) {
        Log.i(TAG, "=== UPDATE EMPLOYEE REQUEST === ID: " + employeeId);
        
        try {
            int id = Integer.parseInt(employeeId);
            
            String postData = extractRequestBody(session);
            Log.i(TAG, "Raw postData content: [" + postData + "]");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                Log.w(TAG, "Update employee failed: empty body");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }

            Employee existing = employeeDAO.getEmployeeById(id);
            if (existing == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "직원을 찾을 수 없습니다: " + employeeId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }

            Employee updatedEmployee = gson.fromJson(postData, Employee.class);
            updatedEmployee.setEmployeeId(id);
            
            if (!updatedEmployee.isValidForSave()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "필수 필드가 누락되었습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 핸드폰 번호 변경 시 중복 확인
            if (!existing.getPhone().equals(updatedEmployee.getPhone())) {
                if (employeeDAO.isPhoneExists(updatedEmployee.getPhone())) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "이미 등록된 핸드폰 번호입니다: " + updatedEmployee.getPhone());
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
                }
            }
            
            int rowsAffected = employeeDAO.updateEmployee(updatedEmployee);
            
            if (rowsAffected > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "직원 정보가 성공적으로 업데이트되었습니다");
                result.put("data", updatedEmployee);
                
                Log.i(TAG, "Employee updated: " + updatedEmployee.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "직원 정보 업데이트에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 직원 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error updating employee", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "직원 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    private Response handleDeleteEmployee(String employeeId) {
        try {
            int id = Integer.parseInt(employeeId);
            
            Employee existing = employeeDAO.getEmployeeById(id);
            if (existing == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "직원을 찾을 수 없습니다: " + employeeId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            int rowsAffected = employeeDAO.deleteEmployee(id);
            
            if (rowsAffected > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "직원이 성공적으로 삭제되었습니다");
                result.put("deletedEmployee", existing);
                
                Log.i(TAG, "Employee deleted: " + existing.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "직원 삭제에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 직원 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error deleting employee", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "직원 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    // ========== 이메일 설정 관련 핸들러 ==========
    
    /**
     * 이메일 설정 조회
     */
    private Response handleGetEmailConfig() {
        try {
            SharedPreferences emailPrefs = context.getSharedPreferences("EmailSettings", Context.MODE_PRIVATE);
            
            Map<String, Object> config = new HashMap<>();
            config.put("smtpHost", emailPrefs.getString("smtp_host", ""));
            config.put("smtpPort", emailPrefs.getString("smtp_port", "587"));
            config.put("security", emailPrefs.getString("security", "tls"));
            config.put("username", emailPrefs.getString("username", ""));
            config.put("password", emailPrefs.getString("password", ""));
            config.put("useAuth", emailPrefs.getBoolean("use_auth", true));
            config.put("senderName", emailPrefs.getString("sender_name", ""));
            config.put("senderEmail", emailPrefs.getString("sender_email", ""));
            config.put("emailSubject", emailPrefs.getString("email_subject", "[쿠폰 발송] {{회사명}} 쿠폰이 발급되었습니다"));
            config.put("emailTemplate", emailPrefs.getString("email_template", 
                "안녕하세요 {{이름}}님,\n\n{{회사명}}에서 쿠폰이 발급되었습니다.\n\n" +
                "쿠폰 코드: {{쿠폰코드}}\n충전 금액: {{충전금액}}원\n포인트: {{포인트}}P\n" +
                "유효기간: {{유효기간}}\n\n감사합니다."));
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", config);
            
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting email config", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "이메일 설정 조회 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * 이메일 설정 저장
     */
    private Response handleSaveEmailConfig(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> configData = gson.fromJson(postData, Map.class);
            
            SharedPreferences emailPrefs = context.getSharedPreferences("EmailSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = emailPrefs.edit();
            
            editor.putString("smtp_host", (String) configData.get("smtpHost"));
            editor.putString("smtp_port", (String) configData.get("smtpPort"));
            editor.putString("security", (String) configData.get("security"));
            editor.putString("username", (String) configData.get("username"));
            editor.putString("password", (String) configData.get("password"));
            editor.putBoolean("use_auth", (Boolean) configData.getOrDefault("useAuth", true));
            editor.putString("sender_name", (String) configData.get("senderName"));
            editor.putString("sender_email", (String) configData.get("senderEmail"));
            editor.putString("email_subject", (String) configData.get("emailSubject"));
            editor.putString("email_template", (String) configData.get("emailTemplate"));
            
            editor.apply();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "이메일 설정이 저장되었습니다");
            
            Log.i(TAG, "Email configuration saved successfully");
            
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving email config", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "이메일 설정 저장 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * 이메일 연결 테스트
     */
    private Response handleTestEmailConnection(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> configData = gson.fromJson(postData, Map.class);
            
            String smtpHost = (String) configData.get("smtpHost");
            String smtpPort = (String) configData.get("smtpPort");
            String username = (String) configData.get("username");
            String password = (String) configData.get("password");
            
            // 필수 값 검증
            if (smtpHost == null || smtpHost.trim().isEmpty() ||
                smtpPort == null || smtpPort.trim().isEmpty() ||
                username == null || username.trim().isEmpty()) {
                
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "SMTP 호스트, 포트, 사용자명은 필수 입력 항목입니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 실제 SMTP 연결 테스트를 위한 코드는 여기서 구현
            // 현재는 간단한 유효성 검사만 수행
            try {
                int port = Integer.parseInt(smtpPort);
                if (port <= 0 || port > 65535) {
                    throw new NumberFormatException("Invalid port range");
                }
            } catch (NumberFormatException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "유효하지 않은 포트 번호입니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // TODO: 실제 SMTP 서버 연결 테스트 구현
            // 현재는 설정 값이 올바른지만 확인
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "이메일 설정이 유효합니다 (실제 연결 테스트는 추후 구현됩니다)");
            
            Log.i(TAG, "Email connection test completed for host: " + smtpHost);
            
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing email connection", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "이메일 연결 테스트 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }

    // ========== 쿠폰 발송 관련 핸들러 ==========
    
    /**
     * 쿠폰 이메일 발송
     */
    private Response handleSendCouponEmail(IHTTPSession session) {
        Log.i(TAG, "[EMAIL-SEND] 이메일 발송 요청 시작");
        
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            Log.i(TAG, "[EMAIL-SEND] POST 데이터 크기: " + (postData != null ? postData.length() : 0));
            Log.i(TAG, "[EMAIL-SEND] POST 데이터 내용: " + postData);
            
            if (postData == null || postData.trim().isEmpty()) {
                Log.e(TAG, "[EMAIL-SEND] 요청 본문이 비어있음");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> requestData;
            try {
                requestData = gson.fromJson(postData, Map.class);
                Log.i(TAG, "[EMAIL-SEND] 파싱된 요청 데이터: " + requestData);
            } catch (Exception e) {
                Log.e(TAG, "[EMAIL-SEND] JSON 파싱 오류", e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "JSON 파싱 오류: " + e.getMessage());
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 필수 파라미터 검증
            Object couponIdObj = requestData.get("couponId");
            String recipientEmail = (String) requestData.get("recipientEmail");
            String recipientName = (String) requestData.get("recipientName");
            String subject = (String) requestData.get("subject");
            String message = (String) requestData.get("message");
            
            Log.i(TAG, "[EMAIL-SEND] 파라미터 검증 - 쿠폰ID: " + couponIdObj + ", 수신자: " + recipientEmail + ", 이름: " + recipientName);
            
            if (couponIdObj == null || recipientEmail == null || recipientEmail.trim().isEmpty()) {
                Log.e(TAG, "[EMAIL-SEND] 필수 파라미터 누락 - 쿠폰ID: " + couponIdObj + ", 이메일: " + recipientEmail);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "쿠폰 ID와 수신자 이메일은 필수 입력 항목입니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            int couponId;
            try {
                if (couponIdObj instanceof Double) {
                    couponId = ((Double) couponIdObj).intValue();
                } else {
                    couponId = Integer.parseInt(couponIdObj.toString());
                }
                Log.i(TAG, "[EMAIL-SEND] 쿠폰 ID 변환 성공: " + couponId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "[EMAIL-SEND] 쿠폰 ID 변환 실패: " + couponIdObj, e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "유효하지 않은 쿠폰 ID입니다: " + couponIdObj);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 기본값 설정
            if (subject == null || subject.trim().isEmpty()) {
                subject = "[쿠폰 발송] " + (recipientName != null ? recipientName + "님의 " : "") + "쿠폰이 발급되었습니다";
            }
            if (message == null || message.trim().isEmpty()) {
                message = "안녕하세요" + (recipientName != null ? " " + recipientName + "님" : "") + ",\n\n쿠폰이 발급되었습니다.\n\n감사합니다.";
            }
            
            Log.i(TAG, "[EMAIL-SEND] 최종 이메일 정보 - 제목: " + subject + ", 메시지 길이: " + message.length());
            
            // 쿠폰 코드 변수를 미리 선언 (스코프 해결)
            String fullCouponCode = null;
            
            // 쿠폰 존재 여부 확인 및 쿠폰 코드 가져오기
            try {
                Coupon coupon = couponDAO.getCouponById(couponId);
                if (coupon == null) {
                    Log.e(TAG, "[EMAIL-SEND] 쿠폰을 찾을 수 없음 - ID: " + couponId);
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "존재하지 않는 쿠폰입니다: " + couponId);
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
                }
                
                // 쿠폰 코드가 없는 경우 생성
                if (coupon.getFullCouponCode() == null || coupon.getFullCouponCode().trim().isEmpty()) {
                    Log.i(TAG, "[EMAIL-SEND] 쿠폰 코드가 없음 - 생성 시작");
                    String generatedCode = coupon.generateFullCouponCode(context);
                    coupon.setFullCouponCode(generatedCode);
                    
                    // 데이터베이스에 업데이트
                    couponDAO.updateCouponCode(couponId, generatedCode);
                    Log.i(TAG, "[EMAIL-SEND] 쿠폰 코드 생성 완료 - " + generatedCode);
                }
                
                // 쿠폰 코드를 변수에 저장
                fullCouponCode = coupon.getFullCouponCode();
                Log.i(TAG, "[EMAIL-SEND] 쿠폰 확인됨 - ID: " + couponId + ", 코드: " + fullCouponCode);
                
            } catch (Exception e) {
                Log.e(TAG, "[EMAIL-SEND] 쿠폰 조회 중 오류", e);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "쿠폰 조회 중 오류가 발생했습니다: " + e.getMessage());
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 발송 기록 저장
            String metadata = gson.toJson(requestData);
            Log.i(TAG, "[EMAIL-SEND] 발송 기록 저장 시작");
            
            long deliveryId = couponDeliveryDAO.insertDelivery(
                couponId, 
                CouponDelivery.TYPE_EMAIL, 
                recipientEmail, 
                subject, 
                message, 
                metadata
            );
            
            Log.i(TAG, "[EMAIL-SEND] 발송 기록 저장 완료 - 배송 ID: " + deliveryId);
            
            if (deliveryId > 0) {
                // 실제 이메일 발송
                Log.i(TAG, "[EMAIL-SEND] 실제 이메일 발송 시작");
                
                // 이메일 설정 확인
                SharedPreferences emailSettings = context.getSharedPreferences("EmailSettings", Context.MODE_PRIVATE);
                String smtpHost = emailSettings.getString("smtp_host", "");
                String smtpPort = emailSettings.getString("smtp_port", "");
                String security = emailSettings.getString("security", "tls");
                String username = emailSettings.getString("username", "");
                String password = emailSettings.getString("password", "");
                boolean useAuth = emailSettings.getBoolean("use_auth", true);
                String senderName = emailSettings.getString("sender_name", "");
                String senderEmail = emailSettings.getString("sender_email", "");
                
                Log.i(TAG, "[EMAIL-SEND] 이메일 설정 확인 - SMTP 호스트: " + smtpHost + ", 포트: " + smtpPort + ", 사용자: " + username + ", 보안: " + security);
                
                boolean emailSentSuccessfully = false;
                String errorMessage = null;
                
                if (smtpHost.isEmpty() || smtpPort.isEmpty() || username.isEmpty()) {
                    Log.w(TAG, "[EMAIL-SEND] 이메일 설정이 불완전함 - 시뮬레이션으로 처리");
                    errorMessage = "이메일 설정이 완료되지 않았습니다. '이메일 설정' 탭에서 SMTP 설정을 완료해주세요.";
                    emailSentSuccessfully = false;
                } else {
                    // 실제 이메일 발송 시도 (별도 스레드에서 실행)
                    try {
                        // final 변수로 복사
                        final String finalSmtpHost = smtpHost;
                        final String finalSmtpPort = smtpPort;
                        final String finalSecurity = security;
                        final String finalUsername = username;
                        final String finalPassword = password;
                        final boolean finalUseAuth = useAuth;
                        final String finalSenderName = senderName;
                        final String finalSenderEmail = senderEmail;
                        final String finalRecipientEmail = recipientEmail;
                        final String finalSubject = subject;
                        final String finalMessage = message;
                        final String finalFullCouponCode = fullCouponCode;
                        
                        // 동기식 실행을 위한 변수들
                        final boolean[] result = {false};
                        final String[] error = {null};
                        final Object lock = new Object();
                        final boolean[] completed = {false};
                        
                        // 별도 스레드에서 이메일 발송
                        Thread emailThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    boolean success = sendEmailActual(finalSmtpHost, finalSmtpPort, finalSecurity, finalUsername, finalPassword, finalUseAuth, 
                                                                    finalSenderName, finalSenderEmail, finalRecipientEmail, finalSubject, finalMessage, finalFullCouponCode);
                                    synchronized (lock) {
                                        result[0] = success;
                                        completed[0] = true;
                                        lock.notify();
                                    }
                                } catch (Exception e) {
                                    synchronized (lock) {
                                        result[0] = false;
                                        error[0] = e.getMessage();
                                        completed[0] = true;
                                        lock.notify();
                                    }
                                }
                            }
                        });
                        
                        emailThread.start();
                        
                        // 결과 대기 (최대 30초)
                        synchronized (lock) {
                            if (!completed[0]) {
                                lock.wait(30000); // 30초 타임아웃
                            }
                        }
                        
                        if (!completed[0]) {
                            // 타임아웃 발생
                            emailThread.interrupt();
                            emailSentSuccessfully = false;
                            errorMessage = "이메일 발송이 타임아웃되었습니다 (30초 초과)";
                            Log.w(TAG, "[EMAIL-SEND] 이메일 발송 타임아웃");
                        } else if (result[0]) {
                            emailSentSuccessfully = true;
                            Log.i(TAG, "[EMAIL-SEND] 실제 이메일 발송 성공");
                        } else {
                            emailSentSuccessfully = false;
                            errorMessage = error[0] != null ? "이메일 발송 실패: " + error[0] : "이메일 발송에 실패했습니다. SMTP 설정을 확인해주세요.";
                            Log.w(TAG, "[EMAIL-SEND] 실제 이메일 발송 실패: " + errorMessage);
                        }
                        
                    } catch (InterruptedException e) {
                        Log.e(TAG, "[EMAIL-SEND] 이메일 발송 대기 중 인터럽트", e);
                        emailSentSuccessfully = false;
                        errorMessage = "이메일 발송 중 인터럽트가 발생했습니다.";
                    } catch (Exception e) {
                        Log.e(TAG, "[EMAIL-SEND] 실제 이메일 발송 중 예외", e);
                        emailSentSuccessfully = false;
                        errorMessage = "이메일 발송 중 오류가 발생했습니다: " + e.getMessage();
                    }
                }
                
                // 발송 상태 업데이트
                String deliveryStatus = emailSentSuccessfully ? CouponDelivery.STATUS_SENT : CouponDelivery.STATUS_FAILED;
                boolean updateSuccess = couponDeliveryDAO.updateDeliveryStatus(deliveryId, deliveryStatus, errorMessage);
                Log.i(TAG, "[EMAIL-SEND] 발송 상태 업데이트 결과: " + updateSuccess + ", 상태: " + deliveryStatus);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", emailSentSuccessfully);
                result.put("deliveryId", deliveryId);
                result.put("couponId", couponId);
                result.put("recipientEmail", recipientEmail);
                result.put("actualEmailSent", emailSentSuccessfully);
                
                if (emailSentSuccessfully) {
                    result.put("message", "이메일 발송이 완료되었습니다.");
                } else {
                    result.put("message", errorMessage != null ? errorMessage : "이메일 발송에 실패했습니다.");
                }
                
                Log.i(TAG, "[EMAIL-SEND] 이메일 발송 완료 - 배송 ID: " + deliveryId + ", 쿠폰 ID: " + couponId + ", 성공: " + emailSentSuccessfully);
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Log.e(TAG, "[EMAIL-SEND] 발송 기록 저장 실패");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "발송 기록 저장에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[EMAIL-SEND] 예외 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "이메일 발송 중 오류가 발생했습니다: " + e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            error.put("stackTrace", android.util.Log.getStackTraceString(e));
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    
    /**
     * 발송 기록 조회
     */
    private Response handleGetDeliveryHistory(IHTTPSession session) {
        Log.i(TAG, "[DELIVERY-HISTORY] 발송 기록 조회 요청 시작");
        
        try {
            Map<String, String> params = session.getParms();
            String couponId = params.get("couponId");
            String deliveryType = params.get("type");
            String status = params.get("status");
            
            Log.i(TAG, "[DELIVERY-HISTORY] 조회 파라미터 - 쿠폰ID: " + couponId + ", 유형: " + deliveryType + ", 상태: " + status);
            
            List<CouponDelivery> deliveries;
            
            if (couponId != null && !couponId.trim().isEmpty()) {
                try {
                    int id = Integer.parseInt(couponId);
                    Log.i(TAG, "[DELIVERY-HISTORY] 쿠폰 ID별 조회: " + id);
                    deliveries = couponDeliveryDAO.getDeliveriesByCouponId(id);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "[DELIVERY-HISTORY] 잘못된 쿠폰 ID 형식: " + couponId);
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "유효하지 않은 쿠폰 ID입니다");
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
                }
            } else if (deliveryType != null && !deliveryType.trim().isEmpty()) {
                Log.i(TAG, "[DELIVERY-HISTORY] 발송 유형별 조회: " + deliveryType);
                deliveries = couponDeliveryDAO.getDeliveriesByType(deliveryType);
            } else if (status != null && !status.trim().isEmpty()) {
                Log.i(TAG, "[DELIVERY-HISTORY] 상태별 조회: " + status);
                deliveries = couponDeliveryDAO.getDeliveriesByStatus(status);
            } else {
                Log.i(TAG, "[DELIVERY-HISTORY] 모든 발송 기록 조회");
                deliveries = couponDeliveryDAO.getAllDeliveries();
            }
            
            Log.i(TAG, "[DELIVERY-HISTORY] 조회 결과: " + deliveries.size() + "건");
            
            // 각 발송 기록에 쿠폰 코드 정보 추가
            for (CouponDelivery delivery : deliveries) {
                try {
                    Log.d(TAG, "[DELIVERY-HISTORY] 발송 기록 상세 정보 - deliveryId: " + delivery.getDeliveryId() + 
                        ", couponId: " + delivery.getCouponId() + 
                        ", deliveryType: " + delivery.getDeliveryType() + 
                        ", deliveryStatus: " + delivery.getDeliveryStatus() + 
                        ", recipient: " + delivery.getRecipientAddress() +
                        ", createdAt: " + delivery.getCreatedAt());
                    
                    Log.d(TAG, "[DELIVERY-HISTORY] 쿠폰 정보 조회 시작 - delivery.getCouponId(): " + delivery.getCouponId());
                    Coupon coupon = couponDAO.getCouponById(delivery.getCouponId());
                    if (coupon != null) {
                        String fullCouponCode = coupon.getFullCouponCode();
                        
                        // 쿠폰 코드가 없는 경우 생성
                        if (fullCouponCode == null || fullCouponCode.trim().isEmpty()) {
                            Log.d(TAG, "[DELIVERY-HISTORY] 쿠폰 코드가 없음 - 생성 시작");
                            fullCouponCode = coupon.generateFullCouponCode(context);
                            coupon.setFullCouponCode(fullCouponCode);
                            couponDAO.updateCouponCode(delivery.getCouponId(), fullCouponCode);
                            Log.d(TAG, "[DELIVERY-HISTORY] 쿠폰 코드 생성 완료 - " + fullCouponCode);
                        }
                        
                        Log.d(TAG, "[DELIVERY-HISTORY] 쿠폰 조회 성공 - ID: " + delivery.getCouponId() + ", 코드: " + fullCouponCode);
                        delivery.setCouponCode(fullCouponCode);
                        Log.d(TAG, "[DELIVERY-HISTORY] setCouponCode 완료 - getCouponCode(): " + delivery.getCouponCode());
                    } else {
                        Log.w(TAG, "[DELIVERY-HISTORY] 쿠폰을 찾을 수 없음 - ID: " + delivery.getCouponId());
                    }
                    
                    // JavaScript용 필드 설정
                    String statusValue = delivery.getDeliveryStatus() != null ? delivery.getDeliveryStatus().toLowerCase() : null;
                    String recipientValue = delivery.getRecipientAddress();
                    String typeValue = delivery.getDeliveryType() != null ? delivery.getDeliveryType().toLowerCase() : null;
                    
                    delivery.setStatus(statusValue);
                    delivery.setRecipient(recipientValue);
                    delivery.setType(typeValue);
                    
                    // status 관련 상세 로그 추가
                    Log.d(TAG, "[DELIVERY-HISTORY] 필드 설정 완료 - " +
                        "원본 deliveryStatus: '" + delivery.getDeliveryStatus() + "', " +
                        "설정한 status: '" + statusValue + "', " +
                        "설정한 recipient: '" + recipientValue + "', " +
                        "설정한 type: '" + typeValue + "'");
                        
                } catch (Exception e) {
                    Log.w(TAG, "[DELIVERY-HISTORY] 쿠폰 정보 조회 실패 - ID: " + delivery.getCouponId(), e);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", deliveries);
            result.put("count", deliveries.size());
            
            // JSON 직렬화 결과 로그
            String jsonResult = gson.toJson(result);
            Log.i(TAG, "[DELIVERY-HISTORY] 발송 기록 조회 완료 - " + deliveries.size() + "건 반환");
            Log.d(TAG, "[DELIVERY-HISTORY] JSON 응답 샘플 (첫 500자): " + 
                (jsonResult.length() > 500 ? jsonResult.substring(0, 500) + "..." : jsonResult));
            
            // 첫 번째 delivery 객체의 JSON 구조 상세 로그
            if (!deliveries.isEmpty()) {
                CouponDelivery firstDelivery = deliveries.get(0);
                String singleDeliveryJson = gson.toJson(firstDelivery);
                Log.d(TAG, "[DELIVERY-HISTORY] 첫 번째 delivery 객체 JSON: " + singleDeliveryJson);
            }
            
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", jsonResult);
            
        } catch (Exception e) {
            Log.e(TAG, "[DELIVERY-HISTORY] 발송 기록 조회 중 예외 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "발송 기록 조회 중 오류가 발생했습니다: " + e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * 실제 이메일 발송 메서드
     */
    private boolean sendEmailActual(String smtpHost, String smtpPort, String security, 
                                   String username, String password, boolean useAuth,
                                   String senderName, String senderEmail, 
                                   String recipientEmail, String subject, String messageText, String couponCode) {
        
        Log.i(TAG, "[EMAIL-ACTUAL] 실제 이메일 발송 시작");
        Log.i(TAG, "[EMAIL-ACTUAL] 설정 - 호스트: " + smtpHost + ", 포트: " + smtpPort + ", 보안: " + security);
        Log.i(TAG, "[EMAIL-ACTUAL] 발신자: " + senderEmail + " (" + senderName + ")");
        Log.i(TAG, "[EMAIL-ACTUAL] 수신자: " + recipientEmail);
        Log.i(TAG, "[EMAIL-ACTUAL] 제목: " + subject);
        
        try {
            // 기본 검증
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                Log.e(TAG, "[EMAIL-ACTUAL] SMTP 호스트가 설정되지 않음");
                return false;
            }
            
            if (recipientEmail == null || !recipientEmail.contains("@")) {
                Log.e(TAG, "[EMAIL-ACTUAL] 잘못된 수신자 이메일: " + recipientEmail);
                return false;
            }
            
            if (username == null || username.trim().isEmpty()) {
                Log.e(TAG, "[EMAIL-ACTUAL] SMTP 사용자명이 설정되지 않음");
                return false;
            }
            
            // 포트 번호 검증
            int port;
            try {
                port = Integer.parseInt(smtpPort);
                if (port <= 0 || port > 65535) {
                    Log.e(TAG, "[EMAIL-ACTUAL] 잘못된 포트 번호: " + smtpPort);
                    return false;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "[EMAIL-ACTUAL] 포트 번호 파싱 오류: " + smtpPort);
                return false;
            }
            
            Log.i(TAG, "[EMAIL-ACTUAL] 순수 Java Socket SMTP 연결 시도 시작");
            
            // Socket을 이용한 직접 SMTP 구현
            return performSmtpEmail(smtpHost, port, security, username, password, useAuth, senderName, senderEmail, recipientEmail, subject, messageText, couponCode);
            
        } catch (Exception e) {
            Log.e(TAG, "[EMAIL-ACTUAL] 이메일 발송 중 예외 발생", e);
            return false;
        }
    }
    
    /**
     * SMTP 이메일 발송 실행
     */
    private boolean performSmtpEmail(String smtpHost, int port, String security, String username, 
                                   String password, boolean useAuth, String senderName, String senderEmail, 
                                   String recipientEmail, String subject, String messageText, String couponCode) {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        File qrImageFile = null;
        
        try {
            // 발신자 이메일 결정
            String fromEmail = senderEmail != null && !senderEmail.isEmpty() ? senderEmail : username;
            
            // QR 코드 생성 (쿠폰 코드가 있는 경우)
            if (couponCode != null && !couponCode.trim().isEmpty()) {
                Log.i(TAG, "[EMAIL-ACTUAL] QR 코드 생성 시작 - 쿠폰코드: " + couponCode);
                String qrFileName = "coupon_qr_" + System.currentTimeMillis();
                qrImageFile = QRCodeGenerator.generateQRCodeImage(context, couponCode, qrFileName);
                
                if (qrImageFile != null) {
                    Log.i(TAG, "[EMAIL-ACTUAL] QR 코드 생성 성공 - 파일: " + qrImageFile.getName() + ", 크기: " + qrImageFile.length() + " bytes");
                } else {
                    Log.w(TAG, "[EMAIL-ACTUAL] QR 코드 생성 실패 - 첨부 없이 이메일 발송");
                }
            } else {
                Log.w(TAG, "[EMAIL-ACTUAL] 쿠폰 코드가 없어 QR 코드 생성 생략");
            }
            
            Log.i(TAG, "[EMAIL-ACTUAL] 연결 설정 - 호스트: " + smtpHost + ", 포트: " + port + ", 보안: " + security);
            
            // SMTP 서버 연결
            if ("ssl".equals(security)) {
                // SSL 연결
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslFactory.createSocket(smtpHost, port);
            } else {
                // 일반 또는 TLS 연결
                socket = new Socket(smtpHost, port);
            }
            
            socket.setSoTimeout(30000); // 30초 타임아웃
            
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            Log.i(TAG, "[EMAIL-ACTUAL] SMTP 서버 연결 성공");
            
            // SMTP 명령어 처리
            String response = reader.readLine();
            Log.i(TAG, "[EMAIL-ACTUAL] 서버 응답: " + response);
            
            if (!response.startsWith("220")) {
                Log.e(TAG, "[EMAIL-ACTUAL] SMTP 서버 연결 실패: " + response);
                return false;
            }
            
            // EHLO 명령
            writer.println("EHLO " + smtpHost);
            response = readMultiLineResponse(reader);
            Log.i(TAG, "[EMAIL-ACTUAL] EHLO 응답: " + response);
            
            // TLS 시작 (필요한 경우)
            if ("tls".equals(security)) {
                writer.println("STARTTLS");
                response = reader.readLine();
                Log.i(TAG, "[EMAIL-ACTUAL] STARTTLS 응답: " + response);
                
                if (response.startsWith("220")) {
                    // TLS 핸드셰이크
                    SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, smtpHost, port, true);
                    sslSocket.startHandshake();
                    
                    socket = sslSocket;
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer = new PrintWriter(socket.getOutputStream(), true);
                    
                    // TLS 후 다시 EHLO
                    writer.println("EHLO " + smtpHost);
                    response = readMultiLineResponse(reader);
                    Log.i(TAG, "[EMAIL-ACTUAL] TLS EHLO 응답: " + response);
                }
            }
            
            // 인증 (필요한 경우)
            if (useAuth && password != null && !password.isEmpty()) {
                writer.println("AUTH LOGIN");
                response = reader.readLine();
                Log.i(TAG, "[EMAIL-ACTUAL] AUTH LOGIN 응답: " + response);
                
                if (response.startsWith("334")) {
                    // 사용자명 전송
                    String encodedUsername = Base64.getEncoder().encodeToString(username.getBytes("UTF-8"));
                    writer.println(encodedUsername);
                    response = reader.readLine();
                    Log.i(TAG, "[EMAIL-ACTUAL] 사용자명 응답: " + response);
                    
                    if (response.startsWith("334")) {
                        // 비밀번호 전송
                        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes("UTF-8"));
                        writer.println(encodedPassword);
                        response = reader.readLine();
                        Log.i(TAG, "[EMAIL-ACTUAL] 비밀번호 응답: " + response);
                        
                        if (!response.startsWith("235")) {
                            Log.e(TAG, "[EMAIL-ACTUAL] 인증 실패: " + response);
                            return false;
                        }
                    }
                }
            }
            
            // 발신자 설정
            writer.println("MAIL FROM:<" + fromEmail + ">");
            response = reader.readLine();
            Log.i(TAG, "[EMAIL-ACTUAL] MAIL FROM 응답: " + response);
            if (!response.startsWith("250")) {
                Log.e(TAG, "[EMAIL-ACTUAL] MAIL FROM 실패: " + response);
                return false;
            }
            
            // 수신자 설정
            writer.println("RCPT TO:<" + recipientEmail + ">");
            response = reader.readLine();
            Log.i(TAG, "[EMAIL-ACTUAL] RCPT TO 응답: " + response);
            if (!response.startsWith("250")) {
                Log.e(TAG, "[EMAIL-ACTUAL] RCPT TO 실패: " + response);
                return false;
            }
            
            // 데이터 전송 시작
            writer.println("DATA");
            response = reader.readLine();
            Log.i(TAG, "[EMAIL-ACTUAL] DATA 응답: " + response);
            if (!response.startsWith("354")) {
                Log.e(TAG, "[EMAIL-ACTUAL] DATA 명령 실패: " + response);
                return false;
            }
            
            // 이메일 헤더와 본문 작성
            String displayName = senderName != null && !senderName.isEmpty() ? senderName : fromEmail;
            writer.println("From: " + displayName + " <" + fromEmail + ">");
            writer.println("To: <" + recipientEmail + ">");
            writer.println("Subject: " + subject);
            writer.println("MIME-Version: 1.0");
            
            // MIME 멀티파트 구성 (첨부파일이 있는 경우)
            if (qrImageFile != null && qrImageFile.exists()) {
                String boundary = "----=_Part_" + System.currentTimeMillis();
                writer.println("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"");
                writer.println("");
                
                // HTML 본문 파트
                writer.println("--" + boundary);
                writer.println("Content-Type: text/html; charset=UTF-8");
                writer.println("Content-Transfer-Encoding: 8bit");
                writer.println("");
                String htmlContent = messageText.replace("\n", "<br>");
                writer.println(htmlContent);
                writer.println("");
                
                // QR 코드 첨부파일 파트
                writer.println("--" + boundary);
                writer.println("Content-Type: image/jpeg; name=\"" + qrImageFile.getName() + "\"");
                writer.println("Content-Transfer-Encoding: base64");
                writer.println("Content-Disposition: attachment; filename=\"" + qrImageFile.getName() + "\"");
                writer.println("");
                
                // 파일을 Base64로 인코딩해서 전송
                try {
                    byte[] fileBytes = readFileToBytes(qrImageFile);
                    if (fileBytes != null) {
                        String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                        // Base64는 76자마다 줄바꿈
                        for (int i = 0; i < base64Content.length(); i += 76) {
                            int endIndex = Math.min(i + 76, base64Content.length());
                            writer.println(base64Content.substring(i, endIndex));
                        }
                        Log.i(TAG, "[EMAIL-ACTUAL] QR 코드 첨부파일 Base64 인코딩 완료");
                    } else {
                        Log.e(TAG, "[EMAIL-ACTUAL] QR 코드 파일 읽기 실패");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "[EMAIL-ACTUAL] QR 코드 첨부 중 오류", e);
                }
                
                writer.println("");
                writer.println("--" + boundary + "--");
                
            } else {
                // 첨부파일이 없는 경우 단순 HTML
                writer.println("Content-Type: text/html; charset=UTF-8");
                writer.println("");
                String htmlContent = messageText.replace("\n", "<br>");
                writer.println(htmlContent);
            }
            
            writer.println(".");
            
            response = reader.readLine();
            Log.i(TAG, "[EMAIL-ACTUAL] 메시지 전송 응답: " + response);
            if (!response.startsWith("250")) {
                Log.e(TAG, "[EMAIL-ACTUAL] 메시지 전송 실패: " + response);
                return false;
            }
            
            // 연결 종료
            writer.println("QUIT");
            response = reader.readLine();
            Log.i(TAG, "[EMAIL-ACTUAL] QUIT 응답: " + response);
            
            Log.i(TAG, "[EMAIL-ACTUAL] 이메일 발송 성공!");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "[EMAIL-ACTUAL] SMTP 통신 중 오류", e);
            return false;
        } finally {
            // 리소스 정리
            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                Log.w(TAG, "[EMAIL-ACTUAL] 리소스 정리 중 오류", e);
            }
            
            // QR 코드 이미지 파일 삭제
            if (qrImageFile != null) {
                boolean deleted = QRCodeGenerator.deleteQRCodeImage(qrImageFile);
                if (deleted) {
                    Log.i(TAG, "[EMAIL-ACTUAL] QR 코드 임시 파일 삭제 완료");
                } else {
                    Log.w(TAG, "[EMAIL-ACTUAL] QR 코드 임시 파일 삭제 실패");
                }
            }
        }
    }
    
    /**
     * SMTP 서버에서 멀티라인 응답 읽기
     */
    private String readMultiLineResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            // SMTP 응답의 마지막 라인은 세 자리 숫자 + 공백으로 시작
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
        
        return response.toString().trim();
    }
    
    /**
     * 파일을 바이트 배열로 읽기
     */
    private byte[] readFileToBytes(File file) {
        if (file == null || !file.exists()) {
            Log.e(TAG, "[FILE-READ] 파일이 존재하지 않음");
            return null;
        }
        
        FileInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        
        try {
            inputStream = new FileInputStream(file);
            outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            byte[] fileBytes = outputStream.toByteArray();
            Log.d(TAG, "[FILE-READ] 파일 읽기 완료 - " + fileBytes.length + " bytes");
            
            return fileBytes;
            
        } catch (IOException e) {
            Log.e(TAG, "[FILE-READ] 파일 읽기 중 IOException", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "[FILE-READ] 파일 읽기 중 예외", e);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "[FILE-READ] 스트림 닫기 실패", e);
            }
        }
    }

    // ========== SMS 설정 관련 메서드들 ==========
    
    /**
     * SMS 설정 조회
     */
    private Response handleGetSmsConfig() {
        Log.i(TAG, "[SMS-CONFIG] SMS 설정 조회 요청");
        
        try {
            SharedPreferences smsSettings = context.getSharedPreferences("SmsSettings", Context.MODE_PRIVATE);
            SharedPreferences businessSettings = context.getSharedPreferences("BusinessSettings", Context.MODE_PRIVATE);
            
            // 관리자 기본설정에서 사업자등록번호 가져오기
            String businessNumber = businessSettings.getString("business_number", "");
            
            Map<String, Object> config = new HashMap<>();
            config.put("businessId", businessNumber);
            config.put("apiUrl", smsSettings.getString("api_url", "https://www.nusome.co.kr/api/request_qr_sms"));
            config.put("senderNumber", smsSettings.getString("sender_number", ""));
            config.put("senderName", smsSettings.getString("sender_name", ""));
            config.put("testMode", smsSettings.getBoolean("test_mode", false));
            config.put("smsTemplate", smsSettings.getString("sms_template", 
                "{company_name}에서 구매하고 결제하신 식권 큐알(QR)코드가 발송되었습니다.\n\n아래 링크를 클릭하시면 식권 큐알(QR)코드를 다운로드 하실 수 있습니다.\n\n큐알(QR)코드 이미지를 다운로드 하셨다가 {company_name}을 이용시에 제시해 주십시오.\n\n{qr_code_url}"));
            config.put("kakaoTemplate", smsSettings.getString("kakao_template", 
                "{company_name}에서 구매하고 결제하신 식권 큐알(QR)코드가 발송되었습니다.\n\n아래 링크를 클릭하시면 식권 큐알(QR)코드를 다운로드 하실 수 있습니다.\n\n큐알(QR)코드 이미지를 다운로드 하셨다가 {company_name}을 이용시에 제시해 주십시오.\n\n{qr_code_url}"));
            
            Log.i(TAG, "[SMS-CONFIG] 사업자등록번호 (BusinessSettings): " + (businessNumber.isEmpty() ? "미설정" : "설정됨"));
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("config", config);
            
            Log.i(TAG, "[SMS-CONFIG] SMS 설정 조회 완료");
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "[SMS-CONFIG] SMS 설정 조회 중 오류", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "SMS 설정 조회 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * SMS 설정 저장
     */
    private Response handleSaveSmsConfig(IHTTPSession session) {
        Log.i(TAG, "[SMS-CONFIG] SMS 설정 저장 요청");
        
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> configData = gson.fromJson(postData, Map.class);
            
            SharedPreferences smsSettings = context.getSharedPreferences("SmsSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = smsSettings.edit();
            
            // 사업자등록번호는 BusinessSettings에서 관리되므로 별도로 저장하지 않음
            editor.putString("api_url", (String) configData.get("apiUrl"));
            editor.putString("sender_number", (String) configData.get("senderNumber"));
            editor.putString("sender_name", (String) configData.get("senderName"));
            editor.putBoolean("test_mode", (Boolean) configData.get("testMode"));
            editor.putString("sms_template", (String) configData.get("smsTemplate"));
            editor.putString("kakao_template", (String) configData.get("kakaoTemplate"));
            
            boolean saved = editor.commit();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", saved);
            result.put("message", saved ? "SMS 설정이 저장되었습니다" : "SMS 설정 저장에 실패했습니다");
            
            Log.i(TAG, "[SMS-CONFIG] SMS 설정 저장 " + (saved ? "성공" : "실패"));
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "[SMS-CONFIG] SMS 설정 저장 중 오류", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "SMS 설정 저장 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * SMS 연결 테스트
     */
    private Response handleTestSmsConnection(IHTTPSession session) {
        Log.i(TAG, "[SMS-TEST] SMS 연결 테스트 요청");
        
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            Map<String, Object> testData = gson.fromJson(postData, Map.class);
            String testPhone = (String) testData.get("senderNumber");
            
            // BusinessSettings에서 사업자등록번호 가져오기
            SharedPreferences businessSettings = context.getSharedPreferences("BusinessSettings", Context.MODE_PRIVATE);
            String businessId = businessSettings.getString("business_number", "");
            
            if (businessId == null || businessId.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "관리자 기본설정에서 사업자등록번호를 먼저 설정해주세요");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 테스트 SMS 발송
            boolean testResult = sendTestSms(businessId, testPhone);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", testResult);
            result.put("message", testResult ? "SMS 연결 테스트 성공" : "SMS 연결 테스트 실패");
            
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            
        } catch (Exception e) {
            Log.e(TAG, "[SMS-TEST] SMS 연결 테스트 중 오류", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "SMS 연결 테스트 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    // ========== SMS/카카오톡 발송 관련 메서드들 ==========
    
    /**
     * 실제 SMS/카카오톡 발송을 담당하는 핸들러 (기존 메서드 수정)
     */
    private Response handleSendCouponSMS(IHTTPSession session) {
        Log.i(TAG, "[SMS-SEND] SMS 발송 요청 시작");
        
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> requestData = gson.fromJson(postData, Map.class);
            Object couponIdObj = requestData.get("couponId");
            
            if (couponIdObj == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "쿠폰 ID가 필요합니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            int couponId = ((Double) couponIdObj).intValue();
            
            // 쿠폰 정보 조회
            Coupon coupon = couponDAO.getCouponById(couponId);
            if (coupon == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "존재하지 않는 쿠폰입니다: " + couponId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 직원 정보 조회
            Employee employee = employeeDAO.getEmployeeById(coupon.getEmployeeId());
            if (employee == null || employee.getPhone() == null || employee.getPhone().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "수신자 전화번호가 없습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 발송 기록 저장
            String metadata = gson.toJson(requestData);
            long deliveryId = couponDeliveryDAO.insertDelivery(
                couponId, 
                CouponDelivery.TYPE_SMS, 
                employee.getPhone(), 
                "SMS 쿠폰 발송", 
                "SMS로 쿠폰이 발송되었습니다", 
                metadata
            );
            
            if (deliveryId > 0) {
                // 실제 SMS 발송
                boolean smsResult = sendActualSms(coupon, employee, "sms");
                
                // 발송 상태 업데이트
                String deliveryStatus = smsResult ? CouponDelivery.STATUS_SENT : CouponDelivery.STATUS_FAILED;
                couponDeliveryDAO.updateDeliveryStatus(deliveryId, deliveryStatus, smsResult ? null : "SMS 발송 실패");
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", smsResult);
                result.put("deliveryId", deliveryId);
                result.put("couponId", couponId);
                result.put("message", smsResult ? "SMS 발송이 완료되었습니다" : "SMS 발송에 실패했습니다");
                
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "발송 기록 저장에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[SMS-SEND] SMS 발송 중 예외 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "SMS 발송 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * 카카오톡 발송 핸들러 (기존 메서드 수정)
     */
    private Response handleSendCouponKakao(IHTTPSession session) {
        Log.i(TAG, "[KAKAO-SEND] 카카오톡 발송 요청 시작");
        
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String postData = body.get("postData");
            
            if (postData == null || postData.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "요청 본문이 비어있습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            Map<String, Object> requestData = gson.fromJson(postData, Map.class);
            Object couponIdObj = requestData.get("couponId");
            
            if (couponIdObj == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "쿠폰 ID가 필요합니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            int couponId = ((Double) couponIdObj).intValue();
            
            // 쿠폰 정보 조회
            Coupon coupon = couponDAO.getCouponById(couponId);
            if (coupon == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "존재하지 않는 쿠폰입니다: " + couponId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 직원 정보 조회
            Employee employee = employeeDAO.getEmployeeById(coupon.getEmployeeId());
            if (employee == null || employee.getPhone() == null || employee.getPhone().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "수신자 전화번호가 없습니다");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json; charset=utf-8", gson.toJson(error));
            }
            
            // 발송 기록 저장
            String metadata = gson.toJson(requestData);
            long deliveryId = couponDeliveryDAO.insertDelivery(
                couponId, 
                CouponDelivery.TYPE_KAKAO, 
                employee.getPhone(), 
                "카카오톡 쿠폰 발송", 
                "카카오톡으로 쿠폰이 발송되었습니다", 
                metadata
            );
            
            if (deliveryId > 0) {
                // 실제 카카오톡 발송
                boolean kakaoResult = sendActualSms(coupon, employee, "kakao");
                
                // 발송 상태 업데이트
                String deliveryStatus = kakaoResult ? CouponDelivery.STATUS_SENT : CouponDelivery.STATUS_FAILED;
                couponDeliveryDAO.updateDeliveryStatus(deliveryId, deliveryStatus, kakaoResult ? null : "카카오톡 발송 실패");
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", kakaoResult);
                result.put("deliveryId", deliveryId);
                result.put("couponId", couponId);
                result.put("message", kakaoResult ? "카카오톡 발송이 완료되었습니다" : "카카오톡 발송에 실패했습니다");
                
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "발송 기록 저장에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[KAKAO-SEND] 카카오톡 발송 중 예외 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "카카오톡 발송 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", gson.toJson(error));
        }
    }
    
    /**
     * subject에서 문자/숫자와 공백만 남기고 특수문자 제거
     */
    private String cleanSubject(String subject) {
        if (subject == null) return "";
        // 한글, 영문, 숫자, 공백만 남기고 나머지 제거
        return subject.replaceAll("[^가-힣a-zA-Z0-9\\s]", "").trim();
    }
    
    /**
     * 누썸 API를 통한 실제 SMS/카카오톡 발송
     */
    private boolean sendActualSms(Coupon coupon, Employee employee, String type) {
        Log.i(TAG, "[NUSOME-SMS] 누썸 API 발송 시작 - 타입: " + type);
        
        try {
            // SMS 설정 조회
            SharedPreferences smsSettings = context.getSharedPreferences("SmsSettings", Context.MODE_PRIVATE);
            SharedPreferences businessSettings = context.getSharedPreferences("BusinessSettings", Context.MODE_PRIVATE);
            
            // BusinessSettings에서 사업자등록번호 가져오기
            String businessId = businessSettings.getString("business_number", "");
            String senderName = smsSettings.getString("sender_name", "쿠폰맨");
            String smsTemplate = smsSettings.getString("sms_template", "");
            String kakaoTemplate = smsSettings.getString("kakao_template", "");
            
            if (businessId.isEmpty()) {
                Log.e(TAG, "[NUSOME-SMS] 관리자 기본설정에서 사업자등록번호가 설정되지 않았습니다");
                return false;
            }
            
            // QR 코드 생성
            String qrData = "coupon:" + coupon.getFullCouponCode() + ":" + coupon.getCashBalance();
            File qrImageFile = QRCodeGenerator.generateQRCodeImage(context, qrData, "qr_" + coupon.getCouponId());
            
            if (qrImageFile == null) {
                Log.e(TAG, "[NUSOME-SMS] QR 코드 생성 실패");
                return false;
            }
            
            // 거래처 정보 조회
            Corporate corporate = corporateDAO.getCorporateById(employee.getCorporateId());
            String companyName = corporate != null ? corporate.getName() : "알 수 없음";
            
            // 템플릿 선택 및 처리
            String template = "sms".equals(type) ? smsTemplate : kakaoTemplate;
            if (template.isEmpty()) {
                template = "{company_name}에서 구매하고 결제하신 식권 큐알(QR)코드가 발송되었습니다.\n\n아래 링크를 클릭하시면 식권 큐알(QR)코드를 다운로드 하실 수 있습니다.\n\n큐알(QR)코드 이미지를 다운로드 하셨다가 {company_name}을 이용시에 제시해 주십시오.\n\n{qr_code_url}";
            }
            
            // BusinessSettings에서 회사명 가져오기
            SharedPreferences businessSettings2 = context.getSharedPreferences("BusinessSettings", Context.MODE_PRIVATE);
            String businessCompanyName = businessSettings2.getString("company_name", "쿠폰맨");
            
            // 템플릿 변수 치환 - {qr_code_url}은 서버에서 치환하도록 그대로 둠
            String processedMessage = template
                // 새로운 형식
                .replace("{company_name}", businessCompanyName)
                // {qr_code_url}은 치환하지 않음 - 서버에서 처리
                // 기존 형식도 지원 (하위 호환성)
                .replace("{{이름}}", employee.getName())
                .replace("{{회사명}}", businessCompanyName) 
                .replace("{{쿠폰코드}}", coupon.getFullCouponCode())
                .replace("{{충전금액}}", String.valueOf(coupon.getCashBalance()))
                .replace("{{포인트}}", String.valueOf(coupon.getPointBalance()))
                .replace("{{유효기간}}", coupon.getExpireDate());
            
            // 누썸 API 요청 데이터 구성
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("business_id", businessId);
            requestData.put("recipient_phone", employee.getPhone());
            requestData.put("recipient_name", employee.getName());
            requestData.put("qr_data", qrData);
            requestData.put("sms_kakao", type);
            
            // 카카오톡인 경우 특별 설정 추가
            String subject;
            if ("kakao".equals(type)) {
                requestData.put("kakao_template_id", "chargable_coupon");
                subject = cleanSubject("식권이 도착 했습니다");
            } else {
                subject = cleanSubject("식권이 발급 되었습니다");
            }
            
            // subject 로그 출력 (특수문자 제거 확인용)
            Log.d(TAG, "[NUSOME-SMS] 처리된 subject: " + subject);
            requestData.put("subject", subject);
            
            requestData.put("message_template", processedMessage);
            
            // HTTP 요청 실행
            String jsonRequest = gson.toJson(requestData);
            Log.d(TAG, "[NUSOME-SMS] 요청 데이터: " + jsonRequest);
            
            boolean result = executeNusomeApiRequest(jsonRequest);
            
            // QR 이미지 파일 정리
            QRCodeGenerator.deleteQRCodeImage(qrImageFile);
            
            Log.i(TAG, "[NUSOME-SMS] 누썸 API 발송 " + (result ? "성공" : "실패"));
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "[NUSOME-SMS] 누썸 API 발송 중 예외", e);
            return false;
        }
    }
    
    /**
     * 테스트 SMS 발송
     */
    private boolean sendTestSms(String businessId, String testPhone) {
        Log.i(TAG, "[NUSOME-TEST] 테스트 SMS 발송 시작");
        
        try {
            // 테스트 요청 데이터 구성
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("business_id", businessId);
            requestData.put("recipient_phone", testPhone != null ? testPhone : "01012345678");
            requestData.put("recipient_name", "테스트");
            requestData.put("qr_data", "test:coupon:1000");
            requestData.put("sms_kakao", "sms");
            requestData.put("subject", cleanSubject("[테스트] SMS 연결 확인"));
            requestData.put("message_template", "SMS 연결 테스트 메시지입니다.");
            
            String jsonRequest = gson.toJson(requestData);
            Log.d(TAG, "[NUSOME-TEST] 테스트 요청 데이터: " + jsonRequest);
            
            return executeNusomeApiRequest(jsonRequest);
            
        } catch (Exception e) {
            Log.e(TAG, "[NUSOME-TEST] 테스트 SMS 발송 중 예외", e);
            return false;
        }
    }
    
    /**
     * 누썸 API HTTP 요청 실행
     */
    private boolean executeNusomeApiRequest(String jsonRequest) {
        Log.i(TAG, "[NUSOME-HTTP] HTTP 요청 시작");
        
        try {
            // OkHttp 클라이언트 생성
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            
            // 요청 생성
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                jsonRequest, 
                okhttp3.MediaType.parse("application/json; charset=utf-8")
            );
            
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://www.nusome.co.kr/api/request_qr_sms")
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("User-Agent", "CouponMan/1.0")
                .build();
            
            Log.d(TAG, "[NUSOME-HTTP] 요청 URL: https://www.nusome.co.kr/api/request_qr_sms");
            
            // 요청 실행
            okhttp3.Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            Log.d(TAG, "[NUSOME-HTTP] 응답 코드: " + response.code());
            Log.d(TAG, "[NUSOME-HTTP] 응답 내용: " + responseBody);
            
            if (response.isSuccessful()) {
                // 응답 파싱
                Map<String, Object> responseData = gson.fromJson(responseBody, Map.class);
                Boolean success = (Boolean) responseData.get("success");
                String message = (String) responseData.get("message");
                String requestId = (String) responseData.get("request_id");
                
                Log.i(TAG, "[NUSOME-HTTP] 누썸 API 응답 - 성공: " + success + ", 메시지: " + message + ", 요청ID: " + requestId);
                
                return success != null && success;
            } else {
                Log.e(TAG, "[NUSOME-HTTP] HTTP 요청 실패 - 코드: " + response.code() + ", 응답: " + responseBody);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[NUSOME-HTTP] HTTP 요청 중 예외", e);
            return false;
        }
    }
}