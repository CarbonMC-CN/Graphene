package net.carbonmc.graphene.optimization.modelable;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.*;

public final class ItemFix {

    public enum PixelDirection {
        A(Direction.WEST, -1, 0),
        B(Direction.EAST, 1, 0),
        C(Direction.UP, 0, -1),
        D(Direction.DOWN, 0, 1);

        public static final PixelDirection[] E = values();

        private final Direction F;
        private final int G;
        private final int H;

        PixelDirection(Direction i, int j, int k) {
            this.F = i;
            this.G = j;
            this.H = k;
        }

        public Direction getDirection() { return F; }
        public int getOffsetX() { return G; }
        public int getOffsetY() { return H; }
        public boolean isVertical() { return this == C || this == D; }
    }

    public static void a(List<BlockElement> l, float m) {
        for (BlockElement n : l) {
            for (BlockElementFace o : n.faces.values()) {
                b(o.uv.uvs, m);
            }
        }
    }

    public static List<BlockElement> b(int p, String q, TextureAtlasSprite r) {
        List<BlockElement> s = new ArrayList<>();

        int t = r.contents().width();
        int u = r.contents().height();
        float v = t / 16.0F;
        float w = u / 16.0F;
        float x = r.uvShrinkRatio();
        int[] y = c(r);

        EnumMap<Direction, BlockElementFace> z = new EnumMap<>(Direction.class);
        z.put(Direction.SOUTH, new BlockElementFace(null, p, q, d(new float[]{0, 0, 16, 16}, 0, x)));
        z.put(Direction.NORTH, new BlockElementFace(null, p, q, d(new float[]{16, 0, 0, 16}, 0, x)));
        s.add(new BlockElement(new Vector3f(0, 0, 7.5f), new Vector3f(16, 16, 8.5f), z, null, true));

        int aa = -1, ab = -1, ac = -1, ad = -1;
        for (int ae = 0; ae < u; ae++) {
            for (int af = 0; af < t; af++) {
                if (!e(r, y, af, ae)) {
                    if (f(r, y, af, ae, PixelDirection.D)) {
                        if (aa == -1) aa = af;
                        ab = af;
                    }
                    if (f(r, y, af, ae, PixelDirection.C)) {
                        if (ac == -1) ac = af;
                        ad = af;
                    }
                } else {
                    if (aa != -1) {
                        s.add(g(Direction.DOWN, p, q, aa, ab, ae, u, x, v, w));
                        aa = -1;
                    }
                    if (ac != -1) {
                        s.add(g(Direction.UP, p, q, ac, ad, ae, u, x, v, w));
                        ac = -1;
                    }
                }
            }
            if (aa != -1) {
                s.add(g(Direction.DOWN, p, q, aa, ab, ae, u, x, v, w));
                aa = -1;
            }
            if (ac != -1) {
                s.add(g(Direction.UP, p, q, ac, ad, ae, u, x, v, w));
                ac = -1;
            }
        }

        aa = -1; ab = -1; ac = -1; ad = -1;
        for (int af = 0; af < t; af++) {
            for (int ae = 0; ae < u; ae++) {
                if (!e(r, y, af, ae)) {
                    if (f(r, y, af, ae, PixelDirection.B)) {
                        if (aa == -1) aa = ae;
                        ab = ae;
                    }
                    if (f(r, y, af, ae, PixelDirection.A)) {
                        if (ac == -1) ac = ae;
                        ad = ae;
                    }
                } else {
                    if (aa != -1) {
                        s.add(h(Direction.EAST, p, q, aa, ab, af, u, x, v, w));
                        aa = -1;
                    }
                    if (ac != -1) {
                        s.add(h(Direction.WEST, p, q, ac, ad, af, u, x, v, w));
                        ac = -1;
                    }
                }
            }
            if (aa != -1) {
                s.add(h(Direction.EAST, p, q, aa, ab, af, u, x, v, w));
                aa = -1;
            }
            if (ac != -1) {
                s.add(h(Direction.WEST, p, q, ac, ad, af, u, x, v, w));
                ac = -1;
            }
        }
        return s;
    }

    public static List<BlockElement> i(int j, String k, TextureAtlasSprite l) {
        Map<Direction, BlockElementFace> m = new EnumMap<>(Direction.class);
        BlockFaceUV n = new BlockFaceUV(new float[]{0, 0, 16, 16}, 0);
        m.put(Direction.SOUTH, new BlockElementFace(null, j, k, n));
        m.put(Direction.NORTH, new BlockElementFace(null, j, k, new BlockFaceUV(new float[]{16, 0, 0, 16}, 0)));
        return List.of(new BlockElement(
                new Vector3f(0, 0, 7.5f),
                new Vector3f(16, 16, 8.5f),
                m, null, true));
    }

