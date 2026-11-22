package astralis.util.render;

import astralis.util.render.font.RetainedFontRun;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class RetainedTextBatch {
    private final List<RetainedFontRun> fontRuns;
    private boolean uploaded;
    private boolean recording;
    final MsdfTextRenderer this$0;

    public RetainedTextBatch(MsdfTextRenderer textRenderer) {
        Objects.requireNonNull(textRenderer);
        this.this$0 = textRenderer;
        super();
        this.fontRuns = new ArrayList<>();
    }

    public void beginRecording() {
        this.recording = !this.uploaded;
        if (this.recording) {
            this.freeRuns();
        }
    }

    public boolean tryCapture(TextDrawRequest request) {
        if (request.getFont() == null || request.getPaint().getPaintType() != PaintMode.STANDARD) {
            return false;
        }

        if (!this.recording) {
            return true;
        }

        RetainedFontRun run = this.fontRuns.isEmpty() ? null : this.fontRuns.getLast();
        if (run == null || run.font != request.getFont()) {
            run = new RetainedFontRun(request.getFont());
            this.fontRuns.add(run);
        }

        this.this$0.recordText(request, run);
        return true;
    }

    public void finishAndDraw() {
        if (this.recording) {
            this.uploadRuns();
            this.uploaded = true;
            this.recording = false;
        }

        for (RetainedFontRun run : this.fontRuns) {
            this.drawRun(run);
        }
    }

    public void invalidate() {
        this.uploaded = false;
    }

    private void uploadRuns() {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        Iterator runIterator = this.fontRuns.iterator();

        while (runIterator.hasNext()) {
            RetainedFontRun run = (RetainedFontRun) runIterator.next();
            if (run.quadCount != 0) {
                run.vertexData.flip();
                int byteSize = run.vertexData.remaining() * 4;
                GpuDevice gpuDevice = RenderSystem.getDevice();
                Supplier labelSupplier = () -> "Sph Retained MSDF VBO";
                run.vertexBuffer = gpuDevice.createBuffer(labelSupplier, 40, byteSize);
                commandEncoder.writeToBuffer(
                        run.vertexBuffer.slice(0L, byteSize),
                        MemoryUtil.memByteBuffer(MemoryUtil.memAddress(run.vertexData), byteSize));
                MemoryUtil.memFree(run.vertexData);
                run.vertexData = null;
            }
        }
    }

    private void drawRun(RetainedFontRun run) {
        if (run.vertexBuffer != null && run.quadCount != 0) {
            this.this$0.currentFont = run.font;
            this.this$0.currentFontTexture = run.font.getTexture();
            this.this$0.currentFontSampler = run.font.getSampler();
            this.this$0.distanceRange =
                    run.font.getDistanceRange() > 0.0F ? run.font.getDistanceRange() : 4.0F;
            this.this$0.atlasWidth = run.font.getAtlasWidth();
            this.this$0.atlasHeight = run.font.getAtlasHeight();
            this.this$0.currentPaint.copyFrom(MsdfTextRenderer.defaultPaint);
            this.this$0.prepareDraw();
            this.this$0.renderRetained(run.vertexBuffer, run.quadCount);
        }
    }

    private void freeRuns() {
        Iterator runIterator = this.fontRuns.iterator();

        while (runIterator.hasNext()) {
            RetainedFontRun run = (RetainedFontRun) runIterator.next();
            run.release();
        }

        this.fontRuns.clear();
    }

    public void dispose() {
        this.freeRuns();
        this.uploaded = false;
        this.recording = false;
    }
}
