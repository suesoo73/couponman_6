package com.example.couponman_6;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {
    private static final String TAG = "QRCodeGenerator";
    
    // QR 코드 설정
    private static final int QR_SIZE = 512; // 512x512 픽셀
    private static final int QR_COLOR_BLACK = Color.BLACK;
    private static final int QR_COLOR_WHITE = Color.WHITE;
    private static final Bitmap.CompressFormat IMAGE_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int JPEG_QUALITY = 90;

    /**
     * 쿠폰 코드를 QR 코드 이미지로 생성
     *
     * @param context      컨텍스트
     * @param couponCode   쿠폰 코드
     * @param fileName     저장할 파일명 (확장자 제외)
     * @return 생성된 이미지 파일 객체, 실패 시 null
     */
    public static File generateQRCodeImage(Context context, String couponCode, String fileName) {
        Log.i(TAG, "[QR-GEN] QR 코드 생성 시작 - 쿠폰코드: " + couponCode + ", 파일명: " + fileName);
        
        if (couponCode == null || couponCode.trim().isEmpty()) {
            Log.e(TAG, "[QR-GEN] 쿠폰 코드가 비어있음");
            return null;
        }

        try {
            // QR 코드 생성 설정
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // 여백 최소화

            // QR 코드 매트릭스 생성
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(couponCode, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            
            Log.i(TAG, "[QR-GEN] QR 코드 매트릭스 생성 완료 - 크기: " + QR_SIZE + "x" + QR_SIZE);

            // 비트맵 생성
            Bitmap bitmap = createBitmapFromMatrix(bitMatrix);
            
            if (bitmap == null) {
                Log.e(TAG, "[QR-GEN] 비트맵 생성 실패");
                return null;
            }

            // 파일로 저장
            File qrImageFile = saveQRCodeToFile(context, bitmap, fileName);
            
            // 비트맵 메모리 해제
            bitmap.recycle();
            
            if (qrImageFile != null) {
                Log.i(TAG, "[QR-GEN] QR 코드 이미지 생성 성공 - 파일: " + qrImageFile.getAbsolutePath());
                Log.i(TAG, "[QR-GEN] 파일 크기: " + qrImageFile.length() + " bytes");
            } else {
                Log.e(TAG, "[QR-GEN] QR 코드 이미지 파일 저장 실패");
            }
            
            return qrImageFile;

        } catch (WriterException e) {
            Log.e(TAG, "[QR-GEN] QR 코드 생성 중 WriterException", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "[QR-GEN] QR 코드 생성 중 예외 발생", e);
            return null;
        }
    }

    /**
     * BitMatrix를 흑백 비트맵으로 변환
     */
    private static Bitmap createBitmapFromMatrix(BitMatrix bitMatrix) {
        try {
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            
            // 배경을 흰색으로 칠하기
            canvas.drawColor(QR_COLOR_WHITE);
            
            Paint paint = new Paint();
            paint.setColor(QR_COLOR_BLACK);
            paint.setStyle(Paint.Style.FILL);
            
            // QR 코드 픽셀 그리기
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y)) {
                        canvas.drawPoint(x, y, paint);
                    }
                }
            }
            
            Log.i(TAG, "[QR-GEN] 비트맵 생성 완료 - " + width + "x" + height + " 픽셀");
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "[QR-GEN] 비트맵 생성 중 오류", e);
            return null;
        }
    }

    /**
     * QR 코드 비트맵을 파일로 저장
     */
    private static File saveQRCodeToFile(Context context, Bitmap bitmap, String fileName) {
        FileOutputStream outputStream = null;
        
        try {
            // 임시 디렉토리에 파일 생성
            File cacheDir = context.getCacheDir();
            File qrImageFile = new File(cacheDir, fileName + ".jpg");
            
            // 기존 파일이 있으면 삭제
            if (qrImageFile.exists()) {
                qrImageFile.delete();
                Log.d(TAG, "[QR-GEN] 기존 파일 삭제: " + qrImageFile.getAbsolutePath());
            }

            outputStream = new FileOutputStream(qrImageFile);
            
            // JPEG로 압축하여 저장
            boolean saved = bitmap.compress(IMAGE_FORMAT, JPEG_QUALITY, outputStream);
            
            if (!saved) {
                Log.e(TAG, "[QR-GEN] 비트맵 압축 실패");
                return null;
            }
            
            outputStream.flush();
            Log.i(TAG, "[QR-GEN] QR 코드 이미지 파일 저장 완료");
            
            return qrImageFile;
            
        } catch (IOException e) {
            Log.e(TAG, "[QR-GEN] 파일 저장 중 IOException", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "[QR-GEN] 파일 저장 중 예외", e);
            return null;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "[QR-GEN] 출력 스트림 닫기 실패", e);
                }
            }
        }
    }

    /**
     * QR 코드 이미지 파일 삭제
     */
    public static boolean deleteQRCodeImage(File qrImageFile) {
        if (qrImageFile == null || !qrImageFile.exists()) {
            Log.w(TAG, "[QR-GEN] 삭제할 파일이 존재하지 않음");
            return true; // 이미 없으므로 성공으로 처리
        }
        
        try {
            boolean deleted = qrImageFile.delete();
            if (deleted) {
                Log.i(TAG, "[QR-GEN] QR 코드 이미지 파일 삭제 완료: " + qrImageFile.getAbsolutePath());
            } else {
                Log.w(TAG, "[QR-GEN] QR 코드 이미지 파일 삭제 실패: " + qrImageFile.getAbsolutePath());
            }
            return deleted;
        } catch (Exception e) {
            Log.e(TAG, "[QR-GEN] QR 코드 이미지 파일 삭제 중 예외", e);
            return false;
        }
    }

    /**
     * 쿠폰 코드의 유효성 검증
     */
    public static boolean isValidCouponCode(String couponCode) {
        return couponCode != null && 
               !couponCode.trim().isEmpty() && 
               couponCode.length() >= 10; // 최소 길이 체크
    }
}