    private static BlockElement g(Direction o, int p, String q, int r, int s, int t, int u, float v, float w, float x) {
        EnumMap<Direction, BlockElementFace> y = new EnumMap<>(Direction.class);
        y.put(o, new BlockElementFace(null, p, q,
                d(new float[]{r / w, t / x, (s + 1) / w, (t + 1) / x}, 0, v)));
        return new BlockElement(
                new Vector3f(r / w, (u - t - 1) / x, 7.5f),
                new Vector3f((s + 1) / w, (u - t) / x, 8.5f),
                y, null, true);
    }

    private static BlockElement h(Direction o, int p, String q, int r, int s, int t, int u, float v, float w, float x) {
        EnumMap<Direction, BlockElementFace> y = new EnumMap<>(Direction.class);
        y.put(o, new BlockElementFace(null, p, q,
                d(new float[]{(t + 1) / w, r / x, t / w, (s + 1) / x}, 0, v)));
        return new BlockElement(
                new Vector3f(t / w, (u - s - 1) / x, 7.5f),
                new Vector3f((t + 1) / w, (u - r) / x, 8.5f),
                y, null, true);
    }

    private static BlockFaceUV d(float[] j, int k, float l) {
        return new BlockFaceUV(b(j.clone(), l), k);
    }

    public static List<BlockElement> j(int tint, String textureName, TextureAtlasSprite sprite) {
        List<BlockElement> elements = new ArrayList<>();
        if (sprite == null) return elements;

        int textureWidth = sprite.contents().width();
        int textureHeight = sprite.contents().height();

        // 安全检查：跳过无效纹理
        if (textureWidth <= 0 || textureHeight <= 0 || textureWidth > 256 || textureHeight > 256) {
            return elements;
        }

        // 只处理单帧纹理
        int[] frames = SpriteHelper.getUniqueFrames(sprite);
        if (frames.length > 1) {
            return elements;
        }

        // 使用第一帧（对于物品纹理通常只有一帧）
        int frameIndex = frames.length > 0 ? frames[0] : 0;

        float pixelWidth = 16.0f / textureWidth;
        float pixelHeight = 16.0f / textureHeight;

        for (int y = 0; y < textureHeight; y++) {
            for (int x = 0; x < textureWidth; x++) {
                // 修复：正确传递参数顺序
                if (isPixelTransparent(sprite, frameIndex, x, y)) {
                    continue;
                }

                EnumMap<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);

                // 计算UV坐标（基于0-16范围）
                float u0 = (float) x / textureWidth * 16.0f;
                float v0 = (float) y / textureHeight * 16.0f;
                float u1 = (float) (x + 1) / textureWidth * 16.0f;
                float v1 = (float) (y + 1) / textureHeight * 16.0f;

                BlockElementFace southFace = new BlockElementFace(
                        null, tint, textureName,
                        new BlockFaceUV(new float[]{u0, v0, u1, v1}, 0)
                );
                faces.put(Direction.SOUTH, southFace);

                BlockElementFace northFace = new BlockElementFace(
                        null, tint, textureName,
                        new BlockFaceUV(new float[]{u1, v0, u0, v1}, 0)
                );
                faces.put(Direction.NORTH, northFace);

                // 添加侧面（修复参数顺序）
                for (PixelDirection dir : PixelDirection.values()) {
                    if (shouldAddSideFace(sprite, frameIndex, x, y, dir)) {
                        faces.put(dir.getDirection(), dir.isVertical() ? southFace : northFace);
                    }
                }

                // 计算方块坐标
                float x0 = x * pixelWidth;
                float y0 = (textureHeight - y - 1) * pixelHeight; // Y轴翻转
                float x1 = (x + 1) * pixelWidth;
                float y1 = (textureHeight - y) * pixelHeight;

                BlockElement element = new BlockElement(
                        new Vector3f(x0, y0, 7.5f),
                        new Vector3f(x1, y1, 8.5f),
                        faces, null, true
                );

                elements.add(element);
            }
            if (sprite == null || sprite.contents().width() <= 0 || sprite.contents().height() <= 0) {
                return Collections.emptyList();
            }

            // 对于非16x16纹理，使用简化的全像素方法
            if (sprite.contents().width() != 16 || sprite.contents().height() != 16) {
                return generateSimplePixelModel(tint, textureName, sprite);
            }

            return generateSimplePixelModel(tint, textureName, sprite);
        }

