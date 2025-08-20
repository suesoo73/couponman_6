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

public class ApiServer extends NanoHTTPD {

    private static final String TAG = "ApiServer";
    private Gson gson;
    private List<Map<String, Object>> coupons;
    private Map<String, String> activeTokens;
    private Context context;
    private SharedPreferences sharedPreferences;
    private CorporateDAO corporateDAO;

    public ApiServer(int port, Context context) {
        super(port);
        this.context = context;
        gson = new Gson();
        activeTokens = new HashMap<>();
        sharedPreferences = context.getSharedPreferences("AdminSettings", Context.MODE_PRIVATE);
        corporateDAO = new CorporateDAO(context);
        corporateDAO.open();
        initializeSampleData();
    }

    @Override
    public void stop() {
        super.stop();
        if (corporateDAO != null) {
            corporateDAO.close();
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
                "GET /api/server/status - 서버 상태 조회"
        });

        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(info));
    }

    private Response handleGetCoupons() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", coupons);
        result.put("count", coupons.size());

        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
    }

    private Response handleGetCoupon(String couponId) {
        for (Map<String, Object> coupon : coupons) {
            if (couponId.equals(coupon.get("id"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", coupon);
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
            }
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "쿠폰을 찾을 수 없습니다: " + couponId);
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error));
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
                    return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("valid", false);
            error.put("message", "존재하지 않는 쿠폰입니다");
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(error));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "요청 처리 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
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

            return newFixedLengthResponse(Response.Status.CREATED, "application/json", gson.toJson(result));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰 생성 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
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
                    return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰을 찾을 수 없습니다: " + couponId);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "쿠폰 업데이트 중 오류가 발생했습니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
        }
    }

    private Response handleServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("uptime", System.currentTimeMillis());
        status.put("couponsCount", coupons.size());
        status.put("memory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(status));
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
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            Map<String, Object> request = gson.fromJson(postData, Map.class);
            String userId = (String) request.get("userId");
            String password = (String) request.get("password");

            Log.i(TAG, "Login attempt for user: " + userId);

            String savedUserId = sharedPreferences.getString("api_user_id", "admin");
            String savedPassword = sharedPreferences.getString("api_password", "admin123");
            
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
                
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
            } else {
                Log.w(TAG, "Login failed for user: " + userId + " (invalid credentials)");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "아이디 또는 패스워드가 올바르지 않습니다");
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", gson.toJson(error));
            }
        } catch (Exception e) {
            Log.e(TAG, "Login processing error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
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
                "application/json", gson.toJson(error));
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
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
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
                            try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
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
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
        } catch (Exception e) {
            Log.e(TAG, "Error getting corporates", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 조회 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처를 찾을 수 없습니다: " + corporateId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error));
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error getting corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 조회 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            // JSON 형식 검증
            if (!postData.trim().startsWith("{")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "잘못된 JSON 형식입니다: " + postData.substring(0, Math.min(50, postData.length())) + "...");
                Log.w(TAG, "Invalid JSON format in create request: " + postData);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            // 사업자등록번호 중복 체크
            if (corporate.getBusinessNumber() != null && !corporate.getBusinessNumber().trim().isEmpty()) {
                if (corporateDAO.isBusinessNumberExists(corporate.getBusinessNumber())) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "이미 등록된 사업자등록번호입니다");
                    return newFixedLengthResponse(Response.Status.CONFLICT, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.CREATED, "application/json", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처 생성에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 생성 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            // JSON 형식 검증
            if (!postData.trim().startsWith("{")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "잘못된 JSON 형식입니다: " + postData.substring(0, Math.min(50, postData.length())) + "...");
                Log.w(TAG, "Invalid JSON format in update request: " + postData);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            Corporate existing = corporateDAO.getCorporateById(id);
            if (existing == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처를 찾을 수 없습니다: " + corporateId);
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            int rowsAffected = corporateDAO.updateCorporate(existing);
            
            if (rowsAffected > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", existing);
                result.put("message", "거래처가 성공적으로 업데이트되었습니다");
                
                Log.i(TAG, "Corporate updated: " + existing.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처 업데이트에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error updating corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error));
            }
            
            int rowsAffected = corporateDAO.deleteCorporate(id);
            
            if (rowsAffected > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "거래처가 성공적으로 삭제되었습니다");
                result.put("deletedCorporate", existing);
                
                Log.i(TAG, "Corporate deleted: " + existing.getName() + " (ID: " + id + ")");
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "거래처 삭제에 실패했습니다");
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "잘못된 거래처 ID 형식입니다");
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
        } catch (Exception e) {
            Log.e(TAG, "Error deleting corporate", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
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
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(error));
            }
            
            List<Corporate> corporates = corporateDAO.searchCorporatesByName(name);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", corporates);
            result.put("count", corporates.size());
            result.put("searchTerm", name);
            
            Log.i(TAG, "Corporate search for '" + name + "' returned " + corporates.size() + " results");
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(result));
        } catch (Exception e) {
            Log.e(TAG, "Error searching corporates", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "거래처 검색 중 오류가 발생했습니다: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", gson.toJson(error));
        }
    }
}