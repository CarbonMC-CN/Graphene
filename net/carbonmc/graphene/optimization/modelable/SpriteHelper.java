package net.carbonmc.graphene.optimization.modelable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
public final class SpriteHelper {
    public static int getFrameCount(TextureAtlasSprite sprite) {
        for (int i = 1; i < 64; i++) {
            try {
                sprite.getPixelRGBA(i, 0, 0);
            } catch (IndexOutOfBoundsException e) {
                return i;
            }
        }
        return 1;
    }
    public static int[] getUniqueFrames(TextureAtlasSprite sprite) {
        int n = getFrameCount(sprite);
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i;
        return arr;
    }
    public static boolean isTransparent(TextureAtlasSprite sprite, int frameIndex, int x, int y) {
        int w = sprite.contents().width();
        int h = sprite.contents().height();
        if (x < 0 || y < 0 || x >= w || y >= h) return true;

        int rgba = sprite.getPixelRGBA(frameIndex, x, y);
        return (rgba >>> 24) == 0;
    }
}