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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tfar.darkness.LightmapAccess;
import tfar.darkness.TextureAccess;

@Mixin(LightTexture.class)
public class MixinLightmapTextureManager implements LightmapAccess {

	@Final
	@Shadow
	private DynamicTexture dynamicTexture;
	@Shadow
	private float torchFlicker;
	@Shadow
	private boolean needsUpdate;

	@Inject(method = "<init>*", at = @At(value = "RETURN"))
	private void afterInit(GameRenderer gameRenderer, Minecraft minecraftClient, CallbackInfo ci) {
		((TextureAccess) dynamicTexture).darkness_enableUploadHook();
	}

	@Override
	public float darkness_prevFlicker() {
		return torchFlicker;
	}

	@Override
	public boolean darkness_isDirty() {
		return needsUpdate;
	}
}
