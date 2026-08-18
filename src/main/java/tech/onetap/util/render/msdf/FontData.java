package tech.onetap.util.render.msdf;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class FontData {

	@SerializedName("atlas")
	private AtlasData atlas;
	@SerializedName("metrics")
	private MetricsData metrics;
	@SerializedName("glyphs")
	private List<GlyphData> glyphs;
	@SerializedName("kerning")
	private List<KerningData> kernings;

	@SerializedName("atlas")
	public AtlasData atlas() {
		return this.atlas;
	}

	@SerializedName("metrics")
	public MetricsData metrics() {
		return this.metrics;
	}

	@SerializedName("glyphs")
	public List<GlyphData> glyphs() {
		return this.glyphs;
	}

	@SerializedName("kerning")
	public List<KerningData> kernings() {
		return this.kernings;
	}

	public static final class AtlasData {

		@SerializedName("distanceRange")
		private float range;
		@SerializedName("width")
		private float width;
		@SerializedName("height")
		private float height;

		@SerializedName("distanceRange")
		public float range() {
			return this.range;
		}

		@SerializedName("width")
		public float width() {
			return this.width;
		}

		@SerializedName("height")
		public float height() {
			return this.height;
		}
	}

	public static final class MetricsData {

		@SerializedName("lineHeight")
		private float lineHeight;
		@SerializedName("ascender")
		private float ascender;
		@SerializedName("descender")
		private float descender;

		@SerializedName("lineHeight")
		public float lineHeight() {
			return this.lineHeight;
		}

		@SerializedName("ascender")
		public float ascender() {
			return this.ascender;
		}

		@SerializedName("descender")
		public float descender() {
			return this.descender;
		}

		public float baselineHeight() {
			return this.lineHeight + this.descender;
		}
	}

	public static final class GlyphData {

		@SerializedName("unicode")
		private int unicode;
		@SerializedName("advance")
		private float advance;
		@SerializedName("planeBounds")
		private BoundsData planeBounds;
		@SerializedName("atlasBounds")
		private BoundsData atlasBounds;

		@SerializedName("unicode")
		public int unicode() {
			return this.unicode;
		}

		@SerializedName("advance")
		public float advance() {
			return this.advance;
		}

		@SerializedName("planeBounds")
		public BoundsData planeBounds() {
			return this.planeBounds;
		}

		@SerializedName("atlasBounds")
		public BoundsData atlasBounds() {
			return this.atlasBounds;
		}
	}

	public static final class BoundsData {

		@SerializedName("left")
		private float left;
		@SerializedName("top")
		private float top;
		@SerializedName("right")
		private float right;
		@SerializedName("bottom")
		private float bottom;

		@SerializedName("left")
		public float left() {
			return this.left;
		}

		@SerializedName("top")
		public float top() {
			return this.top;
		}

		@SerializedName("right")
		public float right() {
			return this.right;
		}

		@SerializedName("bottom")
		public float bottom() {
			return this.bottom;
		}
	}

	public static final class KerningData {

		@SerializedName("unicode1")
		private int leftChar;
		@SerializedName("unicode2")
		private int rightChar;
		@SerializedName("advance")
		private float advance;

		@SerializedName("unicode1")
		public int leftChar() {
			return this.leftChar;
		}

		@SerializedName("unicode2")
		public int rightChar() {
			return this.rightChar;
		}

		@SerializedName("advance")
		public float advance() {
			return this.advance;
		}
	}
}