        return elements;
    }

    // 修复的辅助方法
    private static boolean isPixelTransparent(TextureAtlasSprite sprite, int frameIndex, int x, int y) {
        return SpriteHelper.isTransparent(sprite, frameIndex, x, y);
    }

    private static boolean shouldAddSideFace(TextureAtlasSprite sprite, int frameIndex, int x, int y, PixelDirection direction) {
        int adjacentX = x + direction.getOffsetX();
        int adjacentY = y + direction.getOffsetY();

        // 如果相邻像素在纹理外，需要侧面
        if (adjacentX < 0 || adjacentY < 0 ||
                adjacentX >= sprite.contents().width() ||
                adjacentY >= sprite.contents().height()) {
            return true;
        }

        // 如果相邻像素透明，需要侧面
        return SpriteHelper.isTransparent(sprite, frameIndex, adjacentX, adjacentY);
    }

    private static boolean k(TextureAtlasSprite m, int n, int o) {
        return n < 0 || o < 0 || n >= m.contents().width() || o >= m.contents().height();
    }

    private static boolean l(TextureAtlasSprite m, int n, int o, int p) {
        return k(m, o, p) || SpriteHelper.isTransparent(m, n, o, p);
    }

    private static boolean e(TextureAtlasSprite m, int[] n, int o, int p) {
        for (int q : n) if (!l(m, q, o, p)) return false;
        return true;
    }

    private static boolean f(TextureAtlasSprite m, int[] n, int o, int p, PixelDirection q) {
        int r = o + q.getOffsetX(), s = p + q.getOffsetY();
        if (k(m, r, s)) return true;
        for (int t : n)
            if (!l(m, t, o, p) && l(m, t, r, s))
                return true;
        return false;
    }

    private static float m(float n, float o, float p) {
        return (o - n * p) / (1 - n);
    }

    private static float[] b(float[] n, float o) {
        float p = (n[0] + n[2]) / 2.0F, q = (n[1] + n[3]) / 2.0F;
        n[0] = m(o, n[0], p);
        n[2] = m(o, n[2], p);
        n[1] = m(o, n[1], q);
        n[3] = m(o, n[3], q);
        return n;
    }

    private static int[] c(TextureAtlasSprite r) {
        return SpriteHelper.getUniqueFrames(r);
    }
    public static boolean yun(TextureAtlasSprite sp) {
        if (sp == null) return false;
        if (sp.contents().name().toString().equals("missingno")) return false;
        int w = sp.contents().width();
        int h = sp.contents().height();
        if (w <= 0 || h <= 0) return false;
        int[] frames = SpriteHelper.getUniqueFrames(sp);
        if (frames == null || frames.length > 1) return false;
        return true;
    }
    public static boolean iseeyou(TextureAtlasSprite sp) {
        if (sp == null) return false;
        if (sp.contents().name().toString().equals("missingno")) return false;
        int w = sp.contents().width();
        int h = sp.contents().height();
        return w > 0 && h > 0;
    }
    // 修复透明度检测方法
    private static boolean isPixelTransparent(TextureAtlasSprite sprite, int[] frames, int x, int y) {
        if (frames.length == 0) return true;

        // 只检查第一帧（对于物品纹理通常只有一帧）
        int frame = frames[0];
        return SpriteHelper.isTransparent(sprite, frame, x, y);
    }

    // 修复侧面检测方法
    private static boolean shouldAddSideFace(TextureAtlasSprite sprite, int[] frames, int x, int y, PixelDirection direction) {
        int adjacentX = x + direction.getOffsetX();
        int adjacentY = y + direction.getOffsetY();

        // 如果相邻像素在纹理外，需要侧面
        if (adjacentX < 0 || adjacentY < 0 ||
                adjacentX >= sprite.contents().width() ||
                adjacentY >= sprite.contents().height()) {
            return true;
        }

        // 如果相邻像素透明，需要侧面
        return isPixelTransparent(sprite, frames, adjacentX, adjacentY);
    }
    private static List<BlockElement> generateSimplePixelModel(int tint, String textureName, TextureAtlasSprite sprite) {
        List<BlockElement> elements = new ArrayList<>();
        int w = sprite.contents().width();
        int h = sprite.contents().height();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 只检查第一帧
                if (SpriteHelper.isTransparent(sprite, 0, x, y)) {
                    continue;
                }

                EnumMap<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);

                // 简化的UV计算
                float u0 = 16.0f * x / w;
                float v0 = 16.0f * y / h;
                float u1 = 16.0f * (x + 1) / w;
                float v1 = 16.0f * (y + 1) / h;

                BlockElementFace southFace = new BlockElementFace(
                        null, tint, textureName,
                        new BlockFaceUV(new float[]{u0, v0, u1, v1}, 0)
                );
                faces.put(Direction.SOUTH, southFace);

                BlockElementFace northFace = new BlockElementFace(
                        null, tint, textureName,
                        new BlockFaceUV(new float[]{u1, v0, u0, v1}, 0)
                );
                faces.put(Direction.NORTH, northFace);

                // 不生成侧面，简化模型

                float x0 = 16.0f * x / w;
                float y0 = 16.0f * (h - y - 1) / h;
                float x1 = 16.0f * (x + 1) / w;
                float y1 = 16.0f * (h - y) / h;

                elements.add(new BlockElement(
                        new Vector3f(x0, y0, 7.5f),
                        new Vector3f(x1, y1, 8.5f),
                        faces, null, true
                ));
            }
        }

        return elements;
    }
}