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
package tfar.darkness;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.potion.Effects;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Darkness.MODID)
public class Darkness {
	public static Logger LOG = LogManager.getLogger("Darkness");

	public static final String MODID = "totaldarkness";

	public Darkness() {
		if (FMLEnvironment.dist.isClient()) {
			ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigChange);
			ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
		}
	}


	private void onConfigChange(ModConfig.ModConfigEvent e) {
		bake();
	}

	public static final Config CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	static {
		final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
		CLIENT_SPEC = specPair.getRight();
		CLIENT = specPair.getLeft();
	}

	public static void bake() {
		Config.darkNetherFogEffective = Config.darkNether.get() ? Config.darkNetherFogConfigured.get() : 1.0;
		Config.darkEndFogEffective = Config.darkEnd.get() ? Config.darkEndFogConfigured.get() : 1.0;
	}

	public static boolean blockLightOnly() {
		return Config.blockLightOnly.get();
	}

	public static double darkNetherFog() {
		return Config.darkNetherFogEffective;
	}

	public static double darkEndFog() {
		return Config.darkEndFogEffective;
	}

	private static boolean isDark(World world) {
		final RegistryKey<World> dimType = world.getDimensionKey();
		if (dimType == World.OVERWORLD) {
			return Config.darkOverworld.get();
		} else if (dimType == World.THE_NETHER) {
			return Config.darkNether.get();
		} else if (dimType == World.THE_END) {
			return Config.darkEnd.get();
		} else if (world.getDimensionType().hasSkyLight()) {
			return Config.darkDefault.get();
		} else {
			return Config.darkSkyless.get();
		}
	}

	private static float skyFactor(World world) {
		if (!Config.blockLightOnly.get() && isDark(world)) {
			if (world.getDimensionType().hasSkyLight()) {
				final float angle = world.func_242415_f(0);
				if (angle > 0.25f && angle < 0.75f) {
					final float oldWeight = Math.max(0, (Math.abs(angle - 0.5f) - 0.2f)) * 20;
					final float moon = Config.ignoreMoonPhase.get() ? 0 : world.getMoonFactor();
					return MathHelper.lerp(oldWeight * oldWeight * oldWeight, moon * moon, 1f);
				} else {
					return 1;
				}
			} else {
				return 0;
			}
		} else {
			return 1;
		}
	}

	public static boolean enabled = false;
	private static final float[][] LUMINANCE = new float[16][16];

	public static int darken(int c, int blockIndex, int skyIndex) {
		final float lTarget = LUMINANCE[blockIndex][skyIndex];
		final float r = (c & 0xFF) / 255f;
		final float g = ((c >> 8) & 0xFF) / 255f;
		final float b = ((c >> 16) & 0xFF) / 255f;
		final float l = luminance(r, g, b);
		final float f = l > 0 ? Math.min(1, lTarget / l) : 0;

		return f == 1f ? c : 0xFF000000 | Math.round(f * r * 255) | (Math.round(f * g * 255) << 8) | (Math.round(f * b * 255) << 16);
	}

	public static float luminance(float r, float g, float b) {
		return r * 0.2126f + g * 0.7152f + b * 0.0722f;
	}

	public static void updateLuminance(float tickDelta, Minecraft client, GameRenderer worldRenderer, float prevFlicker) {
		final ClientWorld world = client.world;
		if (world != null) {

			if (!isDark(world) || client.player.isPotionActive(Effects.NIGHT_VISION) ||
							(client.player.isPotionActive(Effects.CONDUIT_POWER) && client.player.getWaterBrightness() > 0) || world.getTimeLightningFlash() > 0) {
				enabled = false;
				return;
			} else {
				enabled = true;
			}

			final float dimSkyFactor = Darkness.skyFactor(world);
			final float ambient = world.getSunBrightness(1.0F);
			final DimensionType dim = world.getDimensionType();
			final boolean blockAmbient = !Darkness.isDark(world);

			for (int skyIndex = 0; skyIndex < 16; ++skyIndex) {
				float skyFactor = 1f - skyIndex / 15f;
				skyFactor = 1 - skyFactor * skyFactor * skyFactor * skyFactor;
				skyFactor *= dimSkyFactor;

				float min = Math.max(skyFactor * 0.05f, Config.minimumLight.get().floatValue());
				final float rawAmbient = ambient * skyFactor;
				final float minAmbient = rawAmbient * (1 - min) + min;
				final float skyBase = dim.getAmbientLight(skyIndex) * minAmbient;

				min = Math.max(0.35f * skyFactor, Config.minimumLight.get().floatValue());
				float v = skyBase * (rawAmbient * (1 - min) + min);
				float skyRed = v;
				float skyGreen = v;
				float skyBlue = skyBase;

				if (worldRenderer.getBossColorModifier(tickDelta) > 0.0F) {
					final float skyDarkness = worldRenderer.getBossColorModifier(tickDelta);
					skyRed = skyRed * (1.0F - skyDarkness) + skyRed * 0.7F * skyDarkness;
					skyGreen = skyGreen * (1.0F - skyDarkness) + skyGreen * 0.6F * skyDarkness;
					skyBlue = skyBlue * (1.0F - skyDarkness) + skyBlue * 0.6F * skyDarkness;
				}

				for (int blockIndex = 0; blockIndex < 16; ++blockIndex) {
					float blockFactor = 1f;
					if (!blockAmbient) {
						blockFactor = 1f - blockIndex / 15f;
						blockFactor = 1 - blockFactor * blockFactor * blockFactor * blockFactor;
					}

					final float blockBase = blockFactor * dim.getAmbientLight(blockIndex) * (prevFlicker * 0.1F + 1.5F);
					min = 0.4f * blockFactor;
					final float blockGreen = blockBase * ((blockBase * (1 - min) + min) * (1 - min) + min);
					final float blockBlue = blockBase * (blockBase * blockBase * (1 - min) + min);

					float red = skyRed + blockBase;
					float green = skyGreen + blockGreen;
					float blue = skyBlue + blockBlue;

					final float f = Math.max(skyFactor, blockFactor);
					min = 0.03f * f;
					red = red * (0.99F - min) + min;
					green = green * (0.99F - min) + min;
					blue = blue * (0.99F - min) + min;

					//the end
					if (world.getDimensionKey() == World.THE_END) {
						red = skyFactor * 0.22F + blockBase * 0.75f;
						green = skyFactor * 0.28F + blockGreen * 0.75f;
						blue = skyFactor * 0.25F + blockBlue * 0.75f;
					}

					if (red > 1.0F) {
						red = 1.0F;
					}

					if (green > 1.0F) {
						green = 1.0F;
					}

					if (blue > 1.0F) {
						blue = 1.0F;
					}

					final float gamma = (float) client.gameSettings.gamma * f;
					float invRed = 1.0F - red;
					float invGreen = 1.0F - green;
					float invBlue = 1.0F - blue;
					invRed = 1.0F - invRed * invRed * invRed * invRed;
					invGreen = 1.0F - invGreen * invGreen * invGreen * invGreen;
					invBlue = 1.0F - invBlue * invBlue * invBlue * invBlue;
					red = red * (1.0F - gamma) + invRed * gamma;
					green = green * (1.0F - gamma) + invGreen * gamma;
					blue = blue * (1.0F - gamma) + invBlue * gamma;

					min = Math.max(0.03f * f, Config.minimumLight.get().floatValue());
					red = red * (0.99F - min) + min;
					green = green * (0.99F - min) + min;
					blue = blue * (0.99F - min) + min;

					if (red > 1.0F) {
						red = 1.0F;
					}

					if (green > 1.0F) {
						green = 1.0F;
					}

					if (blue > 1.0F) {
						blue = 1.0F;
					}

					if (red < 0.0F) {
						red = 0.0F;
					}

					if (green < 0.0F) {
						green = 0.0F;
					}

					if (blue < 0.0F) {
						blue = 0.0F;
					}

					LUMINANCE[blockIndex][skyIndex] = Darkness.luminance(red, green, blue);
				}
			}
		}
	}

	public static class Config {

		static double darkNetherFogEffective;
		static ForgeConfigSpec.DoubleValue darkNetherFogConfigured;
		static ForgeConfigSpec.BooleanValue darkEnd;
		static double darkEndFogEffective;
		static ForgeConfigSpec.DoubleValue darkEndFogConfigured;
		static ForgeConfigSpec.BooleanValue darkSkyless;
		static ForgeConfigSpec.BooleanValue blockLightOnly;
		static ForgeConfigSpec.BooleanValue ignoreMoonPhase;
		static ForgeConfigSpec.BooleanValue darkOverworld;
		static ForgeConfigSpec.BooleanValue darkDefault;
		static ForgeConfigSpec.BooleanValue darkNether;
		static ForgeConfigSpec.DoubleValue minimumLight;


		public Config(ForgeConfigSpec.Builder builder) {
			builder.push("general");
			blockLightOnly = builder.define("only_affect_block_light", false);
			ignoreMoonPhase = builder.define("ignore_moon_phase", false);
			darkOverworld = builder.define("dark_overworld", true);
			darkDefault = builder.define("dark_default", true);
			darkNether = builder.define("dark_nether", true);
			darkNetherFogConfigured = builder.defineInRange("dark_nether_fog", .5, 0, 1d);
			darkEnd = builder.define("dark_end", true);
			darkEndFogConfigured = builder.defineInRange("dark_end_fog", 0, 0, 1d);
			darkSkyless = builder.define("dark_skyless", true);
			minimumLight = builder.defineInRange("minimum_light", 0d,0d,1d);
			builder.pop();
		}
	}
}
