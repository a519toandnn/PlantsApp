package com.midterm.plantsapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View {
    private Paint wavePaint;
    private Path wavePath;
    private int percentage = 0;  // Phần trăm độ ẩm
    private float waveOffset = 0;  // Vị trí hiện tại của sóng

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        wavePaint = new Paint();
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));  // Vẽ sóng chỉ bên trong hình tròn
        wavePath = new Path();

        // Tạo một Runnable để làm sóng di chuyển
        postDelayed(waveRunnable, 16);  // Refresh mỗi 16ms để tạo hiệu ứng mượt
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
        invalidate();  // Vẽ lại khi cập nhật phần trăm
    }

    // Runnable để cập nhật vị trí của sóng
    private final Runnable waveRunnable = new Runnable() {
        @Override
        public void run() {
            waveOffset += 10;  // Di chuyển sóng sang phải
            if (waveOffset > getWidth()) {
                waveOffset = 0;  // Reset vị trí khi sóng đi hết màn hình
            }
            invalidate();  // Vẽ lại
            postDelayed(this, 16);  // Lặp lại sau 16ms
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float height = getHeight();
        float width = getWidth();
        float waveHeight = height * (1 - percentage / 100f);  // Chiều cao sóng dựa vào phần trăm

        // Cập nhật màu sóng dựa trên giá trị phần trăm
        if (percentage > 90) {
            wavePaint.setColor(0xFFFF0000); // Màu đỏ
        }
        else if(percentage > 70){
            wavePaint.setColor(0xFFFFD700); // Màu vàng
        }
        else if(percentage >= 60){
            wavePaint.setColor(0xFF15EA71); // Màu xanh
        } else if (percentage > 20) {
            wavePaint.setColor(0xFFFFD700); // Màu vàng
        } else {
            wavePaint.setColor(0xFFFF0000); // Màu đỏ
        }

        // Tạo một hình tròn để cắt sóng
        float radius = Math.min(width, height) / 2;  // Bán kính của hình tròn
        float centerX = width / 2;
        float centerY = height / 2;

        // Tạo path cho hình tròn
        Path circlePath = new Path();
        circlePath.addCircle(centerX, centerY, radius, Path.Direction.CCW);

        canvas.save();
        canvas.clipPath(circlePath);  // Chỉ cho phép vẽ trong khu vực của hình tròn

        // Vẽ sóng
        wavePath.reset();
        wavePath.moveTo(-width + waveOffset, waveHeight);  // Bắt đầu sóng từ trái ngoài màn hình
        for (int i = -1; i <= 1; i++) {
            wavePath.quadTo(
                    (i * width) + width / 4 + waveOffset, waveHeight - 30,
                    (i * width) + width / 2 + waveOffset, waveHeight);
            wavePath.quadTo(
                    (i * width) + 3 * width / 4 + waveOffset, waveHeight + 30,
                    (i * width) + width + waveOffset, waveHeight);
        }
        wavePath.lineTo(width, height);
        wavePath.lineTo(0, height);
        wavePath.close();

        // Vẽ sóng với chế độ clip
        canvas.drawPath(wavePath, wavePaint);
        canvas.restore();
    }
}
