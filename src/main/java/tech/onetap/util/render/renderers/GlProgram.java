package tech.onetap.util.render.renderers;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderLoader.LoadException;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import tech.onetap.util.IMinecraft;
import tech.onetap.mixin.ShaderProgramAccessor;

import java.util.ArrayList;
import java.util.List;

public class GlProgram implements IMinecraft {
   private static final List<Runnable> REGISTERED_PROGRAMS = new ArrayList<>();
   protected ShaderProgram backingProgram;
   protected ShaderProgramKey programKey;

   public GlProgram(Identifier id, VertexFormat vertexFormat) {
      this.programKey = new ShaderProgramKey(id, vertexFormat, net.minecraft.client.gl.Defines.EMPTY);
      REGISTERED_PROGRAMS.add(() -> {
         try {
            ShaderLoader loader = mc.getShaderLoader();
            if (loader != null) {
               this.backingProgram = loader.getProgramToLoad(this.programKey);
            } else {
               this.backingProgram = null;
            }
            this.setup();
         } catch (LoadException var2) {
            System.err.println("[onetap] Failed to load shader: " + id);
         }
      });
   }

   public RenderPhase renderPhaseProgram() {
      return new net.minecraft.client.render.RenderPhase.ShaderProgram(this.programKey);
   }

   public ShaderProgram use() {
      if (this.backingProgram == null) {
         return null;
      }
      return RenderSystem.setShader(this.programKey);
   }

   protected void setup() {
   }

   public ShaderProgram getBackingProgram() {
      return this.backingProgram;
   }

   public GlUniform findUniform(String name) {
      if (this.backingProgram == null) {
         return null;
      }
      return (GlUniform)((ShaderProgramAccessor)this.backingProgram).getUniformsByName().get(name);
   }

   public static void loadAndSetupPrograms() {
      REGISTERED_PROGRAMS.forEach(Runnable::run);
   }
}
