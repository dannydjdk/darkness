/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package tfar.darkness.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import tfar.darkness.Darkness;
import tfar.darkness.LightmapAccess;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@Final
	@Shadow
	private Minecraft mc;
	@Final
	@Shadow
	private LightTexture lightmapTexture;

	@Inject(method = "renderWorld", at = @At(value = "HEAD"))
	private void onRenderWorld(float tickDelta, long nanos, MatrixStack matrixStack, CallbackInfo ci) {
		final LightmapAccess lightmap = (LightmapAccess) lightmapTexture;

		if (lightmap.darkness_isDirty()) {
			mc.getProfiler().startSection("lightTex");
			Darkness.updateLuminance(tickDelta, mc, (GameRenderer) (Object) this, lightmap.darkness_prevFlicker());
			mc.getProfiler().endSection();
		}
	}
}
