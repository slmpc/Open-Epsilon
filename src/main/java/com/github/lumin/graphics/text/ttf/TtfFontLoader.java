package com.github.lumin.graphics.text.ttf;

import com.github.lumin.graphics.text.GlyphDescriptor;
import com.github.lumin.graphics.text.IFontLoader;
import net.minecraft.resources.Identifier;
import org.lwjgl.stb.STBTruetype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TtfFontLoader implements IFontLoader {

    public final TtfFontFile fontFile;

    private final HashMap<Character, GlyphDescriptor> glyphMap = new HashMap<>();
    private final List<TtfGlyphAtlas> atlases = new ArrayList<>();

    private TtfGlyphAtlas currentAtlas;
    private int atlasId = 0;

    public TtfFontLoader(Identifier ttfFile) {
        this.fontFile = new TtfFontFile(ttfFile, 64, 6);
    }

    @Override
    public void checkAndLoadChar(char ch) {
        if (glyphMap.containsKey(ch)) return;

        TtfGlyph glyph = fontFile.generateGlyph(ch);
        if (glyph.glyphData() == null) return;

        if (currentAtlas == null) {
            createNewAtlas();
        }

        TtfGlyphAtlas.GlyphUV uv = currentAtlas.appendGlyph(glyph);

        if (uv == null) {
            createNewAtlas();
            uv = currentAtlas.appendGlyph(glyph);
        }

        if (uv != null) {
            glyphMap.put(ch, new GlyphDescriptor(
                    currentAtlas, uv,
                    glyph.width(), glyph.height(),
                    glyph.xOffset(), glyph.yOffset(),
                    glyph.advance()
            ));
        }

        STBTruetype.stbtt_FreeSDF(glyph.glyphData());
    }

    private void createNewAtlas() {
        currentAtlas = new TtfGlyphAtlas(atlasId);
        atlases.add(currentAtlas);
        atlasId++;
    }

    @Override
    public void checkAndLoadChars(String chars) {
        for (final var ch : chars.toCharArray()) {
            checkAndLoadChar(ch);
        }
    }


    @Override
    public void destroy() {
        fontFile.destroy();
        for (TtfGlyphAtlas atlas : atlases) {
            atlas.destroy();
        }
        atlases.clear();
        glyphMap.clear();
    }

    @Override
    public GlyphDescriptor getGlyph(char ch) {
        return glyphMap.get(ch);
    }
}
